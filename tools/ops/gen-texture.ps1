# 纯阳巾：生成 64x64 物品贴图
#
# 与 gen-model.ps1 共用同一套网格分配规则：每个长方体占一个 16x8 的格子，
# 按序号从左上到右下排布。格子里画该长方体的底色 + 一圈深色描边 + 顶部高光，
# 保持像素质感（无渐变、无抗锯齿），与模组其他贴图风格一致。
#
# 作者：黑山大叔 / https://space.bilibili.com/19428259 / https://github.com/heiheids

param(
    [string]$Model = 'tools\models\chunyang_jin.model.json',
    [string]$Out = 'src\main\resources\assets\jiangshidao\textures\item\chunyang_jin.png'
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$modelPath = Join-Path $root $Model
$outPath = Join-Path $root $Out
$outDir = Split-Path $outPath -Parent
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }

$def = Get-Content -LiteralPath $modelPath -Raw -Encoding utf8 | ConvertFrom-Json

$TS = 64
$cellW = 8
$cellH = 8

$bmp = New-Object System.Drawing.Bitmap($TS, $TS)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$g.Clear([System.Drawing.Color]::FromArgb(255, 32, 28, 48))

function Convert-Color($hex) {
    $h = $hex.TrimStart('#')
    return [System.Drawing.Color]::FromArgb(
        255,
        [Convert]::ToInt32($h.Substring(0, 2), 16),
        [Convert]::ToInt32($h.Substring(2, 2), 16),
        [Convert]::ToInt32($h.Substring(4, 2), 16))
}

function Shade($c, $factor) {
    $r = [int][math]::Min(255, [math]::Max(0, $c.R * $factor))
    $gg = [int][math]::Min(255, [math]::Max(0, $c.G * $factor))
    $b = [int][math]::Min(255, [math]::Max(0, $c.B * $factor))
    return [System.Drawing.Color]::FromArgb(255, $r, $gg, $b)
}

$index = 0
foreach ($e in $def.elements) {
    $base = Convert-Color $e.color
    $col = $index % 8
    $row = [math]::Floor($index / 8)
    $x0 = $col * $cellW
    $y0 = $row * $cellH
    $index++

    # 底色填充
    $fill = New-Object System.Drawing.SolidBrush($base)
    $g.FillRectangle($fill, $x0, $y0, $cellW, $cellH)
    $fill.Dispose()

    # 顶部一行提亮，底部一行压暗，制造柱面感
    $hi = New-Object System.Drawing.SolidBrush((Shade $base 1.22))
    $g.FillRectangle($hi, $x0, $y0, $cellW, 1)
    $hi.Dispose()
    $lo = New-Object System.Drawing.SolidBrush((Shade $base 0.74))
    $g.FillRectangle($lo, $x0, ($y0 + $cellH - 1), $cellW, 1)
    $lo.Dispose()

    # 左右各一列描边，贴图拼缝处不会糊成一片
    $edge = New-Object System.Drawing.SolidBrush((Shade $base 0.55))
    $g.FillRectangle($edge, $x0, $y0, 1, $cellH)
    $g.FillRectangle($edge, ($x0 + $cellW - 1), $y0, 1, $cellH)
    $edge.Dispose()
}

$g.Dispose()
$bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()

Write-Output ("已生成贴图: {0}  ({1}x{1}, {2} 个格块)" -f $Out, $TS, $index)