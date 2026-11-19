'use strict';
// ---------------------------------------------------------------------------
// 一次性迁移脚本：把反编译恢复出来的「巨类」WorkerHandler（898 行 / 48 方法）
// 按职责拆成若干职责单一的类。
//
// 做法要点：
//  1. 方法体原样搬运，一个字都不改（避免引入行为差异）。
//  2. 搬出去的类统一加静态导入（import static），这样方法体里原有的裸调用
//     （例如 farm() 里调用 takeAndUseTool()）无需改写即可跨类解析。
//  3. 为了让静态导入可用，被搬方法的 private 改为包内可见；
//     常量同理从 private static final 改为 static final。
//  4. 每个新文件沿用原文件的整套 import（未用到的导入只会警告，不会报错）。
//
// 用法：node tools/ops/split-worker.js
// ---------------------------------------------------------------------------

const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..', '..');
const SRC = path.join(ROOT, 'src', 'main', 'java', 'com', 'heishan', 'jiangshidao', 'worker', 'WorkerHandler.java');
const PKG = 'com.heishan.jiangshidao.worker';

// 方法 → 目标类
const GROUPS = {
  WorkerFarming: ['farm', 'plantCrop', 'boostCropGrowth', 'boostSaplings', 'plantSapling',
    'plantSaplingsFromBackpack', 'isPlantableGround', 'nearestBlock'],
  WorkerFishing: ['fish', 'fishingBonus', 'enchantLevel', 'equipRodFromStorage', 'swapIntoMainHand',
    'findFishingRod', 'rollVanillaFishing', 'randomTreasure', 'hasWaterNearby',
    'containsFishingRod', 'randomFish'],
  WorkerWoodcutting: ['chopWood', 'findChoppingTarget', 'isFlyingWorker', 'trunkBaseY', 'trunkTopY',
    'fellLeaves', 'clearBlockingLeaves'],
  WorkerMining: ['mine'],
  WorkerTools: ['takeAndUseTool', 'takeAnyTool', 'useUp'],
  WorkerStorage: ['deposit', 'flushBackpack', 'mergeIntoBackpack', 'usedSlots',
    'insertIntoContainers', 'tryPut'],
};

// 留在 WorkerHandler 的主干
const KEEP = ['tickLevel', 'tickWorker', 'hint', 'runJob', 'workInterval', 'canFarmAt',
  'canOperateAt', 'walkTo', 'returnHomeIfFar', 'backpackOf'];

const text = fs.readFileSync(SRC, 'utf8');
const lines = text.split(/\r?\n/);

// ---- 1) 收集导入区与类体范围 ----
const importEnd = lines.findIndex((l) => l.startsWith('public final class WorkerHandler'));
if (importEnd < 0) throw new Error('未找到类声明');
const imports = lines.slice(0, importEnd).filter((l) => l.startsWith('import '));
const classLine = lines[importEnd];
const bodyEnd = lines.length - 1; // 最后一行是类的右花括号

