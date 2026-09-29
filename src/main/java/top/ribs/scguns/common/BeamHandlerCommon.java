package top.ribs.scguns.common;


import top.ribs.scguns.util.ScEnchants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.common.Tags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams.Builder;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import top.ribs.scguns.Config;
import top.ribs.scguns.common.network.ServerPlayHandler;
import top.ribs.scguns.init.ModTags;

public class BeamHandlerCommon {
   public BeamHandlerCommon() {
      super();
   }

   public static class BeamMiningManager {
      private static final Map<UUID, Integer> playerBreakingIds = new HashMap<>();
      private static final Map<BlockPos, BeamHandlerCommon.BeamMiningManager.MiningProgress> miningProgress = new HashMap<>();
      // Breaker ids count downwards so they can never equal an entity id. That matters because
      // ServerLevel#destroyBlockProgress skips the player whose entity id equals the breaker id - vanilla
      // does not have to send a miner their own crack, since the client tracks its own mining locally -
      // and entity ids are only ever >= 0. With the old counter starting at 1, the first player to fire a
      // mining gun was handed the id 1, and a world where that player's own entity id is also 1 gets no
      // crack packets at all: the server still deepens the stage (a probe run while diagnosing this
      // printed 1, 3, 5, 6, 8, 9 going out) and nothing is ever drawn, which is what the player saw. Negative ids also
      // keep clear of the ids the client uses for its own mining, which is why the id is not simply the
      // player's.
      private static int nextBreakerId = -1;
      private static final long RESET_TIMEOUT = 1000L;
      private static final double GLASS_PENETRATION_DAMAGE_REDUCTION = 0.15;

      public BeamMiningManager() {
         super();
      }

      public static HitResult getBeamHitResult(Level world, Vec3 startVec, Vec3 endVec, Entity shooter, double maxDistance) {
         Vec3 currentPos = startVec;
         Vec3 direction = endVec.subtract(startVec).normalize();
         List<BlockHitResult> glassPenetrations = new ArrayList<>();
         double distanceTraveled = 0.0;

         for (double remainingDamageMultiplier = 1.0; distanceTraveled < maxDistance; distanceTraveled = currentPos.subtract(startVec).length()) {
            Vec3 nextEndVec = currentPos.add(direction.scale(maxDistance - distanceTraveled));
            BlockHitResult blockHit = world.clip(new ClipContext(currentPos, nextEndVec, Block.COLLIDER, Fluid.NONE, shooter));
            if (blockHit.getType() == Type.MISS) {
               EntityHitResult entityHit = ServerPlayHandler.rayTraceEntities(world, shooter, currentPos, nextEndVec);
               if (entityHit != null) {
                  BeamHandlerCommon.BeamMiningManager.ExtendedEntityHitResult extendedEntityHit = BeamHandlerCommon.BeamMiningManager.ExtendedEntityHitResult.fromEntityHitResult(
                     entityHit
                  );
                  extendedEntityHit.setDamageMultiplier(remainingDamageMultiplier);
                  return extendedEntityHit;
               }

               return blockHit;
            }

            BlockState hitState = world.getBlockState(blockHit.getBlockPos());
            if (!isGlassBlock(hitState)) {
               EntityHitResult entityHit = ServerPlayHandler.rayTraceEntities(world, shooter, currentPos, blockHit.getLocation());
               if (entityHit != null) {
                  BeamHandlerCommon.BeamMiningManager.ExtendedEntityHitResult extendedEntityHit = BeamHandlerCommon.BeamMiningManager.ExtendedEntityHitResult.fromEntityHitResult(
                     entityHit
                  );
                  extendedEntityHit.setDamageMultiplier(remainingDamageMultiplier);
                  return extendedEntityHit;
               }

               BeamHandlerCommon.BeamMiningManager.ExtendedBlockHitResult extendedBlockHit = BeamHandlerCommon.BeamMiningManager.ExtendedBlockHitResult.fromBlockHitResult(
                  blockHit
               );
               extendedBlockHit.setDamageMultiplier(remainingDamageMultiplier);
               extendedBlockHit.setGlassPenetrations(glassPenetrations);
               return extendedBlockHit;
            }

            glassPenetrations.add(blockHit);
            remainingDamageMultiplier *= 0.85;
            currentPos = blockHit.getLocation().add(direction.scale(0.01));
         }

         return BlockHitResult.miss(
            endVec, Direction.UP, new BlockPos(Mth.floor(endVec.x), Mth.floor(endVec.y), Mth.floor(endVec.z))
         );
      }

      private static boolean isGlassBlock(BlockState state) {
         // 0.5.5 asked for `#forge:glass`. That tag does not exist in 1.21.1: NeoForge renamed the
         // common tags to `c:`, which is also what this port's own data files use, so the old key
         // resolved to nothing and every tagged glass (stained glass, modded glass) stopped counting
         // as glass - the beam refused to pass through it instead of continuing. Tags.Blocks is the
         // 1.21.1 spelling of the same tag.
         return state.is(Tags.Blocks.GLASS_BLOCKS)
            || state.is(Blocks.GLASS)
            || state.is(Blocks.GLASS_PANE)
            || state.is(Blocks.TINTED_GLASS);
      }

