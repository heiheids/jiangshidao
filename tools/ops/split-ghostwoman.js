'use strict';
// ---------------------------------------------------------------------------
// 一次性迁移脚本：把 GhostWomanEntity（447 行）内部的 5 个静态嵌套类
// 抽成同包下的独立类，实体类只保留自身逻辑。
//
// 与 split-worker.js 同样的原则：代码原样搬移，仅调整访问修饰符
// （private static final class → final class，包内可见）。
//
// 用法：node tools/ops/split-ghostwoman.js
// ---------------------------------------------------------------------------

const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..', '..');
const DIR = path.join(ROOT, 'src', 'main', 'java', 'com', 'heishan', 'jiangshidao', 'entity');
const SRC = path.join(DIR, 'GhostWomanEntity.java');
const PKG = 'com.heishan.jiangshidao.entity';

const text = fs.readFileSync(SRC, 'utf8');
const lines = text.split(/\r?\n/);
const importEnd = lines.findIndex((l) => l.startsWith('public class GhostWomanEntity'));
if (importEnd < 0) throw new Error('未找到类声明');
const imports = lines.slice(0, importEnd).filter((l) => l.startsWith('import '));

// 收集嵌套类块（缩进 3 空格的 class 声明）
const inner = [];
for (let i = importEnd + 1; i < lines.length; i++) {
  const m = /^ {3}(?:public |private |protected )?(?:static )?(?:final )?class (\w+)/.exec(lines[i]);
  if (!m) continue;
  let depth = 0;
  let end = -1;
  for (let j = i; j < lines.length; j++) {
    for (const ch of lines[j]) {
      if (ch === '{') depth++;
      else if (ch === '}') depth--;
    }
    if (depth === 0 && j > i) { end = j; break; }
  }
  if (end < 0) continue;
  inner.push({ name: m[1], start: i, end });
  i = end;
}

const DESCR = {
  GhostChargeGoal: '鬼女冲锋目标：接近玩家并攻击',
  GhostMoveControl: '鬼女移动控制：贴地/飞行切换与减速处理',
  GhostRandomMoveGoal: '鬼女随机游走目标',
  GhostTargetPlayerGoal: '鬼女索敌目标：按可见性与半径锁定玩家',
  GhostWomanGoalStats: '鬼女 AI 运行统计（调试用计数）',
};

// 每个类型是否需要 public（被实体公开 API 返回的要 public）
const PUBLIC = new Set(['GhostWomanGoalStats']);

for (const item of inner) {
  const body = lines.slice(item.start, item.end + 1)
    .map((l) => l
      .replace(/^ {3}(?:private |public )?(?:static )?(?:final )?class /, PUBLIC.has(item.name) ? 'public final class ' : 'final class ')
    )
    .join('\n');
  const content = [
    `package ${PKG};`,
    '',
    ...imports,
    '',
    '/**',
    ` * ${DESCR[item.name] || ''}`,
    ' * <p>自 {@link GhostWomanEntity} 原样抽出。',
    ' */',
    body,
    '',
  ].join('\n');
  fs.writeFileSync(path.join(DIR, `${item.name}.java`), content, 'utf8');
  console.log(`写入 ${item.name}.java（${item.end - item.start + 1} 行）`);
}

// 从实体类中删除这些块
const keep = [];
let skipTo = -1;
for (let i = 0; i < lines.length; i++) {
  if (i <= skipTo) continue;
  const hit = inner.find((x) => x.start === i);
  if (hit) {
    skipTo = hit.end;
    continue;
  }
  keep.push(lines[i]);
}

let out = keep.join('\n');
// 内部类搬走后，实体里对它们的引用需要保留可解析：静态嵌套类改为同包顶层类，名字不变，无需改写。
// 但 public static GhostWomanEntity.GhostWomanGoalStats 需去掉外层限定
out = out.replace(/GhostWomanEntity\.GhostWomanGoalStats/g, 'GhostWomanGoalStats');
fs.writeFileSync(SRC, out, 'utf8');
console.log(`重写 GhostWomanEntity.java（${out.split('\n').length} 行）`);
