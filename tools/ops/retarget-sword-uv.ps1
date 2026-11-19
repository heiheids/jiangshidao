# 方案A：为剑齿/符纸划分独立 UV 区域（仅改 UV 数字，几何尺寸完全不动）
#
# 问题背景：
#   剑身 UV 区域 [0,0,6,11] 被 19 个元素共用 —— 包括 blade（宽 3.0）和
#   16 个观念齿（宽 0.19~0.42）。它们把同一幅 6 列渐变压缩进极窄的齿面，
#   于是每个小齿上都出现一份微缩渐变，视觉上就是剑刃两侧的碎纹。
#
# 解决办法：
#   把齿划到一块纯色区域，压缩后看不出任何图案；
#   符纸原本与剑身有 1 行重叠（y=10..11），一并挪到独立区域。
#
# 新布局（16x16）：
#   blade/tip  0, 0, 6,11   剑身，纵向木纹渐变（保持）
#   teeth     10, 7,13,10   齿，纯色（新增）
#   talisman   6, 6,10,11   符纸（从 0,10,6,16 挪来，消除与剑身的重叠）
#   guard      6, 0,10, 6   护手（保持）
#   grip      10, 0,14, 6   握柄（保持）
#
# 作者：黑山大叔 / https://space.bilibili.com/19428259 / https://github.com/heiheids

$ErrorActionPreference = 'Stop'

$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$modelDir = Join-Path $root 'src\main\resources\assets\jiangshidao\models\item'

# 元素前缀 -> 目标 UV
$BLADE_UV  = @(0, 0, 6, 11)
$TOOTH_UV  = @(10, 7, 13, 10)
$PAPER_UV  = @(6, 6, 10, 11)

$targets = @('peach_wood_sword', 'thunder_wood_sword', 'tian_fa_lei_ji_mu_sword')

# 哪些元素用齿的 UV
function Is-Tooth($name) {
    return ($name -like 'tooth_*')
}
# 哪些元素用符纸的 UV
function Is-Paper($name) {
    return ($name -eq 'talisman')
}

foreach ($name in $targets) {
    $path = Join-Path $modelDir "$name.json"
    $def = Get-Content -LiteralPath $path -Raw -Encoding utf8 | ConvertFrom-Json

    $changed = 0
    foreach ($el in $def.elements) {
        $target = $null
        if (Is-Tooth $el.name) { $target = $TOOTH_UV }
        elseif (Is-Paper $el.name) { $target = $PAPER_UV }

        if ($null -eq $target) { continue }

        foreach ($fp in $el.faces.PSObject.Properties) {
            $face = $fp.Value
            # 只改 UV，texture 与其它字段保持原样
            $face.uv = [double[]]$target
            $changed++
        }
    }

    $json = $def | ConvertTo-Json -Depth 20
    Set-Content -LiteralPath $path -Value $json -Encoding utf8 -NoNewline
    Write-Output ("{0,-28} 修改 {1} 个面的 UV" -f $name, $changed)
}

Write-Output ''
Write-Output '完成：仅改动 UV 数值，元素的 from/to 坐标未做任何修改'
