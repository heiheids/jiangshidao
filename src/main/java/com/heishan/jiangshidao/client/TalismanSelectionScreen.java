package com.heishan.jiangshidao.client;

import com.heishan.jiangshidao.JiangshiDao;
import com.heishan.jiangshidao.network.SelectTalismanPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * TalismanSelectionScreen —— 客户端界面。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class TalismanSelectionScreen extends Screen {
   private static final String[] TYPE_IDS = new String[]{"sealing", "burning", "thunder", "subjugation"};
   private static final String[] NAME_KEYS = new String[]{
      "item.jiangshidao.sealing_talisman",
      "item.jiangshidao.burning_talisman",
      "item.jiangshidao.thunder_talisman",
      "item.jiangshidao.subjugation_talisman"
   };
   private final BlockPos pos;

   public TalismanSelectionScreen(BlockPos pos) {
      super(Component.translatable("screen.jiangshidao.talisman_select"));
      this.pos = pos;
   }

   protected void init() {
      super.init();
      int buttonWidth = 160;
      int buttonHeight = 20;
      int gap = 4;
      int totalHeight = TYPE_IDS.length * (buttonHeight + gap) - gap;
      int startY = this.height / 2 - totalHeight / 2;

      for (int i = 0; i < TYPE_IDS.length; i++) {
         String typeId = TYPE_IDS[i];
         int y = startY + i * (buttonHeight + gap);
         this.addRenderableWidget(
            Button.builder(Component.translatable(NAME_KEYS[i]), b -> this.select(typeId))
               .bounds(this.width / 2 - buttonWidth / 2, y, buttonWidth, buttonHeight)
               .build()
         );
      }
   }

   private void select(String typeId) {
      if (ClientPlayNetworking.canSend(SelectTalismanPayload.TYPE)) {
         ClientPlayNetworking.send(new SelectTalismanPayload(this.pos, typeId));
      } else {
         JiangshiDao.LOGGER.warn("[符纸] 服务端没有登记 select_talisman 包，本次选择未提交");
      }

      this.onClose();
   }

   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractRenderState(graphics, mouseX, mouseY, partialTick);
      graphics.centeredText(this.font, this.title, this.width / 2, this.height / 2 - TYPE_IDS.length * 12 - 24, -1);
   }

   public boolean isPauseScreen() {
      return false;
   }
}
