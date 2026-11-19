'use strict';
// ---------------------------------------------------------------------------
// 共享模型定义 —— 清代官服僵尸（Jiangshi / Qing-official zombie）
//
// 单位：1 unit = 1 纹理像素（16 单位 = 1 方块）。
// 坐标系：y 轴向上（与 Minecraft ModelPart 一致），原点位于双脚中心地面，
//         +Z 为模型正前方，+X 为模型自身左侧。
//
// 该文件同时服务于：
//   ① 3D 预览渲染（tools/render）
//   ② 后续 Java 版 EntityModel 生成器（同一套数据，避免两处维护）
// ---------------------------------------------------------------------------

const MATERIALS = ['skin', 'skinDark', 'clothTeal', 'clothRed', 'clothDark', 'gold', 'hatRed'];

const rad = (d) => (d * Math.PI) / 180;

/**
 * @param {string} id
 * @param {string} part   head / body / armL / armR / legL / legR
 * @param {number[]} min
 * @param {number[]} max
 * @param {string} mat
 * @param {object} [opt]  { uvMode, tag, xform }
 */
function box(id, part, min, max, mat, opt = {}) {
  return { id, part, min, max, mat, uvMode: opt.uvMode || 'tile', tag: opt.tag || '', xform: opt.xform || null };
}

// ---------------------------------------------------------------------------
// 手臂：局部坐标里自然下垂建盒，再绕肩 pivot 整体前伸 + 略微内收。
// 局部：原点=肩关节，-Y=手臂延伸方向（前伸后指向世界 +Z），+Z=世界 +Y。
// ---------------------------------------------------------------------------
const ARM_PITCH = -84;   // 绕 +X 轴向 +Z 前伸（-90 = 完全水平）
const ARM_INWARD = 1.5;  // 绕 Y 轴轻微内收
const SHOULDER_Y = 26.4;
const SHOULDER_X = 5.6;

function armBoxes(side) {
  const prefix = side < 0 ? 'R_' : 'L_';
  const part = side < 0 ? 'armR' : 'armL';
  const pivot = [side * SHOULDER_X, SHOULDER_Y, 0];
  const xf = { pivot, rot: [ARM_PITCH, side * ARM_INWARD, 0] };
  const xo = (a) => side * a;

  const L = [
    // [x0, x1, y0, y1, z0, z1, mat, id]
    [2.9, 7.1, -10.4, 0.0, -2.9, 2.9, 'clothTeal', 'sleeve'],
    [2.8, 7.2, -10.0, -8.3, -3.1, 3.1, 'clothRed', 'cuff'],
    [3.1, 6.9, -13.5, -10.2, -2.5, 2.5, 'skin', 'hand'],
    [3.0, 4.2, -12.9, -10.6, -3.4, -2.1, 'skin', 'thumb'],
    // 三根手指（中段）
    [3.2, 4.4, -16.2, -13.5, -2.4, 1.7, 'skin', 'fingerA'],
    [4.6, 5.8, -16.3, -13.5, -2.4, 1.7, 'skin', 'fingerB'],
    [6.0, 6.9, -16.1, -13.5, -2.4, 1.7, 'skin', 'fingerC'],
    // 指尖（下勾，共用一块以免过于零碎）
    [3.2, 6.9, -17.4, -16.2, -2.4, -0.2, 'skin', 'fingerTip'],
  ];

  return L.map(([x0, x1, y0, y1, z0, z1, mat, id]) => {
    const a = xo(x0), b = xo(x1);
    return box(`${prefix}${id}`, part,
      [Math.min(a, b), SHOULDER_Y + y0, z0],
      [Math.max(a, b), SHOULDER_Y + y1, z1], mat, { xform: xf });
  });
}

