package top.ribs.scguns.event;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import top.ribs.scguns.Config;
import top.ribs.scguns.client.screen.GunBenchRecipe;
import top.ribs.scguns.common.recipe.GunBenchUnlockKeys;
import top.ribs.scguns.config.RaidConfig;
import top.ribs.scguns.entity.player.GunTier;
import top.ribs.scguns.entity.player.PlayerGunProgression;

@EventBusSubscriber(
   modid = "scguns",
   bus = Bus.GAME
)
public class GunProgressionEventHandler {
   public GunProgressionEventHandler() {
      super();
   }

   @SubscribeEvent
   public static void onItemPickup(ItemEntityPickupEvent.Pre event) {
      Player player = event.getPlayer();
      ItemStack stack = event.getItemEntity().getItem();
      if (!player.level().isClientSide) {
         PlayerGunProgression progression = PlayerGunProgression.get(player);
         if (progression.checkAndUpdateFromItem(stack)) {
            PlayerGunProgression.save(player, progression);
            sendTierUnlockedMessage(player, progression.getCurrentTier());
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerClone(Clone event) {
      if (event.isWasDeath()) {
         Player oldPlayer = event.getOriginal();
         Player newPlayer = event.getEntity();
         PlayerGunProgression oldProgression = PlayerGunProgression.get(oldPlayer);
         PlayerGunProgression.save(newPlayer, oldProgression);
      }
   }

   @SubscribeEvent
   public static void onItemCrafted(ItemCraftedEvent event) {
      Player player = event.getEntity();
      ItemStack stack = event.getCrafting();
      if (!player.level().isClientSide) {
         PlayerGunProgression progression = PlayerGunProgression.get(player);
         if (progression.checkAndUpdateFromItem(stack)) {
            PlayerGunProgression.save(player, progression);
            sendTierUnlockedMessage(player, progression.getCurrentTier());
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerLogin(PlayerLoggedInEvent event) {
      Player player = event.getEntity();
      if (!player.level().isClientSide) {
         // Login only syncs the stored progression; it deliberately does not announce a tier, so
         // reconnecting with a better gun in the bag does not spam the player on every join.
         PlayerGunProgression progression = PlayerGunProgression.get(player);
         if (updateFromInventory(player, progression)) {
            PlayerGunProgression.save(player, progression);
         }

         if (player instanceof ServerPlayer serverPlayer) {
            awardBenchRecipesForHeldBlueprints(serverPlayer);
         }
      }
   }

   /**
    * Unlock every gun bench recipe whose key item the player is already carrying.
    *
    * <p>The unlock advancements watch {@code inventory_changed}, which only fires on a <em>change</em>:
    * a player who already held a blueprint before those advancements existed never unlocks anything
    * from it, so the recipe book showed two recipes (the two that have no blueprint and are unlocked by
    * their tick fallback) instead of the eleven the copper blueprint covers (HANDOFF 39.6). Running this
    * on login makes the book correct regardless of when the blueprint arrived; a blueprint obtained
    * later still unlocks live, through the advancement.</p>
    *
    * <p>The key item is not always a blueprint: the four turret recipes key on the turret platform
    * ({@link GunBenchUnlockKeys}), so those are awarded here too instead of unlocking themselves
    * (HANDOFF section 43).</p>
    */
   private static void awardBenchRecipesForHeldBlueprints(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }

      List<RecipeHolder<?>> unlocked = new ArrayList<>();

      for (RecipeHolder<GunBenchRecipe> holder : level.getRecipeManager().getAllRecipesFor(GunBenchRecipe.Type.INSTANCE)) {
         GunBenchRecipe recipe = holder.value();

         for (ItemStack carried : player.getInventory().items) {
            if (GunBenchUnlockKeys.matches(recipe, carried)) {
               unlocked.add(holder);
               break;
            }
         }

         // The main inventory alone is 36 slots: a blueprint parked in the offhand is still
         // "carried", and the recipe book should say so (HANDOFF 42.3).
         if (!unlocked.contains(holder) && GunBenchUnlockKeys.matches(recipe, player.getOffhandItem())) {
            unlocked.add(holder);
         }
      }

      if (!unlocked.isEmpty()) {
         player.awardRecipes(unlocked);
      }
   }

   /**
    * Third acquisition path: a gun taken out of a chest, a loot bag or a villager trade arrives
    * through a container menu. 0.5.5 watched only pickups and vanilla crafting, so finding a
    * higher-tier gun in a structure raised the tier silently - and the whole point of the tier system
    * is that the world reacts to it.
    */
   @SubscribeEvent
   public static void onContainerClose(PlayerContainerEvent.Close event) {
      Player player = event.getEntity();
      if (!player.level().isClientSide) {
         PlayerGunProgression progression = PlayerGunProgression.get(player);
         if (updateFromInventory(player, progression)) {
            PlayerGunProgression.save(player, progression);
            sendTierUnlockedMessage(player, progression.getCurrentTier());
         }
      }
   }

   /** True when anything in the player's inventory raised the progression. */
   private static boolean updateFromInventory(Player player, PlayerGunProgression progression) {
      boolean updated = false;

      for (ItemStack stack : player.getInventory().items) {
         if (progression.checkAndUpdateFromItem(stack)) {
            updated = true;
         }
      }

      return updated;
   }

   /**
    * Announce a gun tier unlock.
    *
    * <p>The port used to print three terse lines ("Gun Tier Unlocked: X", "Enemies can now spawn
    * with:", "These Raids will now target you:"). The player asked for the consequence to be spelled
    * out in the sentence itself, so the second line now reads "【X】等级的敌人和袭击现在可能出现！"
    * and a level-up sound marks the moment, since chat alone scrolled past unnoticed.</p>
    * <p>HANDOFF section 81: the showProgressionMessages option is client-side, and this runs on the
    * server - a dedicated server would throw "Cannot get config value before config is loaded" and kill
    * the tier-up message (and, being inside an event handler, possibly more than that), so it reads
    * through {@code Config.clientOr}.</p>
    */
   public static void sendTierUnlockedMessage(Player player, GunTier tier) {
      if ((Boolean)Config.clientOr(Config.CLIENT.display.showProgressionMessages)) {
         if (tier != null && tier.getLevel() != 0) {
            Component tierName = Component.translatable("gun_tier.scguns." + tier.getId())
               .withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.BOLD});
            Component message = Component.translatable("progression.scguns.tier_unlocked", new Object[]{tierName}).withStyle(ChatFormatting.YELLOW);
            player.sendSystemMessage(message);
            List<GunTier> availableTiers = tier.getAvailableMobTiers();
            if (!availableTiers.isEmpty()) {
               Component mobMessage = Component.translatable("progression.scguns.enemies_and_raids_can_spawn",
                  new Object[]{joinTierNames(availableTiers)}).withStyle(ChatFormatting.RED);
               player.sendSystemMessage(mobMessage);
            }

            sendRaidUnlockedMessage(player, tier);
            player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
         }
      }
   }

   /** Join tier names with a separators that each language can spell its own way (、in Chinese). */
   private static Component joinTierNames(List<GunTier> tiers) {
      MutableComponent joined = Component.empty();
      for (int i = 0; i < tiers.size(); i++) {
         joined.append(Component.translatable("gun_tier.scguns." + tiers.get(i).getId())
            .withStyle(ChatFormatting.GOLD));
         if (i < tiers.size() - 1) {
            joined.append(Component.translatable("progression.scguns.list_separator").withStyle(ChatFormatting.RED));
         }
      }

      return joined;
   }

   private static void sendRaidUnlockedMessage(Player player, GunTier tier) {
      int raidLevel = tier.getRaidLevel();
      if (raidLevel > 0) {
         List<RaidConfig.RaidData> availableRaids = RaidConfig.getRaidsForLevel(raidLevel);
         if (!availableRaids.isEmpty()) {
            MutableComponent raidList = Component.empty();

            for (int i = 0; i < availableRaids.size(); i++) {
               RaidConfig.RaidData raid = availableRaids.get(i);
               raidList.append(Component.translatable("raid.scguns." + raid.raidId()).withStyle(ChatFormatting.DARK_RED));
               if (i < availableRaids.size() - 1) {
                  raidList.append(Component.translatable("progression.scguns.list_separator").withStyle(ChatFormatting.GRAY));
               }
            }

            player.sendSystemMessage(Component.translatable("progression.scguns.raids_can_spawn", new Object[]{raidList})
               .withStyle(ChatFormatting.DARK_GRAY));
         }
      }
   }
}
