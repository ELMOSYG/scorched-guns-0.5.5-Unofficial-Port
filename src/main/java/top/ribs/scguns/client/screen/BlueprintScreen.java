package top.ribs.scguns.client.screen;



import top.ribs.scguns.util.NbtHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import top.ribs.scguns.network.PacketHandler;
import top.ribs.scguns.network.message.C2SMessageSetBlueprintRecipe;
import net.minecraft.client.gui.components.Renderable;

@OnlyIn(Dist.CLIENT)
public class BlueprintScreen extends Screen {
   public static final ResourceLocation BLUEPRINT_TEXTURE = ResourceLocation.fromNamespaceAndPath("scguns", "textures/gui/blueprint.png");
   protected static final int IMAGE_WIDTH = 192;
   protected static final int IMAGE_HEIGHT = 192;
   private static final int SLOT_SIZE = 16;
   private final List<BlueprintScreen.RecipeSlot> recipeSlots = new ArrayList<>();
   private final InteractionHand hand;
   private final ItemStack[] recipeItems = new ItemStack[12];
   private int currentPage = 0;
   private int maxPages = 0;
   private BlueprintScreen.BlueprintPageButton nextPageButton;
   private BlueprintScreen.BlueprintPageButton prevPageButton;
   private final ItemStack blueprintStack;
   private final List<BlueprintScreen.DisplayEntry> displayEntries = new ArrayList<>();
   private static final List<String> GUN_ORDER = new ArrayList<>(
      Arrays.asList(
         "flintlock_pistol",
         "handcannon",
         "musket",
         "blunderbuss",
         "doublet",
         "repeating_musket",
         "longarm",
         "fencer_carabine",
         "fencer_thumper",
         "laser_musket",
         "plasmabuss",
         "teslock_rifle",
         "pax",
         "winnie",
         "winnie_millend",
         "red_raydar",
         "callwell",
         "callwell_conversion",
         "callwell_terminal",
         "saketini",
         "saketini_ironport",
         "kiln_gun",
         "big_bore",
         "scrapper",
         "rusty_gnat",
         "umax_pistol",
         "makeshift_rifle",
         "boomstick",
         "bruiser",
         "llr_director",
         "birdfeeder",
         "whistler",
         "blooper",
         "arc_worker",
         "defender_pistol",
         "trenchur",
         "greaser_smg",
         "m3_carabine",
         "m3_marksman",
         "combat_shotgun",
         "venturi",
         "iron_javelin",
         "iron_spear",
         "auvtomag",
         "pulsar",
         "gyrojet_pistol",
         "brawler",
         "crusader",
         "mk43_rifle",
         "triquetra",
         "rocket_rifle",
         "ultra_knight_hawk",
         "floundergat",
         "hyperbaria",
         "marlin",
         "bomb_lance",
         "hullbreaker",
         "sequoia",
         "spirulida",
         "mokova",
         "mak_mkii",
         "stilleto",
         "railworker",
         "stiletto",
         "turnpike",
         "killer_23",
         "homemaker",
         "kalaskah",
         "basker",
         "tl_runner",
         "stigg",
         "whizzbanger",
         "krauser",
         "soul_drummer",
         "uppercut",
         "micina",
         "valora",
         "prush_gun",
         "drill",
         "drill_conversion",
         "lockewood",
         "zilk_45",
         "rg_jigsaw",
         "nailer",
         "inertial",
         "minksy",
         "mas_55",
         "mas_peddler",
         "inquisitor",
         "plasgun",
         "truant",
         "cyclone",
         "shard_culler",
         "m22_waltz",
         "waltz_conversion",
         "osgood_50",
         "grandle_og",
         "grandle",
         "cogloader",
         "gale",
         "jr_wristbreaker",
         "jackhammer",
         "howler",
         "howler_conversion",
         "gauss_rifle",
         "libertas",
         "niami",
         "hammer_gl",
         "spitfire",
         "gattaler",
         "thunderhead",
         "scratches",
         "cr4k_mining_laser",
         "dozier_rl",
         "empty_blasphemy",
         "blasphemy",
         "pyroclastic_flow",
         "freyr",
         "mangalitsa",
         "vulcanic_repeater",
         "trotters",
         "super_shotgun",
         "whispers",
         "echoes_2",
         "sculk_resonator",
         "forlorn_hope",
         "carapice",
         "shellurker",
         "weevil",
         "dark_matter",
         "lone_wonder",
         "raygun",
         "prima_materia",
         "rat_king_and_queen",
         "locust",
         "sterilizer",
         "newborn_cyst",
         "earths_corpse",
         "flayed_god",
         "nervepinch",
         "terra_incognita",
         "astella",
         "exo_suit_helmet",
         "exo_suit_chestplate",
         "exo_suit_leggings",
         "exo_suit_boots"
      )
   );
   private static final Map<ResourceLocation, List<String>> LORE_ONLY_ITEMS = new HashMap<>();

