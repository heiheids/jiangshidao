package com.heishan.jiangshidao.block;

import com.heishan.jiangshidao.registry.ModBlockEntities;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 铜铃方块实体：记录这只铜铃绑定的怪物。
 *
 * <p>绑定信息必须存在方块实体里，而不是靠"就近扫描降服单位"推断。原因：
 * {@code WorkerData}（名字 / 职业 / home）存在服务端持久化数据里，客户端读不到，
 * 早先按坐标反查的方案在客户端永远失败，表现为"收过怪的铜铃放下后仍显示未绑定"。
 * 方块实体的数据会随区块同步给客户端，因此 HUD 能直接读到绑定关系。
 *
 * <p>绑定一直保持，直到怪物真正死亡（死亡时由服务端清除）。
 *
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class BronzeBellBlockEntity extends BlockEntity {
   private UUID monsterId;
   private String monsterName = "";
   /** 这只铃绑定的怪物该干什么活（WorkerJob.id()），空串表示未分配。 */
   private String jobId = "";

   public BronzeBellBlockEntity(BlockPos pos, BlockState state) {
      super(ModBlockEntities.BRONZE_BELL.get(), pos, state);
   }

   /**
    * 绑定一只怪物。
    *
    * @param id   怪物 UUID，用于服务端反查（死亡判定、挖掘时回收）
    * @param name 显示名，供客户端 HUD 直接使用
    */
   public void bind(UUID id, String name) {
      this.monsterId = id;
      this.monsterName = name == null ? "" : name;
      this.markUpdated();
   }

   /** 解除绑定（怪物死亡或铃被回收时调用）。 */
   public void unbind() {
      this.monsterId = null;
      this.monsterName = "";
      this.jobId = "";
      this.markUpdated();
   }

   public boolean isBound() {
      return this.monsterId != null;
   }

   public UUID monsterId() {
      return this.monsterId;
   }

   public String monsterName() {
      return this.monsterName;
   }

   /** 读取这只铃记录的任务 id（可能为空串）。 */
   public String jobId() {
      return this.jobId;
   }

   /** 记录任务 id。 */
   public void setJobId(String id) {
      this.jobId = id == null ? "" : id;
      this.markUpdated();
   }

   /** 写入后立刻同步给客户端并落盘。 */
   private void markUpdated() {
      this.setChanged();
      if (this.level != null && !this.level.isClientSide()) {
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
      }
   }

   protected void saveAdditional(ValueOutput tag) {
      super.saveAdditional(tag);
      if (this.monsterId != null) {
         tag.putString("MonsterId", this.monsterId.toString());
      }

      tag.putString("MonsterName", this.monsterName);
      tag.putString("JobId", this.jobId);
   }

   protected void loadAdditional(ValueInput tag) {
      super.loadAdditional(tag);
      String id = tag.getStringOr("MonsterId", "");
      if (!id.isEmpty()) {
         try {
            this.monsterId = UUID.fromString(id);
         } catch (IllegalArgumentException invalidUuid) {
            this.monsterId = null;
         }
      } else {
         this.monsterId = null;
      }

      this.monsterName = tag.getStringOr("MonsterName", "");
      this.jobId = tag.getStringOr("JobId", "");
   }

   public CompoundTag getUpdateTag(Provider registries) {
      return this.saveCustomOnly(registries);
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   /** 取指定位置的铜铃方块实体；不是铜铃则返回 null。 */
   public static BronzeBellBlockEntity at(Level level, BlockPos pos) {
      if (level == null) {
         return null;
      } else {
         return level.getBlockEntity(pos) instanceof BronzeBellBlockEntity bell ? bell : null;
      }
   }
}
