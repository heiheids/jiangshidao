# 生成桃子相关贴图（纯像素画，16x16）
#   1) textures/item/peach.png            —— 物品图标（模型用 minecraft:item/generated）
#   2) textures/block/peach_leaves_peach.png —— 桃叶上的桃子覆盖层
#
# 两张都按「铺满整张 16x16」绘制：物品图标整张即图标；
# 覆盖层的面 UV 取 [0,0,16,16]，所以桃子在贴图里要画满，
# 透明区域会被 cutout 丢弃，自然形成桃形轮廓。
#
# 作者：黑山大叔 / https://space.bilibili.com/19428259 / https://github.com/heiheids

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent

function New-Canvas {
    $bmp = New-Object System.Drawing.Bitmap(16, 16)
    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(0, 0, 0, 0))
        }
    }
    return $bmp
}

function Px($bmp, $x, $y, $r, $g, $b) {
    if ($x -ge 0 -and $x -lt 16 -and $y -ge 0 -and $y -lt 16) {
        $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $r, $g, $b))
    }
}

# 桃子轮廓：按行给出左右边界，做出「上尖下圆、稍偏右」的桃形
$ROWS = @{
    2  = @(7, 8)
    3  = @(6, 9)
    4  = @(5, 10)
    5  = @(4, 11)
    6  = @(3, 12)
    7  = @(2, 13)
    8  = @(2, 13)
    9  = @(2, 13)
    10 = @(2, 13)
    11 = @(3, 12)
    12 = @(4, 11)
    13 = @(5, 10)
    14 = @(6, 9)
    15 = @(7, 8)
}

function Draw-Peach($bmp, $withLeaf) {
    # 主体
    foreach ($y in $ROWS.Keys) {
        $l = $ROWS[$y][0]
        $r = $ROWS[$y][1]
        for ($x = $l; $x -le $r; $x++) { Px $bmp $x $y 236 124 143 }
    }
    # 左侧高光
    foreach ($y in 5..11) {
        $l = $ROWS[$y][0]
        Px $bmp $l $y 250 168 180
        Px $bmp ($l + 1) $y 250 168 180
    }
    # 右侧阴影
    foreach ($y in 8..14) {
        $r = $ROWS[$y][1]
        Px $bmp $r $y 196 88 108
    }
    # 底部阴影
    foreach ($x in 6..9) { Px $bmp $x 15 196 88 108 }

    if ($withLeaf) {
        # 叶片与果柄
        foreach ($px in @(@(9, 1), @(10, 1), @(11, 1), @(12, 1), @(9, 2), @(10, 2), @(11, 2), @(12, 2), @(10, 3), @(11, 3))) {
            Px $bmp $px[0] $px[1] 86 142 74
        }
        foreach ($px in @(@(10, 1), @(11, 2))) { Px $bmp $px[0] $px[1] 58 104 52 }
        Px $bmp 8 2 106 78 50
        Px $bmp 8 3 106 78 50
    }
}

# 1) 物品图标
$icon = New-Canvas
Draw-Peach $icon $true
$out1 = Join-Path $root 'src\main\resources\assets\jiangshidao\textures\item\peach.png'
$icon.Save($out1, [System.Drawing.Imaging.ImageFormat]::Png)
$icon.Dispose()

# 2) 桃叶覆盖层（画得更大，铺满整张，靠 cutout 裁出轮廓）
$overlay = New-Canvas
Draw-Peach $overlay $false
$out2 = Join-Path $root 'src\main\resources\assets\jiangshidao\textures\block\peach_leaves_peach.png'
$overlay.Save($out2, [System.Drawing.Imaging.ImageFormat]::Png)
$overlay.Dispose()

Write-Output "已生成: textures\item\peach.png"
Write-Output "已生成: textures\block\peach_leaves_peach.png"