   public static void registerGunOrder(String itemName) {
      if (!GUN_ORDER.contains(itemName)) {
         GUN_ORDER.add(itemName);
      }
   }

   public static void registerGunOrder(List<String> itemNames) {
      for (String itemName : itemNames) {
         registerGunOrder(itemName);
      }
   }

   public static void insertGunOrder(int index, String itemName) {
      if (!GUN_ORDER.contains(itemName)) {
         GUN_ORDER.add(Math.min(index, GUN_ORDER.size()), itemName);
      }
   }

   public static void registerLoreOnlyItem(ResourceLocation blueprintId, String itemName) {
      LORE_ONLY_ITEMS.computeIfAbsent(blueprintId, k -> new ArrayList<>()).add(itemName);
   }

   public static void registerLoreOnlyItems(ResourceLocation blueprintId, List<String> itemNames) {
      LORE_ONLY_ITEMS.computeIfAbsent(blueprintId, k -> new ArrayList<>()).addAll(itemNames);
   }

   public static List<String> getGunOrder() {
      return new ArrayList<>(GUN_ORDER);
   }

   public static List<String> getLoreOnlyItems(ResourceLocation blueprintId) {
      return new ArrayList<>(LORE_ONLY_ITEMS.getOrDefault(blueprintId, Collections.emptyList()));
   }

   public BlueprintScreen(ItemStack blueprintStack, Player player, InteractionHand hand) {
      super(Component.translatable("screen.scguns.blueprint.title"));
      this.blueprintStack = blueprintStack;
      this.hand = hand;
      Arrays.fill(this.recipeItems, ItemStack.EMPTY);
      this.loadAvailableEntries();
      this.loadActiveRecipeAsCurrentPage();
   }

   private void loadActiveRecipeAsCurrentPage() {
      ResourceLocation activeRecipeId = getActiveRecipe(this.blueprintStack);
      if (activeRecipeId != null) {
         for (int i = 0; i < this.displayEntries.size(); i++) {
            BlueprintScreen.DisplayEntry entry = this.displayEntries.get(i);
            if (entry.hasRecipe && entry.id != null && entry.id.equals(activeRecipeId)) {
               this.currentPage = i;
               this.loadCurrentPageRecipe();
               return;
            }
         }
      }

      this.loadCurrentPageRecipe();
   }

   private void loadCurrentPageRecipe() {
      if (this.currentPage < this.displayEntries.size()) {
         BlueprintScreen.DisplayEntry entry = this.displayEntries.get(this.currentPage);
         if (entry.hasRecipe) {
            this.loadRecipeIntoSlots(entry.recipe);
         } else {
            this.loadLoreItemIntoSlots(entry.itemStack);
         }
      }
   }

   protected void init() {
      super.init();
      this.createMenuControls();
      this.setupRecipeSlots();
   }