// ---------------------------------------------------------------------------
// 腿：大部分被长袍遮住，只露靴子
// ---------------------------------------------------------------------------
function legBoxes(side) {
  const prefix = side < 0 ? 'R_' : 'L_';
  const part = side < 0 ? 'legR' : 'legL';
  const L = [
    [1.0, 6.4, 4.6, 15.4, -3.4, 3.4, 'clothTeal', 'leg'],
    [0.8, 6.6, 0.0, 4.8, -3.6, 4.6, 'clothDark', 'boot'],
    [0.9, 6.5, 0.0, 2.6, 4.6, 6.9, 'clothDark', 'toe'],
  ];
  return L.map(([x0, x1, y0, y1, z0, z1, mat, id]) => {
    const a = side * x0, b = side * x1;
    return box(`${prefix}${id}`, part, [Math.min(a, b), y0, z0], [Math.max(a, b), y1, z1], mat);
  });
}

// ---------------------------------------------------------------------------
// 头 + 官帽（红顶、黑檐、金托）
// ---------------------------------------------------------------------------
const HEAD = [
  box('head', 'head', [-5.4, 29.4, -5.0], [5.4, 38.4, 5.0], 'skin'),
  box('neck', 'head', [-3.0, 27.2, -3.0], [3.0, 29.8, 3.0], 'skinDark'),
  box('hatBand', 'head', [-7.7, 35.9, -7.0], [7.7, 38.4, 7.0], 'clothDark'),
  box('hatDome', 'head', [-6.2, 38.4, -5.8], [6.2, 41.6, 5.8], 'hatRed'),
  box('hatCrown', 'head', [-3.8, 41.6, -3.6], [3.8, 43.2, 3.6], 'hatRed'),
  box('hatRing', 'head', [-1.7, 43.2, -1.7], [1.7, 43.7, 1.7], 'gold'),
  box('hatKnob', 'head', [-1.0, 43.7, -1.0], [1.0, 44.9, 1.0], 'hatRed'),
];

// ---------------------------------------------------------------------------
// 躯干 + 长袍
// ---------------------------------------------------------------------------
const BODY = [
  box('torso', 'body', [-7.5, 20.0, -5.0], [7.5, 28.6, 5.0], 'clothTeal'),
  box('collar', 'body', [-4.6, 26.8, -4.2], [4.6, 29.4, 4.2], 'clothRed'),
  box('belt', 'body', [-8.0, 18.6, -6.4], [8.0, 20.2, 6.4], 'clothDark'),
  box('badge', 'body', [-3.2, 21.6, 5.0], [3.2, 27.4, 5.45], 'gold', { uvMode: 'stretch', tag: 'badge' }),

  // 长袍主体（石青）+ 前襟红色镶边 + 底摆红边
  box('skirt', 'body', [-7.5, 6.4, -6.0], [7.5, 18.6, 6.0], 'clothTeal'),
  box('hemBand', 'body', [-7.7, 5.6, -6.2], [7.7, 7.4, 6.2], 'clothRed'),
  box('frontTrimL', 'body', [5.3, 7.4, 5.9], [7.5, 18.6, 6.22], 'clothRed'),
  box('frontTrimR', 'body', [-7.5, 7.4, 5.9], [-5.3, 18.6, 6.22], 'clothRed'),
  box('torsoTrimL', 'body', [5.6, 20.2, 4.95], [7.5, 28.6, 5.22], 'clothRed'),
  box('torsoTrimR', 'body', [-7.5, 20.2, 4.95], [-5.6, 28.6, 5.22], 'clothRed'),
];

function buildBoxes() {
  const out = [...HEAD, ...BODY, ...armBoxes(-1), ...armBoxes(+1), ...legBoxes(-1), ...legBoxes(+1)];
  const ids = new Set();
  for (const b of out) {
    if (ids.has(b.id)) throw new Error(`重复的 box id: ${b.id}`);
    ids.add(b.id);
  }
  return out;
}

const MODEL = {
  id: 'jiangshi',
  height: 44.3,
  depth: 19.0,
  width: 18.2,
};

module.exports = { MATERIALS, buildBoxes, MODEL, box, rad, ARM_PITCH };
