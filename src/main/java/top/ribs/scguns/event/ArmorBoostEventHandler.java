package top.ribs.scguns.event;



import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import top.ribs.scguns.init.ModItems;

@EventBusSubscriber(
   modid = "scguns"
)
public class ArmorBoostEventHandler {
   private static final int RESISTANCE_LEVEL = 1;
   private static final int EFFECT_DURATION = 80;

   public ArmorBoostEventHandler() {
      super();
   }

   @SubscribeEvent
   public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
      if (event.getEntity() instanceof Player player) {
         applyResistanceBoost(player);
      }
   }

   @SubscribeEvent
      // 0.5.5 listened on Forge's LivingTickEvent, which only fired for living
   // entities. NeoForge's per-entity tick event fires for every entity, so this
   // is pinned to the player tick event: no cast, and item entities no longer
   // tick through this handler.
   public static void onPlayerTick(PlayerTickEvent.Post event) {
      if (event.getEntity() instanceof Player player) {
         applyResistanceBoost(player);
      }
   }

   private static void applyResistanceBoost(Player player) {
      boolean holdingSpecialItem = isSpecialItem(player.getMainHandItem()) || isSpecialItem(player.getOffhandItem());
      if (holdingSpecialItem) {
         player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 1, false, false));
      }
   }

   private static boolean isSpecialItem(ItemStack itemStack) {
      return itemStack.getItem() == ModItems.SHELLURKER.get();
   }
}
