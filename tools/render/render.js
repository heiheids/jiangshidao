'use strict';
// ---------------------------------------------------------------------------
// 预览渲染入口：生成多个机位的 PNG + 一张拼版总览图 + 图集。
// 用法：node tools/render/render.js [--size 1024] [--out out/preview]
// ---------------------------------------------------------------------------

const fs = require('fs');
const path = require('path');
const { buildBoxes, MODEL } = require('../model/model');
const { facesOf } = require('../model/faces');
const { buildAtlasLayout, makeUvOf } = require('./atlas');
const { paintAtlas } = require('../art/texture');
const { Canvas, renderFrame } = require('./raster');
const { makeCamera } = require('./math');
const { encodePNG } = require('../lib/png');

const ROOT = path.resolve(__dirname, '..', '..');

function argOf(name, def) {
  const i = process.argv.indexOf(`--${name}`);
  return i >= 0 && process.argv[i + 1] ? process.argv[i + 1] : def;
}

const SIZE = parseInt(argOf('size', '1024'), 10);
const OUT = path.resolve(ROOT, argOf('out', 'out/preview'));

const VIEWS = [
  { name: '1-front', yaw: 0, pitch: 3, label: '正视图 Front' },
  { name: '2-three-quarter', yaw: 32, pitch: 10, label: '四分之三视角 3/4' },
  { name: '3-side', yaw: 88, pitch: 3, label: '侧视图 Side' },
  { name: '4-back', yaw: 180, pitch: 5, label: '背视图 Back' },
  { name: '5-face', yaw: 0, pitch: 6, target: [0, 36.0, 2.0], distance: 46, label: '面部特写 Face' },
];

function composeSheet(canvases, size, gap = 6, bg = [250, 250, 250]) {
  const cols = 3;
  const rows = Math.ceil(canvases.length / cols);
  const W = cols * size + (cols + 1) * gap;
  const H = rows * size + (rows + 1) * gap;
  const data = Buffer.alloc(W * H * 4);
  for (let i = 0; i < W * H; i++) {
    data[i * 4] = bg[0];
    data[i * 4 + 1] = bg[1];
    data[i * 4 + 2] = bg[2];
    data[i * 4 + 3] = 255;
  }
  canvases.forEach((cv, i) => {
    const cx = (i % cols) * (size + gap) + gap;
    const cy = Math.floor(i / cols) * (size + gap) + gap;
    for (let y = 0; y < size; y++) {
      const srcRow = y * size * 4;
      cv.color.copy(data, ((cy + y) * W + cx) * 4, srcRow, srcRow + size * 4);
    }
  });
  return { width: W, height: H, data };
}

function main() {
  fs.mkdirSync(OUT, { recursive: true });

  const boxes = buildBoxes();
  const faces = boxes.flatMap((b) => facesOf(b));
  const layout = buildAtlasLayout(faces);
  const uvOf = makeUvOf(layout.placed, layout.side);
  const atlas = paintAtlas(layout, faces, { scale: 4 });

  fs.writeFileSync(path.join(OUT, 'atlas.png'),
    encodePNG({ width: atlas.width, height: atlas.height, data: atlas.data }));

  const target = [0, MODEL.height * 0.5, 3.0];
  const distance = 98;
  const canvases = [];

  for (const v of VIEWS) {
    const t = v.target || target;
    const cam = makeCamera({ target: t, distance: v.distance || distance, yaw: v.yaw, pitch: v.pitch, fov: 26 });
    const canvas = new Canvas(SIZE);
    canvas.clear([255, 255, 255]);
    renderFrame(canvas, { cam, faces, uvOf, atlas, size: SIZE });
    fs.writeFileSync(path.join(OUT, `${v.name}.png`),
      encodePNG({ width: SIZE, height: SIZE, data: canvas.color }));
    canvases.push(canvas);
    console.log('渲染完成:', `${v.name}.png`, '—', v.label);
  }

  const sheet = composeSheet(canvases, SIZE);
  fs.writeFileSync(path.join(OUT, 'preview_sheet.png'),
    encodePNG({ width: sheet.width, height: sheet.height, data: sheet.data }));
  console.log('拼版完成: preview_sheet.png');

  fs.writeFileSync(path.join(OUT, 'render-report.txt'), [
    `画布尺寸: ${SIZE}x${SIZE}`,
    `盒子数: ${boxes.length}`,
    `面数: ${faces.length}`,
    `图集: ${layout.side}x${layout.side} (x${atlas.scale} 输出 ${atlas.width}x${atlas.height})`,
    `模型高度: ${MODEL.height} 单位`,
    `机位: ${VIEWS.map((v) => `${v.name}(yaw=${v.yaw},pitch=${v.pitch})`).join(', ')}`,
  ].join('\n'), 'utf8');

  console.log(`\n模型统计: ${boxes.length} 个盒子 / ${faces.length} 个面 / 图集 ${layout.side}px`);
  console.log(`模型高 ${MODEL.height} 单位，含手臂总深约 ${MODEL.depth} 单位`);
}

if (require.main === module) main();
module.exports = { VIEWS };
