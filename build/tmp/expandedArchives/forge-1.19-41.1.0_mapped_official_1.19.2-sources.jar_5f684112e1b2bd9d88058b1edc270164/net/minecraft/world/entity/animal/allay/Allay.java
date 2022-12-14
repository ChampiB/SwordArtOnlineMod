package net.minecraft.world.entity.animal.allay;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.GameEventTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.vibrations.VibrationListener;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class Allay extends PathfinderMob implements InventoryCarrier, VibrationListener.VibrationListenerConfig {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int f_218298_ = 16;
   private static final Vec3i ITEM_PICKUP_REACH = new Vec3i(1, 1, 1);
   private static final int f_218300_ = 5;
   private static final float PATHFINDING_BOUNDING_BOX_PADDING = 0.5F;
   protected static final ImmutableList<SensorType<? extends Sensor<? super Allay>>> SENSOR_TYPES = ImmutableList.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS, SensorType.HURT_BY, SensorType.NEAREST_ITEMS);
   protected static final ImmutableList<MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(MemoryModuleType.PATH, MemoryModuleType.LOOK_TARGET, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryModuleType.WALK_TARGET, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryModuleType.HURT_BY, MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM, MemoryModuleType.LIKED_PLAYER, MemoryModuleType.LIKED_NOTEBLOCK_POSITION, MemoryModuleType.LIKED_NOTEBLOCK_COOLDOWN_TICKS, MemoryModuleType.ITEM_PICKUP_COOLDOWN_TICKS, MemoryModuleType.IS_PANICKING);
   public static final ImmutableList<Float> THROW_SOUND_PITCHES = ImmutableList.of(0.5625F, 0.625F, 0.75F, 0.9375F, 1.0F, 1.0F, 1.125F, 1.25F, 1.5F, 1.875F, 2.0F, 2.25F, 2.5F, 3.0F, 3.75F, 4.0F);
   private final DynamicGameEventListener<VibrationListener> f_218302_;
   private final SimpleContainer inventory = new SimpleContainer(1);
   private float holdingItemAnimationTicks;
   private float holdingItemAnimationTicks0;

   public Allay(EntityType<? extends Allay> p_218310_, Level p_218311_) {
      super(p_218310_, p_218311_);
      this.moveControl = new FlyingMoveControl(this, 20, true);
      this.setCanPickUpLoot(this.canPickUpLoot());
      this.f_218302_ = new DynamicGameEventListener<>(new VibrationListener(new EntityPositionSource(this, this.getEyeHeight()), 16, this, (VibrationListener.ReceivingEvent)null, 0.0F, 0));
   }

   protected Brain.Provider<Allay> brainProvider() {
      return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
   }

   protected Brain<?> makeBrain(Dynamic<?> p_218344_) {
      return AllayAi.makeBrain(this.brainProvider().makeBrain(p_218344_));
   }

   public Brain<Allay> getBrain() {
      return (Brain<Allay>)super.getBrain();
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0D).add(Attributes.FLYING_SPEED, (double)0.1F).add(Attributes.MOVEMENT_SPEED, (double)0.1F).add(Attributes.ATTACK_DAMAGE, 2.0D).add(Attributes.FOLLOW_RANGE, 48.0D);
   }

   protected PathNavigation createNavigation(Level p_218342_) {
      FlyingPathNavigation flyingpathnavigation = new FlyingPathNavigation(this, p_218342_);
      flyingpathnavigation.setCanOpenDoors(false);
      flyingpathnavigation.setCanFloat(true);
      flyingpathnavigation.setCanPassDoors(true);
      return flyingpathnavigation;
   }

   public void travel(Vec3 p_218382_) {
      if (this.isEffectiveAi() || this.isControlledByLocalInstance()) {
         if (this.isInWater()) {
            this.moveRelative(0.02F, p_218382_);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale((double)0.8F));
         } else if (this.isInLava()) {
            this.moveRelative(0.02F, p_218382_);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.5D));
         } else {
            this.moveRelative(this.getSpeed(), p_218382_);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale((double)0.91F));
         }
      }

      this.calculateEntityAnimation(this, false);
   }

   protected float getStandingEyeHeight(Pose p_218356_, EntityDimensions p_218357_) {
      return p_218357_.height * 0.6F;
   }

   public boolean causeFallDamage(float p_218321_, float p_218322_, DamageSource p_218323_) {
      return false;
   }

   public boolean hurt(DamageSource p_218339_, float p_218340_) {
      Entity $$3 = p_218339_.getEntity();
      if ($$3 instanceof Player player) {
         Optional<UUID> optional = this.getBrain().getMemory(MemoryModuleType.LIKED_PLAYER);
         if (optional.isPresent() && player.getUUID().equals(optional.get())) {
            return false;
         }
      }

      return super.hurt(p_218339_, p_218340_);
   }

   protected void playStepSound(BlockPos p_218364_, BlockState p_218365_) {
   }

   protected void checkFallDamage(double p_218316_, boolean p_218317_, BlockState p_218318_, BlockPos p_218319_) {
   }

   protected SoundEvent getAmbientSound() {
      return this.hasItemInSlot(EquipmentSlot.MAINHAND) ? SoundEvents.ALLAY_AMBIENT_WITH_ITEM : SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;
   }

   protected SoundEvent getHurtSound(DamageSource p_218369_) {
      return SoundEvents.ALLAY_HURT;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.ALLAY_DEATH;
   }

   protected float getSoundVolume() {
      return 0.4F;
   }

   protected void customServerAiStep() {
      this.level.getProfiler().push("allayBrain");
      this.getBrain().tick((ServerLevel)this.level, this);
      this.level.getProfiler().pop();
      this.level.getProfiler().push("allayActivityUpdate");
      AllayAi.updateActivity(this);
      this.level.getProfiler().pop();
      super.customServerAiStep();
   }

   public void aiStep() {
      super.aiStep();
      if (!this.level.isClientSide && this.isAlive() && this.tickCount % 10 == 0) {
         this.heal(1.0F);
      }

   }

   public void tick() {
      super.tick();
      if (this.level.isClientSide) {
         this.holdingItemAnimationTicks0 = this.holdingItemAnimationTicks;
         if (this.hasItemInHand()) {
            this.holdingItemAnimationTicks = Mth.clamp(this.holdingItemAnimationTicks + 1.0F, 0.0F, 5.0F);
         } else {
            this.holdingItemAnimationTicks = Mth.clamp(this.holdingItemAnimationTicks - 1.0F, 0.0F, 5.0F);
         }
      } else {
         this.f_218302_.getListener().tick(this.level);
      }

   }

   public boolean canPickUpLoot() {
      return !this.isOnPickupCooldown() && this.hasItemInHand();
   }

   public boolean hasItemInHand() {
      return !this.getItemInHand(InteractionHand.MAIN_HAND).isEmpty();
   }

   public boolean canTakeItem(ItemStack p_218380_) {
      return false;
   }

   private boolean isOnPickupCooldown() {
      return this.getBrain().checkMemory(MemoryModuleType.ITEM_PICKUP_COOLDOWN_TICKS, MemoryStatus.VALUE_PRESENT);
   }

   protected InteractionResult mobInteract(Player p_218361_, InteractionHand p_218362_) {
      ItemStack itemstack = p_218361_.getItemInHand(p_218362_);
      ItemStack itemstack1 = this.getItemInHand(InteractionHand.MAIN_HAND);
      if (itemstack1.isEmpty() && !itemstack.isEmpty()) {
         ItemStack itemstack3 = itemstack.copy();
         itemstack3.setCount(1);
         this.setItemInHand(InteractionHand.MAIN_HAND, itemstack3);
         if (!p_218361_.getAbilities().instabuild) {
            itemstack.shrink(1);
         }

         this.level.playSound(p_218361_, this, SoundEvents.ALLAY_ITEM_GIVEN, SoundSource.NEUTRAL, 2.0F, 1.0F);
         this.getBrain().setMemory(MemoryModuleType.LIKED_PLAYER, p_218361_.getUUID());
         return InteractionResult.SUCCESS;
      } else if (!itemstack1.isEmpty() && p_218362_ == InteractionHand.MAIN_HAND && itemstack.isEmpty()) {
         this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
         this.level.playSound(p_218361_, this, SoundEvents.ALLAY_ITEM_TAKEN, SoundSource.NEUTRAL, 2.0F, 1.0F);
         this.swing(InteractionHand.MAIN_HAND);

         for(ItemStack itemstack2 : this.getInventory().removeAllItems()) {
            BehaviorUtils.throwItem(this, itemstack2, this.position());
         }

         this.getBrain().eraseMemory(MemoryModuleType.LIKED_PLAYER);
         p_218361_.addItem(itemstack1);
         return InteractionResult.SUCCESS;
      } else {
         return super.mobInteract(p_218361_, p_218362_);
      }
   }

   public SimpleContainer getInventory() {
      return this.inventory;
   }

   protected Vec3i getPickupReach() {
      return ITEM_PICKUP_REACH;
   }

   public boolean wantsToPickUp(ItemStack p_218387_) {
      ItemStack itemstack = this.getItemInHand(InteractionHand.MAIN_HAND);
      return !itemstack.isEmpty() && itemstack.sameItemStackIgnoreDurability(p_218387_) && this.inventory.canAddItem(p_218387_);
   }

   protected void pickUpItem(ItemEntity p_218359_) {
      InventoryCarrier.pickUpItem(this, this, p_218359_);
   }

   protected void sendDebugPackets() {
      super.sendDebugPackets();
      DebugPackets.sendEntityBrain(this);
   }

   public boolean isFlapping() {
      return !this.isOnGround();
   }

   public void updateDynamicGameEventListener(BiConsumer<DynamicGameEventListener<?>, ServerLevel> p_218348_) {
      Level level = this.level;
      if (level instanceof ServerLevel serverlevel) {
         p_218348_.accept(this.f_218302_, serverlevel);
      }

   }

   public boolean updateDuplicationCooldown() {
      return this.animationSpeed > 0.3F;
   }

   public float getHoldingItemAnimationProgress(float p_218395_) {
      return Mth.lerp(p_218395_, this.holdingItemAnimationTicks0, this.holdingItemAnimationTicks) / 5.0F;
   }

   protected void dropEquipment() {
      super.dropEquipment();
      this.inventory.removeAllItems().forEach(this::spawnAtLocation);
      ItemStack itemstack = this.getItemBySlot(EquipmentSlot.MAINHAND);
      if (!itemstack.isEmpty() && !EnchantmentHelper.hasVanishingCurse(itemstack)) {
         this.spawnAtLocation(itemstack);
         this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
      }

   }

   public boolean removeWhenFarAway(double p_218384_) {
      return false;
   }

   public boolean shouldListen(ServerLevel p_218333_, GameEventListener p_218334_, BlockPos p_218335_, GameEvent p_218336_, GameEvent.Context p_218337_) {
      if (this.level == p_218333_ && !this.isRemoved() && !this.isNoAi()) {
         if (!this.brain.hasMemoryValue(MemoryModuleType.LIKED_NOTEBLOCK_POSITION)) {
            return true;
         } else {
            Optional<GlobalPos> optional = this.brain.getMemory(MemoryModuleType.LIKED_NOTEBLOCK_POSITION);
            return optional.isPresent() && optional.get().dimension() == p_218333_.dimension() && optional.get().pos().equals(p_218335_);
         }
      } else {
         return false;
      }
   }

   public void onSignalReceive(ServerLevel p_218325_, GameEventListener p_218326_, BlockPos p_218327_, GameEvent p_218328_, @Nullable Entity p_218329_, @Nullable Entity p_218330_, float p_218331_) {
      if (p_218328_ == GameEvent.NOTE_BLOCK_PLAY) {
         AllayAi.hearNoteblock(this, new BlockPos(p_218327_));
      }

   }

   public TagKey<GameEvent> getListenableEvents() {
      return GameEventTags.ALLAY_CAN_LISTEN;
   }

   public void addAdditionalSaveData(CompoundTag p_218367_) {
      super.addAdditionalSaveData(p_218367_);
      p_218367_.put("Inventory", this.inventory.createTag());
      VibrationListener.codec(this).encodeStart(NbtOps.INSTANCE, this.f_218302_.getListener()).resultOrPartial(LOGGER::error).ifPresent((p_218353_) -> {
         p_218367_.put("listener", p_218353_);
      });
   }

   public void readAdditionalSaveData(CompoundTag p_218350_) {
      super.readAdditionalSaveData(p_218350_);
      this.inventory.fromTag(p_218350_.getList("Inventory", 10));
      if (p_218350_.contains("listener", 10)) {
         VibrationListener.codec(this).parse(new Dynamic<>(NbtOps.INSTANCE, p_218350_.getCompound("listener"))).resultOrPartial(LOGGER::error).ifPresent((p_218346_) -> {
            this.f_218302_.updateListener(p_218346_, this.level);
         });
      }

   }

   protected boolean shouldStayCloseToLeashHolder() {
      return false;
   }

   public Iterable<BlockPos> iteratePathfindingStartNodeCandidatePositions() {
      AABB aabb = this.getBoundingBox();
      int i = Mth.floor(aabb.minX - 0.5D);
      int j = Mth.floor(aabb.maxX + 0.5D);
      int k = Mth.floor(aabb.minZ - 0.5D);
      int l = Mth.floor(aabb.maxZ + 0.5D);
      int i1 = Mth.floor(aabb.minY - 0.5D);
      int j1 = Mth.floor(aabb.maxY + 0.5D);
      return BlockPos.betweenClosed(i, i1, k, j, j1, l);
   }

   public Vec3 getLeashOffset() {
      return new Vec3(0.0D, (double)this.getEyeHeight() * 0.6D, (double)this.getBbWidth() * 0.1D);
   }
}