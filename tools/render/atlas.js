'use strict';
// 盒子 UV 映射：把每个盒子的 6 个面映射到图集中的独立矩形。
// 采用“每个面独立矩形”的方案（而非 Minecraft 原生 body 展开），
// 好处：预览阶段零重叠、任意尺寸盒子都安全；后续生成 Java 模型时可一键切回展开式。

const { matRGB } = require('../art/palette');

/** 面尺寸 → 纹理像素尺寸（1 unit = 1 px，取整并至少 1px） */
function facePixelSize(f) {
  return [Math.max(1, Math.round(f.w)), Math.max(1, Math.round(f.h))];
}

function buildAtlasLayout(faces) {
  const { packFaces } = require('../lib/packer');
  const items = faces.map((f) => {
    const [w, h] = facePixelSize(f);
    return { key: f.key, w, h };
  });
  const { side, placed } = packFaces(items, { padding: 1, minSide: 64, maxSide: 1024 });
  return { side, placed };
}

/** 返回 uvOf(face) => [[u,v]x4]（TL,TR,BR,BL），v 轴向下（纹理坐标） */
function makeUvOf(placed, side) {
  return (f) => {
    const r = placed[f.key];
    if (!r) throw new Error(`面未排版: ${f.key}`);
    const u0 = r.x / side;
    const u1 = (r.x + r.w) / side;
    const v0 = r.y / side;
    const v1 = (r.y + r.h) / side;
    return [[u0, v0], [u1, v0], [u1, v1], [u0, v1]];
  };
}

module.exports = { buildAtlasLayout, makeUvOf, facePixelSize, matRGB };
