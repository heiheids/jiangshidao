package com.heishan.jiangshidao.item;

import com.heishan.jiangshidao.accessory.Accessories;
import com.heishan.jiangshidao.entity.FlyingSwordEntity;
import com.heishan.jiangshidao.registry.ModEffects;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * WoodSwordItem —— 物品行为（使用、命中、提示等）。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class WoodSwordItem extends Item {
   public static final ToolMaterial WOOD_TIER = new ToolMaterial(BlockTags.INCORRECT_FOR_WOODEN_TOOL, 180, 2.0F, 1.5F, 18, ItemTags.PLANKS);
   private static final double SLOW_RADIUS = 15.0;
   private static final int SLOW_DURATION = 100;
   private static final int SLOW_AMPLIFIER = 6;
   private static final int SLOW_COOLDOWN = 120;
   private final WoodSwordItem.Variant variant;
   public static final float TIAN_FA_THUNDER_MULTIPLIER = 1.5F;
   public static final float TIAN_FA_UNDEAD_MULTIPLIER = 1.5F;
   public static final int THUNDER_BURN_MULTIPLIER = 2;

   private float undeadBonusDamage() {
      return switch (this.variant) {
         case PEACH -> 3.0F;
         case THUNDER, TIAN_FA -> 5.0F;
      };
   }

   public static int sealTicksFor(WoodSwordItem.Variant variant) {
      return switch (variant) {
         case PEACH -> 120;
         case THUNDER -> 180;
         case TIAN_FA -> 220;
      };
   }

   private int onHitSealTicks() {
      return sealTicksFor(this.variant);
   }

   public WoodSwordItem(WoodSwordItem.Variant variant, Properties properties) {
      super(properties);
      this.variant = variant;
   }

   public WoodSwordItem.Variant variant() {
      return this.variant;
   }

   public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
      Optional<ResourceKey<Enchantment>> key = enchantment.unwrapKey();
      if (key.isEmpty()) {
         return false;
      }

      ResourceKey<Enchantment> id = key.get();
      return id.equals(Enchantments.UNBREAKING) || id.equals(Enchantments.MENDING);
   }

   public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
      super.hurtEnemy(stack, target, attacker);
      if (!target.level().isClientSide()) {
         if (isUndead(target)) {
            target.hurt(target.damageSources().indirectMagic(attacker, attacker), this.undeadBonusDamage());
            target.addEffect(new MobEffectInstance(ModEffects.SEALED, this.onHitSealTicks(), 0, false, true, true));
            if (target.level() instanceof ServerLevel server) {
               server.sendParticles(
                  ParticleTypes.ENCHANTED_HIT, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 10, 0.25, 0.25, 0.25, 0.1
               );
               server.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.5F, 1.6F);
            }
         }

         this.applyWoodEffect(target, attacker);
      }
   }

   private void applyWoodEffect(LivingEntity target, LivingEntity attacker) {
      if (target.level() instanceof ServerLevel server) {
         switch (this.variant) {
            case PEACH:
            default:
               break;
            case THUNDER:
               target.damageCooldownTime = 0;
               target.igniteForTicks(600);
               float burn = isUndead(target) ? 5.2F : 4.0F;
               target.hurt(server.damageSources().lava(), burn);
               server.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 14, 0.3, 0.4, 0.3, 0.03);
               server.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 0.7F, 1.4F);
               break;
            case TIAN_FA:
               target.damageCooldownTime = 0;
               float thunder = target.getMaxHealth() * 0.2F * 1.5F;
               if (isUndead(target)) {
                  thunder *= 1.45F;
               }

               if (thunder < 0.75F) {
                  thunder = 0.75F;
               }

               target.hurt(server.damageSources().lightningBolt(), thunder);
               server.sendParticles(
                  ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 18, 0.35, 0.45, 0.35, 0.15
               );
               server.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.6F, 1.5F);
         }
      }
   }

   public InteractionResult use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (!level.isClientSide() && player.isShiftKeyDown() && Accessories.insertIntoQuiver(player, stack)) {
         return InteractionResult.SUCCESS;
      }

      if (player.getCooldowns().isOnCooldown(stack)) {
         return InteractionResult.FAIL;
      }

      if (level.isClientSide()) {
         spawnClientGoldenParticles(level, player);
         return InteractionResult.SUCCESS;
      }

      AABB area = player.getBoundingBox().inflate(15.0);
      List<LivingEntity> monsters = level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive() && e instanceof Enemy);
      int count = 0;

      for (LivingEntity mob : monsters) {
         mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 6, false, true, true));
         count++;
      }

      if (level instanceof ServerLevel server) {
         spawnGoldenParticles(server, player);
         server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.5F, 1.6F);
      }

      int cooldown = Accessories.hasQuiver(player) ? Math.round(80.0F) : 120;
      player.getCooldowns().addCooldown(stack, cooldown);
      player.sendOverlayMessage(Component.literal("§e缓慢笼罩了 " + count + " 个怪物（5 秒）"));
      return InteractionResult.SUCCESS;
   }

   private static void spawnGoldenParticles(ServerLevel server, Player player) {
      int COUNT = 24;

      for (int i = 0; i < 24; i++) {
         double phi = Math.acos(1.0 - 2.0 * ((i + 0.5) / 24.0));
         double theta = Math.PI * (1.0 + Math.sqrt(5.0)) * i;
         double r = 3.0;
         double dx = Math.sin(phi) * Math.cos(theta) * r;
         double dy = Math.cos(phi) * r;
         double dz = Math.sin(phi) * Math.sin(theta) * r;
         server.sendParticles(ParticleTypes.WAX_ON, player.getX() + dx, player.getY() + 1.0 + dy, player.getZ() + dz, 1, 0.0, 0.0, 0.0, 0.0);
      }
   }

   private static void spawnClientGoldenParticles(Level level, Player player) {
      int COUNT = 24;

      for (int i = 0; i < 24; i++) {
         double phi = Math.acos(1.0 - 2.0 * ((i + 0.5) / 24.0));
         double theta = Math.PI * (1.0 + Math.sqrt(5.0)) * i;
         double r = 3.0;
         double dx = Math.sin(phi) * Math.cos(theta) * r;
         double dy = Math.cos(phi) * r;
         double dz = Math.sin(phi) * Math.sin(theta) * r;
         level.addParticle(ParticleTypes.WAX_ON, player.getX() + dx, player.getY() + 1.0 + dy, player.getZ() + dz, 0.0, 0.0, 0.0);
      }
   }

   public static FlyingSwordEntity spawnFlyingSword(Level level, Player player, ItemStack stack) {
      WoodSwordItem.Variant variant = stack.getItem() instanceof WoodSwordItem woodSword ? woodSword.variant : WoodSwordItem.Variant.PEACH;
      FlyingSwordEntity sword = new FlyingSwordEntity(level, player, stack, variant);
      Vec3 eye = player.getEyePosition();
      sword.setPos(eye.x, eye.y - 0.2, eye.z);
      sword.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.6F, 0.0F);
      level.addFreshEntity(sword);
      level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.7F, 1.3F);
      return sword;
   }

   public static void throwSword(Level level, Player player, ItemStack stack) {
      if (!level.isClientSide()) {
         WoodSwordItem.Variant variant = stack.getItem() instanceof WoodSwordItem woodSword ? woodSword.variant : WoodSwordItem.Variant.PEACH;
         FlyingSwordEntity sword = new FlyingSwordEntity(level, player, stack, variant);
         Vec3 eye = player.getEyePosition();
         sword.setPos(eye.x, eye.y - 0.2, eye.z);
         sword.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.6F, 0.0F);
         level.addFreshEntity(sword);
         level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.7F, 1.3F);
         if (!player.getAbilities().instabuild) {
            stack.shrink(1);
         }
      }
   }

   private static boolean isUndead(LivingEntity entity) {
      return entity.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD);
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, context, display, tooltip, flag);
      switch (this.variant) {
         case PEACH:
            tooltip.accept(Component.translatable("tooltip.jiangshidao.peach_wood_sword.desc").withStyle(ChatFormatting.RED));
            break;
         case THUNDER:
            tooltip.accept(Component.translatable("tooltip.jiangshidao.thunder_wood_sword.desc").withStyle(ChatFormatting.DARK_PURPLE));
            tooltip.accept(Component.translatable("tooltip.jiangshidao.thunder_wood_sword.attack").withStyle(ChatFormatting.BLUE));
            tooltip.accept(Component.translatable("tooltip.jiangshidao.thunder_wood_sword.burn").withStyle(ChatFormatting.GOLD));
            break;
         case TIAN_FA:
            tooltip.accept(Component.translatable("tooltip.jiangshidao.tian_fa_lei_ji_mu_sword.desc").withStyle(ChatFormatting.GOLD));
            tooltip.accept(Component.translatable("tooltip.jiangshidao.tian_fa_lei_ji_mu_sword.attack").withStyle(ChatFormatting.BLUE));
            tooltip.accept(Component.translatable("tooltip.jiangshidao.tian_fa_lei_ji_mu_sword.wrath").withStyle(ChatFormatting.LIGHT_PURPLE));
      }

      tooltip.accept(Component.translatable("tooltip.jiangshidao.wood_sword.slow").withStyle(ChatFormatting.YELLOW));
      tooltip.accept(Component.translatable("tooltip.jiangshidao.wood_sword.throw").withStyle(ChatFormatting.GOLD));
   }

   public enum Variant {
      PEACH("peach", 0.0F),
      THUNDER("thunder", 4.0F),
      TIAN_FA("tian_fa", 8.0F);

      private final String id;
      private final float attackBonus;

      Variant(String id, float attackBonus) {
         this.id = id;
         this.attackBonus = attackBonus;
      }

      public String id() {
         return this.id;
      }

      public float attackBonus() {
         return this.attackBonus;
      }

      public static WoodSwordItem.Variant byId(String id) {
         for (WoodSwordItem.Variant v : values()) {
            if (v.id.equals(id)) {
               return v;
            }
         }

         return PEACH;
      }
   }
}
