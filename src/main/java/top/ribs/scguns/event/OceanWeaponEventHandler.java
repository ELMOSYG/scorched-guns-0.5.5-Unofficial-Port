package top.ribs.scguns.event;



import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
public class OceanWeaponEventHandler {
   private static final Map<UUID, Boolean> appliedDolphinGrace = new HashMap<>();

   public OceanWeaponEventHandler() {
      super();
   }

   @SubscribeEvent
   public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
      if (event.getEntity() instanceof Player player) {
         applyDolphinGrace(player);
      }
   }

   @SubscribeEvent
      // 0.5.5 listened on Forge's LivingTickEvent, which only fired for living
   // entities. NeoForge's per-entity tick event fires for every entity, so this
   // is pinned to the player tick event: no cast, and item entities no longer
   // tick through this handler.
   public static void onPlayerTick(PlayerTickEvent.Post event) {
      if (event.getEntity() instanceof Player player) {
         applyDolphinGrace(player);
      }
   }

   private static void applyDolphinGrace(Player player) {
      ItemStack mainHandItem = player.getMainHandItem();
      ItemStack offHandItem = player.getOffhandItem();
      boolean holdingOceanWeapon = isOceanWeapon(mainHandItem) || isOceanWeapon(offHandItem);
      boolean isInWater = player.isEyeInFluid(FluidTags.WATER);
      MobEffectInstance dolphinGraceEffect = player.getEffect(MobEffects.DOLPHINS_GRACE);
      UUID playerId = player.getUUID();
      boolean wasAppliedByUs = appliedDolphinGrace.getOrDefault(playerId, false);
      if (holdingOceanWeapon && isInWater) {
         if (dolphinGraceEffect == null || dolphinGraceEffect.getDuration() <= 10) {
            player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 60, 0, false, false, false));
            appliedDolphinGrace.put(playerId, true);
         } else if (dolphinGraceEffect.getDuration() > 60) {
            appliedDolphinGrace.put(playerId, false);
         }
      } else if (wasAppliedByUs && dolphinGraceEffect != null && dolphinGraceEffect.getDuration() <= 60) {
         player.removeEffect(MobEffects.DOLPHINS_GRACE);
         appliedDolphinGrace.remove(playerId);
      } else if (!holdingOceanWeapon || !isInWater) {
         appliedDolphinGrace.remove(playerId);
      }
   }

   private static boolean isOceanWeapon(ItemStack itemStack) {
      return !itemStack.isEmpty() && itemStack.is(ModTags.Items.OCEAN_GUN);
   }
}
