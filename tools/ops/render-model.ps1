# 纯阳巾（道冠）模型预览渲染器
#
# 读取 tools/models/*.model.json（长方体列表），用正交投影渲染成多视角预览图。
# 只用 .NET 内置的 System.Drawing，不引入任何第三方依赖。
# 每个视角自动计算包围盒并缩放居中，模型不会超出面板。
#
# 作者：黑山大叔 / https://space.bilibili.com/19428259 / https://github.com/heiheids

param(
    [string]$Model = 'tools\models\chunyang_jin.model.json',
    [string]$Out = '预览图\纯阳巾_模型预览.png'
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$modelPath = Join-Path $root $Model
$outPath = Join-Path $root $Out
$outDir = Split-Path $outPath -Parent
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }

$def = Get-Content -LiteralPath $modelPath -Raw -Encoding utf8 | ConvertFrom-Json

# 长方体的 8 个顶点：0..3 在 z 小的一面，4..7 在 z 大的一面
function Get-Corners($e) {
    $x1 = [double]$e.from[0]; $y1 = [double]$e.from[1]; $z1 = [double]$e.from[2]
    $x2 = [double]$e.to[0];   $y2 = [double]$e.to[1];   $z2 = [double]$e.to[2]
    return @(
        @($x1, $y1, $z1), @($x2, $y1, $z1), @($x2, $y2, $z1), @($x1, $y2, $z1),
        @($x1, $y1, $z2), @($x2, $y1, $z2), @($x2, $y2, $z2), @($x1, $y2, $z2)
    )
}

$FACES = @(
    @{ idx = @(0, 1, 2, 3); n = @(0, 0, -1) },
    @{ idx = @(5, 4, 7, 6); n = @(0, 0, 1) },
    @{ idx = @(4, 0, 3, 7); n = @(-1, 0, 0) },
    @{ idx = @(1, 5, 6, 2); n = @(1, 0, 0) },
    @{ idx = @(4, 5, 1, 0); n = @(0, -1, 0) },
    @{ idx = @(3, 2, 6, 7); n = @(0, 1, 0) }
)

function Convert-Color($hex) {
    $h = $hex.TrimStart('#')
    return [System.Drawing.Color]::FromArgb(
        255,
        [Convert]::ToInt32($h.Substring(0, 2), 16),
        [Convert]::ToInt32($h.Substring(2, 2), 16),
        [Convert]::ToInt32($h.Substring(4, 2), 16))
}

function Rotate-Point($p, $cosY, $sinY, $cosP, $sinP) {
    $x = $p[0]; $y = $p[1]; $z = $p[2]
    $xr = $x * $cosY - $z * $sinY
    $zr = $x * $sinY + $z * $cosY
    $yr = $y * $cosP - $zr * $sinP
    $zr2 = $y * $sinP + $zr * $cosP
    return @($xr, $yr, $zr2)
}

# 光照：偏前上方的柔和主光，保证暗色面料也能看出体积
$LIGHT = @(-0.35, 0.80, -0.48)
$llen = [Math]::Sqrt(($LIGHT[0] * $LIGHT[0]) + ($LIGHT[1] * $LIGHT[1]) + ($LIGHT[2] * $LIGHT[2]))
$LIGHT = @(($LIGHT[0] / $llen), ($LIGHT[1] / $llen), ($LIGHT[2] / $llen))

$views = @(
    @{ name = '立体视角'; yaw = 35;  pitch = 16 },
    @{ name = '正面';     yaw = 180; pitch = 0 },
    @{ name = '侧面';     yaw = 270; pitch = 0 },
    @{ name = '背面';     yaw = 0;   pitch = 0 }
)

$panelW = 320
$panelH = 660
$W = $panelW * $views.Count
$H = $panelH

$bmp = New-Object System.Drawing.Bitmap($W, $H)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.Clear([System.Drawing.Color]::FromArgb(255, 20, 20, 28))

$labelFont = New-Object System.Drawing.Font('Microsoft YaHei', 13, [System.Drawing.FontStyle]::Regular)
$labelBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 220, 220, 230))
$edgePen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(130, 8, 8, 14), 1.0)
$divPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(60, 120, 120, 140), 1.0)

