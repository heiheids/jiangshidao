# 纯阳巾实现静态校验器
#
# 把"编译能过、但运行时功能不存在"的那类问题逐项交叉检查：
# 物品定义 → 模型 → 贴图 → 配方 → 语言键 → 代码常量 → 混入配置 → 打包产物。
#
# 用法：pwsh -NoProfile -File tools\ops\validate-chunyang.ps1
#
# 作者：黑山大叔 / https://space.bilibili.com/19428259 / https://github.com/heiheids

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem

$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$assets = Join-Path $root 'src\main\resources\assets\jiangshidao'
$data = Join-Path $root 'src\main\resources\data\jiangshidao'
$java = Join-Path $root 'src\main\java\com\heishan\jiangshidao'

$pass = 0
$fail = 0

function Check($name, $ok, $detail) {
    if ($ok) {
        $script:pass++
        Write-Output ("  [PASS] {0}{1}" -f $name, $(if ($detail) { "  ($detail)" } else { "" }))
    } else {
        $script:fail++
        Write-Output ("  [FAIL] {0}{1}" -f $name, $(if ($detail) { "  ($detail)" } else { "" }))
    }
}

Write-Output "=== 纯阳巾静态校验 ==="

# 1. 物品定义
Write-Output "`n[1] 物品定义"
$itemDefPath = Join-Path $assets 'items\chunyang_jin.json'
Check 'items/chunyang_jin.json 存在' (Test-Path $itemDefPath)
if (Test-Path $itemDefPath) {
    $itemDef = Get-Content -LiteralPath $itemDefPath -Raw | ConvertFrom-Json
    Check '定义含 model.type' ($itemDef.model.type -eq 'minecraft:model') $itemDef.model.type
    Check '定义指向 item/chunyang_jin' ($itemDef.model.model -eq 'jiangshidao:item/chunyang_jin') $itemDef.model.model
}

# 2. 模型
Write-Output "`n[2] 模型"
$modelPath = Join-Path $assets 'models\item\chunyang_jin.json'
Check 'models/item/chunyang_jin.json 存在' (Test-Path $modelPath)
if (Test-Path $modelPath) {
    $model = Get-Content -LiteralPath $modelPath -Raw | ConvertFrom-Json
    Check '模型可解析' ($null -ne $model)
    Check 'elements 非空' ($model.elements.Count -gt 0) ("{0} 个长方体" -f $model.elements.Count)
    Check '含 parent 字段' ($null -ne $model.parent) $model.parent

    $badBounds = 0
    $badTexture = 0
    foreach ($e in $model.elements) {
        for ($k = 0; $k -lt 3; $k++) {
            if ([double]$e.from[$k] -gt [double]$e.to[$k]) { $badBounds++ }
        }
        foreach ($faceName in $e.faces.PSObject.Properties.Name) {
            $ref = $e.faces.$faceName.texture
            if ($ref -eq '#all' -and -not $model.textures.all) { $badTexture++ }
        }
    }
    Check 'from <= to 全部成立' ($badBounds -eq 0) ("违规 {0} 处" -f $badBounds)
    Check '面贴图引用可解析' ($badTexture -eq 0) ("违规 {0} 处" -f $badTexture)
    Check 'textures.all 已定义' ($null -ne $model.textures.all) $model.textures.all
}

# 3. 贴图
Write-Output "`n[3] 贴图"
$texPath = Join-Path $assets 'textures\item\chunyang_jin.png'
Check 'textures/item/chunyang_jin.png 存在' (Test-Path $texPath)
if (Test-Path $texPath) {
    Add-Type -AssemblyName System.Drawing
    $img = [System.Drawing.Image]::FromFile((Resolve-Path $texPath))
    Check '贴图尺寸 64x64' (($img.Width -eq 64) -and ($img.Height -eq 64)) ("{0}x{1}" -f $img.Width, $img.Height)
    $img.Dispose()
}

# 4. 配方
Write-Output "`n[4] 配方"
$recipePath = Join-Path $data 'recipe\chunyang_jin.json'
Check 'recipe/chunyang_jin.json 存在' (Test-Path $recipePath)
if (Test-Path $recipePath) {
    $recipe = Get-Content -LiteralPath $recipePath -Raw | ConvertFrom-Json
    Check 'type 为 crafting_shaped' ($recipe.type -eq 'minecraft:crafting_shaped') $recipe.type
    Check '3x3 图案' ($recipe.pattern.Count -eq 3) ($recipe.pattern -join '/')
    $flat = ($recipe.pattern -join '')
    Check '外圈 8 个线' (($flat.ToCharArray() | Where-Object { $_ -eq 'S' }).Count -eq 8)
    Check '中心 1 个金锭' ($flat[4] -eq 'G')
    Check 'key.S = minecraft:string' ($recipe.key.S -eq 'minecraft:string') $recipe.key.S
    Check 'key.G = minecraft:gold_ingot' ($recipe.key.G -eq 'minecraft:gold_ingot') $recipe.key.G
    Check '产物为 jiangshidao:chunyang_jin' ($recipe.result.id -eq 'jiangshidao:chunyang_jin') $recipe.result.id
}

