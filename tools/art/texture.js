'use strict';
// ---------------------------------------------------------------------------
// 程序化贴图绘制：为图集中每个「面矩形」涂色。
// 设计要点：
//   · 基础材质（布/皮/金）由 palette 统一取色，带轻微噪点与方向性明暗，
//     避免大面纯色导致的“塑料感”。
//   · 特殊面（脸、补子、帽顶、靴、袖口）单独绘制细节。
//   · 所有坐标基于「面局部归一化 UV」，因此与图集尺寸、缩放倍率解耦。
// ---------------------------------------------------------------------------

const { PALETTE: P, hash2 } = require('./palette');

/** 颜色工具 */
const mix = (a, b, t) => [
  Math.round(a[0] + (b[0] - a[0]) * t),
  Math.round(a[1] + (b[1] - a[1]) * t),
  Math.round(a[2] + (b[2] - a[2]) * t),
];
const shade = (c, k) => [
  Math.max(0, Math.min(255, Math.round(c[0] * k))),
  Math.max(0, Math.min(255, Math.round(c[1] * k))),
  Math.max(0, Math.min(255, Math.round(c[2] * k))),
];

class FacePainter {
  constructor(img, rect, seed) {
    this.img = img;
    this.rect = rect;
    this.seed = seed;
  }

  /** 归一化 (u,v) → 像素写入 */
  put(u, v, color, alpha = 1) {
    const { x, y, w, h } = this.rect;
    const px = Math.min(w - 1, Math.max(0, Math.floor(u * w)));
    const py = Math.min(h - 1, Math.max(0, Math.floor(v * h)));
    const i = ((y + py) * this.img.width + (x + px)) * 4;
    const d = this.img.data;
    if (alpha >= 1) {
      d[i] = color[0]; d[i + 1] = color[1]; d[i + 2] = color[2]; d[i + 3] = 255;
    } else {
      d[i] = Math.round(d[i] * (1 - alpha) + color[0] * alpha);
      d[i + 1] = Math.round(d[i + 1] * (1 - alpha) + color[1] * alpha);
      d[i + 2] = Math.round(d[i + 2] * (1 - alpha) + color[2] * alpha);
      d[i + 3] = 255;
    }
  }

  /** 归一化矩形填充 */
  box(u0, v0, u1, v1, color, alpha = 1) {
    const { x, y, w, h } = this.rect;
    const px0 = Math.floor(u0 * w), px1 = Math.ceil(u1 * w);
    const py0 = Math.floor(v0 * h), py1 = Math.ceil(v1 * h);
    for (let py = Math.max(0, py0); py < Math.min(h, py1); py++) {
      for (let px = Math.max(0, px0); px < Math.min(w, px1); px++) {
        this.put((px + 0.5) / w, (py + 0.5) / h, color, alpha);
      }
    }
  }

  /** 逐像素抖动填充（布料噪点） */
  fill(base, { noise = 0.06, vGrad = 0.1, uGrad = 0.0, seed = this.seed } = {}) {
    const { x, y, w, h } = this.rect;
    const d = this.img.data;
    for (let py = 0; py < h; py++) {
      for (let px = 0; px < w; px++) {
        const n = (hash2(px + this.seed * 7919, py, seed) - 0.5) * 2 * noise;
        const g = 1 + vGrad * (0.5 - py / Math.max(1, h - 1)) + uGrad * (px / Math.max(1, w - 1) - 0.5);
        const c = shade(base, g + n);
        const i = ((y + py) * this.img.width + (x + px)) * 4;
        d[i] = c[0]; d[i + 1] = c[1]; d[i + 2] = c[2]; d[i + 3] = 255;
      }
    }
  }

