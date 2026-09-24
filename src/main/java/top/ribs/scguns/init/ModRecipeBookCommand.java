package top.ribs.scguns.init;

import com.mojang.brigadier.CommandDispatcher;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.RecipeBook;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import top.ribs.scguns.client.screen.GunBenchRecipe;
import top.ribs.scguns.common.recipe.GunBenchBookTabs;
import top.ribs.scguns.common.recipe.GunBenchUnlockKeys;

/**
 * {@code /scguns recipebook} - report which gun bench recipes a player has unlocked.
 *
 * <p>Written because "the bench only shows N guns" is otherwise unanswerable from a screenshot: the
 * recipe book only draws recipes the player has <b>unlocked</b>, and one blueprint unlocks one tier, so
 * a short list is usually correct rather than broken. This prints the server's own count, grouped by
 * blueprint, so the book's contents become a number to compare instead of something to guess at.</p>
 *
 * <p>Registered from {@link top.ribs.scguns.event.ModCommandsRegister} as a second root: Brigadier
 * merges two {@code scguns} literals, which keeps this out of the deeply nested tree in
 * {@link ModCommands} (HANDOFF section 39.7).</p>
 */
public final class ModRecipeBookCommand {
   private ModRecipeBookCommand() {
   }

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(Commands.literal("scguns")
         .then(Commands.literal("recipebook")
            // Self first, like "progression check": the whole point is that a player can see why
            // their own book is short without an operator reading the server log for them.
            .executes(context -> executeCheckRecipeBook((CommandSourceStack)context.getSource(), null))
            .then(Commands.argument("player", EntityArgument.player()).executes(context -> {
               ServerPlayer player = EntityArgument.getPlayer(context, "player");
               return executeCheckRecipeBook((CommandSourceStack)context.getSource(), player);
            }))));
   }

   private static int executeCheckRecipeBook(CommandSourceStack source, ServerPlayer requested) {
      ServerPlayer target = requested;

      if (target == null) {
         if (source.getEntity() instanceof ServerPlayer self) {
            target = self;
         } else {
            source.sendFailure(Component.translatable("commands.scguns.recipebook.no_console"));
            return 0;
         }
      } else if (!source.hasPermission(2)) {
         source.sendFailure(Component.translatable("commands.scguns.no_permission"));
         return 0;
      }

      RecipeBook book = target.getRecipeBook();
      List<RecipeHolder<GunBenchRecipe>> recipes =
         source.getServer().getRecipeManager().getAllRecipesFor(GunBenchRecipe.Type.INSTANCE);

      // Unlock key item -> {unlocked, total}. The key is the recipe's blueprint, or the turret
      // platform for the four turret recipes that have none (HANDOFF section 43) - what the player
      // needs to be carrying, which is exactly the question this command answers.
      Map<Item, int[]> byBlueprint = new LinkedHashMap<>();
      // Book tab -> {unlocked, total}, the same split the client's tabs use (HANDOFF section 44).
      Map<GunBenchBookTabs.Tab, int[]> byTab = new LinkedHashMap<>();
      int unlocked = 0;

      for (RecipeHolder<GunBenchRecipe> holder : recipes) {
         Item key = GunBenchUnlockKeys.keyFor(holder.value()).getItem();
         int[] counts = byBlueprint.computeIfAbsent(key, ignored -> new int[2]);
         counts[1]++;
         boolean known = book.contains(holder);
         if (known) {
            counts[0]++;
            unlocked++;
         }

         int[] tabCounts = byTab.computeIfAbsent(GunBenchBookTabs.of(holder.value()), ignored -> new int[2]);
         tabCounts[1]++;
         if (known) {
            tabCounts[0]++;
         }
      }

      List<Map.Entry<Item, int[]>> entries = new ArrayList<>(byBlueprint.entrySet());
      entries.sort((a, b) -> {
         int byTotal = Integer.compare(b.getValue()[1], a.getValue()[1]);
         return byTotal != 0 ? byTotal : blueprintKey(a.getKey()).compareTo(blueprintKey(b.getKey()));
      });

      int total = recipes.size();
      int unlockedCount = unlocked;
      ServerPlayer shown = target;
      source.sendSuccess(() -> Component.translatable("commands.scguns.recipebook.header",
         shown.getDisplayName()).withStyle(ChatFormatting.GOLD), false);
      source.sendSuccess(() -> Component.translatable("commands.scguns.recipebook.total",
         unlockedCount, total).withStyle(ChatFormatting.WHITE), false);

      int[] guns = byTab.getOrDefault(GunBenchBookTabs.Tab.GUNS, new int[2]);
      int[] turrets = byTab.getOrDefault(GunBenchBookTabs.Tab.TURRETS, new int[2]);
      int[] exoSuit = byTab.getOrDefault(GunBenchBookTabs.Tab.EXO_SUIT, new int[2]);
      source.sendSuccess(() -> Component.translatable("commands.scguns.recipebook.tabs",
         guns[0], guns[1], turrets[0], turrets[1], exoSuit[0], exoSuit[1])
         .withStyle(ChatFormatting.AQUA), false);

      for (Map.Entry<Item, int[]> entry : entries) {
         Component name = new ItemStack(entry.getKey()).getHoverName();
         int got = entry.getValue()[0];
         int all = entry.getValue()[1];
         ChatFormatting colour = got == 0 ? ChatFormatting.DARK_GRAY
            : (got == all ? ChatFormatting.GREEN : ChatFormatting.YELLOW);
         source.sendSuccess(() -> Component.translatable("commands.scguns.recipebook.entry", name, got, all)
            .withStyle(colour), false);
      }

      source.sendSuccess(() -> Component.translatable("commands.scguns.recipebook.hint")
         .withStyle(ChatFormatting.GRAY), false);
      return unlockedCount;
   }

   private static String blueprintKey(Item item) {
      return BuiltInRegistries.ITEM.getKey(item).toString();
   }
}
