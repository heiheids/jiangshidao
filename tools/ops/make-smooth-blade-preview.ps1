# 生成「平滑剑刃」预览变体模型（仅用于效果图预览，不影响正式模型）
#
# 做法：
#   · 删除全部 tooth_* 锯齿元素
#   · 把 blade 略微加宽，形成一把干净的直刃
#   · 其余元素（护手/握柄/符纸/剑尖）完全保留原样
#
# 输出到 tools\preview-models\，正式 assets 目录下的模型不动。
#
# 作者：黑山大叔 / https://space.bilibili.com/19428259 / https://github.com/heiheids

$ErrorActionPreference = 'Stop'

$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$srcDir = Join-Path $root 'src\main\resources\assets\jiangshidao\models\item'
$outDir = Join-Path $root 'tools\preview-models'
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }

$targets = @('peach_wood_sword', 'thunder_wood_sword', 'tian_fa_lei_ji_mu_sword')

foreach ($name in $targets) {
    $src = Join-Path $srcDir "$name.json"
    $def = Get-Content -LiteralPath $src -Raw -Encoding utf8 | ConvertFrom-Json

    $kept = @()
    foreach ($el in $def.elements) {
        if ($el.name -like 'tooth_*') { continue }   # 丢弃所有锯齿
        $kept += $el
    }

    # 剑身加宽：左右各扩 0.35，使直刃更饱满
    foreach ($el in $kept) {
        if ($el.name -eq 'blade') {
            $el.from[0] = [double]$el.from[0] - 0.35
            $el.to[0] = [double]$el.to[0] + 0.35
        }
    }

    $def.elements = $kept
    $json = $def | ConvertTo-Json -Depth 20
    $outPath = Join-Path $outDir "$name.json"
    Set-Content -LiteralPath $outPath -Value $json -Encoding utf8 -NoNewline
    Write-Output ("{0,-28} 原 {1} 元素 -> 预览 {2} 元素（已去除锯齿）" -f $name, 25, $kept.Count)
}

Write-Output ''
Write-Output "完成：预览模型输出到 tools\preview-models\（正式 assets 未改动）"
