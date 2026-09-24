package top.ribs.scguns.util;

import io.netty.buffer.ByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class BufferUtil {
   public BufferUtil() {
      super();
   }

   public static void writeItemStackToBufIgnoreTag(ByteBuf buf, ItemStack stack) {
      if (stack.isEmpty()) {
         buf.writeShort(-1);
      } else {
         buf.writeShort(Item.getId(stack.getItem()));
         buf.writeByte(stack.getCount());
      }
   }

   public static ItemStack readItemStackFromBufIgnoreTag(ByteBuf buf) {
      int id = buf.readShort();
      return id < 0 ? ItemStack.EMPTY : new ItemStack(Item.byId(id), buf.readByte());
   }
}