// ---- 2) 按缩进 3 空格的方法签名定位方法块（花括号配平）----
const methods = new Map();
for (let i = importEnd + 1; i < bodyEnd; i++) {
  const m = /^ {3}(?:public |private |protected )?(?:static )?[\w<>\[\],.?\s]+\s(\w+)\([^;]*$/.exec(lines[i]);
  if (!m) continue;
  if (/^\s*\/\//.test(lines[i])) continue;
  // 向下找到与起始同缩进的 '   }'
  let depth0 = 0;
  let end = -1;
  for (let j = i; j < bodyEnd; j++) {
    for (const ch of lines[j]) {
      if (ch === '{') depth0++;
      else if (ch === '}') depth0--;
    }
    if (depth0 === 0 && j > i) { end = j; break; }
  }
  if (end < 0) continue;
  methods.set(m[1], { start: i, end });
  i = end;
}

// ---- 3) 组装各文件 ----
const used = new Set();
const owners = new Map();
for (const [cls, names] of Object.entries(GROUPS)) {
  for (const n of names) {
    if (!methods.has(n)) throw new Error(`方法不存在: ${n}`);
    owners.set(n, cls);
    used.add(n);
  }
}
for (const n of KEEP) {
  if (!methods.has(n)) throw new Error(`主干方法不存在: ${n}`);
  owners.set(n, 'WorkerHandler');
  used.add(n);
}

function bodyOf(names) {
  return names.map((n) => {
    const { start, end } = methods.get(n);
    return lines.slice(start, end + 1).map((l) => l.replace(/^( {3})(private )?static /, '   static ')).join('\n');
  }).join('\n\n');
}

const staticImports = ['WorkerHandler', 'WorkerFarming', 'WorkerFishing', 'WorkerWoodcutting',
  'WorkerMining', 'WorkerTools', 'WorkerStorage']
  .map((c) => `import static ${PKG}.${c}.*;`).join('\n');

const HEADER = `${imports.join('\n')}\n\n// 拆分后的跨类静态导入：方法体保持原样，裸调用在此解析。\n${staticImports}`;

// 3a) 新类
for (const [cls, names] of Object.entries(GROUPS)) {
  const content = [
    `package ${PKG};`,
    '',
    HEADER,
    '',
    `/**`,
    ` * 工人任务：${describe(cls)}。`,
    ` * 由 {@link WorkerHandler} 调度，方法体自 WorkerHandler 原样拆出。`,
    ` */`,
    `final class ${cls} {`,
    `   private ${cls}() {`,
    `   }`,
    '',
    bodyOf(names),
    `}`,
    '',
  ].join('\n');
  fs.writeFileSync(path.join(ROOT, 'src', 'main', 'java', 'com', 'heishan', 'jiangshidao', 'worker', `${cls}.java`), content, 'utf8');
  console.log(`写入 ${cls}.java（${names.length} 个方法）`);
}

// 3b) 重写 WorkerHandler：只保留主干
const kept = lines.slice(importEnd, bodyEnd + 1);
const keepBlocks = [];
const constants = [];
let pendingConst = [];
for (let i = importEnd + 1; i < bodyEnd; i++) {
  const line = lines[i];
  if (/^ {3}(public |private )?static final /.test(line) || /^ {3}private static final /.test(line)) {
    constants.push(line.replace(/^ {3}private static final /, '   static final '));
    continue;
  }
  if (methods.has([...methods.keys()].find((k) => methods.get(k).start === i))) {
    const name = [...methods.keys()].find((k) => methods.get(k).start === i);
    if (KEEP.includes(name)) keepBlocks.push(lines.slice(i, methods.get(name).end + 1).join('\n'));
    i = methods.get(name).end;
  }
}

const handler = [
  `package ${PKG};`,
  '',
  HEADER,
  '',
  `/**`,
  ` * 工人调度核心：负责扫描、节流、派活与家用回归。`,
  ` * <p>具体任务实现已按职责拆到 WorkerFarming / WorkerFishing / WorkerWoodcutting /`,
  ` * WorkerMining，工具取用与仓储投递拆到 WorkerTools / WorkerStorage。`,
  ` *`,
  ` * <p>作者：黑山大叔 —— https://space.bilibili.com/19428259`,
  ` */`,
  classLine,
  '',
  ...constants,
  `   private WorkerHandler() {`,
  `   }`,
  '',
  ...keepBlocks.map((b) => b + '\n'),
].join('\n') + '}\n';

fs.writeFileSync(SRC, handler, 'utf8');
console.log(`重写 WorkerHandler.java（保留 ${keepBlocks.length} 个主干方法 + ${constants.length} 个常量）`);

function describe(cls) {
  return {
    WorkerFarming: '种植、催熟与作物收割',
    WorkerFishing: '垂钓、附魔加成与战利品表',
    WorkerWoodcutting: '伐木、树叶清理与树苗补种',
    WorkerMining: '采矿',
    WorkerTools: '工具取用与耐久消耗',
    WorkerStorage: '背包与容器之间的存取投递',
  }[cls] || '';
}
