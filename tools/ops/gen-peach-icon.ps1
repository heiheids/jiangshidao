# 生成桃子物品贴图（16x16 像素画风）
#
# 采用「纯像素绘制」而不是几何图形填充：逐像素写入，不依赖 GDI 的抗锯齿，
# 保证 16x16 下的边缘硬朗、颜色区块清晰。物品模型用 minecraft:item/generated，
# 整张贴图直接作为图标使用，因此图案必须铺满并居中。
#
# 作者：黑山大叔 / https://space.bilibili.com/19428259 / https://github.com/heiheids

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$out = Join-Path $root 'src\main\resources\assets\jiangshidao\textures\item\peach.png'

$bmp = New-Object System.Drawing.Bitmap(16, 16)

function Set-Px($x, $y, $r, $g, $b, $a = 255) {
    if ($x -ge 0 -and $x -lt 16 -and $y -ge 0 -and $y -lt 16) {
        $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($a, $r, $g, $b))
    }
}

# 先铺满全透明
for ($y = 0; $y -lt 16; $y++) {
    for ($x = 0; $x -lt 16; $x++) { Set-Px $x $y 0 0 0 0 }
}

# 桃子主体：一个圆润的桃形，占 x2..13 / y4..15
# 逐行定义左右边界，做出上尖下圆的桃子轮廓
$bodyRows = @{
    4  = @(7, 10)
    5  = @(5, 12)
    6  = @(4, 13)
    7  = @(3, 13)
    8  = @(3, 13)
    9  = @(3, 13)
    10 = @(3, 13)
    11 = @(4, 12)
    12 = @(5, 11)
    13 = @(6, 10)
    14 = @(7, 9)
}

foreach ($y in $bodyRows.Keys) {
    $l = $bodyRows[$y][0]
    $r = $bodyRows[$y][1]
    for ($x = $l; $x -le $r; $x++) { Set-Px $x $y 235 122 140 }
}

# 左侧高光带（偏亮）
foreach ($y in 5..11) {
    $l = $bodyRows[$y][0]
    Set-Px $l $y 250 165 178
    Set-Px ($l + 1) $y 250 165 178
}

# 右下阴影带（偏深）
foreach ($y in 9..13) {
    $r = $bodyRows[$y][1]
    Set-Px $r $y 196 84 106
}

# 顶部凹槽
Set-Px 8 3 196 84 106
Set-Px 9 3 196 84 106
Set-Px 8 4 196 84 106

# 叶片（右上，绿色）
foreach ($px in @(@(10, 1), @(11, 1), @(12, 1), @(13, 1), @(10, 2), @(11, 2), @(12, 2), @(13, 2), @(11, 3), @(12, 3))) {
    Set-Px $px[0] $px[1] 84 140 72
}
# 叶脉
foreach ($px in @(@(11, 1), @(12, 2))) { Set-Px $px[0] $px[1] 58 104 52 }

# 果柄（深棕）
Set-Px 9 2 104 76 48
Set-Px 9 3 104 76 48

$bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()

Write-Output "已生成: textures\item\peach.png (16x16 像素画)"