   private void setupRecipeSlots() {
      this.recipeSlots.clear();
      int centerX = (this.width - 192) / 2;
      int centerY = 2;
      this.recipeSlots.add(new BlueprintScreen.RecipeSlot(centerX + 26, centerY + 17, 0));
      this.recipeSlots.add(new BlueprintScreen.RecipeSlot(centerX + 44, centerY + 17, 1));
      this.recipeSlots.add(new BlueprintScreen.RecipeSlot(centerX + 62, centerY + 17, 2));
      this.recipeSlots.add(new BlueprintScreen.RecipeSlot(centerX + 80, centerY + 17, 3));
      this.recipeSlots.add(new BlueprintScreen.RecipeSlot(centerX + 26, centerY + 35, 4));
      this.recipeSlots.add(new BlueprintScreen.RecipeSlot(centerX + 44, centerY + 35, 5));
      this.recipeSlots.add(new BlueprintScreen.RecipeSlot(centerX + 62, centerY + 35, 6));
      this.recipeSlots.add(new BlueprintScreen.RecipeSlot(centerX + 80, centerY + 35, 7));
      this.recipeSlots.add(new BlueprintScreen.RecipeSlot(centerX + 26, centerY + 53, 8));
      this.recipeSlots.add(new BlueprintScreen.RecipeSlot(centerX + 62, centerY + 53, 9));
      this.recipeSlots.add(new BlueprintScreen.RecipeSlot(centerX + 116, centerY + 17, 11));
      this.recipeSlots.add(new BlueprintScreen.RecipeSlot(centerX + 140, centerY + 44, 10));
   }

