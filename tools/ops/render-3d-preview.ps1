# 把游戏内模型 + 贴图渲染成 3D 效果图（逐像素纹理映射）
#
# 与旧版 render-model-preview.ps1 的区别：
#   旧版每个面只取 UV 区域的平均色 —— 会把木纹、箍、同心圆磨纹全抹平，
#   预览图完全不能反映游戏内观感。
#   本版把每个面细分成网格，按 UV 逐点采样贴图，做真正的纹理映射。
#
# 实现要点：
#   · 面按较长一边切成 N 份（N 由面上像素数决定），每份再按 UV 采样
#   · 背面剔除 + 画家算法排序
#   · 物品模型 y 轴向下，屏幕 y 直接沿用
#   · 命名注意：$FACE_DEFS 与 $elemFaces 分开，避免 PowerShell 变量名不区分大小写互相覆盖
#
# 作者：黑山大叔 / https://space.bilibili.com/19428259 / https://github.com/heiheids

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$assetRoot = Join-Path $root 'src\main\resources\assets\jiangshidao'
$outDir = Join-Path $root '预览图'
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }

$FACE_DEFS = @{
    north = @{ idx = @(0, 1, 2, 3); n = @(0, 0, -1) }
    south = @{ idx = @(5, 4, 7, 6); n = @(0, 0, 1) }
    west  = @{ idx = @(4, 0, 3, 7); n = @(-1, 0, 0) }
    east  = @{ idx = @(1, 5, 6, 2); n = @(1, 0, 0) }
    up    = @{ idx = @(3, 2, 6, 7); n = @(0, 1, 0) }
    down  = @{ idx = @(4, 5, 1, 0); n = @(0, -1, 0) }
}
$FACE_ORDER = @('north', 'south', 'west', 'east', 'up', 'down')

function Get-Corners($el) {
    $x1 = [double]$el.from[0]; $y1 = [double]$el.from[1]; $z1 = [double]$el.from[2]
    $x2 = [double]$el.to[0];   $y2 = [double]$el.to[1];   $z2 = [double]$el.to[2]
    return @(
        @($x1, $y1, $z1), @($x2, $y1, $z1), @($x2, $y2, $z1), @($x1, $y2, $z1),
        @($x1, $y1, $z2), @($x2, $y1, $z2), @($x2, $y2, $z2), @($x1, $y2, $z2)
    )
}

function Rotate-Pt($p, $cosY, $sinY, $cosP, $sinP) {
    $x = $p[0]; $y = $p[1]; $z = $p[2]
    $xr = $x * $cosY + $z * $sinY
    $zr = -$x * $sinY + $z * $cosY
    $yr = $y * $cosP - $zr * $sinP
    $zr2 = $y * $sinP + $zr * $cosP
    return @($xr, $yr, $zr2)
}

$LIGHT = @(-0.40, -0.50, -0.77)
$llen = [Math]::Sqrt(($LIGHT[0]*$LIGHT[0]) + ($LIGHT[1]*$LIGHT[1]) + ($LIGHT[2]*$LIGHT[2]))
$LIGHT = @(($LIGHT[0]/$llen), ($LIGHT[1]/$llen), ($LIGHT[2]/$llen))

