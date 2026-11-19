package com.heishan.jiangshidao.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.util.Mth;

/**
 * JiangshiModel。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class JiangshiModel extends HumanoidModel<HumanoidRenderState> {
   public static final float ARM_PITCH = -1.466F;
   private final boolean brute;

   public JiangshiModel(ModelPart root, boolean brute) {
      super(root);
      this.brute = brute;
   }

   public static LayerDefinition createBodyLayer() {
      return createBodyLayer(false);
   }

   public static LayerDefinition createBruteLayer() {
      return createBodyLayer(true);
   }

   public static LayerDefinition createBodyLayer(boolean brute) {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      int torsoHalfW = brute ? 7 : 4;
      int torsoHalfD = brute ? 4 : 3;
      int torsoH = 9;
      float shoulderX = brute ? 7.6F : 4.8F;
      int armW = brute ? 6 : 4;
      int armH = brute ? 10 : 9;
      int armD = brute ? 6 : 4;
      PartDefinition head = root.addOrReplaceChild(
         "head",
         CubeListBuilder.create().texOffs(brute ? 83 : 41, brute ? 1 : 1).addBox(-4.5F, -8.0F, -4.5F, 9.0F, 8.0F, 9.0F),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      head.addOrReplaceChild(
         "hat_band", CubeListBuilder.create().texOffs(brute ? 27 : 1, brute ? 21 : 23).addBox(-6.5F, -10.5F, -6.0F, 13.0F, 3.0F, 12.0F), PartPose.ZERO
      );
      head.addOrReplaceChild(
         "hat_dome", CubeListBuilder.create().texOffs(brute ? 79 : 73, brute ? 21 : 23).addBox(-5.5F, -13.0F, -5.0F, 11.0F, 3.0F, 10.0F), PartPose.ZERO
      );
      head.addOrReplaceChild(
         "hat_crown", CubeListBuilder.create().texOffs(brute ? 17 : 1, brute ? 39 : 40).addBox(-3.5F, -14.5F, -3.0F, 7.0F, 2.0F, 6.0F), PartPose.ZERO
      );
      head.addOrReplaceChild(
         "hat_ring", CubeListBuilder.create().texOffs(brute ? 45 : 29, brute ? 39 : 40).addBox(-1.5F, -15.0F, -1.5F, 3.0F, 1.0F, 3.0F), PartPose.ZERO
      );
      head.addOrReplaceChild(
         "hat_knob", CubeListBuilder.create().texOffs(brute ? 59 : 117, brute ? 39 : 23).addBox(-1.0F, -16.0F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.ZERO
      );
      head.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F), PartPose.ZERO);
      PartDefinition body = root.addOrReplaceChild(
         "body",
         CubeListBuilder.create().texOffs(brute ? 35 : 79, brute ? 1 : 1).addBox(-torsoHalfW, 0.0F, -torsoHalfD, torsoHalfW * 2, 9.0F, torsoHalfD * 2),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      int skirtW = brute ? 10 : 11;
      int skirtD = brute ? 6 : 8;
      body.addOrReplaceChild("skirt", CubeListBuilder.create().texOffs(1, 1).addBox(-skirtW / 2.0F, 9.0F, -skirtD / 2.0F, skirtW, 12.0F, skirtD), PartPose.ZERO);
      root.addOrReplaceChild(
         "right_arm",
         CubeListBuilder.create().texOffs(brute ? 1 : 111, brute ? 21 : 1).addBox(-armW / 2.0F, 0.0F, -armD / 2.0F, armW, armH, armD),
         PartPose.offsetAndRotation(-shoulderX, 2.0F, 0.0F, -1.466F, 0.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "left_arm",
         CubeListBuilder.create().texOffs(brute ? 1 : 111, brute ? 21 : 1).mirror().addBox(-armW / 2.0F, 0.0F, -armD / 2.0F, armW, armH, armD),
         PartPose.offsetAndRotation(shoulderX, 2.0F, 0.0F, -1.466F, 0.0F, 0.0F)
      );
      int legH = 9;
      int legW = brute ? 3 : 4;
      int legD = brute ? 4 : 5;
      float legX = brute ? 1.7F : 2.2F;
      root.addOrReplaceChild(
         "right_leg",
         CubeListBuilder.create().texOffs(brute ? 1 : 53, brute ? 39 : 23).addBox(-legW / 2.0F, 0.0F, -legD / 2.0F, legW, 9.0F, legD),
         PartPose.offset(-legX, 15.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "left_leg",
         CubeListBuilder.create().texOffs(brute ? 1 : 53, brute ? 39 : 23).mirror().addBox(-legW / 2.0F, 0.0F, -legD / 2.0F, legW, 9.0F, legD),
         PartPose.offset(legX, 15.0F, 0.0F)
      );
      return LayerDefinition.create(mesh, 128, 128);
   }

   public void setupAnim(HumanoidRenderState state) {
      super.setupAnim(state);
      float limbSwing = state.walkAnimationPos;
      float limbSwingAmount = state.walkAnimationSpeed;
      float ageInTicks = state.ageInTicks;
      this.rightLeg.xRot = 0.0F;
      this.rightLeg.yRot = 0.0F;
      this.rightLeg.zRot = 0.0F;
      this.leftLeg.xRot = 0.0F;
      this.leftLeg.yRot = 0.0F;
      this.leftLeg.zRot = 0.0F;
      float armSway = Mth.cos(limbSwing * 0.6662F) * 0.08F * limbSwingAmount;
      this.rightArm.xRot = -1.466F + armSway;
      this.leftArm.xRot = -1.466F - armSway;

      // 僵尸手臂是本模组的固定造型（前伸 84°），手持物品会被压在身侧朝下，
      // 正常视角基本看不到手里的镐/工具。持物时把手臂抬高一些，
      // 让手上的工具露出来（仅影响持物的那只手）。
      if (state.rightHandItemStack != null && !state.rightHandItemStack.isEmpty()) {
         this.rightArm.xRot = -1.1F + armSway;
      }

      if (state.leftHandItemStack != null && !state.leftHandItemStack.isEmpty()) {
         this.leftArm.xRot = -1.1F - armSway;
      }
      float idleBob = Mth.sin(ageInTicks * 0.15F) * 0.02F;
      float hopBob = Mth.abs(Mth.sin(ageInTicks * 0.6F)) * 0.1F * Math.min(1.0F, limbSwingAmount * 2.0F);
      float bob = idleBob + hopBob;
      this.body.y = bob;
      this.head.y = bob;
      this.rightArm.y = bob;
      this.leftArm.y = bob;
      if (this.brute) {
         this.body.z = 0.0F;
      }
   }
}
