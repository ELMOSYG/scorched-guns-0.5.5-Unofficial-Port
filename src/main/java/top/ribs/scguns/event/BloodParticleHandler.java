package top.ribs.scguns.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import top.ribs.scguns.Config;
import top.ribs.scguns.network.PacketHandler;
import top.ribs.scguns.network.message.S2CMessageBlood;

/**
 * Optionally lets damage that did not come from a gun throw blood as well.
 *
 * <p>Blood normally only comes from this mod's own projectiles: each projectile entity sends
 * {@code S2CMessageBlood} itself when it hits. This adds a generic hook so that other weapons do it too -
 * melee, bows, and a player's pets - without touching any of the per-projectile code. The colour is decided
 * on the client from the entity type carried in the packet, so nothing about the particle logic is
 * duplicated.</p>
 *
 * <p>Scope, as specified by the player: only damage with player involvement (the attacker is a player, or a
 * pet owned by one), only to players within 32 blocks, and only for hits of at least 0.5 damage.
 * Environmental damage never qualifies, because the damage source has no responsible entity. Hits from this
 * mod's own projectiles are skipped - they already sent the packet and would otherwise bleed twice.</p>
 */
@EventBusSubscriber(modid = "scguns")
public final class BloodParticleHandler {
   /** Players further away than this do not receive the packet. */
   private static final double MAX_DISTANCE = 32.0D;
   /** Damage below this does not throw blood, so chip damage does not spam particles. */
   private static final float MIN_DAMAGE = 0.5F;

   private BloodParticleHandler() {
   }

   @SubscribeEvent
   public static void onLivingDamage(LivingDamageEvent.Post event) {
      if (!Config.SERVER.bloodFromAnyDamage.get()) {
         return;
      }

      LivingEntity victim = event.getEntity();
      if (victim.level().isClientSide() || event.getNewDamage() < MIN_DAMAGE) {
         return;
      }

      Entity attacker = event.getSource().getEntity();
      boolean playerInvolved = attacker instanceof Player
         || (attacker instanceof OwnableEntity pet && pet.getOwner() instanceof Player);
      if (!playerInvolved) {
         return;
      }

      // This mod's own projectiles already send the blood packet when they hit.
      if (event.getSource().getDirectEntity()
         instanceof top.ribs.scguns.entity.projectile.ProjectileEntity) {
         return;
      }

      double x = victim.getX();
      double y = victim.getY() + victim.getBbHeight() * 0.5D;
      double z = victim.getZ();
      S2CMessageBlood packet = new S2CMessageBlood(x, y, z, victim.getType());

      double maxDistanceSq = MAX_DISTANCE * MAX_DISTANCE;
      for (ServerPlayer player : ((ServerLevel)victim.level()).players()) {
         if (player.distanceToSqr(victim) <= maxDistanceSq) {
            PacketHandler.getPlayChannel().sendToPlayer(() -> player, packet);
         }
      }
   }
}