function Resolve-TextureFile($res) {
    if ($res -like 'minecraft:*') { return $null }
    $r = $res
    if ($r -like 'jiangshidao:*') { $r = $r.Substring('jiangshidao:'.Length) }
    $p = Join-Path $assetRoot ("textures\" + $r + ".png")
    if (Test-Path $p) { return $p }
    return $null
}

function Render-Model($modelRel, $label, $outName, $yawDeg, $pitchDeg, $canvasSize) {
    $modelPath = Join-Path $assetRoot $modelRel
    if (-not (Test-Path $modelPath)) {
        $alt = Join-Path (Join-Path $root 'tools\preview-models') (Split-Path $modelRel -Leaf)
        if (Test-Path $alt) { $modelPath = $alt }
    }
    $def = Get-Content -LiteralPath $modelPath -Raw -Encoding utf8 | ConvertFrom-Json

    $texCache = @{}
    foreach ($tp in $def.textures.PSObject.Properties) {
        $v = $tp.Value
        if ($v -like '#*') { continue }
        $file = Resolve-TextureFile $v
        if ($file) { $texCache[$tp.Name] = [System.Drawing.Bitmap]::FromFile($file) }
    }

    $yawRad = $yawDeg * [Math]::PI / 180.0
    $pitchRad = $pitchDeg * [Math]::PI / 180.0
    $cosY = [Math]::Cos($yawRad); $sinY = [Math]::Sin($yawRad)
    $cosP = [Math]::Cos($pitchRad); $sinP = [Math]::Sin($pitchRad)

    # 预处理：旋转所有顶点，求包围盒
    $prepared = @()
    $minX = [double]::MaxValue; $maxX = [double]::MinValue
    $minY = [double]::MaxValue; $maxY = [double]::MinValue
    foreach ($el in $def.elements) {
        $corners = Get-Corners $el
        $rot = @()
        foreach ($c in $corners) {
            $p = Rotate-Pt $c $cosY $sinY $cosP $sinP
            $rot += , $p
            if ($p[0] -lt $minX) { $minX = $p[0] }
            if ($p[0] -gt $maxX) { $maxX = $p[0] }
            if ($p[1] -lt $minY) { $minY = $p[1] }
            if ($p[1] -gt $maxY) { $maxY = $p[1] }
        }
        $prepared += @{ rot = $rot; elemFaces = $el.faces; raw = $el }
    }

    $inner = $canvasSize * 0.82
    $spanX = [Math]::Max(0.001, $maxX - $minX)
    $spanY = [Math]::Max(0.001, $maxY - $minY)
    $scale = [Math]::Min($inner / $spanX, $inner / $spanY)
    $cx = $canvasSize / 2.0 - (($minX + $maxX) / 2.0) * $scale
    $cy = $canvasSize / 2.0 - (($minY + $maxY) / 2.0) * $scale

    # 细分网格密度：每个世界单位切成 SUB 份
    $SUB = 14

    $cells = @()
    foreach ($item in $prepared) {
        $rot = $item.rot
        $elemFaces = $item.elemFaces
        $raw = $item.raw
        foreach ($faceName in $FACE_ORDER) {
            $prop = $elemFaces.PSObject.Properties[$faceName]
            if ($null -eq $prop) { continue }
            $faceData = $prop.Value

            $fdef = $FACE_DEFS[$faceName]
            $nrot = Rotate-Pt $fdef.n $cosY $sinY $cosP $sinP
            if ($nrot[2] -ge -0.01) { continue }   # 背面剔除

            $lambert = -($nrot[0]*$LIGHT[0] + $nrot[1]*$LIGHT[1] + $nrot[2]*$LIGHT[2])
            if ($lambert -lt 0) { $lambert = 0 }
            $bright = 0.72 + 0.32 * $lambert

            # 贴图
            $texRef = $faceData.texture
            if ($texRef -like '#*') { $texVar = $texRef.Substring(1) } else { $texVar = $texRef }
            $bmp = $null
            if ($texCache.ContainsKey($texVar)) { $bmp = $texCache[$texVar] }

            $uv = $faceData.uv
            $u0 = [double]$uv[0]; $v0 = [double]$uv[1]
            $u1 = [double]$uv[2]; $v1 = [double]$uv[3]

            $i0 = $fdef.idx[0]; $i1 = $fdef.idx[1]; $i2 = $fdef.idx[2]; $i3 = $fdef.idx[3]
            $p00 = $rot[$i0]; $p10 = $rot[$i1]; $p11 = $rot[$i2]; $p01 = $rot[$i3]

            for ($a = 0; $a -lt $SUB; $a++) {
                for ($b = 0; $b -lt $SUB; $b++) {
                    $fa0 = $a / $SUB; $fa1 = ($a + 1) / $SUB
                    $fb0 = $b / $SUB; $fb1 = ($b + 1) / $SUB

                    $quad = @()
                    foreach ($pair in @(@($fa0, $fb0), @($fa1, $fb0), @($fa1, $fb1), @($fa0, $fb1))) {
                        $fa = $pair[0]; $fb = $pair[1]
                        $px = (1-$fa)*(1-$fb)*$p00[0] + $fa*(1-$fb)*$p10[0] + $fa*$fb*$p11[0] + (1-$fa)*$fb*$p01[0]
                        $py = (1-$fa)*(1-$fb)*$p00[1] + $fa*(1-$fb)*$p10[1] + $fa*$fb*$p11[1] + (1-$fa)*$fb*$p01[1]
                        $pz = (1-$fa)*(1-$fb)*$p00[2] + $fa*(1-$fb)*$p10[2] + $fa*$fb*$p11[2] + (1-$fa)*$fb*$p01[2]
                        $quad += , (New-Object System.Drawing.PointF(
                            [single]($cx + $px * $scale), [single]($cy + $py * $scale)))
                    }

                    # 采样该小格中心对应的贴图像素
                    $uc = $u0 + ($u1 - $u0) * (($fa0 + $fa1) / 2.0)
                    $vc = $v0 + ($v1 - $v0) * (($fb0 + $fb1) / 2.0)
                    $col = [System.Drawing.Color]::FromArgb(255, 128, 118, 108)
                    if ($bmp) {
                        $sx = [int][Math]::Floor($uc / 16.0 * $bmp.Width)
                        $sy = [int][Math]::Floor($vc / 16.0 * $bmp.Height)
                        if ($sx -lt 0) { $sx = 0 }; if ($sx -ge $bmp.Width) { $sx = $bmp.Width - 1 }
                        if ($sy -lt 0) { $sy = 0 }; if ($sy -ge $bmp.Height) { $sy = $bmp.Height - 1 }
                        $col = $bmp.GetPixel($sx, $sy)
                        if ($col.A -eq 0) {
                            # 透明像素：这一小块不画（等价于 cutout）
                            continue
                        }
                    }

                    $depth = ($p00[2] + $p11[2]) / 2.0
                    $cells += @{ pts = $quad; depth = $depth; color = $col; bright = $bright }
                }
            }
        }
    }

    $bmpOut = New-Object System.Drawing.Bitmap($canvasSize, ($canvasSize + 24))
    $g = [System.Drawing.Graphics]::FromImage($bmpOut)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.Clear([System.Drawing.Color]::FromArgb(255, 34, 34, 42))

    foreach ($cell in ($cells | Sort-Object { $_.depth })) {
        $c = $cell.color
        $r = [int][Math]::Min(255, $c.R * $cell.bright)
        $gg = [int][Math]::Min(255, $c.G * $cell.bright)
        $bb = [int][Math]::Min(255, $c.B * $cell.bright)
        $brush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, $r, $gg, $bb))
        $g.FillPolygon($brush, [System.Drawing.PointF[]]$cell.pts)
        $brush.Dispose()
    }

    $font = New-Object System.Drawing.Font('Microsoft YaHei', 10)
    $tb = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 230, 230, 235))
    $g.DrawString($label, $font, $tb, [single]8, [single]($canvasSize + 3))
    $g.Dispose()

    $bmpOut.Save((Join-Path $outDir $outName), [System.Drawing.Imaging.ImageFormat]::Png)
    $bmpOut.Dispose()
    foreach ($k in $texCache.Keys) { $texCache[$k].Dispose() }

    Write-Output ("{0}: {1} 个贴图小块 -> {2}" -f $label, $cells.Count, $outName)
}

