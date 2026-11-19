# 纯阳巾：把预览模型（tools/models/*.model.json）转换成 Minecraft 物品模型 JSON
#
# 预览模型坐标系：y 向上、正面朝 -z、原点在冠底中心。
# Minecraft 物品模型坐标系：y 向上、0..16、模型以 (8,8) 为中心。
# 因此 x/z 平移 +8，y 保持不变（都是向上）。冠底贴在 y=0。
#
# 每个长方体的 6 个面共用贴图 "all"，UV 按面的实际尺寸换算，保证纹理不拉伸。
#
# 作者：黑山大叔 / https://space.bilibili.com/19428259 / https://github.com/heiheids

param(
    [string]$Model = 'tools\models\chunyang_jin.model.json',
    [string]$Out = 'src\main\resources\assets\jiangshidao\models\item\chunyang_jin.json'
)

$ErrorActionPreference = 'Stop'

$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$modelPath = Join-Path $root $Model
$outPath = Join-Path $root $Out
$outDir = Split-Path $outPath -Parent
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }

$def = Get-Content -LiteralPath $modelPath -Raw -Encoding utf8 | ConvertFrom-Json

# 贴图尺寸（与 gen-texture.ps1 必须一致）
$TS = 64

function Round2($v) { return [math]::Round([double]$v, 3) }

# 面 -> (u 轴来源, v 轴来源)，用于按实际尺寸取 UV
# north(-z)/south(+z): u=x, v=y ; east(+x)/west(-x): u=z, v=y ; up(+y)/down(-y): u=x, v=z
$FACE_DEFS = [ordered]@{
    north = @('x', 'y')
    south = @('x', 'y')
    east  = @('z', 'y')
    west  = @('z', 'y')
    up    = @('x', 'z')
    down  = @('x', 'z')
}

# 把每个长方体的一个面映射到贴图上的固定区域（按序号排列，避免重叠）
$slots = @()
$cellW = 16
$cellH = 8
$index = 0

$elements = @()
foreach ($e in $def.elements) {
    $x1 = Round2 $e.from[0]; $y1 = Round2 $e.from[1]; $z1 = Round2 $e.from[2]
    $x2 = Round2 $e.to[0];   $y2 = Round2 $e.to[1];   $z2 = Round2 $e.to[2]

    $sizes = @{
        x = [math]::Abs($x2 - $x1)
        y = [math]::Abs($y2 - $y1)
        z = [math]::Abs($z2 - $z1)
    }

    # 该长方体在贴图上的格子
    $col = $index % 4
    $row = [math]::Floor($index / 4)
    $u0 = $col * $cellW
    $v0 = $row * $cellH
    $index++

    $faces = @{}
    foreach ($face in $FACE_DEFS.Keys) {
        $axes = $FACE_DEFS[$face]
        $w = $sizes[$axes[0]]
        $h = $sizes[$axes[1]]
        if ($w -le 0) { $w = 0.5 }
        if ($h -le 0) { $h = 0.5 }

        # 按面尺寸映射到格子内，最小 0.5 像素避免退化
        $uw = [math]::Min($cellW, $w * 2)
        $vh = [math]::Min($cellH, $h * 2)
        $faces[$face] = @{
            uv = @($u0, $v0, ($u0 + $uw), ($v0 + $vh))
            texture = '#all'
        }
    }

    $elements += [ordered]@{
        name = $e.name
        from = @(($x1 + 8.0), $y1, ($z1 + 8.0))
        to   = @(($x2 + 8.0), $y2, ($z2 + 8.0))
        faces = $faces
    }
}

$model = [ordered]@{
    credit = '作者：黑山大叔  主页：https://space.bilibili.com/19428259  GitHub：https://github.com/heiheids'
    textures = [ordered]@{
        all = 'jiangshidao:item/chunyang_jin'
        particle = 'jiangshidao:item/chunyang_jin'
    }
    elements = $elements
    display = [ordered]@{
        gui = [ordered]@{ rotation = @(0, 0, 0); translation = @(0, 0, 0); scale = @(0.72, 0.72, 0.72) }
        ground = [ordered]@{ rotation = @(0, 0, 0); translation = @(0, 3, 0); scale = @(0.3, 0.3, 0.3) }
        fixed = [ordered]@{ rotation = @(0, 0, 0); translation = @(0, 0, 0); scale = @(1.0, 1.0, 1.0) }
        thirdperson_righthand = [ordered]@{ rotation = @(0, 0, 0); translation = @(0, 2, 0); scale = @(0.5, 0.5, 0.5) }
        firstperson_righthand = [ordered]@{ rotation = @(0, 0, 0); translation = @(0, 0, 0); scale = @(0.5, 0.5, 0.5) }
    }
}

