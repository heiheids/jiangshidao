# 重新设计：三把剑 / 箭囊 / 石磨 贴图（仅贴图，不改模型几何）
#
# 设计原则：
#   · 平滑、块面化，不要碎密细节
#   · 保留材质特征：木剑=木纹、箭囊=皮革+金属箍、石磨=石材同心圆
#   · 每个 UV 区域只用连续几档明暗，靠形状而非噪点表现质感
#
# UV 区域由模型决定（只读，不改）：
#   剑类： 剑身 0,0,6,11 | 护手 6,0,10,6 | 握柄 10,0,14,6 | 符纸 0,10,6,16
#   箭囊 / 石磨：整张 16x16
#
# 作者：黑山大叔 / https://space.bilibili.com/19428259 / https://github.com/heiheids

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$texRoot = Join-Path $root 'src\main\resources\assets\jiangshidao\textures'

function New-Canvas {
    $bmp = New-Object System.Drawing.Bitmap(16, 16)
    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(0, 0, 0, 0))
        }
    }
    return $bmp
}

function Px($b, $x, $y, $hex) {
    if ($x -lt 0 -or $x -gt 15 -or $y -lt 0 -or $y -gt 15) { return }
    $h = $hex.TrimStart('#')
    $b.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(
        255,
        [Convert]::ToInt32($h.Substring(0, 2), 16),
        [Convert]::ToInt32($h.Substring(2, 2), 16),
        [Convert]::ToInt32($h.Substring(4, 2), 16)))
}

function Rect($b, $x0, $y0, $x1, $y1, $hex) {
    for ($y = $y0; $y -le $y1; $y++) {
        for ($x = $x0; $x -le $x1; $x++) { Px $b $x $y $hex }
    }
}

# ─────────────────────────────────────────────────────────────
# 一、木剑
# 剑身 6 列做成连续明暗（暗 → 中 → 亮 → 高光），模拟圆柱受光，
# 不插入突兀的暗纹列，整体平滑。
# ─────────────────────────────────────────────────────────────
function Draw-WoodSword($file, $c1, $c2, $c3, $c4, $c5, $guard, $guardHi, $grip, $gripHi, $paper, $mark) {
    $b = New-Canvas

    # ---- 剑身 0..5 列 x 0..10 行 ----
    Rect $b 0 0 0 10 $c1
    Rect $b 1 0 1 10 $c2
    Rect $b 2 0 3 10 $c3
    Rect $b 4 0 4 10 $c4
    Rect $b 5 0 5 10 $c5

    # ---- 护手 6..9 x 0..5 ----
    Rect $b 6 0 9 5 $guard
    Rect $b 6 0 9 1 $guardHi

    # ---- 握柄 10..13 x 0..5 ----
    Rect $b 10 0 13 5 $grip
    Rect $b 10 0 10 5 $gripHi
    Rect $b 12 0 12 5 $gripHi

    # ---- 符纸 6..9 x 6..10（方案A：从 0,10,6,16 挪到独立区域）----
    Rect $b 6 6 9 10 $paper
    Rect $b 6 6 9 6 $mark
    Rect $b 6 10 9 10 $mark
    Rect $b 7 8 8 8 $mark

    # ---- 剑齿 10..12 x 7..9（方案A：纯色，压缩后不出现图案）----
    Rect $b 10 7 12 9 $c3

    $b.Save((Join-Path $texRoot $file), [System.Drawing.Imaging.ImageFormat]::Png)
    $b.Dispose()
    Write-Output "已生成: $file"
}

# 桃木剑：暖红木
Draw-WoodSword 'item\peach_wood_sword.png' `
    '#5E1410' '#77201A' '#8C2A20' '#B84232' '#D89880' `
    '#3A1E10' '#5E3A1E' `
    '#2A1A14' '#463024' `
    '#D9BE94' '#8E3A22'

# 雷击木剑：焦褐木 + 浅金高光
Draw-WoodSword 'item\thunder_wood_sword.png' `
    '#241708' '#3E2A18' '#5C4022' '#8A6634' '#C9A468' `
    '#20140C' '#3A2414' `
    '#181010' '#2E2018' `
    '#CDB078' '#7A5410'

# 天罚雷击木剑：深灰木 + 冷银高光（整体提亮，保证结构可辨）
Draw-WoodSword 'item\tian_fa_lei_ji_mu_sword.png' `
    '#332C26' '#443A32' '#5A4E44' '#8C8078' '#C8D4E0' `
    '#2A2420' '#453A30' `
    '#1E1A16' '#342C26' `
    '#B8A878' '#4E3058'

# ─────────────────────────────────────────────────────────────
# 二、箭囊：皮革筒身 + 顶部开口 + 两道金箍 + 肩带
# 左右明暗表现圆柱体积，金箍用亮色拉开层次。
# ─────────────────────────────────────────────────────────────
$q = New-Canvas

# 筒身主体
Rect $q 2 4 13 15 '#7E5230'
# 左侧受光
Rect $q 2 4 3 15 '#A06B3C'
# 右侧阴影
Rect $q 12 4 13 15 '#4A2E1A'

# 顶部开口：外沿亮边 + 内部深腔
Rect $q 2 4 13 5 '#C08A50'
Rect $q 4 5 11 6 '#241408'
Rect $q 5 6 10 6 '#180C04'

# 中部金箍
Rect $q 2 8 13 8 '#D4A855'
Rect $q 2 9 13 9 '#8A6428'

# 底部金箍
Rect $q 2 13 13 13 '#D4A855'
Rect $q 2 14 13 14 '#8A6428'

# 筒身底部收口
Rect $q 2 15 13 15 '#3A2412'

# 肩带
Rect $q 0 6 1 10 '#A06B3C'
Rect $q 0 6 0 10 '#5E3C20'
Rect $q 14 10 15 12 '#A06B3C'
Rect $q 15 10 15 12 '#5E3C20'

$q.Save((Join-Path $texRoot 'item\quiver.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$q.Dispose()
Write-Output '已生成: item\quiver.png'

# ─────────────────────────────────────────────────────────────
# 三、石磨盘面：同心圆磨纹 + 中央磨眼 + 四道放射浅槽
# ─────────────────────────────────────────────────────────────
$s = New-Canvas
for ($y = 0; $y -lt 16; $y++) {
    for ($x = 0; $x -lt 16; $x++) {
        $dx = $x - 7.5; $dy = $y - 7.5
        $d = [math]::Sqrt($dx * $dx + $dy * $dy)
        $hex = '#8E8E8E'
        if ($d -gt 7.4) { $hex = '#555555' }
        elseif ($d -gt 6.6) { $hex = '#A6A6A6' }
        elseif ($d -gt 4.6) { $hex = '#7E7E7E' }
        elseif ($d -gt 3.8) { $hex = '#A6A6A6' }
        elseif ($d -gt 1.8) { $hex = '#8E8E8E' }
        else { $hex = '#4A4A4A' }
        Px $s $x $y $hex
    }
}
Rect $s 7 7 8 8 '#2A2A2A'
Rect $s 7 1 8 2 '#6A6A6A'
Rect $s 7 13 8 14 '#6A6A6A'
Rect $s 1 7 2 8 '#6A6A6A'
Rect $s 13 7 14 8 '#6A6A6A'

$s.Save((Join-Path $texRoot 'block\stone_mill.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$s.Dispose()
Write-Output '已生成: block\stone_mill.png'

Write-Output ''
Write-Output '完成：三把剑 / 箭囊 / 石磨 贴图已重绘（模型几何未改动）'
