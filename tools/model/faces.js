'use strict';
// 面几何：给定盒子（含可选 xform），产出 6 个面。
// 顶点顺序统一为 TL, TR, BR, BL（贴图视角，TL 即 (u=0, v=0)）。

const { rad } = require('./model');

/** 绕 pivot 做 XYZ 顺序旋转（角度制） */
function rotXYZ(p, pivot, rot) {
  let q = p.slice();
  if (rot[0]) {
    const a = rad(rot[0]), c = Math.cos(a), s = Math.sin(a);
    const y = q[1] - pivot[1], z = q[2] - pivot[2];
    q = [q[0], pivot[1] + y * c - z * s, pivot[2] + y * s + z * c];
  }
  if (rot[1]) {
    const a = rad(rot[1]), c = Math.cos(a), s = Math.sin(a);
    const x = q[0] - pivot[0], z = q[2] - pivot[2];
    q = [pivot[0] + x * c + z * s, q[1], pivot[2] - x * s + z * c];
  }
  if (rot[2]) {
    const a = rad(rot[2]), c = Math.cos(a), s = Math.sin(a);
    const x = q[0] - pivot[0], y = q[1] - pivot[1];
    q = [pivot[0] + x * c - y * s, pivot[1] + x * s + y * c, q[2]];
  }
  return q;
}

function dirOf(rot, n) {
  if (!rot) return n.slice();
  const d = rotXYZ(n, [0, 0, 0], rot);
  const len = Math.hypot(d[0], d[1], d[2]) || 1;
  return [d[0] / len, d[1] / len, d[2] / len];
}

// 每个面：角点（TL,TR,BR,BL）+ 外法线
const FACE_DEFS = [
  {
    face: 'pz', normal: [0, 0, 1],
    corners: (b) => [[b.min[0], b.max[1], b.max[2]], [b.max[0], b.max[1], b.max[2]],
    [b.max[0], b.min[1], b.max[2]], [b.min[0], b.min[1], b.max[2]]],
  },
  {
    face: 'nz', normal: [0, 0, -1],
    corners: (b) => [[b.max[0], b.max[1], b.min[2]], [b.min[0], b.max[1], b.min[2]],
    [b.min[0], b.min[1], b.min[2]], [b.max[0], b.min[1], b.min[2]]],
  },
  {
    face: 'px', normal: [1, 0, 0],
    corners: (b) => [[b.max[0], b.max[1], b.max[2]], [b.max[0], b.max[1], b.min[2]],
    [b.max[0], b.min[1], b.min[2]], [b.max[0], b.min[1], b.max[2]]],
  },
  {
    face: 'nx', normal: [-1, 0, 0],
    corners: (b) => [[b.min[0], b.max[1], b.min[2]], [b.min[0], b.max[1], b.max[2]],
    [b.min[0], b.min[1], b.max[2]], [b.min[0], b.min[1], b.min[2]]],
  },
  {
    face: 'py', normal: [0, 1, 0],
    corners: (b) => [[b.min[0], b.max[1], b.min[2]], [b.max[0], b.max[1], b.min[2]],
    [b.max[0], b.max[1], b.max[2]], [b.min[0], b.max[1], b.max[2]]],
  },
  {
    face: 'ny', normal: [0, -1, 0],
    corners: (b) => [[b.min[0], b.min[1], b.max[2]], [b.max[0], b.min[1], b.max[2]],
    [b.max[0], b.min[1], b.min[2]], [b.min[0], b.min[1], b.min[2]]],
  },
];

function facesOf(b) {
  const xf = b.xform;
  const rot = xf ? xf.rot : null;
  const pivot = xf ? xf.pivot : [0, 0, 0];
  const out = [];
  for (const def of FACE_DEFS) {
    const raw = def.corners(b);
    const world = rot ? raw.map((p) => rotXYZ(p, pivot, rot)) : raw;
    const n = dirOf(rot, def.normal);
    const e1 = [world[1][0] - world[0][0], world[1][1] - world[0][1], world[1][2] - world[0][2]];
    const e2 = [world[3][0] - world[0][0], world[3][1] - world[0][1], world[3][2] - world[0][2]];
    out.push({
      key: `${b.id}/${def.face}`,
      box: b,
      face: def.face,
      normal: n,
      corners: world,
      w: Math.hypot(e1[0], e1[1], e1[2]),
      h: Math.hypot(e2[0], e2[1], e2[2]),
    });
  }
  return out;
}

module.exports = { facesOf, rotXYZ, dirOf, FACE_DEFS };