# 手动构造 JSON 文本：PowerShell 的 ConvertTo-Json 无法序列化嵌套 OrderedDictionary，
# 会把整个对象压成一个类型名字符串，因此这里逐字段拼接。
# 手动拼接 JSON：PowerShell 的 ConvertTo-Json 无法序列化嵌套 OrderedDictionary，
# 会把整个对象压成一个类型名字符串，因此这里逐字段构造。
$inv = [System.Globalization.CultureInfo]::InvariantCulture
$sb = New-Object System.Text.StringBuilder
[void]$sb.Append("{`n")
[void]$sb.Append('  "credit": "作者：黑山大叔  主页：https://space.bilibili.com/19428259  GitHub：https://github.com/heiheids",' + "`n")
[void]$sb.Append('  "parent": "minecraft:block/block",' + "`n")
[void]$sb.Append('  "textures": { "all": "jiangshidao:item/chunyang_jin", "particle": "jiangshidao:item/chunyang_jin" },' + "`n")
[void]$sb.Append('  "elements": [' + "`n")

$i = 0
foreach ($e in $def.elements) {
    $x1 = [math]::Round([double]$e.from[0] + 8, 3)
    # 物品模型 y_mc=0 在「上方」（Y 轴向下），其内部映射到姿态坐标为 pose_y=(8-y_mc)/16；
    # 渲染层又做 scale(-1,-1,1) 翻转，最终 pose_y=(y_mc-8)/16 —— 即 y_mc 越大越靠上。
    # 预览模型 y 向上，所以要得到正确的上下方向必须用 y_mc = 8 + y_preview：
    #   冠顶 y_preview=12.65 → y_mc=20.65 → pose +0.79（朝上）
    #   巾带 y_preview=-4.4 → y_mc=3.6   → pose -0.28（朝下垂到脑后）
    # 注意：y_mc 会超过 16，这是允许的（模型坐标不限于 0..16）。
    $y1 = [math]::Round(8 + [double]$e.from[1], 3)
    $z1 = [math]::Round([double]$e.from[2] + 8, 3)
    $x2 = [math]::Round([double]$e.to[0] + 8, 3)
    $y2 = [math]::Round(8 + [double]$e.to[1], 3)
    $z2 = [math]::Round([double]$e.to[2] + 8, 3)

    # 贴图 64x64 = 8列 x 8行 个 8x8 像素格，共 64 格（元素 35 个足够）。
    # Minecraft 的 UV 空间默认是 0..16，1 UV 单位 = 4 像素，所以 8 像素 = 2 UV 单位。
    # 之前把像素坐标（0..64）直接当 UV 用，超出 0..16 空间，导致整张贴图采样错位。
    $col = $i % 8
    $row = [math]::Floor($i / 8)
    $u0 = $col * 2
    $v0 = $row * 2

    $faces = @()
    foreach ($face in @('north', 'south', 'east', 'west', 'up', 'down')) {
        $faces += ('        "' + $face + '": { "uv": [' + $u0 + ', ' + $v0 + ', ' + ($u0 + 2) + ', ' + ($v0 + 2) + '], "texture": "#all" }')
    }

    [void]$sb.Append("    {`n")
    [void]$sb.Append('      "name": "' + $e.name + '",' + "`n")
    [void]$sb.Append('      "from": [' + $x1.ToString($inv) + ', ' + $y1.ToString($inv) + ', ' + $z1.ToString($inv) + '],' + "`n")
    [void]$sb.Append('      "to": [' + $x2.ToString($inv) + ', ' + $y2.ToString($inv) + ', ' + $z2.ToString($inv) + '],' + "`n")
    [void]$sb.Append("      `"faces`": {`n")
    [void]$sb.Append(($faces -join ",`n") + "`n")
    [void]$sb.Append("      }`n")
    $tail = if ($i -lt $def.elements.Count - 1) { "," } else { "" }
    [void]$sb.Append("    }" + $tail + "`n")
    $i++
}

[void]$sb.Append("  ],`n")
[void]$sb.Append("  `"display`": {`n")
[void]$sb.Append('    "gui": { "rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.72, 0.72, 0.72] },' + "`n")
[void]$sb.Append('    "ground": { "rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.3, 0.3, 0.3] },' + "`n")
[void]$sb.Append('    "fixed": { "rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1.0, 1.0, 1.0] },' + "`n")
[void]$sb.Append('    "thirdperson_righthand": { "rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5] },' + "`n")
[void]$sb.Append('    "firstperson_righthand": { "rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5] }' + "`n")
[void]$sb.Append("  }`n")
[void]$sb.Append("}`n")

Set-Content -LiteralPath $outPath -Value $sb.ToString() -Encoding utf8 -NoNewline
Write-Output ("已生成模型: {0}（{1} 个长方体）" -f $Out, $def.elements.Count)