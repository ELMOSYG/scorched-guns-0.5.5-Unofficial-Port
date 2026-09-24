package top.ribs.scguns.network.message;


import net.minecraft.core.registries.BuiltInRegistries;
import com.mrcrayfish.framework.api.network.MessageContext;
import java.util.Objects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import top.ribs.scguns.client.network.ClientPlayHandler;

public class S2CMessageBlood {
   private double x;
   private double y;
   private double z;
   private EntityType<?> entityType;

   public S2CMessageBlood() {
      super();
   }

   public S2CMessageBlood(double x, double y, double z, EntityType<?> entityType) {
      super();
      this.x = x;
      this.y = y;
      this.z = z;
      this.entityType = entityType;
   }

   public void encode(S2CMessageBlood message, FriendlyByteBuf buffer) {
      buffer.writeDouble(message.x);
      buffer.writeDouble(message.y);
      buffer.writeDouble(message.z);
      buffer.writeResourceLocation(Objects.requireNonNull(BuiltInRegistries.ENTITY_TYPE.getKey(message.entityType)));
   }

   public S2CMessageBlood decode(FriendlyByteBuf buffer) {
      double x = buffer.readDouble();
      double y = buffer.readDouble();
      double z = buffer.readDouble();
      ResourceLocation entityTypeLocation = buffer.readResourceLocation();
      EntityType<?> entityType = (EntityType<?>)BuiltInRegistries.ENTITY_TYPE.get(entityTypeLocation);
      return new S2CMessageBlood(x, y, z, entityType);
   }

   public void handle(S2CMessageBlood message, MessageContext context) {
      context.execute(() -> ClientPlayHandler.handleMessageBlood(message));
      context.setHandled(true);
   }

   public double getX() {
      return this.x;
   }

   public double getY() {
      return this.y;
   }

   public double getZ() {
      return this.z;
   }

   public EntityType<?> getEntityType() {
      return this.entityType;
   }
}
