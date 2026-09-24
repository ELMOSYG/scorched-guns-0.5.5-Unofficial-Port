package top.ribs.scguns.item.attachment;

import javax.annotation.Nullable;
import net.minecraft.world.item.ItemStack;
import top.ribs.scguns.item.attachment.impl.Attachment;

public interface IAttachment<T extends Attachment> {
   IAttachment.Type getType();

   T getProperties();

   default boolean canAttachTo(ItemStack stack) {
      return true;
   }

   public static enum Type {
      SCOPE("scope", "Scope", "scope"),
      BARREL("barrel", "Barrel", "barrel"),
      STOCK("stock", "Stock", "stock"),
      UNDER_BARREL("under_barrel", "Under_Barrel", "underBarrel"),
      MAGAZINE("magazine", "Magazine", "magazine");

      private final String translationKey;
      private final String tagKey;
      private final String serializeKey;

      private Type(String translationKey, String tagKey, String serializeKey) {
         this.translationKey = translationKey;
         this.tagKey = tagKey;
         this.serializeKey = serializeKey;
      }

      public String getTranslationKey() {
         return this.translationKey;
      }

      public String getTagKey() {
         return this.tagKey;
      }

      public String getSerializeKey() {
         return this.serializeKey;
      }

      @Nullable
      public static IAttachment.Type byTagKey(String s) {
         for (IAttachment.Type type : values()) {
            if (type.tagKey.equalsIgnoreCase(s)) {
               return type;
            }
         }

         return null;
      }

      public String getName() {
         return this.tagKey;
      }
   }
}
