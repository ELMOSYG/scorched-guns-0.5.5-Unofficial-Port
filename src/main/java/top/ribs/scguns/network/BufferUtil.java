package top.ribs.scguns.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

public class BufferUtil {
   public BufferUtil() {
      super();
   }

   public static void writeVec3(FriendlyByteBuf buffer, Vec3 vec) {
      buffer.writeDouble(vec.x);
      buffer.writeDouble(vec.y);
      buffer.writeDouble(vec.z);
   }

   public static Vec3 readVec3(FriendlyByteBuf buffer) {
      return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
   }
}
