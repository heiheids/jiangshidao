package com.heishan.jiangshidao.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * GhostWomanModel。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class GhostWomanModel extends EntityModel<GhostWomanRenderState> {
   private static final int TEX_W = 128;
   private static final int TEX_H = 128;
   private static final float BASE_LEAN_DEG = 18.0F;
   private static final float MAX_EXTRA_LEAN_DEG = 8.0F;
   private static final float GROUND_LEAN_FACTOR = 0.35F;
   private final ModelPart head;
   private final ModelPart torso;
   private final ModelPart skirt;
   private final ModelPart armLeft;
   private final ModelPart armRight;
   private final ModelPart armLeftLower;
   private final ModelPart armRightLower;
   private final float torsoBaseY;
   private final float skirtBaseY;

   public GhostWomanModel(ModelPart root) {
      super(root);
      this.torso = root.getChild("torso");
      this.skirt = root.getChild("skirt");
      this.head = this.torso.getChild("head");
      this.armLeft = this.torso.getChild("arm_left_upper");
      this.armRight = this.torso.getChild("arm_right_upper");
      this.armLeftLower = this.armLeft.getChild("arm_left_lower");
      this.armRightLower = this.armRight.getChild("arm_right_lower");
      this.torsoBaseY = this.torso.y;
      this.skirtBaseY = this.skirt.y;
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      PartDefinition torsoDef = root.addOrReplaceChild(
         "torso", CubeListBuilder.create().texOffs(87, 1).addBox(-3.0F, -12.0F, -2.0F, 6.0F, 12.0F, 4.0F), PartPose.offset(0.0F, 2.0F, 0.0F)
      );
      PartDefinition headPart = torsoDef.addOrReplaceChild(
         "head", CubeListBuilder.create().texOffs(53, 1).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.offset(0.0F, -20.0F, 0.0F)
      );
      headPart.addOrReplaceChild("hair_top", CubeListBuilder.create().texOffs(81, 36).addBox(-5.0F, -2.0F, -5.0F, 10.0F, 2.0F, 10.0F), PartPose.ZERO);
      headPart.addOrReplaceChild("hair_back", CubeListBuilder.create().texOffs(21, 36).addBox(-4.0F, -1.0F, 2.0F, 8.0F, 11.0F, 3.0F), PartPose.ZERO);
      headPart.addOrReplaceChild("hair_left", CubeListBuilder.create().texOffs(109, 1).addBox(4.0F, -1.0F, -3.0F, 2.0F, 9.0F, 7.0F), PartPose.ZERO);
      headPart.addOrReplaceChild("hair_right", CubeListBuilder.create().texOffs(1, 36).addBox(-6.0F, -1.0F, -3.0F, 2.0F, 9.0F, 7.0F), PartPose.ZERO);
      headPart.addOrReplaceChild("hair_long", CubeListBuilder.create().texOffs(1, 1).addBox(-4.0F, -1.0F, 4.0F, 8.0F, 31.0F, 2.0F), PartPose.ZERO);
      torsoDef.addOrReplaceChild("shoulder", CubeListBuilder.create().texOffs(17, 68).addBox(-4.0F, -13.0F, -2.0F, 8.0F, 1.0F, 4.0F), PartPose.ZERO);
      torsoDef.addOrReplaceChild("collar", CubeListBuilder.create().texOffs(43, 68).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 1.0F, 4.0F), PartPose.ZERO);
      torsoDef.addOrReplaceChild("placket", CubeListBuilder.create().texOffs(123, 36).addBox(0.0F, -12.0F, -3.0F, 1.0F, 11.0F, 1.0F), PartPose.ZERO);
      PartDefinition armLeft = torsoDef.addOrReplaceChild(
         "arm_left_upper", CubeListBuilder.create().texOffs(45, 36).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F), PartPose.offset(5.0F, -10.0F, 0.0F)
      );
      PartDefinition armLeftLower = armLeft.addOrReplaceChild(
         "arm_left_lower", CubeListBuilder.create().texOffs(1, 54).addBox(-1.5F, 0.0F, -2.0F, 3.0F, 8.0F, 4.0F), PartPose.offset(0.0F, 10.0F, 0.0F)
      );
      PartDefinition cuffLeft = armLeftLower.addOrReplaceChild(
         "cuff_left", CubeListBuilder.create().texOffs(99, 54).addBox(-1.5F, 0.0F, -2.0F, 3.0F, 2.0F, 4.0F), PartPose.offset(0.0F, 8.0F, 0.0F)
      );
      cuffLeft.addOrReplaceChild(
         "hand_left", CubeListBuilder.create().texOffs(71, 54).addBox(-1.5F, 0.0F, -1.0F, 3.0F, 4.0F, 3.0F), PartPose.offset(0.0F, 2.0F, 0.0F)
      );
      PartDefinition armRight = torsoDef.addOrReplaceChild(
         "arm_right_upper", CubeListBuilder.create().texOffs(63, 36).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F), PartPose.offset(-5.0F, -10.0F, 0.0F)
      );
      PartDefinition armRightLower = armRight.addOrReplaceChild(
         "arm_right_lower", CubeListBuilder.create().texOffs(17, 54).addBox(-1.5F, 0.0F, -2.0F, 3.0F, 8.0F, 4.0F), PartPose.offset(0.0F, 10.0F, 0.0F)
      );
      PartDefinition cuffRight = armRightLower.addOrReplaceChild(
         "cuff_right", CubeListBuilder.create().texOffs(1, 68).addBox(-1.5F, 0.0F, -2.0F, 3.0F, 2.0F, 4.0F), PartPose.offset(0.0F, 8.0F, 0.0F)
      );
      cuffRight.addOrReplaceChild(
         "hand_right", CubeListBuilder.create().texOffs(85, 54).addBox(-1.5F, 0.0F, -1.0F, 3.0F, 4.0F, 3.0F), PartPose.offset(0.0F, 2.0F, 0.0F)
      );
      PartDefinition skirtDef = root.addOrReplaceChild(
         "skirt", CubeListBuilder.create().texOffs(23, 1).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 20.0F, 6.0F), PartPose.offset(0.0F, 2.0F, 0.0F)
      );
      skirtDef.addOrReplaceChild(
         "hem", CubeListBuilder.create().texOffs(33, 54).addBox(-5.0F, 20.0F, -4.0F, 10.0F, 2.0F, 8.0F), PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      return LayerDefinition.create(mesh, 128, 128);
   }

   public void setupAnim(GhostWomanRenderState state) {
      float ageInTicks = state.ageInTicks;
      float active = Mth.clamp(state.horizontalSpeed / 0.35F, 0.0F, 1.0F);
      float BASE_LEAN_RAD = (float) (Math.PI / 10);
      float MAX_EXTRA_LEAN_RAD = 0.13962634F;
      float baseLean = state.airborne ? (float) (Math.PI / 10) : 0.10995574F;
      float leanX = baseLean + 0.13962634F * active;
      this.torso.xRot = leanX;
      this.torso.zRot = Mth.sin(ageInTicks * 0.12F) * 0.05F;
      float netHeadYaw = state.yRot - state.bodyRot;
      float headPitch = state.xRot;
      this.head.xRot = headPitch * 0.35F * (float) (Math.PI / 180.0);
      this.head.yRot = netHeadYaw * 0.5F * (float) (Math.PI / 180.0);
      this.skirt.xRot = leanX;
      this.skirt.zRot = Mth.sin(ageInTicks * 0.04F) * 0.012F;
      float ARM_PITCH = -1.466F;
      float FOREARM_EXTRA = -0.12F;
      this.armLeft.xRot = -1.466F;
      this.armRight.xRot = -1.466F;
      this.armLeftLower.xRot = -0.12F;
      this.armRightLower.xRot = -0.12F;
      float spread = 0.06F + Math.abs(leanX) * 0.05F;
      this.armLeft.zRot = -spread;
      this.armRight.zRot = spread;
      float floatBob = Mth.sin(ageInTicks * 0.06F) * 0.35F;
      this.torso.y = this.torsoBaseY - floatBob;
      this.skirt.y = this.skirtBaseY - floatBob * 0.5F;
   }
}
