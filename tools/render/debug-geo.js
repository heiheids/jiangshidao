'use strict';
// 调试：只渲染几何（每面纯色），用于判断问题出在模型还是 UV。
const fs = require('fs');
const path = require('path');
const { buildBoxes, MODEL } = require('../model/model');
const { facesOf } = require('../model/faces');
const { buildAtlasLayout, makeUvOf } = require('./atlas');
const { Canvas, renderFrame } = require('./raster');
const { makeCamera } = require('./math');
const { encodePNG } = require('../lib/png');

const ROOT = path.resolve(__dirname, '..', '..');
const OUT = path.join(ROOT, 'out', 'debug');
fs.mkdirSync(OUT, { recursive: true });

const boxes = buildBoxes();
const faces = boxes.flatMap((b) => facesOf(b));
const layout = buildAtlasLayout(faces);
const uvOf = makeUvOf(layout.placed, layout.side);
const atlas = { width: 1, height: 1, data: Buffer.from([255, 255, 255, 255]) };

const COLORS = {
  pz: [0, 120, 255], nz: [0, 200, 120], px: [255, 80, 80],
  py: [255, 220, 0], ny: [160, 0, 255], nx: [0, 220, 220],
};
const flat = (f) => COLORS[f.face];

const SIZE = 768;
const target = [0, MODEL.height * 0.5, 3.0];
for (const [name, yaw, pitch] of [['geo-front', 0, 5], ['geo-34', 34, 12], ['geo-side', 88, 4], ['geo-back', 180, 6]]) {
  const cam = makeCamera({ target, distance: 98, yaw, pitch, fov: 26 });
  const cv = new Canvas(SIZE);
  cv.clear([255, 255, 255]);
  renderFrame(cv, { cam, faces, uvOf, atlas, size: SIZE, flat });
  fs.writeFileSync(path.join(OUT, `${name}.png`), encodePNG({ width: SIZE, height: SIZE, data: cv.color }));
}

// 输出每个面的世界尺寸，查找异常
const rows = faces.map((f) => `${f.key.padEnd(28)} ${f.w.toFixed(1)}x${f.h.toFixed(1)}`)
  .sort();
fs.writeFileSync(path.join(OUT, 'face-sizes.txt'), rows.join('\n'), 'utf8');
console.log('ok', faces.length);
