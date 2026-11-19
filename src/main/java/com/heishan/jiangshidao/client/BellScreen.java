package com.heishan.jiangshidao.client;

import com.heishan.jiangshidao.network.WorkerPayloads;
import com.heishan.jiangshidao.worker.Talent;
import com.heishan.jiangshidao.worker.WorkerJob;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/**
 * BellScreen —— 客户端界面。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class BellScreen extends Screen {
   private static final int PANEL_W = 264;
   private static final int PANEL_H = 190;
   private static final int TALENT_W = 124;
   private static final int TALENT_GAP = 8;
   private static final int TALENT_ROW_H = 14;
   private static final int JOB_COLS = 2;
   private static final int JOB_X = 12;
   private static final int JOB_Y = 74;
   private static final int JOB_W = 116;
   private static final int JOB_H = 20;
   private static final int JOB_GAP_X = 8;
   private static final int JOB_GAP_Y = 2;
   private final String workerName;
   private final String healthText;
   private final CompoundTag talents;
   private WorkerJob job;
   private int leftPos;
   private int topPos;
   private final Button[] jobButtons = new Button[WorkerJob.values().length];
   private EditBox nameBox;

   public BellScreen(String jobId, String workerName, String healthText, CompoundTag talents) {
      super(Component.translatable("screen.jiangshidao.bell"));
      this.job = WorkerJob.byId(jobId);
      this.workerName = workerName == null ? "" : workerName;
      this.healthText = healthText != null && !healthText.isEmpty() ? healthText : "?";
      this.talents = talents == null ? new CompoundTag() : talents;
   }

   protected void init() {
      int totalW = 396;
      int startX = Math.max(4, (this.width - totalW) / 2);
      this.leftPos = startX + 124 + 8;
      this.topPos = Math.max(4, (this.height - 190) / 2);
      this.buildJobButtons();
      this.nameBox = new EditBox(this.font, this.leftPos + 12, this.topPos + 146, 160, 18, Component.translatable("gui.jiangshidao.worker.name_hint"));
      this.nameBox.setMaxLength(16);
      this.nameBox.setValue(this.workerName);
      this.addRenderableWidget(this.nameBox);
      this.addRenderableWidget(
         Button.builder(Component.translatable("gui.jiangshidao.worker.rename"), b -> this.sendName())
            .bounds(this.leftPos + 176, this.topPos + 146, 76, 18)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.translatable("gui.jiangshidao.worker.stop"), b -> this.select(WorkerJob.IDLE))
            .bounds(this.leftPos + 12, this.topPos + 168, 116, 18)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.translatable("gui.done"), b -> this.onClose()).bounds(this.leftPos + 136, this.topPos + 168, 116, 18).build()
      );
      this.refreshJobButtons();
   }

   private void buildJobButtons() {
      WorkerJob[] jobs = WorkerJob.values();

      for (int i = 0; i < jobs.length && i < this.jobButtons.length; i++) {
         WorkerJob option = jobs[i];
         int col = i % 2;
         int row = i / 2;
         Button button = Button.builder(option.displayName(), b -> this.select(option))
            .bounds(this.leftPos + 12 + col * 124, this.topPos + 74 + row * 22, 116, 20)
            .build();
         button.active = option.implemented();
         this.jobButtons[i] = button;
         this.addRenderableWidget(button);
      }
   }

   private void refreshJobButtons() {
      WorkerJob[] jobs = WorkerJob.values();

      for (int i = 0; i < this.jobButtons.length && i < jobs.length; i++) {
         Button button = this.jobButtons[i];
         if (button != null) {
            boolean selected = jobs[i] == this.job;
            button.setMessage(jobs[i].displayName().copy().withStyle(selected ? ChatFormatting.GREEN : ChatFormatting.WHITE));
         }
      }
   }

   private void select(WorkerJob option) {
      if (option.implemented()) {
         this.job = option;
         this.refreshJobButtons();
         ClientPlayNetworking.send(new WorkerPayloads.SetJob(1, 0, option.id()));
      }
   }

   private void sendName() {
      String name = this.nameBox == null ? "" : this.nameBox.getValue().trim();
      if (!name.isEmpty()) {
         ClientPlayNetworking.send(new WorkerPayloads.SetName(1, 0, name));
      }
   }

   public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractBackground(graphics, mouseX, mouseY, partialTick);
      int x = this.leftPos;
      int y = this.topPos;
      this.drawTalentPanel(graphics, this.leftPos - 124 - 8, y);
      graphics.fill(RenderPipelines.GUI, x - 1, y - 1, x + 264 + 1, y + 190 + 1, -13948117);
      graphics.fill(RenderPipelines.GUI, x, y, x + 264, y + 190, -3750202);
      graphics.fill(RenderPipelines.GUI, x + 8, y + 72, x + 264 - 8, y + 73, -7631989);
      graphics.fill(RenderPipelines.GUI, x + 8, y + 140, x + 264 - 8, y + 141, -7631989);
   }

   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractRenderState(graphics, mouseX, mouseY, partialTick);
      int x = this.leftPos;
      int y = this.topPos;
      graphics.text(this.font, this.title, x + 12, y + 10, -12566464, false);
      graphics.text(this.font, Component.translatable("gui.jiangshidao.worker.name", new Object[]{this.workerName}), x + 12, y + 26, -14671840, false);
      graphics.text(this.font, Component.translatable("gui.jiangshidao.worker.health", new Object[]{this.healthText}), x + 12, y + 38, -7667712, false);
      graphics.text(this.font, Component.translatable("gui.jiangshidao.worker.current", new Object[]{this.job.displayName()}), x + 12, y + 50, -16760704, false);
      graphics.text(this.font, Component.translatable("gui.jiangshidao.worker.pick"), x + 12, y + 61, -10461088, false);
   }

   private void drawTalentPanel(GuiGraphicsExtractor graphics, int x, int y) {
      Talent[] all = Talent.values();
      int height = 15 + all.length * 14;
      graphics.fill(RenderPipelines.GUI, x - 1, y - 1, x + 124 + 1, y + height + 1, -13948117);
      graphics.fill(RenderPipelines.GUI, x, y, x + 124, y + height, -3750202);
      graphics.text(this.font, Component.translatable("gui.jiangshidao.worker.talents"), x + 4, y + 4, -12566464, false);
      int rowY = y + 16;

      for (Talent talent : all) {
         int level = 0;
         String value = "";
         if (this.talents.contains(talent.id())) {
            CompoundTag row = this.talents.getCompoundOrEmpty(talent.id());
            level = row.getIntOr("lv", 0);
            value = row.getStringOr("v", "");
         }

         graphics.text(this.font, Component.translatable(talent.translationKey()), x + 4, rowY, level > 0 ? -14671840 : -9408400, false);
         String stars = level > 0 ? Talent.stars(level) : "—";
         graphics.text(this.font, stars, x + 40, rowY, level > 0 ? -4684277 : -7303024, false);
         graphics.text(this.font, value, x + 124 - 6 - this.font.width(value), rowY, -16752640, false);
         rowY += 14;
      }
   }

   public boolean isPauseScreen() {
      return false;
   }
}
