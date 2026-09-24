package top.ribs.scguns.event;



import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import top.ribs.scguns.init.ModTags;

@EventBusSubscriber(
   modid = "scguns"
)
public class PiglinWeaponEventHandler {
   private static final String COOLDOWN_TAG = "PiglinWeaponCooldown";

   public PiglinWeaponEventHandler() {
      super();
   }

   @SubscribeEvent
   public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
      if (event.getEntity() instanceof Player player) {
         applyLavaResistance(player);
      }
   }

   @SubscribeEvent
      // 0.5.5 listened on Forge's LivingTickEvent, which only fired for living
   // entities. NeoForge's per-entity tick event fires for every entity, so this
   // is pinned to the player tick event: no cast, and item entities no longer
   // tick through this handler.
   public static void onPlayerTick(PlayerTickEvent.Post event) {
      if (event.getEntity() instanceof Player player) {
         applyLavaResistance(player);
      }
   }

   private static void applyLavaResistance(Player player) {
      ItemStack mainHandItem = player.getMainHandItem();
      ItemStack offHandItem = player.getOffhandItem();
      boolean holdingSpecialItem = isPiglinWeapon(mainHandItem) || isPiglinWeapon(offHandItem);
      boolean isInLava = player.isEyeInFluid(FluidTags.LAVA);
      int cooldown = getCooldown(player);
      if (holdingSpecialItem && isInLava && cooldown <= 0) {
         player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 400, 0, false, false, true));
         setCooldown(player, 600);
      } else if (cooldown > 0) {
         reduceCooldown(player);
      }
   }

   private static boolean isPiglinWeapon(ItemStack itemStack) {
      return !itemStack.isEmpty() && itemStack.is(ModTags.Items.PIGLIN_GUN);
   }

   private static int getCooldown(Player player) {
      CompoundTag tag = player.getPersistentData();
      return tag.contains("PiglinWeaponCooldown") ? tag.getInt("PiglinWeaponCooldown") : 0;
   }

   private static void setCooldown(Player player, int cooldown) {
      player.getPersistentData().putInt("PiglinWeaponCooldown", cooldown);
   }

   private static void reduceCooldown(Player player) {
      int cooldown = getCooldown(player);
      if (cooldown > 0) {
         setCooldown(player, cooldown - 1);
      }
   }
}
