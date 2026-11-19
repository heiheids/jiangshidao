'use strict';
// ---------------------------------------------------------------------------
// 调色板：参照参考图取色（清代官服僵尸）
//   肤色：僵尸绿，眼窝深陷偏黑，牙齿发黄
//   官服：石青/墨绿缎面 + 暗红镶边 + 黑色腰带与帽檐
//   帽子：红顶 + 金顶珠座
// ---------------------------------------------------------------------------

const PALETTE = {
  skin:       [0x6E, 0xA6, 0x50], // 基础僵尸绿
  skinLight:  [0x8C, 0xC0, 0x66], // 高光
  skinMid:    [0x5E, 0x91, 0x43], // 中间调
  skinDark:   [0x3E, 0x63, 0x2B], // 阴影
  skinShadow: [0x2A, 0x42, 0x1D], // 眼窝 / 鼻下
  eyeBlack:   [0x12, 0x18, 0x0E],
  tooth:      [0xD8, 0xD2, 0xAE],

  clothTeal:      [0x3F, 0x5A, 0x55], // 石青袍身
  clothTealLight: [0x55, 0x74, 0x6C],
  clothTealDark:  [0x2C, 0x40, 0x3C],
  clothRed:       [0x76, 0x2E, 0x38], // 暗红镶边
  clothRedLight:  [0x93, 0x3E, 0x48],
  clothRedDark:   [0x55, 0x1E, 0x27],
  clothDark:      [0x23, 0x26, 0x2A], // 帽檐 / 腰带 / 靴
  clothDarkLight: [0x35, 0x39, 0x40],

  gold:      [0xB8, 0x99, 0x4C],      // 补子金边 / 顶珠托
  goldLight: [0xD8, 0xBC, 0x6E],
  goldDark:  [0x7E, 0x66, 0x2E],

  hatRed:      [0x8E, 0x1F, 0x24],    // 帽顶
  hatRedLight: [0xB3, 0x2C, 0x30],
  hatRedDark:  [0x66, 0x14, 0x18],
};

/** 材质 → 主色（供渲染器做基础色/回退） */
const MATERIAL_COLORS = {
  skin: PALETTE.skin,
  skinDark: PALETTE.skinMid,
  clothTeal: PALETTE.clothTeal,
  clothRed: PALETTE.clothRed,
  clothDark: PALETTE.clothDark,
  gold: PALETTE.gold,
  hatRed: PALETTE.hatRed,
};

/** hex/rgb → 归一化 */
function matRGB([r, g, b], a = 1) {
  return [r / 255, g / 255, b / 255, a];
}

/** 稳定伪随机（同输入同输出），用于布料噪点 */
function hash2(x, y, seed) {
  let h = (x * 374761393 + y * 668265263 + seed * 2147483647) | 0;
  h = (h ^ (h >>> 13)) * 1274126177;
  return ((h ^ (h >>> 16)) >>> 0) / 4294967295;
}

module.exports = { PALETTE, MATERIAL_COLORS, matRGB, hash2 };