   protected void createMenuControls() {
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).bounds(this.width / 2 - 100, 196, 200, 20).build());
      BlueprintScreen.DisplayEntry currentEntry = this.currentPage < this.displayEntries.size() ? this.displayEntries.get(this.currentPage) : null;
      boolean canSetRecipe = currentEntry != null && currentEntry.hasRecipe;
      Button setRecipeButton = Button.builder(Component.translatable("screen.scguns.blueprint.button.set_recipe"), button -> this.setActiveRecipe())
         .bounds(this.width / 2 - 100, 220, 200, 20)
         .build();
      setRecipeButton.active = canSetRecipe;
      this.addRenderableWidget(setRecipeButton);
      this.createPageControls();
   }

   private void setActiveRecipe() {
      if (this.currentPage < this.displayEntries.size()) {
         BlueprintScreen.DisplayEntry entry = this.displayEntries.get(this.currentPage);
         if (entry.hasRecipe && entry.id != null) {
            PacketHandler.getPlayChannel().sendToServer(new C2SMessageSetBlueprintRecipe(this.hand, entry.id.toString()));
            this.saveActiveRecipe(this.blueprintStack, entry.id);
         }
      }

      this.onClose();
   }

   private void createPageControls() {
      int centerX = (this.width - 192) / 2;
      int centerY = 2;
      int buttonY = centerY + 192 - 40;
      int rightSide = centerX + 192 - 35;
      this.prevPageButton = (BlueprintScreen.BlueprintPageButton)this.addRenderableWidget(
         new BlueprintScreen.BlueprintPageButton(rightSide - 40, buttonY, Component.literal("<"), button -> this.previousPage())
      );
      this.nextPageButton = (BlueprintScreen.BlueprintPageButton)this.addRenderableWidget(
         new BlueprintScreen.BlueprintPageButton(rightSide - 15, buttonY, Component.literal(">"), button -> this.nextPage())
      );
      this.updatePageButtonStates();
   }

   private void previousPage() {
      if (this.currentPage > 0) {
         this.currentPage--;
         this.loadCurrentPageRecipe();
         this.updatePageButtonStates();
      }
   }

   private void nextPage() {
      if (this.currentPage < this.maxPages - 1) {
         this.currentPage++;
         this.loadCurrentPageRecipe();
         this.updatePageButtonStates();
      }
   }

   private void updatePageButtonStates() {
      if (this.prevPageButton != null) {
         this.prevPageButton.active = this.currentPage > 0;
         this.prevPageButton.visible = true;
      }

      if (this.nextPageButton != null) {
         this.nextPageButton.active = this.currentPage < this.maxPages - 1;
         this.nextPageButton.visible = true;
      }
   }

   private void loadAvailableEntries() {
      Level level = Minecraft.getInstance().level;
      if (level != null) {
         this.displayEntries.clear();
         // The RecipeHolder carries the id 1.21 removed from Recipe itself. Unwrapping it here (as
         // this used to) threw the id away, and since the codec path gives every recipe the same
         // placeholder id, every blueprint then resolved to the same recipe - the first one, which
         // is what made every blueprint show the Gauss Rifle.
         List<net.minecraft.world.item.crafting.RecipeHolder<GunBenchRecipe>> allRecipes = level.getRecipeManager()
            .getAllRecipesFor(GunBenchRecipe.Type.INSTANCE);
         if (this.blueprintStack.isEmpty()) {
            for (net.minecraft.world.item.crafting.RecipeHolder<GunBenchRecipe> recipe : allRecipes) {
               this.displayEntries.add(new BlueprintScreen.DisplayEntry(recipe));
            }
         } else {
            for (net.minecraft.world.item.crafting.RecipeHolder<GunBenchRecipe> recipe : allRecipes) {
               if (recipe.value().getBlueprint().test(this.blueprintStack)) {
                  this.displayEntries.add(new BlueprintScreen.DisplayEntry(recipe));
               }
            }

            ResourceLocation blueprintItemId = BuiltInRegistries.ITEM.getKey(this.blueprintStack.getItem());
            if (blueprintItemId != null) {
               List<String> loreItems = LORE_ONLY_ITEMS.get(blueprintItemId);
               if (loreItems != null) {
                  for (String itemName : loreItems) {
                     ResourceLocation itemLocation = ResourceLocation.fromNamespaceAndPath(blueprintItemId.getNamespace(), itemName);
                     Item item = (Item)BuiltInRegistries.ITEM.get(itemLocation);
                     if (item != null) {
                        this.displayEntries.add(new BlueprintScreen.DisplayEntry(new ItemStack(item)));
                     }
                  }
               }
            }
         }

         this.sortEntriesByProgression();
         this.maxPages = Math.max(1, this.displayEntries.size());
         this.currentPage = 0;
      }
   }

   private void sortEntriesByProgression() {
      this.displayEntries.sort((entry1, entry2) -> {
         Level level = Minecraft.getInstance().level;
         if (level == null) {
            return 0;
         } else {
            String item1Name = this.getItemNameFromEntry(entry1, level);
            String item2Name = this.getItemNameFromEntry(entry2, level);
            int index1 = this.getOrderIndex(item1Name);
            int index2 = this.getOrderIndex(item2Name);
            return Integer.compare(index1, index2);
         }
      });
   }

   private String getItemNameFromEntry(BlueprintScreen.DisplayEntry entry, Level level) {
      ItemStack resultItem = entry.hasRecipe ? entry.recipe.getResultItem(level.registryAccess()) : entry.itemStack;
      if (!resultItem.isEmpty()) {
         ResourceLocation itemLocation = BuiltInRegistries.ITEM.getKey(resultItem.getItem());
         if (itemLocation != null) {
            return itemLocation.getPath();
         }
      }

      return "";
   }

   private int getOrderIndex(String itemName) {
      int index = GUN_ORDER.indexOf(itemName);
      return index == -1 ? Integer.MAX_VALUE : index;
   }

   private void loadRecipeIntoSlots(GunBenchRecipe recipe) {
      Level level = Minecraft.getInstance().level;
      if (level != null) {
         this.clearRecipeItems();

         for (int i = 0; i < recipe.getIngredients().size() && i < 10; i++) {
            Ingredient ingredient = (Ingredient)recipe.getIngredients().get(i);
            if (!ingredient.isEmpty()) {
               ItemStack[] stacks = ingredient.getItems();
               if (stacks.length > 0) {
                  this.recipeItems[i] = stacks[0].copy();
               }
            }
         }

         if (!recipe.getBlueprint().isEmpty()) {
            ItemStack[] blueprintStacks = recipe.getBlueprint().getItems();
            if (blueprintStacks.length > 0) {
               this.recipeItems[11] = blueprintStacks[0].copy();
            }
         }

         this.recipeItems[10] = recipe.getResultItem(level.registryAccess()).copy();
      }
   }

   private void loadLoreItemIntoSlots(ItemStack item) {
      this.clearRecipeItems();
      this.recipeItems[10] = item.copy();
   }

   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
      int centerX = (this.width - 192) / 2;
      int centerY = 2;
      guiGraphics.blit(BLUEPRINT_TEXTURE, centerX, centerY, 0, 0, 192, 192);
      this.renderRecipeSlots(guiGraphics, mouseX, mouseY);
      this.renderGunInfo(guiGraphics, centerX, centerY);
      // Screen.render renders the background (and its blur) itself, so calling
      // super.render here would run the blur a second time and smear everything
      // drawn above. Replicate the super implementation instead, exactly as
      // vanilla's AbstractContainerScreen.render does.
      for (Renderable renderable : this.renderables) {
         renderable.render(guiGraphics, mouseX, mouseY, partialTick);
      }
      this.renderItemTooltips(guiGraphics, mouseX, mouseY);
   }

   private void renderGunInfo(GuiGraphics guiGraphics, int centerX, int centerY) {
      if (this.currentPage < this.displayEntries.size()) {
         BlueprintScreen.DisplayEntry entry = this.displayEntries.get(this.currentPage);
         Level level = Minecraft.getInstance().level;
         if (level == null) {
            return;
         }

         ItemStack resultItem = entry.hasRecipe ? entry.recipe.getResultItem(level.registryAccess()) : entry.itemStack;
         if (!resultItem.isEmpty()) {
            String gunName = Component.translatable(resultItem.getDescriptionId()).getString();
            PoseStack poseStack = guiGraphics.pose();
            int titleY = centerY + 77;
            poseStack.pushPose();
            poseStack.scale(1.1F, 1.1F, 1.0F);
            int scaledX = (int)((float)(centerX + 30) / 1.1F);
            int scaledY = (int)((float)titleY / 1.1F);
            guiGraphics.drawString(this.font, gunName, scaledX, scaledY, 2170967, false);
            poseStack.popPose();
            int titleHeight = (int)(9.0F * 1.1F);
            int descriptionY = titleY + titleHeight + 6;
            // 1.20.1: Item.toString() was the registry path ("musket"). 1.21 returns the
            // full registry name ("scguns:musket"), which matches no scguns.desc.* key, so
            // every blueprint page fell back to scguns.desc.unknown.
            ResourceLocation resultItemId = BuiltInRegistries.ITEM.getKey(resultItem.getItem());
            String descriptionKey = "scguns.desc." + (resultItemId == null ? "" : resultItemId.getPath());
            String description = Component.translatable(descriptionKey).getString();
            if (description.equals(descriptionKey)) {
               description = Component.translatable("scguns.desc.unknown").getString();
            }

            int maxWidth = 162;
            List<String> wrappedLines = this.wrapText(description, maxWidth);
            poseStack.pushPose();
            poseStack.scale(0.9F, 0.9F, 1.0F);

            for (int i = 0; i < wrappedLines.size(); i++) {
               int scaledDescX = (int)((float)(centerX + 22) / 0.9F);
               int scaledDescY = (int)((float)(descriptionY + i * 9) / 0.9F);
               guiGraphics.drawString(this.font, wrappedLines.get(i), scaledDescX, scaledDescY, 4868459, false);
            }

            poseStack.popPose();
         }
      }
   }

   private List<String> wrapText(String text, int maxWidth) {
      List<String> lines = new ArrayList<>();
      String[] words = text.split(" ");
      StringBuilder currentLine = new StringBuilder();

      for (String word : words) {
         String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
         if (this.font.width(testLine) <= maxWidth) {
            currentLine = new StringBuilder(testLine);
         } else if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
            currentLine = new StringBuilder(word);
         } else {
            lines.add(word);
         }
      }

      if (!currentLine.isEmpty()) {
         lines.add(currentLine.toString());
      }

      return lines;
   }

   private void renderRecipeSlots(GuiGraphics guiGraphics, int mouseX, int mouseY) {
      for (BlueprintScreen.RecipeSlot slot : this.recipeSlots) {
         boolean isHovered = this.isMouseOverSlot(slot, mouseX, mouseY);
         if (isHovered) {
            guiGraphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, -2130706433);
         }

         ItemStack itemStack = this.recipeItems[slot.index];
         if (!itemStack.isEmpty()) {
            guiGraphics.renderItem(itemStack, slot.x, slot.y);
            guiGraphics.renderItemDecorations(this.font, itemStack, slot.x, slot.y);
         }
      }
   }

   private void renderItemTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
      for (BlueprintScreen.RecipeSlot slot : this.recipeSlots) {
         if (this.isMouseOverSlot(slot, mouseX, mouseY)) {
            ItemStack itemStack = this.recipeItems[slot.index];
            if (!itemStack.isEmpty()) {
               guiGraphics.renderTooltip(this.font, itemStack, mouseX, mouseY);
            }
            break;
         }
      }
   }

   private boolean isMouseOverSlot(BlueprintScreen.RecipeSlot slot, int mouseX, int mouseY) {
      return mouseX >= slot.x && mouseX < slot.x + 16 && mouseY >= slot.y && mouseY < slot.y + 16;
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (super.keyPressed(keyCode, scanCode, modifiers)) {
         return true;
      } else {
         return switch (keyCode) {
            case 266 -> {
               if (this.prevPageButton.active) {
                  this.prevPageButton.onPress();
               }

               yield true;
            }
            case 267 -> {
               if (this.nextPageButton.active) {
                  this.nextPageButton.onPress();
               }

               yield true;
            }
            default -> false;
         };
      }
   }

   public boolean isPauseScreen() {
      return false;
   }

   private void clearRecipeItems() {
      Arrays.fill(this.recipeItems, ItemStack.EMPTY);
   }

   private void saveActiveRecipe(ItemStack blueprint, ResourceLocation recipeId) {
      NbtHelper.getOrCreateTag(blueprint).putString("ActiveRecipe", recipeId.toString());
   }

   public static ResourceLocation getActiveRecipe(ItemStack blueprint) {
      if (NbtHelper.hasTag(blueprint)) {
         assert NbtHelper.getTag(blueprint) != null;

         if (NbtHelper.getTag(blueprint).contains("ActiveRecipe")) {
            String recipeIdString = NbtHelper.getTag(blueprint).getString("ActiveRecipe");
            return ResourceLocation.parse(recipeIdString);
         }
      }

      return null;
   }

   public static String getActiveRecipeName(ItemStack blueprint) {
      ResourceLocation recipeId = getActiveRecipe(blueprint);
      if (recipeId == null) {
         return null;
      } else {
         Level level = Minecraft.getInstance().level;
         if (level == null) {
            return null;
         } else {
            Optional<GunBenchRecipe> recipe = level.getRecipeManager()
               .byKey(recipeId)
               .filter(holder -> holder.value() instanceof GunBenchRecipe)
               .map(holder -> (GunBenchRecipe)holder.value());
            return recipe.<String>map(gunBenchRecipe -> gunBenchRecipe.getResultItem(level.registryAccess()).getDisplayName().getString()).orElse(null);
         }
      }
   }

   static {
      LORE_ONLY_ITEMS.put(ResourceLocation.fromNamespaceAndPath("scguns", "piglin_blueprint"), new ArrayList<>(Arrays.asList("blasphemy", "super_shotgun")));
      LORE_ONLY_ITEMS.put(ResourceLocation.fromNamespaceAndPath("scguns", "frontier_blueprint"), new ArrayList<>(Arrays.asList("kiln_gun")));
   }

   public static class BlueprintPageButton extends Button {
      public BlueprintPageButton(int x, int y, Component text, OnPress onPress) {
         super(x, y, 20, 20, text, onPress, DEFAULT_NARRATION);
      }

      public void playDownSound(SoundManager soundManager) {
         Minecraft.getInstance().player.playSound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F);
      }
   }

   private static class DisplayEntry {
      /** The recipe's real id, taken from the holder. Null for a lore-only display stack. */
      final ResourceLocation id;
      final GunBenchRecipe recipe;
      final ItemStack itemStack;
      final boolean hasRecipe;

      DisplayEntry(net.minecraft.world.item.crafting.RecipeHolder<GunBenchRecipe> holder) {
         this.id = holder.id();
         this.recipe = holder.value();
         this.itemStack = null;
         this.hasRecipe = true;
      }

      DisplayEntry(ItemStack itemStack) {
         this.id = null;
         this.recipe = null;
         this.itemStack = itemStack;
         this.hasRecipe = false;
      }
   }

   private static record RecipeSlot(int x, int y, int index) {
      private RecipeSlot(int x, int y, int index) {
         this.x = x;
         this.y = y;
         this.index = index;
      }
   }
}
