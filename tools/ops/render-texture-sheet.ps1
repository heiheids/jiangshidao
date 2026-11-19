# 生成贴图效果图：把 16x16 贴图放大 8 倍并排展示，便于观察设计
#
# 放大用最近邻插值，保持像素硬边（不做模糊），反映游戏内的真实观感。
#
# 作者：黑山大叔 / https://space.bilibili.com/19428259 / https://github.com/heiheids

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$texRoot = Join-Path $root 'src\main\resources\assets\jiangshidao\textures'
$outDir = Join-Path $root '预览图'
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }
$outPath = Join-Path $outDir '贴图重设计_效果图.png'

$items = @(
    @{ file = 'item\peach_wood_sword.png';        label = '桃木剑' },
    @{ file = 'item\thunder_wood_sword.png';      label = '雷击木剑' },
    @{ file = 'item\tian_fa_lei_ji_mu_sword.png'; label = '天罚雷击木剑' },
    @{ file = 'item\quiver.png';                  label = '箭囊' },
    @{ file = 'block\stone_mill.png';             label = '石磨' }
)

$scale = 8
$cell = 16 * $scale          # 128
$pad = 16
$labelH = 26
$W = $items.Count * ($cell + $pad) + $pad
$H = $cell + $labelH + $pad * 2

$bmp = New-Object System.Drawing.Bitmap($W, $H)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$g.Clear([System.Drawing.Color]::FromArgb(255, 32, 32, 40))

$font = New-Object System.Drawing.Font('Microsoft YaHei', 10)
$brush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 230, 230, 235))
$gridPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(40, 255, 255, 255), 1.0)

# 棋盘底：表示透明区域
function Draw-Checker($g, $x0, $y0, $size, $step) {
    $c1 = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 60, 60, 68))
    $c2 = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 46, 46, 54))
    for ($y = 0; $y -lt $size; $y += $step) {
        for ($x = 0; $x -lt $size; $x += $step) {
            $b = if ((($x / $step) + ($y / $step)) % 2 -eq 0) { $c1 } else { $c2 }
            $g.FillRectangle($b, ($x0 + $x), ($y0 + $y), $step, $step)
        }
    }
    $c1.Dispose(); $c2.Dispose()
}

$i = 0
foreach ($it in $items) {
    $x0 = $pad + $i * ($cell + $pad)
    $y0 = $pad

    Draw-Checker $g $x0 $y0 $cell ($scale * 2)

    $srcPath = Join-Path $texRoot $it.file
    if (Test-Path $srcPath) {
        $src = [System.Drawing.Bitmap]::FromFile($srcPath)
        $g.DrawImage($src, (New-Object System.Drawing.Rectangle($x0, $y0, $cell, $cell)))
        $src.Dispose()
    }

    # 8 像素网格线，方便看结构
    for ($k = 0; $k -le 16; $k += 8) {
        $p = $x0 + $k * $scale
        $g.DrawLine($gridPen, $p, $y0, $p, ($y0 + $cell))
        $q = $y0 + $k * $scale
        $g.DrawLine($gridPen, $x0, $q, ($x0 + $cell), $q)
    }

    $g.DrawString($it.label, $font, $brush, [single]$x0, [single]($y0 + $cell + 5))
    $i++
}

$g.Dispose()
$bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()

Write-Output ("已生成效果图: 预览图\贴图重设计_效果图.png  ({0}x{1})" -f $W, $H)