  /** 边缘暗化，增强方块交界处的体积感 */
  vignette(base, k = 0.72, width = 0.12) {
    const { x, y, w, h } = this.rect;
    const d = this.img.data;
    for (let py = 0; py < h; py++) {
      for (let px = 0; px < w; px++) {
        const eu = Math.min(px, w - 1 - px) / Math.max(1, w - 1);
        const ev = Math.min(py, h - 1 - py) / Math.max(1, h - 1);
        const e = Math.min(eu, ev);
        if (e < width) {
          const t = 1 - e / width;
          const i = ((y + py) * this.img.width + (x + px)) * 4;
          const c = shade([d[i], d[i + 1], d[i + 2]], 1 - (1 - k) * t);
          d[i] = c[0]; d[i + 1] = c[1]; d[i + 2] = c[2];
        }
      }
    }
  }
}

// ---------------------------------------------------------------------------
// 各类面的画法
// ---------------------------------------------------------------------------

function paintFabric(fp, base, light, dark) {
  fp.fill(base, { noise: 0.05, vGrad: 0.14 });
  const { w, h } = fp.rect;

  // 织物质感：竖向细线 + 随机磨损斑块（模仿参考图中缎面的斑驳）
  for (let px = 0; px < w; px += 3) {
    fp.box(px / w, 0, (px + 1) / w, 1, light, 0.06);
  }
  const patches = Math.max(3, Math.round((w * h) / 90));
  for (let i = 0; i < patches; i++) {
    const r1 = hash2(i * 13 + fp.seed, 7, fp.seed);
    const r2 = hash2(i * 29 + fp.seed, 11, fp.seed);
    const r3 = hash2(i * 7 + fp.seed, 3, fp.seed);
    const cw = 1 + Math.floor(r3 * 3) / w;
    const ch = 1 + Math.floor(r1 * 3) / h;
    const cx = r1 * (1 - cw);
    const cy = r2 * (1 - ch);
    fp.box(cx, cy, cx + cw, cy + ch, r3 > 0.5 ? dark : light, 0.16 + r2 * 0.16);
  }
  fp.vignette(base, 0.76, 0.16);
}

function paintTrim(fp, base, light, dark) {
  fp.fill(base, { noise: 0.045, vGrad: 0.2 });
  const h = fp.rect.h;
  fp.box(0, 0, 1, 1 / h, light, 0.35);       // 上缘高光
  fp.box(0, 1 - 1 / h, 1, 1, dark, 0.5);     // 下缘阴影
  fp.vignette(base, 0.7, 0.16);
}

function paintSkin(fp, base) {
  fp.fill(base, { noise: 0.06, vGrad: 0.1 });
  const { w, h } = fp.rect;
  // 尸斑：不规则深色块
  const n = Math.max(2, Math.round((w * h) / 120));
  for (let i = 0; i < n; i++) {
    const r1 = hash2(i * 17 + fp.seed, 5, fp.seed);
    const r2 = hash2(i * 31 + fp.seed, 9, fp.seed);
    const bw = (1 + Math.floor(r1 * 3)) / w;
    const bh = (1 + Math.floor(r2 * 3)) / h;
    fp.box(r1 * (1 - bw), r2 * (1 - bh), r1 * (1 - bw) + bw, r2 * (1 - bh) + bh,
      P.skinDark, 0.18 + r2 * 0.14);
  }
  fp.vignette(base, 0.78, 0.14);
}

