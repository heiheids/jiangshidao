'use strict';
// 3D 数学：向量、相机、投影。零依赖。

const V = {
  sub: (a, b) => [a[0] - b[0], a[1] - b[1], a[2] - b[2]],
  add: (a, b) => [a[0] + b[0], a[1] + b[1], a[2] + b[2]],
  mul: (a, s) => [a[0] * s, a[1] * s, a[2] * s],
  dot: (a, b) => a[0] * b[0] + a[1] * b[1] + a[2] * b[2],
  cross: (a, b) => [
    a[1] * b[2] - a[2] * b[1],
    a[2] * b[0] - a[0] * b[2],
    a[0] * b[1] - a[1] * b[0],
  ],
  len: (a) => Math.hypot(a[0], a[1], a[2]),
  norm: (a) => {
    const l = Math.hypot(a[0], a[1], a[2]) || 1;
    return [a[0] / l, a[1] / l, a[2] / l];
  },
};

/**
 * 轨道相机。yaw/pitch 单位为角度。
 * yaw = 0 时相机位于 +Z（正对模型正面）；yaw 增大→相机向 +X 侧移动。
 */
function makeCamera({ target, distance, yaw, pitch, fov = 24 }) {
  const y = (yaw * Math.PI) / 180;
  const p = (pitch * Math.PI) / 180;
  const dir = [
    Math.sin(y) * Math.cos(p),
    Math.sin(p),
    Math.cos(y) * Math.cos(p),
  ];
  const pos = V.add(target, V.mul(dir, distance));
  const f = V.norm(V.sub(target, pos));
  const r = V.norm(V.cross(f, [0, 1, 0]));
  const u = V.cross(r, f);
  return { pos, f, r, u, fov, target };
}

/** 世界坐标 → 相机坐标（xc 右, yc 上, zc 前） */
function toCamera(cam, p) {
  const d = V.sub(p, cam.pos);
  return [V.dot(d, cam.r), V.dot(d, cam.u), V.dot(d, cam.f)];
}

/** 相机坐标 → 屏幕像素（方形画布） */
function project(cam, pc, size) {
  const f = 1 / Math.tan((cam.fov * Math.PI) / 360);
  const z = Math.max(1e-6, pc[2]);
  return [
    size / 2 + (pc[0] / z) * f * (size / 2),
    size / 2 - (pc[1] / z) * f * (size / 2),
    z,
  ];
}

module.exports = { V, makeCamera, toCamera, project };