$instances = @()
foreach ($e in $def.elements) {
    $instances += @{ corners = (Get-Corners $e); color = (Convert-Color $e.color) }
}

for ($vi = 0; $vi -lt $views.Count; $vi++) {
    $v = $views[$vi]
    $yawRad = $v.yaw * [Math]::PI / 180.0
    $pitchRad = $v.pitch * [Math]::PI / 180.0
    $cosY = [Math]::Cos($yawRad); $sinY = [Math]::Sin($yawRad)
    $cosP = [Math]::Cos($pitchRad); $sinP = [Math]::Sin($pitchRad)

    # 第一遍：变换所有顶点，求屏幕包围盒
    $rotSets = @()
    $minX = [double]::MaxValue; $maxX = [double]::MinValue
    $minY = [double]::MaxValue; $maxY = [double]::MinValue
    foreach ($inst in $instances) {
        $rs = @()
        foreach ($c in $inst.corners) {
            $p = Rotate-Point $c $cosY $sinY $cosP $sinP
            $rs += , $p
            if ($p[0] -lt $minX) { $minX = $p[0] }
            if ($p[0] -gt $maxX) { $maxX = $p[0] }
            if ($p[1] -lt $minY) { $minY = $p[1] }
            if ($p[1] -gt $maxY) { $maxY = $p[1] }
        }
        $rotSets += , $rs
    }

    $spanX = [Math]::Max(0.001, $maxX - $minX)
    $spanY = [Math]::Max(0.001, $maxY - $minY)
    $scale = [Math]::Min(($panelW - 66) / $spanX, ($panelH - 150) / $spanY)

    $cx = $panelW * $vi + $panelW / 2.0 - (($minX + $maxX) / 2.0) * $scale
    $cy = $panelH / 2.0 + 18 + (($minY + $maxY) / 2.0) * $scale

    # 第二遍：收集可见面
    $polys = @()
    for ($ei = 0; $ei -lt $instances.Count; $ei++) {
        $rotated = $rotSets[$ei]
        foreach ($f in $FACES) {
            $n = Rotate-Point $f.n $cosY $sinY $cosP $sinP
            if ($n[2] -le 0.02) { continue }

            $pts = @()
            $depth = 0.0
            foreach ($i in $f.idx) {
                $p = $rotated[$i]
                $pts += , (New-Object System.Drawing.PointF(
                    [single]($cx + $p[0] * $scale),
                    [single]($cy - $p[1] * $scale)))
                $depth += $p[2]
            }

            $depth = $depth / $f.idx.Count
            $lambert = ($n[0] * $LIGHT[0]) + ($n[1] * $LIGHT[1]) + ($n[2] * $LIGHT[2])
            if ($lambert -lt 0) { $lambert = 0 }
            $bright = 0.60 + 0.40 * $lambert

            $polys += @{ pts = $pts; depth = $depth; color = $instances[$ei].color; bright = $bright }
        }
    }

    foreach ($p in ($polys | Sort-Object { $_.depth })) {
        $c = $p.color
        $r = [int][Math]::Min(255, $c.R * $p.bright)
        $gg = [int][Math]::Min(255, $c.G * $p.bright)
        $b = [int][Math]::Min(255, $c.B * $p.bright)
        $brush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, $r, $gg, $b))
        $g.FillPolygon($brush, [System.Drawing.PointF[]]$p.pts)
        $g.DrawPolygon($edgePen, [System.Drawing.PointF[]]$p.pts)
        $brush.Dispose()
    }

    $g.DrawString($v.name, $labelFont, $labelBrush, [single]($panelW * $vi + 16), [single]14)
    if ($vi -gt 0) {
        $g.DrawLine($divPen, [single]($panelW * $vi), [single]40, [single]($panelW * $vi), [single]($panelH - 10))
    }
}

$g.Dispose()
$bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()

Write-Output ("已渲染: {0}  ({1}x{2})" -f $Out, $W, $H)
