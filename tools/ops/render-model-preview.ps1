# 把游戏内模型 + 新贴图渲染成 3D 效果图（正交投影 + 画家算法）
#
# 关键点：
#   · 逐个面按 UV 区域取贴图平均色（面很小，平均色足够表达材质）
#   · 面法线做背面剔除，剩余按深度排序后绘制
#   · 物品模型 Y 轴向下，屏幕 Y 直接沿用（大 y 在下）
#
# 注意命名：贴图表用 $FACE_DEFS，元素面集合用 $elemFaces，避免 PowerShell
# 变量名不区分大小写造成覆盖（上次就是 $faces 覆盖了 $FACES 导致整屏空白）。
#
# 作者：黑山大叔 / https://space.bilibili.com/19428259 / https://github.com/heiheids

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$assetRoot = Join-Path $root 'src\main\resources\assets\jiangshidao'
$outDir = Join-Path $root '预览图'
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }

# 面名 -> 顶点索引 + 法线（物品模型坐标：x 右, y 下, z 朝屏幕外为负）
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
    # 先绕 Y 轴（yaw）
    $xr = $x * $cosY + $z * $sinY
    $zr = -$x * $sinY + $z * $cosY
    # 再绕 X 轴（pitch）
    $yr = $y * $cosP - $zr * $sinP
    $zr2 = $y * $sinP + $zr * $cosP
    return @($xr, $yr, $zr2)
}

# 光照：从左上前方打光
$LIGHT = @(-0.40, -0.50, -0.77)
$llen = [Math]::Sqrt(($LIGHT[0]*$LIGHT[0]) + ($LIGHT[1]*$LIGHT[1]) + ($LIGHT[2]*$LIGHT[2]))
$LIGHT = @(($LIGHT[0]/$llen), ($LIGHT[1]/$llen), ($LIGHT[2]/$llen))

# 解析模型里的 textures 段，找到贴图实际文件
function Resolve-TextureFile($res) {
    $r = $res
    if ($r -like 'jiangshidao:*') { $r = $r.Substring('jiangshidao:'.Length) }
    elseif ($r -like 'minecraft:*') {
        # 原版贴图也尝试从内置 jar 外找一份；找不到就返回 null，用中性灰代替
        return $null
    }
    $candidates = @(
        (Join-Path $assetRoot "textures\$r.png"),
        (Join-Path $assetRoot ("textures\" + ($r -replace '^block/', 'block\') + ".png"))
    )
    foreach ($c in $candidates) { if (Test-Path $c) { return $c } }
    return $null
}

function Render-Model($modelRel, $label, $outName, $yawDeg, $pitchDeg, $padRatio) {
    $modelPath = Join-Path $assetRoot $modelRel
    $def = Get-Content -LiteralPath $modelPath -Raw -Encoding utf8 | ConvertFrom-Json

    # 贴图缓存：变量名 -> Bitmap
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

    # 预处理每个元素
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
        $prepared += @{ rot = $rot; elemFaces = $el.faces }
    }

    $canvas = 320
    $inner = $canvas * $padRatio
    $spanX = [Math]::Max(0.001, $maxX - $minX)
    $spanY = [Math]::Max(0.001, $maxY - $minY)
    $scale = [Math]::Min($inner / $spanX, $inner / $spanY)
    $cx = $canvas / 2.0 - (($minX + $maxX) / 2.0) * $scale
    $cy = $canvas / 2.0 - (($minY + $maxY) / 2.0) * $scale

    # 收集可见面
    $polys = @()
    foreach ($item in $prepared) {
        $rot = $item.rot
        $elemFaces = $item.elemFaces
        foreach ($faceName in $FACE_ORDER) {
            $prop = $elemFaces.PSObject.Properties[$faceName]
            if ($null -eq $prop) { continue }
            $faceData = $prop.Value

            $fdef = $FACE_DEFS[$faceName]
            $nrot = Rotate-Pt $fdef.n $cosY $sinY $cosP $sinP
            # 相机在 -z 方向看向 +z：法线 z 分量为负才朝向相机
            if ($nrot[2] -ge -0.01) { continue }

            # 取面中心深度（用于排序）
            $pts = @()
            $depth = 0.0
            foreach ($i in $fdef.idx) {
                $p = $rot[$i]
                $pts += , (New-Object System.Drawing.PointF(
                    [single]($cx + $p[0] * $scale), [single]($cy + $p[1] * $scale)))
                $depth += $p[2]
            }
            $depth = $depth / $fdef.idx.Count

            # 采样贴图：取 UV 矩形内的平均色，比单点更能代表整面的材质
            $texRef = $faceData.texture
            if ($texRef -like '#*') { $texVar = $texRef.Substring(1) } else { $texVar = $texRef }
            $color = [System.Drawing.Color]::FromArgb(255, 128, 118, 108)
            if ($texCache.ContainsKey($texVar)) {
                $bmp = $texCache[$texVar]
                $uv = $faceData.uv
                $u0 = [int][Math]::Floor(([double]$uv[0] / 16.0) * $bmp.Width)
                $u1 = [int][Math]::Ceiling(([double]$uv[2] / 16.0) * $bmp.Width)
                $v0 = [int][Math]::Floor(([double]$uv[1] / 16.0) * $bmp.Height)
                $v1 = [int][Math]::Ceiling(([double]$uv[3] / 16.0) * $bmp.Height)
                if ($u1 -le $u0) { $u1 = $u0 + 1 }
                if ($v1 -le $v0) { $v1 = $v0 + 1 }
                if ($u0 -lt 0) { $u0 = 0 }; if ($u0 -ge $bmp.Width) { $u0 = $bmp.Width - 1 }
                if ($u1 -gt $bmp.Width) { $u1 = $bmp.Width }
                if ($v0 -lt 0) { $v0 = 0 }; if ($v0 -ge $bmp.Height) { $v0 = $bmp.Height - 1 }
                if ($v1 -gt $bmp.Height) { $v1 = $bmp.Height }
                $rs = 0; $gs = 0; $bs = 0; $cnt = 0
                for ($vy = $v0; $vy -lt $v1; $vy++) {
                    for ($vx = $u0; $vx -lt $u1; $vx++) {
                        $cp = $bmp.GetPixel($vx, $vy)
                        if ($cp.A -gt 0) { $rs += $cp.R; $gs += $cp.G; $bs += $cp.B; $cnt++ }
                    }
                }
                if ($cnt -gt 0) {
                    $color = [System.Drawing.Color]::FromArgb(255, [int]($rs/$cnt), [int]($gs/$cnt), [int]($bs/$cnt))
                }
            }

            if ($lambert -lt 0) { $lambert = 0 }
            $bright = 0.94 + 0.10 * $lambert

            $polys += @{ pts = $pts; depth = $depth; color = $color; bright = $bright }
        }
    }

    $bmpOut = New-Object System.Drawing.Bitmap($canvas, ($canvas + 26))
    $g = [System.Drawing.Graphics]::FromImage($bmpOut)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.Clear([System.Drawing.Color]::FromArgb(255, 34, 34, 42))

    $edgePen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(110, 10, 10, 14), 1.0)
    foreach ($poly in ($polys | Sort-Object { - $_.depth })) {
        $c = $poly.color
        $r = [int][Math]::Min(255, $c.R * $poly.bright)
        $gg = [int][Math]::Min(255, $c.G * $poly.bright)
        $bb = [int][Math]::Min(255, $c.B * $poly.bright)
        $brush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, $r, $gg, $bb))
        $g.FillPolygon($brush, [System.Drawing.PointF[]]$poly.pts)
        $g.DrawPolygon($edgePen, [System.Drawing.PointF[]]$poly.pts)
        $brush.Dispose()
    }

    $font = New-Object System.Drawing.Font('Microsoft YaHei', 10)
    $tb = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 230, 230, 235))
    $g.DrawString($label, $font, $tb, [single]6, [single]($canvas + 4))
    $g.Dispose()

    $outPath = Join-Path $outDir $outName
    $bmpOut.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmpOut.Dispose()
    foreach ($k in $texCache.Keys) { $texCache[$k].Dispose() }

    Write-Output ("{0}: {1} 个可见面 -> {2}" -f $label, $polys.Count, $outName)
}

