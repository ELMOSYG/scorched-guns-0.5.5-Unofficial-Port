package top.ribs.scguns.network.message;

import com.mrcrayfish.framework.api.network.MessageContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import top.ribs.scguns.client.handler.GunRenderingHandler;

/**
 * Tells one client to play the bayonet charge's impact animation - the gun jolting back when the charge hits
 * something.
 *
 * <p>The server used to call {@link GunRenderingHandler#triggerBanzaiImpact()} directly from
 * {@code MeleeAttackHandler}, which is a client-only class: on a dedicated server the animation therefore never
 * played. The charge's other client feedback already travels this way (see {@code S2CMessageMeleeAttack}).</p>
 */
public class S2CMessageBanzaiImpact {
   public S2CMessageBanzaiImpact() {
      super();
   }

   public S2CMessageBanzaiImpact(RegistryFriendlyByteBuf buf) {
      super();
   }

   public void encode(S2CMessageBanzaiImpact message, RegistryFriendlyByteBuf buf) {
   }

   public S2CMessageBanzaiImpact decode(RegistryFriendlyByteBuf buf) {
      return new S2CMessageBanzaiImpact(buf);
   }

   public void handle(S2CMessageBanzaiImpact message, MessageContext context) {
      context.execute(GunRenderingHandler.get()::triggerBanzaiImpact);
      context.setHandled(true);
   }
}
