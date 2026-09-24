package top.ribs.scguns.common;


import top.ribs.scguns.util.NbtHelper;
import javax.annotation.Nullable;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import top.ribs.scguns.config.MobGuideConfig;

public class MobGuideHelper {
   public MobGuideHelper() {
      super();
   }

   @Nullable
   public static ItemStack createGuideBook(EntityType<?> entityType) {
      MobGuideConfig.MobGuide guide = MobGuideConfig.getGuide(entityType);
      if (guide == null) {
         return null;
      } else {
         ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
         CompoundTag tag = NbtHelper.getOrCreateTag(book);
         tag.putString("title", guide.getTitle().getString());
         String authorKey = guide.titleKey().replace(".title", ".author");
         Component authorComponent = Component.translatable(authorKey);
         tag.putString("author", authorComponent.getString());
         tag.putInt("generation", 0);
         ListTag pages = new ListTag();

         for (MobGuideConfig.GuidePage page : guide.getPages()) {
            Component textComponent = page.getTextComponent();
            // Guide pages are always plain translatable components and this helper has no level in
            // scope, so an empty lookup provider gives exactly the 1.20.1 provider-less JSON.
            String jsonText = Serializer.toJson(textComponent, RegistryAccess.EMPTY);
            pages.add(StringTag.valueOf(jsonText));
         }

         tag.put("pages", pages);
         tag.putBoolean("resolved", false);
         return book;
      }
   }
}
