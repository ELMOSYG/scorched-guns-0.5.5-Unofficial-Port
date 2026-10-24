package top.ribs.scguns.common;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import top.ribs.scguns.config.MobGuideConfig;

/**
 * Builds the written guidebook a scamp or viventrum package hands out.
 *
 * <p>A written book's contents are a {@link DataComponents#WRITTEN_BOOK_CONTENT} component in 1.21, not the
 * {@code title}/{@code author}/{@code pages} tags 1.20 had. The port kept writing the old tags, so the book arrived
 * blank and could not be opened at all - a written book whose content component is missing has nothing to show. The
 * {@code resolved} flag matters too: an unresolved book is treated as still being edited and refuses to open.</p>
 */
public class MobGuideHelper {
   public MobGuideHelper() {
      super();
   }

   @Nullable
   public static ItemStack createGuideBook(EntityType<?> entityType) {
      MobGuideConfig.MobGuide guide = MobGuideConfig.getGuide(entityType);

      if (guide == null) {
         return null;
      }

      String authorKey = guide.titleKey().replace(".title", ".author");
      Component author = Component.translatable(authorKey);

      List<Filterable<Component>> pages = new ArrayList<>();

      for (MobGuideConfig.GuidePage page : guide.getPages()) {
         pages.add(Filterable.passThrough(page.getTextComponent()));
      }

      ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
      book.set(
         DataComponents.WRITTEN_BOOK_CONTENT,
         new WrittenBookContent(
            // 1.21.1 takes the title as a Filterable<String> (the component title arrived later).
            Filterable.passThrough(guide.getTitle().getString()),
            author.getString(),
            0,
            pages,
            true
         )
      );
      return book;
   }
}