      public static void updateBlockMining(Level world, BlockPos pos, ServerPlayer player, Gun modifiedGun) {
         BlockState state = world.getBlockState(pos);
         if (!state.isAir() && !isGlassBlock(state)) {
            if (!handleFragileBlock(world, pos, state, modifiedGun)) {
               if ((Boolean)Config.COMMON.gameplay.griefing.enableBeamMining.get() && modifiedGun.getGeneral().canMine()) {
                  handleBeamMining(world, pos, state, player, modifiedGun);
               }
            }
         }
      }

      private static boolean handleFragileBlock(Level world, BlockPos pos, BlockState state, Gun modifiedGun) {
         if ((Boolean)Config.COMMON.gameplay.griefing.enableGlassBreaking.get() && state.is(ModTags.Blocks.FRAGILE)) {
            float destroySpeed = state.getDestroySpeed(world, pos);
            if (destroySpeed < 0.0F) {
               return false;
            } else {
               float baseChance = ((Double)Config.COMMON.gameplay.griefing.fragileBaseBreakChance.get()).floatValue();
               float beamModifier = modifiedGun.getGeneral().getFireMode() == FireMode.BEAM ? 2.0F : 1.5F;
               float chance = baseChance * beamModifier / (destroySpeed + 1.0F);
               if (world.random.nextFloat() < chance) {
                  world.destroyBlock(pos, (Boolean)Config.COMMON.gameplay.griefing.fragileBlockDrops.get());
                  return true;
               } else {
                  return false;
               }
            }
         } else {
            return false;
         }
      }

      private static void handleBeamMining(Level world, BlockPos pos, BlockState state, ServerPlayer player, Gun modifiedGun) {
         float hardness = state.getDestroySpeed(world, pos);
         if (!(hardness < 0.0F)) {
            BeamHandlerCommon.BeamMiningManager.MiningProgress progress = miningProgress.computeIfAbsent(
               pos, k -> new BeamHandlerCommon.BeamMiningManager.MiningProgress(player.getUUID())
            );
            if (progress.minerId.equals(player.getUUID())) {
               progress.isActive = true;
               progress.lastUpdate = System.currentTimeMillis();
               float miningSpeed = modifiedGun.getGeneral().getMiningSpeed();
               float progressIncrement = miningSpeed / (hardness * 10.0F);
               // The stage as it stood *before* this update, which is what the break test below has
               // to look at: lastStage is assigned a few lines down, so testing it after that
               // assignment always sees 9 on the very tick the block finished (HANDOFF 83.10).
               int stageBeforeUpdate = progress.lastStage;
               progress.progress += progressIncrement;
               int newStage = Math.min((int)(progress.progress * 10.0F), 9);
               if (newStage != progress.lastStage) {
                  progress.lastStage = newStage;
                  if (world instanceof ServerLevel serverLevel) {
                     serverLevel.destroyBlockProgress(progress.breakerId, pos, newStage);
                  }
               }

               if (progress.progress >= 1.0F && stageBeforeUpdate >= 9) {
                  // Break only once stage 9 has already been on screen for a tick.
                  //
                  // The increment is miningSpeed / (hardness * 10), so a fast mining gun finishes a
                  // soft block inside a single tick: cr4k_mining_laser (14.0) does dirt at 2.8 per
                  // tick, so the first update reached 1.0, sent stage 9 and the -1 that clears it,
                  // then removed the block - the crack was never drawn for a single frame, which is
                  // what the player reported as "no break texture". The two slow beam guns (1.0)
                  // take 15 ticks on stone and always looked fine, which is why it only ever showed
                  // up on the fast one.
                  //
                  // `stageBeforeUpdate` is what makes this work. The first version of this fix
                  // tested `progress.lastStage` here, which had *just* been set to 9 by the lines
                  // above, so the condition held on the very first tick and nothing was deferred -
                  // the player reported it still broken. With the pre-update value the break lands
                  // one tick after stage 9 was sent, exactly the frame the client needs, and the
                  // mining rate is untouched. The alternative - pacing the stage increments - would
                  // slow mining down, which is a balance decision, not a fix.
                  if (world instanceof ServerLevel serverLevel) {
                     serverLevel.destroyBlockProgress(progress.breakerId, pos, -1);
                  }

                  miningProgress.remove(pos);
                  if (player.gameMode.getGameModeForPlayer() == GameType.CREATIVE) {
                     world.removeBlock(pos, false);
                  } else {
                     breakBlockWithEnchantments(world, pos, state, player);
                  }
               }
            }
         }
      }