/** 脸：眼窝、鼻、嘴、眉心（参照参考图：深陷黑眼窝 + 微张嘴露黄牙） */
function paintFace(fp) {
  const { w, h } = fp.rect;
  paintSkin(fp, P.skin);
  if (w < 6 || h < 6) return;

  // 额头高光 / 颧骨阴影
  fp.box(0.12, 0.02, 0.88, 0.10, P.skinLight, 0.3);
  fp.box(0.0, 0.42, 0.10, 0.66, P.skinMid, 0.4);
  fp.box(0.90, 0.42, 1.0, 0.66, P.skinMid, 0.4);

  // 眉骨暗带
  fp.box(0.12, 0.22, 0.88, 0.27, P.skinDark, 0.5);

  // 眼窝（深陷成方块，参考图即为此形）
  const eyeY0 = 0.26, eyeY1 = 0.47;
  fp.box(0.14, eyeY0, 0.44, eyeY1, P.skinShadow, 0.95);
  fp.box(0.56, eyeY0, 0.86, eyeY1, P.skinShadow, 0.95);
  // 瞳孔：更大更黑，贴近参考图
  fp.box(0.19, 0.295, 0.39, 0.44, P.eyeBlack, 1);
  fp.box(0.61, 0.295, 0.81, 0.44, P.eyeBlack, 1);
  // 瞳孔中的一点浑浊高光，避免全黑的“空洞感”
  fp.box(0.205, 0.312, 0.235, 0.342, [0x6A, 0x8A, 0x5A], 0.55);
  fp.box(0.625, 0.312, 0.655, 0.342, [0x6A, 0x8A, 0x5A], 0.55);
  // 眼窝下缘阴影
  fp.box(0.14, 0.47, 0.44, 0.52, P.skinDark, 0.65);
  fp.box(0.56, 0.47, 0.86, 0.52, P.skinDark, 0.65);

  // 鼻骨（细窄阴影）+ 鼻孔
  fp.box(0.47, 0.44, 0.53, 0.60, P.skinMid, 0.35);
  fp.box(0.435, 0.585, 0.475, 0.615, P.skinShadow, 0.9);
  fp.box(0.525, 0.585, 0.565, 0.615, P.skinShadow, 0.9);

  // 嘴：横长微张，露参差黄牙
  fp.box(0.22, 0.70, 0.78, 0.88, P.skinShadow, 0.98);
  const teeth = [
    [0.235, 0.315], [0.325, 0.405], [0.415, 0.495],
    [0.505, 0.585], [0.595, 0.675], [0.685, 0.765],
  ];
  teeth.forEach(([a, b], i) => {
    const top = 0.705;
    const bot = i % 3 === 0 ? 0.805 : 0.788;
    fp.box(a, top, b, bot, P.tooth, 0.96);
    // 牙缝：深色竖线
    fp.box(b - 0.006, top, b, bot, [0x5A, 0x52, 0x3A], 0.75);
  });
  // 下唇 + 下颌阴影
  fp.box(0.22, 0.875, 0.78, 0.905, P.skinDark, 0.55);
  fp.box(0.14, 0.92, 0.86, 1.0, P.skinDark, 0.24);

  // 眉骨投下的阴影（增强凶相）
  fp.box(0.15, 0.215, 0.44, 0.245, P.skinShadow, 0.5);
  fp.box(0.56, 0.215, 0.85, 0.245, P.skinShadow, 0.5);
}

/** 头部侧面（耳部与鬓角） */
function paintHeadSide(fp) {
  paintSkin(fp, P.skin);
  const { w, h } = fp.rect;
  if (w < 4 || h < 4) return;
  // 耳廓（参考图侧脸可见方形耳）
  fp.box(0.58, 0.38, 0.84, 0.58, P.skinMid, 0.6);
  fp.box(0.62, 0.42, 0.80, 0.54, P.skinShadow, 0.55);
  fp.box(0.58, 0.38, 0.60, 0.58, P.skinDark, 0.45);
  // 太阳穴 / 颧骨
  fp.box(0.0, 0.52, 0.30, 0.60, P.skinMid, 0.3);
  fp.box(0.0, 0.20, 0.26, 0.26, P.skinDark, 0.35);
  fp.box(0.0, 0.72, 0.24, 0.80, P.skinDark, 0.3);
}

