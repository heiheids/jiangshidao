'use strict';
// ---------------------------------------------------------------------------
// 为所有公开类型补统一的类级 Javadoc（含作者信息）。
//
// 规则：
//  · 已存在类级 Javadoc 的文件跳过（不覆盖人工注释）。
//  · 只在类型声明正上方插入一段，保持原有缩进与空行风格。
//  · 幂等：重复执行不会重复插入。
//
// 用法：node tools/ops/add-author-javadoc.js
// ---------------------------------------------------------------------------

const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..', '..');
const SRC = path.join(ROOT, 'src', 'main', 'java');

const AUTHOR = [
  ' * <p>作者：黑山大叔',
  ' * <br>主页：https://space.bilibili.com/19428259',
  ' * <br>源码：https://github.com/heiheids',
];

function walk(dir, acc = []) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) walk(p, acc);
    else if (e.isFile() && e.name.endsWith('.java')) acc.push(p);
  }
  return acc;
}

let changed = 0;
let skipped = 0;

for (const file of walk(SRC)) {
  const text = fs.readFileSync(file, 'utf8');
  const lines = text.split(/\r?\n/);

  // 找公开类型声明行（顶层，缩进 0）
  const decl = lines.findIndex((l) => /^(public |final |abstract )*(class|record|enum|interface)\s+\w+/.test(l) || /^public (final )?(class|record|enum|interface)\s+\w+/.test(l));
  if (decl < 0) continue;

  // 判断上方是否已有 Javadoc
  let j = decl - 1;
  while (j >= 0 && lines[j].trim() === '') j--;
  if (j >= 0 && lines[j].trim().endsWith('*/')) { skipped++; continue; }

  const name = (/^(?:public |final |abstract )*(?:class|record|enum|interface)\s+(\w+)/.exec(lines[decl]) || [])[1] || '类型';
  const desc = describe(name);
  const block = [
    '/**',
    ` * ${desc}`,
    ...AUTHOR,
    ' */',
  ];

  lines.splice(decl, 0, ...block);
  fs.writeFileSync(file, lines.join('\n'), 'utf8');
  changed++;
}

console.log(`已补作者信息：${changed} 个文件；跳过（已有类级注释）：${skipped} 个`);

function describe(name) {
  // 依据命名给一句朴素但准确的定位说明，避免硬编码文案。
  if (/BlockEntity$/.test(name)) return `${name} —— 方块实体，负责该方块的状态与刻逻辑。`;
  if (/Block$/.test(name)) return `${name} —— 方块定义与交互行为。`;
  if (/Item$/.test(name)) return `${name} —— 物品行为（使用、命中、提示等）。`;
  if (/Entity$/.test(name)) return `${name} —— 实体定义、刻逻辑与战斗行为。`;
  if (/Renderer$/.test(name)) return `${name} —— 客户端渲染器。`;
  if (/Screen$/.test(name)) return `${name} —— 客户端界面。`;
  if (/Overlay$/.test(name)) return `${name} —— HUD 覆盖层绘制。`;
  if (/Menu$/.test(name)) return `${name} —— 容器菜单数据与槽位布局。`;
  if (/Handler$/.test(name)) return `${name} —— 事件/任务处理。`;
  if (/Mixin$/.test(name)) return `${name} —— 混入实现，改写原版行为。`;
  if (/Payload$/.test(name)) return `${name} —— 网络包体定义。`;
  if (/Registry|^Reg$/.test(name)) return `${name} —— 注册相关。`;
  if (/Data$|^Talent/.test(name)) return `${name} —— 数据模型。`;
  if (/Cache/.test(name)) return `${name} —— 客户端缓存。`;
  return `${name}。`;
}
