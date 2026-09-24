package top.ribs.scguns.event;

import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent.PickupXp;
import top.ribs.scguns.item.animated.AnimatedSculkGunItem;

/**
 * The sculk guns' own repair-from-XP (0.5.5's {@code GunXpHandler}).
 *
 * <p>These guns carry no enchantment for this: picking up experience mends them directly, at
 * {@link #SCULK_REPAIR_RATIO} of 0.5.5's rate. Normal guns are repaired by vanilla Mending instead,
 * so they are deliberately not touched here.</p>
 *
 * <h2>Why the registration changed (HANDOFF section 61)</h2>
 *
 * <p>0.5.5 declared this class {@code @EventBusSubscriber(value = {Dist.CLIENT})}, and the upstream
 * 1.21.1 port kept that. The effect is that the whole handler ran only on the <b>client</b>: the
 * mutation landed on the client's copy of the gun (thrown away at the next sync) and the reduced
 * {@code orb.value} was equally meaningless there. Measured on a server: a sculk gun at 100 damage
 * kept exactly 100 damage after picking up an orb. The class now runs on both physical sides and
 * returns early on the client, so the change is made where the item actually lives.</p>
 *
 * <p>The attachment loop that 0.5.5 had here is gone: it was dead for the same reason, and
 * {@code AttachmentMendingHandler} does that job on the server, with the write-back an installed
 * attachment needs.</p>
 */
@EventBusSubscriber(modid = "scguns")
public class GunXpHandler {
   private static final float SCULK_REPAIR_RATIO = 0.5F;

   public GunXpHandler() {
      super();
   }

   @SubscribeEvent
   public static void onPlayerXpPickup(PickupXp event) {
      Player player = event.getEntity();
      if (player.level().isClientSide) {
         return;
      }

      ItemStack mainHand = player.getMainHandItem();
      if (!(mainHand.getItem() instanceof AnimatedSculkGunItem) || !mainHand.isDamaged()) {
         return;
      }

      ExperienceOrb orb = event.getOrb();
      int remainingXp = orb.value;
      if (remainingXp <= 0) {
         return;
      }

      int repairAmount = Math.min((int)((float)(remainingXp * 2) * SCULK_REPAIR_RATIO), mainHand.getDamageValue());
      if (repairAmount > 0) {
         mainHand.setDamageValue(mainHand.getDamageValue() - repairAmount);
         // 0.5.5's arithmetic, kept as it was: the sculk gun pays one point per point of durability
         // repaired, where an attachment's repair costs half a point (see AttachmentMendingHandler).
         orb.value = remainingXp - repairAmount;
      }
   }
}
