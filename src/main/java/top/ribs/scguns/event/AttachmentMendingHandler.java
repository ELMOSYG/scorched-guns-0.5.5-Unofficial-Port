package top.ribs.scguns.event;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.item.attachment.IAttachment;
import top.ribs.scguns.util.NbtHelper;
import top.ribs.scguns.util.ScEnchants;

/**
 * Lets an installed attachment benefit from Mending (HANDOFF section 57).
 *
 * <p>Vanilla repairs with XP in {@code ExperienceOrb.repairPlayerItems}, which walks the player's
 * <b>inventory</b> for stacks carrying {@code REPAIR_WITH_XP}. An attachment is not an inventory item:
 * it lives inside the gun's own tag ({@code Attachments.<type>}, read back by
 * {@link Gun#getAttachment}), so vanilla structurally cannot see it - which is why an attachment's
 * durability was never repaired by the Mending on the gun that carries it.</p>
 *
 * <p>This hooks the same moment vanilla does: {@code PlayerXpEvent.PickupXp} is posted at the top of
 * {@code ExperienceOrb.playerTouch}, before vanilla's repair runs. The repair uses vanilla's own maths
 * ({@code modifyDurabilityToRepairFromXp} over {@code xp * getXpRepairRatio()}), so the amount matches
 * what the same enchantment would do on an inventory item.</p>
 *
 * <p><b>Rule chosen by the player:</b> only an attachment that <b>carries Mending itself</b> is
 * repaired - the gun's own Mending does not extend to its attachments. The orb's XP is <b>not</b>
 * consumed either: this does not touch vanilla's pickup, so the gun still gets repaired exactly as
 * before. Turning that into a shared XP pool would mean taking over the orb pickup, which is a bigger
 * behavioural change than this fix needs; say the word if it should.</p>
 */
@EventBusSubscriber(modid = "scguns", bus = EventBusSubscriber.Bus.GAME)
public final class AttachmentMendingHandler {
   private AttachmentMendingHandler() {
   }

   @SubscribeEvent
   public static void onPickupXp(PlayerXpEvent.PickupXp event) {
      Player player = event.getEntity();
      if (player.level().isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
         return;
      }

      ExperienceOrb orb = event.getOrb();
      int xp = orb == null ? 0 : orb.getValue();
      if (xp <= 0) {
         return;
      }

      mendAttachments(serverPlayer, player.getMainHandItem(), xp);
      mendAttachments(serverPlayer, player.getOffhandItem(), xp);
   }

   private static void mendAttachments(ServerPlayer player, ItemStack gun, int xp) {
      if (gun.isEmpty() || !(gun.getItem() instanceof GunItem)) {
         return;
      }

      for (IAttachment.Type type : IAttachment.Type.values()) {
         ItemStack attachment = Gun.getAttachment(type, gun);
         if (attachment.isEmpty() || !attachment.isDamageableItem() || attachment.getDamageValue() <= 0) {
            continue;
         }

         // The player's rule: an attachment needs its own Mending; the gun's is not enough.
         if (ScEnchants.level(attachment, Enchantments.MENDING) <= 0) {
            continue;
         }

         int repair = EnchantmentHelper.modifyDurabilityToRepairFromXp(player.serverLevel(), attachment,
            (int)((float)xp * attachment.getXpRepairRatio()));
         int applied = Math.min(repair, attachment.getDamageValue());
         if (applied <= 0) {
            continue;
         }

         attachment.setDamageValue(attachment.getDamageValue() - applied);
         // Same write-back the wear path uses, including its "never replace an attachment with an
         // empty encode" guard (HANDOFF section 60).
         Gun.setAttachment(gun, type, attachment, player.registryAccess());
      }
   }
}