/** 头部侧面（耳部与鬓角） */
function paintHeadSide(fp) {
  paintSkin(fp, P.skin);
  const { w, h } = fp.rect;
  if (w < 4 || h < 4) return;
  fp.box(0.62, 0.36, 0.86, 0.56, P.skinMid, 0.55);  // 耳廓
  fp.box(0.66, 0.40, 0.80, 0.50, P.skinShadow, 0.5);
  fp.box(0.0, 0.18, 0.22, 0.26, P.skinDark, 0.4);   // 眉梢阴影
  fp.box(0.0, 0.7, 0.2, 0.78, P.skinDark, 0.3);
}

function paintHeadTop(fp) {
  fp.fill(P.skin, { noise: 0.06, vGrad: 0.1 });
  fp.box(0.0, 0.0, 1.0, 0.16, P.skinLight, 0.25);
  fp.vignette(P.skin, 0.8, 0.12);
}

/** 官补（胸前补子）：外金框 + 深底 + 仙鹤纹 */
function paintBadge(fp) {
  const { w, h } = fp.rect;
  fp.fill(P.gold, { noise: 0.05, vGrad: 0.15 });
  fp.box(0.06, 0.06, 0.94, 0.94, [0x24, 0x2C, 0x30], 1);
  fp.box(0.03, 0.03, 0.97, 0.06, P.goldLight, 1);
  fp.box(0.03, 0.94, 0.97, 0.97, P.goldDark, 1);
  if (w < 8 || h < 8) return;
  // 一轮金日
  fp.box(0.62, 0.14, 0.76, 0.28, P.gold, 1);
  // 仙鹤（极简像素纹样）
  const g = P.gold;
  fp.box(0.34, 0.30, 0.44, 0.36, g, 1);      // 头
  fp.box(0.28, 0.31, 0.34, 0.33, g, 1);      // 喙
  fp.box(0.42, 0.36, 0.52, 0.62, g, 1);      // 颈
  fp.box(0.28, 0.44, 0.62, 0.56, g, 1);      // 身
  fp.box(0.18, 0.42, 0.30, 0.46, g, 1);      // 尾
  fp.box(0.44, 0.54, 0.52, 0.74, g, 0.85);   // 腿
  fp.box(0.30, 0.62, 0.46, 0.72, [0x1A, 0x22, 0x28], 1); // 水波留白
  fp.box(0.46, 0.78, 0.80, 0.84, g, 0.9);    // 浪
  fp.box(0.22, 0.84, 0.34, 0.88, g, 0.6);
}

/** 腰带 / 帽檐 / 靴：深色缎面 */
function paintDarkCloth(fp, base = P.clothDark) {
  fp.fill(base, { noise: 0.05, vGrad: 0.25 });
  const h = fp.rect.h;
  fp.box(0, 0, 1, 1 / h, P.clothDarkLight, 0.4);
  fp.vignette(base, 0.72, 0.15);
}

/** 帽顶：红缎 + 顶面高光 */
function paintHatRed(fp, isTop) {
  fp.fill(P.hatRed, { noise: 0.05, vGrad: isTop ? 0.05 : 0.22 });
  const h = fp.rect.h;
  fp.box(0, 0, 1, 1 / h, P.hatRedLight, 0.45);
  fp.box(0, 1 - 1 / h, 1, 1, P.hatRedDark, 0.55);
  fp.vignette(P.hatRed, 0.75, 0.14);
}

function paintGold(fp) {
  fp.fill(P.gold, { noise: 0.07, vGrad: 0.3 });
  fp.box(0, 0, 1, 0.12, P.goldLight, 0.6);
  fp.vignette(P.gold, 0.7, 0.16);
}

function paintHand(fp) {
  paintSkin(fp, P.skin);
  const { w, h } = fp.rect;
  if (w < 4 || h < 4) return;
  // 指节横纹（手背）
  fp.box(0.0, 0.34, 1.0, 0.39, P.skinDark, 0.3);
  fp.box(0.0, 0.66, 1.0, 0.71, P.skinDark, 0.26);
  fp.box(0.0, 0.04, 1.0, 0.11, P.skinLight, 0.18);
}