      private static void breakBlockWithEnchantments(Level world, BlockPos pos, BlockState blockState, ServerPlayer player) {
         ItemStack weapon = player.getMainHandItem();
         BlockEntity blockEntity = blockState.hasBlockEntity() ? world.getBlockEntity(pos) : null;
         int silkTouchLevel = ScEnchants.level(weapon, Enchantments.SILK_TOUCH);
         if (silkTouchLevel > 0) {
            net.minecraft.world.level.block.Block.dropResources(blockState, world, pos, blockEntity, player, weapon);
            world.removeBlock(pos, false);
            world.levelEvent(2001, pos, net.minecraft.world.level.block.Block.getId(blockState));
         } else {
            int fortuneLevel = ScEnchants.level(weapon, Enchantments.FORTUNE);
            if (fortuneLevel > 0 && world instanceof ServerLevel serverLevel) {
               Builder builder = new Builder(serverLevel)
                  .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                  .withParameter(LootContextParams.TOOL, weapon)
                  .withOptionalParameter(LootContextParams.THIS_ENTITY, player)
                  .withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockEntity);
               List<ItemStack> drops = blockState.getDrops(builder);
               if (blockState.is(Tags.Blocks.ORES)) {
                  for (ItemStack drop : drops) {
                     net.minecraft.world.level.block.Block.popResource(world, pos, drop);
                  }
               } else {
                  for (ItemStack drop : drops) {
                     net.minecraft.world.level.block.Block.popResource(world, pos, drop);
                  }
               }
            } else {
               net.minecraft.world.level.block.Block.dropResources(blockState, world, pos, blockEntity, player, weapon);
            }

            world.removeBlock(pos, false);
            world.levelEvent(2001, pos, net.minecraft.world.level.block.Block.getId(blockState));
         }
      }

      public static void tickMiningProgress(Level world) {
         long currentTime = System.currentTimeMillis();
         Iterator<Entry<BlockPos, BeamHandlerCommon.BeamMiningManager.MiningProgress>> iterator = miningProgress.entrySet().iterator();

         while (iterator.hasNext()) {
            Entry<BlockPos, BeamHandlerCommon.BeamMiningManager.MiningProgress> entry = iterator.next();
            BeamHandlerCommon.BeamMiningManager.MiningProgress progress = entry.getValue();
            if (!progress.isActive && currentTime - progress.lastUpdate >= 1000L) {
               if (world instanceof ServerLevel serverLevel) {
                  serverLevel.destroyBlockProgress(progress.breakerId, entry.getKey(), -1);
               }

               iterator.remove();
            } else if (progress.isActive || currentTime - progress.lastUpdate < 50L) {
               progress.isActive = false;
            }
         }
      }

      public static class ExtendedBlockHitResult extends BlockHitResult {
         private double damageMultiplier = 1.0;
         private List<BlockHitResult> glassPenetrations = new ArrayList<>();

         public ExtendedBlockHitResult(Vec3 location, Direction direction, BlockPos blockPos, boolean insideBlock) {
            super(location, direction, blockPos, insideBlock);
         }

         public static BeamHandlerCommon.BeamMiningManager.ExtendedBlockHitResult fromBlockHitResult(BlockHitResult original) {
            return new BeamHandlerCommon.BeamMiningManager.ExtendedBlockHitResult(
               original.getLocation(), original.getDirection(), original.getBlockPos(), original.isInside()
            );
         }

         public void setDamageMultiplier(double multiplier) {
            this.damageMultiplier = multiplier;
         }

         public double getDamageMultiplier() {
            return this.damageMultiplier;
         }

         public void setGlassPenetrations(List<BlockHitResult> penetrations) {
            this.glassPenetrations = penetrations;
         }

         public List<BlockHitResult> getGlassPenetrations() {
            return this.glassPenetrations;
         }
      }

      public static class ExtendedEntityHitResult extends EntityHitResult {
         private double damageMultiplier = 1.0;

         public ExtendedEntityHitResult(Entity entity, Vec3 location) {
            super(entity, location);
         }

         public void setDamageMultiplier(double multiplier) {
            this.damageMultiplier = multiplier;
         }

         public double getDamageMultiplier() {
            return this.damageMultiplier;
         }

         public static BeamHandlerCommon.BeamMiningManager.ExtendedEntityHitResult fromEntityHitResult(EntityHitResult original) {
            return new BeamHandlerCommon.BeamMiningManager.ExtendedEntityHitResult(original.getEntity(), original.getLocation());
         }
      }

      private static class MiningProgress {
         float progress = 0.0F;
         long lastUpdate = System.currentTimeMillis();
         UUID minerId;
         int breakerId;
         boolean isActive;
         int lastStage;

         public MiningProgress(UUID minerId) {
            super();
            this.minerId = minerId;
            this.breakerId = BeamHandlerCommon.BeamMiningManager.playerBreakingIds
               .computeIfAbsent(minerId, k -> BeamHandlerCommon.BeamMiningManager.nextBreakerId--);
            this.isActive = true;
            this.lastStage = -1;
         }
      }
   }
}
