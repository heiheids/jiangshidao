'use strict';
// 光栅化渲染器：z-buffer + 透视正确的贴图采样 + 简单 Lambert 光照。
// 输出 RGBA 缓冲，供 png.js 编码。

const { V, toCamera, project } = require('./math');

class Canvas {
  constructor(size) {
    this.size = size;
    this.color = Buffer.alloc(size * size * 4, 0);
    this.depth = new Float32Array(size * size).fill(Infinity);
  }

  clear([r, g, b] = [0, 0, 0]) {
    const { size, color } = this;
    for (let i = 0; i < size * size; i++) {
      color[i * 4] = r;
      color[i * 4 + 1] = g;
      color[i * 4 + 2] = b;
      color[i * 4 + 3] = 255;
    }
    this.depth.fill(Infinity);
  }
}

/** 从纹理图集按归一化 uv 双线性采样（返回 [r,g,b,a]，0..255） */
function sampleAtlas(atlas, u, v) {
  const { width, height, data } = atlas;
  const x = Math.min(width - 1, Math.max(0, Math.floor(u * width)));
  const y = Math.min(height - 1, Math.max(0, Math.floor(v * height)));
  const i = (y * width + x) * 4;
  return [data[i], data[i + 1], data[i + 2], data[i + 3]];
}

/**
 * 渲染一帧。
 * @param {Canvas} canvas
 * @param {object} opts { cam, faces, uvOf, atlas, light }
 */
function renderFrame(canvas, opts) {
  const { cam, faces, uvOf, atlas, light = [0.62, -0.55, 0.85], ambient = 0.52, size, flat = null } = opts;
  const L = V.norm(light);
  const { size: S } = canvas;

  // 1) 相机变换 + 背面剔除 + 可见性排序（由远到近）
  const prepared = [];
  for (const f of faces) {
    const camPts = f.corners.map((p) => toCamera(cam, p));
    if (camPts.some((p) => p[2] <= 0.5)) continue;

    // 法线必须变换到相机空间后再做剔除（不能与世界空间方向混用）
    const nc = [
      V.dot(f.normal, cam.r),
      V.dot(f.normal, cam.u),
      V.dot(f.normal, cam.f),
    ];
    // 相机空间：x 右, y 上, z 前；点到相机中心的视线方向即 camPts[0]。
    const facing = V.dot(nc, V.norm(camPts[0]));
    if (facing > -1e-3) continue; // 背离相机 → 剔除

    const scr = camPts.map((p) => project(cam, p, S));
    const uv = uvOf(f); // [[u,v] x4] 对应 TL,TR,BR,BL
    const ndl = Math.max(0, V.dot(f.normal, L));
    const shade = Math.min(1.28, ambient + 0.62 * ndl);
    // 侧面轻微加深，增强方块立体感
    const sideDark = f.face === 'px' || f.face === 'nx' ? 0.94 : f.face === 'py' || f.face === 'ny' ? 1.02 : 1;
    const tint = shade * sideDark;
    const zAvg = (camPts[0][2] + camPts[1][2] + camPts[2][2] + camPts[3][2]) / 4;
    prepared.push({ f, scr, uv, tint, zAvg, flatColor: flat ? flat(f) : null });
  }
  prepared.sort((a, b) => b.zAvg - a.zAvg);

  // 2) 逐面光栅化（分割为两个三角形，带纹理透视校正）
  for (const item of prepared) {
    const { scr, uv, tint, flatColor } = item;
    // 三角形索引：TL(0) TR(1) BR(2) / TL(0) BR(2) BL(3)
    drawTri(canvas, [0, 1, 2].map((i) => scr[i]), [0, 1, 2].map((i) => uv[i]), atlas, tint, flatColor);
    drawTri(canvas, [0, 2, 3].map((i) => scr[i]), [0, 2, 3].map((i) => uv[i]), atlas, tint, flatColor);
  }
}

function drawTri(canvas, p, uvs, atlas, tint, flatColor) {
  const { size: S, color, depth } = canvas;
  const [a, b, c] = p;
  const minX = Math.max(0, Math.floor(Math.min(a[0], b[0], c[0])));
  const maxX = Math.min(S - 1, Math.ceil(Math.max(a[0], b[0], c[0])));
  const minY = Math.max(0, Math.floor(Math.min(a[1], b[1], c[1])));
  const maxY = Math.min(S - 1, Math.ceil(Math.max(a[1], b[1], c[1])));
  if (minX > maxX || minY > maxY) return;

  const area = (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0]);
  if (Math.abs(area) < 1e-9) return;
  const inv = 1 / area;

  // 透视校正：插值 1/z 与 uv/z
  const iz = [1 / a[2], 1 / b[2], 1 / c[2]];

  for (let y = minY; y <= maxY; y++) {
    const py = y + 0.5;
    for (let x = minX; x <= maxX; x++) {
      const px = x + 0.5;
      const w0 = ((b[0] - px) * (c[1] - py) - (b[1] - py) * (c[0] - px)) * inv;
      const w1 = ((c[0] - px) * (a[1] - py) - (c[1] - py) * (a[0] - px)) * inv;
      const w2 = 1 - w0 - w1;
      if (w0 < 0 || w1 < 0 || w2 < 0) continue;

      const z = 1 / (w0 * iz[0] + w1 * iz[1] + w2 * iz[2]);
      const idx = y * S + x;
      if (z >= depth[idx]) continue;

      const u = (w0 * uvs[0][0] * iz[0] + w1 * uvs[1][0] * iz[1] + w2 * uvs[2][0] * iz[2]) * z;
      const v = (w0 * uvs[0][1] * iz[0] + w1 * uvs[1][1] * iz[1] + w2 * uvs[2][1] * iz[2]) * z;
      const rgba = flatColor ? [flatColor[0], flatColor[1], flatColor[2], 255] : sampleAtlas(atlas, u, v);
      const [r, g, bb, al] = rgba;
      if (al < 8) continue;

      color[idx * 4] = Math.min(255, r * tint);
      color[idx * 4 + 1] = Math.min(255, g * tint);
      color[idx * 4 + 2] = Math.min(255, bb * tint);
      color[idx * 4 + 3] = 255;
      depth[idx] = z;
    }
  }
}

module.exports = { Canvas, renderFrame, sampleAtlas };