# 5. 语言键
Write-Output "`n[5] 语言键"
$needed = @(
    'item.jiangshidao.chunyang_jin',
    'tooltip.jiangshidao.chunyang_jin.desc',
    'tooltip.jiangshidao.chunyang_jin.effect.reveal',
    'tooltip.jiangshidao.chunyang_jin.effect.loot',
    'tooltip.jiangshidao.chunyang_jin.effect.peace',
    'tooltip.jiangshidao.chunyang_jin.wear.progress',
    'tooltip.jiangshidao.chunyang_jin.wear.ready',
    'tooltip.jiangshidao.chunyang_jin.advice'
)
foreach ($langName in @('zh_cn', 'en_us')) {
    $lang = Get-Content -LiteralPath (Join-Path $assets "lang\$langName.json") -Raw
    $missing = @()
    foreach ($k in $needed) {
        if ($lang -notmatch [regex]::Escape("`"$k`"")) { $missing += $k }
    }
    Check "$langName 键齐全" ($missing.Count -eq 0) $(if ($missing.Count -eq 0) { "{0} 个" -f $needed.Count } else { "缺: " + ($missing -join ', ') })
}

# 6. 代码常量
Write-Output "`n[6] 代码常量"
$itemJava = Get-Content -LiteralPath (Join-Path $java 'item\ChunyangJinItem.java') -Raw
Check 'ACTIVATE_DAYS = 10' ($itemJava -match 'ACTIVATE_DAYS = 10;')
Check 'TICKS_PER_DAY = 24000' ($itemJava -match 'TICKS_PER_DAY = 24000;')
Check 'REVEAL_RADIUS = 25.0' ($itemJava -match 'REVEAL_RADIUS = 25\.0;')
Check '测试门槛默认不由常量强制' ($itemJava -match 'readIntProperty\("jiangshidao\.chunyang\.activate", 0\)')
Check '门槛默认值为 10 游戏日' ($itemJava -match 'TEST_ACTIVATE_TICKS > 0 \? TEST_ACTIVATE_TICKS : ACTIVATE_TICKS')
$slotJava = Get-Content -LiteralPath (Join-Path $java 'accessory\AccessorySlot.java') -Raw
Check 'HEAD 槽接受纯阳巾' ($slotJava -match 'case HEAD -> stack\.getItem\(\) instanceof ChunyangJinItem;')

# 7. 混入配置
Write-Output "`n[7] 混入配置"
$mixinCfg = Get-Content -LiteralPath (Join-Path $root 'src\main\resources\jiangshidao.mixins.json') -Raw
Check '含双倍掉落混入' ($mixinCfg -match 'LivingEntityDoubleDropMixin')
Check '含亡灵抑制混入' ($mixinCfg -match 'MobTargetSuppressMixin')

# 8. 打包产物
Write-Output "`n[8] 打包产物"
$jarPath = Join-Path $root 'build\libs\jiangshidao-2.1.0.jar'
Check 'jar 已构建' (Test-Path $jarPath)
if (Test-Path $jarPath) {
    $zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path $jarPath))
    $required = @(
        'com/heishan/jiangshidao/item/ChunyangJinItem.class',
        'com/heishan/jiangshidao/event/ChunyangJinHandler.class',
        'com/heishan/jiangshidao/client/HeadwearRenderer.class',
        'com/heishan/jiangshidao/client/HeadwearRenderData.class',
        'com/heishan/jiangshidao/mixin/LivingEntityDoubleDropMixin.class',
        'com/heishan/jiangshidao/mixin/MobTargetSuppressMixin.class',
        'assets/jiangshidao/items/chunyang_jin.json',
        'assets/jiangshidao/models/item/chunyang_jin.json',
        'assets/jiangshidao/textures/item/chunyang_jin.png',
        'data/jiangshidao/recipe/chunyang_jin.json'
    )
    $lost = @()
    foreach ($n in $required) {
        if (-not $zip.GetEntry($n)) { $lost += $n }
    }
    Check 'jar 内 10 项产物齐全' ($lost.Count -eq 0) $(if ($lost.Count -eq 0) { '全部存在' } else { '缺: ' + ($lost -join ', ') })
    $zip.Dispose()
}

Write-Output "`n=== 结果：通过 $pass 项，失败 $fail 项 ==="
if ($fail -gt 0) { exit 1 }
