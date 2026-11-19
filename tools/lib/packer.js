'use strict';
// 货架式贴图打包器：把一堆面矩形塞进方形图集，零重叠、确定性。
// 每个条目需要 { key, w, h }（整数像素）。

function packFaces(items, { padding = 1, minSide = 64, maxSide = 512 } = {}) {
  const sorted = items
    .map((it) => ({ ...it, w: Math.ceil(it.w), h: Math.ceil(it.h) }))
    .sort((a, b) => (b.h - a.h) || (b.w - a.w) || (a.key < b.key ? -1 : 1));

  for (let side = minSide; side <= maxSide; side *= 2) {
    const placed = {};
    const shelves = []; // {y, h, x}
    let ok = true;
    for (const it of sorted) {
      const bw = it.w + padding * 2;
      const bh = it.h + padding * 2;
      let target = null;
      for (const sh of shelves) {
        if (sh.h >= bh && sh.x + bw <= side) { target = sh; break; }
      }
      if (target) {
        placed[it.key] = { x: target.x + padding, y: target.y + padding, w: it.w, h: it.h, side };
        target.x += bw;
        continue;
      }
      // 新开一层（层高取首个盒子高度）
      const lastY = shelves.length ? shelves[shelves.length - 1].y + shelves[shelves.length - 1].h : 0;
      if (lastY + bh > side || bw > side) { ok = false; break; }
      const sh = { y: lastY, h: bh, x: bw };
      shelves.push(sh);
      placed[it.key] = { x: padding, y: sh.y + padding, w: it.w, h: it.h, side };
    }
    if (ok) return { side, placed };
  }
  throw new Error('贴图打包失败：图集尺寸不足');
}

module.exports = { packFaces };