$panels = @(
    @{ model = 'preview-models\peach_wood_sword.json';    label = '桃木剑(平滑刃)'; out = 'a.png'; yaw = 20; pitch = 10 },
    @{ model = 'preview-models\thunder_wood_sword.json';  label = '雷击木剑(平滑刃)'; out = 'b.png'; yaw = 20; pitch = 10 },
    @{ model = 'preview-models\tian_fa_lei_ji_mu_sword.json'; label = '天罚雷击木剑(平滑刃)'; out = 'c.png'; yaw = 20; pitch = 10 },
    @{ model = 'models\item\quiver.json';                  label = '箭囊';         out = 'd.png'; yaw = 20; pitch = 10 },
    @{ model = 'models\block\stone_mill_upper.json';       label = '石磨上扇';     out = 'e.png'; yaw = 0;  pitch = 75 }
)

$size = 300
foreach ($p in $panels) { Render-Model $p.model $p.label $p.out $p.yaw $p.pitch $size }

$gap = 10
$W = $panels.Count * $size + ($panels.Count + 1) * $gap
$H = $size + 24 + $gap * 2
$sheet = New-Object System.Drawing.Bitmap($W, $H)
$sg = [System.Drawing.Graphics]::FromImage($sheet)
$sg.Clear([System.Drawing.Color]::FromArgb(255, 22, 22, 28))
$i = 0
foreach ($p in $panels) {
    $img = [System.Drawing.Image]::FromFile((Join-Path $outDir $p.out))
    $sg.DrawImage($img, ($gap + $i * ($size + $gap)), $gap)
    $img.Dispose()
    $i++
}
$sg.Dispose()
$sheet.Save((Join-Path $outDir '贴图重设计_平滑剑刃效果图.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$sheet.Dispose()
foreach ($p in $panels) { Remove-Item -LiteralPath (Join-Path $outDir $p.out) -Force }

Write-Output ("已生成: 预览图\贴图重设计_平滑剑刃效果图.png ({0}x{1})" -f $W, $H)