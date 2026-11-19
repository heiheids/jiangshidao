package com.heishan.jiangshidao.block;

import com.heishan.jiangshidao.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * TalismanPaperBlockEntity —— 方块实体，负责该方块的状态与刻逻辑。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class TalismanPaperBlockEntity extends BlockEntity {
   private String selectedType = "";

   public TalismanPaperBlockEntity(BlockPos pos, BlockState state) {
      super(ModBlockEntities.TALISMAN_PAPER.get(), pos, state);
   }

   public String selectedType() {
      return this.selectedType;
   }

   public void setSelectedType(String type) {
      this.selectedType = type == null ? "" : type;
      this.setChanged();
   }

   public boolean hasSelection() {
      return !this.selectedType.isEmpty();
   }

   protected void saveAdditional(ValueOutput tag) {
      super.saveAdditional(tag);
      tag.putString("SelectedType", this.selectedType);
   }

   protected void loadAdditional(ValueInput tag) {
      super.loadAdditional(tag);
      this.selectedType = tag.getStringOr("SelectedType", "");
   }

   public Component statusText() {
      return !this.hasSelection()
         ? Component.translatable("message.jiangshidao.paper.noselect")
         : Component.translatable("message.jiangshidao.paper.selected", new Object[]{this.selectedType});
   }

   private static Direction facingOf(BlockState state) {
      return Direction.UP;
   }
}