/** 分派：根据 box id + face 选择画法 */
function paintBoxFace(fp, face) {
  const id = face.box.id.replace(/^[RL]_/, '');
  const f = face.face;
  const isTop = f === 'py';
  const isBottom = f === 'ny';

  if (id === 'head') {
    if (f === 'pz') return paintFace(fp);
    if (f === 'px' || f === 'nx') return paintHeadSide(fp);
    if (isTop) return paintHeadTop(fp);
    if (isBottom) return paintSkin(fp, P.skinDark);
    return paintSkin(fp, P.skinMid);
  }
  if (id === 'neck') return paintSkin(fp, P.skinMid);
  if (id === 'hatBand') return paintDarkCloth(fp);
  if (id === 'hatDome' || id === 'hatCrown') return paintHatRed(fp, isTop);
  if (id === 'hatRing') return paintGold(fp);
  if (id === 'hatKnob') return paintHatRed(fp, isTop);
  if (id === 'badge') return paintBadge(fp);
  if (id === 'collar') return paintTrim(fp, P.clothRed, P.clothRedLight, P.clothRedDark);
  if (id === 'belt') return paintDarkCloth(fp);
  if (id.startsWith('frontTrim') || id.startsWith('torsoTrim')
    || id === 'hemBand') {
    return paintTrim(fp, P.clothRed, P.clothRedLight, P.clothRedDark);
  }
  if (id === 'skirt') {
    paintFabric(fp, P.clothTeal, P.clothTealLight, P.clothTealDark);
    if (f === 'pz') {
      // 前襟中缝 + 两侧竖向褶
      fp.box(0.485, 0.0, 0.515, 1.0, P.clothTealDark, 0.55);
      fp.box(0.18, 0.0, 0.20, 1.0, P.clothTealDark, 0.25);
      fp.box(0.80, 0.0, 0.82, 1.0, P.clothTealDark, 0.25);
    }
    return;
  }
  if (id === 'torso') {
    paintFabric(fp, P.clothTeal, P.clothTealLight, P.clothTealDark);
    if (f === 'pz') {
      fp.box(0.0, 0.0, 1.0, 0.08, P.clothTealLight, 0.35);
    }
    return;
  }
  if (id === 'sleeve') {
    paintFabric(fp, P.clothTeal, P.clothTealLight, P.clothTealDark);
    // 袖口处加深，模拟袖筒的层次
    if (f === 'pz') fp.box(0, 0.72, 1, 1, P.clothTealDark, 0.4);
    return;
  }
  if (id === 'shoulderCap') return paintTrim(fp, P.clothRed, P.clothRedLight, P.clothRedDark);
  if (id === 'cuff') return paintTrim(fp, P.clothRed, P.clothRedLight, P.clothRedDark);
  if (id === 'hand' || /^(finger|thumb)/.test(id)) return paintHand(fp);
  if (id === 'leg') return paintFabric(fp, P.clothTeal, P.clothTealLight, P.clothTealDark);
  if (id === 'boot' || id === 'toe') return paintDarkCloth(fp);
  if (isBottom) return paintDarkCloth(fp, [0x1A, 0x1D, 0x20]);

  return paintFabric(fp, P.clothTeal, P.clothTealLight, P.clothTealDark);
}

function paintAtlas({ side, placed }, faces, { scale = 1 } = {}) {
  const width = side * scale;
  const height = side * scale;
  const data = Buffer.alloc(width * height * 4, 0);
  const img = { width, height, data };

  let seed = 1;
  for (const f of faces) {
    const r = placed[f.key];
    const rect = {
      x: r.x * scale,
      y: r.y * scale,
      w: r.w * scale,
      h: r.h * scale,
    };
    const fp = new FacePainter(img, rect, seed++);
    paintBoxFace(fp, f);
  }
  return { width, height, data, scale, side };
}

module.exports = { paintAtlas, mix, shade };