$panels = @(
    @{ model = 'models\item\peach_wood_sword.json';        label = '桃木剑';        out = 'sword1.png'; yaw = 22; pitch = 8;  pad = 0.80 },
    @{ model = 'models\item\thunder_wood_sword.json';      label = '雷击木剑';      out = 'sword2.png'; yaw = 22; pitch = 8;  pad = 0.80 },
    @{ model = 'models\item\tian_fa_lei_ji_mu_sword.json'; label = '天罚雷击木剑';  out = 'sword3.png'; yaw = 22; pitch = 8;  pad = 0.80 },
    @{ model = 'models\item\quiver.json';                  label = '箭囊';          out = 'quiver.png'; yaw = 24; pitch = 6;  pad = 0.80 },
    @{ model = 'models\block\stone_mill_upper.json';       label = '石磨上扇';      out = 'mill.png';   yaw = 0;  pitch = 78; pad = 0.80 }
)

foreach ($p in $panels) {
    Render-Model $p.model $p.label $p.out $p.yaw $p.pitch $p.pad
}

# 拼图
$cell = 320
$gap = 12
$W = $panels.Count * $cell + ($panels.Count + 1) * $gap
$H = $cell + 26 + $gap * 2
$sheet = New-Object System.Drawing.Bitmap($W, $H)
$sg = [System.Drawing.Graphics]::FromImage($sheet)
$sg.Clear([System.Drawing.Color]::FromArgb(255, 24, 24, 30))
$i = 0
foreach ($p in $panels) {
    $img = [System.Drawing.Image]::FromFile((Join-Path $outDir $p.out))
    $sg.DrawImage($img, ($gap + $i * ($cell + $gap)), $gap)
    $img.Dispose()
    $i++
}
$sg.Dispose()
$sheetPath = Join-Path $outDir '贴图重设计_模型效果图.png'
$sheet.Save($sheetPath, [System.Drawing.Imaging.ImageFormat]::Png)
$sheet.Dispose()

foreach ($p in $panels) { Remove-Item -LiteralPath (Join-Path $outDir $p.out) -Force }
Write-Output ("已生成: 预览图\贴图重设计_模型效果图.png ({0}x{1})" -f $W, $H)