package com.heishan.jiangshidao.client;

import com.heishan.jiangshidao.menu.WorkerMenu;
import com.heishan.jiangshidao.network.WorkerPayloads;
import com.heishan.jiangshidao.worker.Talent;
import com.heishan.jiangshidao.worker.WorkerJob;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * WorkerContainerScreen —— 客户端界面。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class WorkerContainerScreen extends AbstractContainerScreen<WorkerMenu> {
   private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
   private static final int PANEL_W = 176;
   private static final int PANEL_H = 274;
   private static final WorkerJob[] JOBS = new WorkerJob[]{WorkerJob.FISHING, WorkerJob.WOODCUTTING, WorkerJob.MINING, WorkerJob.FARMING};
   private WorkerJob current = WorkerJob.IDLE;
   private boolean jobMenuOpen;
   private boolean followOn = true;
   private boolean attackOn = true;
   private Button followButton;
   private Button attackButton;
   private EditBox nameBox;
   private final Button[] jobButtons = new Button[JOBS.length];

   public WorkerContainerScreen(WorkerMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title, 176, 274);
      this.titleLabelX = 8;
      this.titleLabelY = 6;
      this.inventoryLabelX = 8;
      this.inventoryLabelY = 158;
   }

   protected void init() {
      super.init();
      int x = this.leftPos;
      int y = this.topPos;
      int row = y + 274 - 24;
      this.nameBox = new EditBox(this.font, x + 100, y + 20, 68, 16, Component.translatable("gui.jiangshidao.worker.name_hint"));
      this.nameBox.setMaxLength(16);
      this.nameBox.setValue("");
      this.addRenderableWidget(this.nameBox);
      this.addRenderableWidget(
         Button.builder(Component.translatable("gui.jiangshidao.worker.rename"), b -> this.sendName()).bounds(x + 100, y + 40, 68, 16).build()
      );
      this.addRenderableWidget(
         Button.builder(Component.translatable("gui.jiangshidao.worker.stop"), b -> this.setJob(WorkerJob.IDLE)).bounds(x + 8, row, 52, 18).build()
      );
      this.addRenderableWidget(
         Button.builder(Component.translatable("gui.jiangshidao.worker.start"), b -> this.startWork()).bounds(x + 64, row, 52, 18).build()
      );
      this.addRenderableWidget(Button.builder(Component.translatable("gui.jiangshidao.worker.menu"), b -> {
         this.jobMenuOpen = !this.jobMenuOpen;
         this.refreshJobButtons();
      }).bounds(x + 120, row, 48, 18).build());
      this.buildJobButtons();
      this.buildCombatButtons();
   }

   private void buildJobButtons() {
      int popupX = this.leftPos + 176 + 10;
      int popupY = this.topPos + 18;

      for (int i = 0; i < JOBS.length; i++) {
         WorkerJob job = JOBS[i];
         Button button = Button.builder(job.displayName(), b -> {
            this.setJob(job);
            this.jobMenuOpen = false;
            this.refreshJobButtons();
         }).bounds(popupX, popupY + i * 20, 100, 18).build();
         button.visible = false;
         this.jobButtons[i] = button;
         this.addRenderableWidget(button);
      }
   }

   private void refreshJobButtons() {
      for (Button button : this.jobButtons) {
         if (button != null) {
            button.visible = this.jobMenuOpen;
         }
      }
   }

   private void setJob(WorkerJob job) {
      this.current = job;
      ClientPlayNetworking.send(new WorkerPayloads.SetJob(0, ((WorkerMenu)this.menu).workerId(), job.id()));
   }

   private void startWork() {
      if (this.current == WorkerJob.IDLE) {
         this.jobMenuOpen = true;
         this.refreshJobButtons();
      } else {
         this.setJob(this.current);
      }
   }

   public void applyServerJob(WorkerJob job) {
      this.current = job;
      this.refreshJobButtons();
   }

   private void sendName() {
      String name = this.nameBox == null ? "" : this.nameBox.getValue().trim();
      if (!name.isEmpty()) {
         ClientPlayNetworking.send(new WorkerPayloads.SetName(0, ((WorkerMenu)this.menu).workerId(), name));
      }
   }

   private void buildCombatButtons() {
      int cx = this.leftPos + 176 + 10;
      int cy = this.topPos + 116;
      this.followButton = Button.builder(Component.empty(), b -> {
         this.followOn = !this.followOn;
         this.sendCombat();
         this.refreshCombatLabels();
      }).bounds(cx, cy, 100, 18).build();
      this.attackButton = Button.builder(Component.empty(), b -> {
         this.attackOn = !this.attackOn;
         this.sendCombat();
         this.refreshCombatLabels();
      }).bounds(cx, cy + 22, 100, 18).build();
      this.addRenderableWidget(this.followButton);
      this.addRenderableWidget(this.attackButton);
      this.refreshCombatLabels();
   }

   private void refreshCombatLabels() {
      if (this.followButton != null) {
         this.followButton.setMessage(Component.translatable(this.followOn ? "gui.jiangshidao.worker.follow.on" : "gui.jiangshidao.worker.follow.off"));
      }

      if (this.attackButton != null) {
         this.attackButton.setMessage(Component.translatable(this.attackOn ? "gui.jiangshidao.worker.attack.on" : "gui.jiangshidao.worker.attack.off"));
      }
   }

   private void sendCombat() {
      ClientPlayNetworking.send(new WorkerPayloads.SetCombat(((WorkerMenu)this.menu).workerId(), this.followOn, this.attackOn));
   }

   public void applyCombat(boolean follow, boolean attack) {
      this.followOn = follow;
      this.attackOn = attack;
      this.refreshCombatLabels();
   }

   public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractBackground(graphics, mouseX, mouseY, partialTick);
      int x = this.leftPos;
      int y = this.topPos;
      graphics.fill(RenderPipelines.GUI, x - 1, y - 1, x + this.imageWidth + 1, y + this.imageHeight + 1, -13948117);
      graphics.fill(RenderPipelines.GUI, x, y, x + this.imageWidth, y + this.imageHeight, -3750202);
      this.drawTalentPanel(graphics, x, y);
      // 职业列表不再绘制整块灰色底板：按钮本身自带外观，与下方"跟随/战斗"一致，
      // 背景留空让游戏画面透出来。

      graphics.fill(RenderPipelines.GUI, x + 6, y + 94, x + this.imageWidth - 6, y + 95, -7631989);
      graphics.fill(RenderPipelines.GUI, x + 6, y + 160, x + this.imageWidth - 6, y + 161, -7631989);

      for (Slot slot : ((WorkerMenu)this.menu).slots) {
         graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x + slot.x - 1, y + slot.y - 1, 18, 18);
      }
   }

   protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
      super.extractLabels(graphics, mouseX, mouseY);
      graphics.text(this.font, Component.translatable("gui.jiangshidao.worker.equip"), 28, 20, -12566464, false);
      graphics.text(this.font, Component.translatable("gui.jiangshidao.worker.pack"), 28, 88, -12566464, false);
      graphics.text(this.font, Component.translatable("gui.jiangshidao.worker.current", new Object[]{this.current.displayName()}), 100, 62, -16760704, false);
   }

   private void drawTalentPanel(GuiGraphicsExtractor graphics, int x, int y) {
      Talent[] all = Talent.values();
      CompoundTag data = ((WorkerMenu)this.menu).talentData();
      int pw = 124;
      int px = Math.max(4, x - pw - 8);
      int rowH = 14;
      int ph = 15 + all.length * rowH;
      graphics.fill(RenderPipelines.GUI, px - 1, y - 1, px + pw + 1, y + ph + 1, -13948117);
      graphics.fill(RenderPipelines.GUI, px, y, px + pw, y + ph, -3750202);
      graphics.text(this.font, Component.translatable("gui.jiangshidao.worker.talents"), px + 4, y + 4, -12566464, false);
      int rowY = y + 16;

      for (Talent talent : all) {
         int level = 0;
         String value = "";
         if (data.contains(talent.id())) {
            CompoundTag row = data.getCompoundOrEmpty(talent.id());
            level = row.getIntOr("lv", 0);
            value = row.getStringOr("v", "");
         }

         graphics.text(this.font, Component.translatable(talent.translationKey()), px + 4, rowY, level > 0 ? -14671840 : -9408400, false);
         String stars = level > 0 ? Talent.stars(level) : "—";
         graphics.text(this.font, stars, px + 40, rowY, level > 0 ? -4684277 : -7303024, false);
         graphics.text(this.font, value, px + pw - 6 - this.font.width(value), rowY, -16752640, false);
         rowY += rowH;
      }
   }

   public boolean isPauseScreen() {
      return false;
   }
}
