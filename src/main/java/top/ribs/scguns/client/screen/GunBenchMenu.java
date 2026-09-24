package top.ribs.scguns.client.screen;

import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.NotNull;
import top.ribs.scguns.common.recipe.ContainerRecipeInput;
import top.ribs.scguns.common.recipe.ScgunsRecipeBookTypes;
import top.ribs.scguns.entity.player.PlayerGunProgression;
import top.ribs.scguns.event.GunProgressionEventHandler;
import top.ribs.scguns.item.BlueprintItem;

/**
 * Gun bench container.
 *
 * <p>Extends {@link RecipeBookMenu} so the bench gets vanilla's recipe book (HANDOFF section 39).
 * The "grid" is the ten attachment slots in the order the recipe's ingredient list uses, which is
 * why {@link #getGridWidth()} is 1 and {@link #getGridHeight()} is 10: vanilla's placement code
 * addresses grid cells through {@code getSlot(index)}, so a single column maps index i to
 * {@code slots.get(i)}. The blueprint sits outside that grid and is placed by
 * {@link #handlePlacement} itself.</p>
 */
public class GunBenchMenu extends RecipeBookMenu<ContainerRecipeInput, GunBenchRecipe> {
   private final Container container;
   private final ContainerLevelAccess containerAccess;
   private final Player player;
   public static final int SLOT_GRIP = 8;
   public static final int SLOT_MAGAZINE = 9;
   public static final int SLOT_BARREL_1 = 6;
   public static final int SLOT_BARREL_2 = 7;
   public static final int SLOT_INTERNAL_1 = 4;
   public static final int SLOT_INTERNAL_2 = 5;
   public static final int SLOT_TOP_INTERNAL_1 = 0;
   public static final int SLOT_TOP_INTERNAL_2 = 1;
   public static final int SLOT_TOP_BARREL_1 = 2;
   public static final int SLOT_TOP_BARREL_2 = 3;
   public static final int SLOT_OUTPUT = 10;
   public static final int SLOT_BLUEPRINT = 11;

   public GunBenchMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
      this(id, playerInventory, new SimpleContainer(12), ContainerLevelAccess.NULL);
   }

   public GunBenchMenu(int id, Inventory playerInventory, ContainerLevelAccess containerAccess) {
      this(id, playerInventory, new SimpleContainer(12), containerAccess);
   }

   public GunBenchMenu(int id, Inventory playerInventory, Container container, ContainerLevelAccess containerAccess) {
      super(ModMenuTypes.GUN_BENCH.get(), id);
      checkContainerSize(container, 12);
      this.container = container;
      this.containerAccess = containerAccess;
      this.player = playerInventory.player;
      container.startOpen(playerInventory.player);
      // Menu slot order matters for the recipe book: vanilla's PlaceRecipe starts its slot counter at
      // 0, skips the output slot when it passes it, and RecipeBookComponent draws the result ghost at
      // slots.get(0). With the output anywhere but slot 0 the ghost recipe lands one slot off - the
      // player saw the crafted gun's projection sitting on the first attachment slot (HANDOFF 39.6).
      // So: 0 = output, 1..10 = the ten attachment slots in ingredient order (the grid), 11 = blueprint.
      this.addSlot(new Slot(container, 10, 140, 44) {
         public boolean mayPlace(ItemStack stack) {
            return false;
         }

         public void onTake(Player player, ItemStack stack) {
            super.onTake(player, stack);
            if (!player.level().isClientSide && !stack.isEmpty()) {
               PlayerGunProgression progression = PlayerGunProgression.get(player);
               if (progression.checkAndUpdateFromItem(stack)) {
                  PlayerGunProgression.save(player, progression);
                  GunProgressionEventHandler.sendTierUnlockedMessage(player, progression.getCurrentTier());
               }
            }

            GunBenchMenu.this.consumeIngredients();
         }
      });
      this.addSlot(new Slot(container, 0, 26, 17));
      this.addSlot(new Slot(container, 1, 44, 17));
      this.addSlot(new Slot(container, 2, 62, 17));
      this.addSlot(new Slot(container, 3, 80, 17));
      this.addSlot(new Slot(container, 4, 26, 35));
      this.addSlot(new Slot(container, 5, 44, 35));
      this.addSlot(new Slot(container, 6, 62, 35));
      this.addSlot(new Slot(container, 7, 80, 35));
      this.addSlot(new Slot(container, 8, 26, 53));
      this.addSlot(new Slot(container, 9, 62, 53));
      // 0.5.5 added the blueprint slot twice (two menu slots over container 11 at the same position),
      // which also made the old shift-click range point at the output slot instead of the blueprint.
      this.addSlot(new Slot(container, 11, 116, 17) {
         public boolean mayPlace(@NotNull ItemStack stack) {
            return stack.getItem() instanceof BlueprintItem;
         }

         public void setChanged() {
            super.setChanged();
            GunBenchMenu.this.attemptAutoCrafting();
         }
      });

      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
         }
      }

      for (int col = 0; col < 9; col++) {
         this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
      }

      this.slotsChanged(container);
   }

   // ------------------------------------------------------------------ recipe book (HANDOFF 39)

   /**
    * Menu slot index of the crafting result.
    *
    * <p>Zero on purpose: vanilla's {@code PlaceRecipe} counts slots from 0 and skips the output slot,
    * and {@code RecipeBookComponent} draws the result ghost at {@code slots.get(0)}. Putting the output
    * anywhere else shifts the whole ghost recipe by one slot (HANDOFF 39.6).</p>
    */
   public static final int MENU_SLOT_OUTPUT = 0;
   /** Menu slot index of the blueprint, which sits after the grid. */
   public static final int MENU_SLOT_BLUEPRINT = 11;
   /** First menu slot of the attachment grid: the recipe's ingredient i lands on {@code MENU_SLOT_GRID_START + i}. */
   public static final int MENU_SLOT_GRID_START = 1;
   /** Attachment slots = the recipe book's grid size. */
   public static final int GRID_SIZE = 10;

   /**
    * Set while the recipe book is filling the bench. The blueprint slot's {@code setChanged} runs the
    * bench's own auto-craft, which pulls the materials for the blueprint's *stored* recipe; left
    * alone it would undo exactly the recipe the player just clicked.
    */
   private boolean placingFromBook;

   @Override
   public RecipeBookType getRecipeBookType() {
      return ScgunsRecipeBookTypes.gunBench();
   }

   @Override
   public void fillCraftSlotsStackedContents(StackedContents contents) {
      for (int i = 0; i < GRID_SIZE; i++) {
         contents.accountSimpleStack(this.container.getItem(i));
      }

      contents.accountSimpleStack(this.container.getItem(SLOT_BLUEPRINT));
   }

   @Override
   public void clearCraftingContent() {
      for (int i = 0; i < GRID_SIZE; i++) {
         this.returnContainerSlotToPlayer(i);
      }

      this.returnContainerSlotToPlayer(SLOT_BLUEPRINT);
   }

   /** Hand one bench slot back to the player, dropping it only if the inventory is full. */
   private void returnContainerSlotToPlayer(int containerIndex) {
      ItemStack stack = this.container.getItem(containerIndex);
      if (!stack.isEmpty()) {
         this.container.setItem(containerIndex, ItemStack.EMPTY);
         if (!this.player.getInventory().add(stack)) {
            this.player.drop(stack, false);
         }
      }
   }

   @Override
   public boolean recipeMatches(RecipeHolder<GunBenchRecipe> recipe) {
      return recipe.value().matches(new ContainerRecipeInput(this.container), this.player.level());
   }

   @Override
   public int getResultSlotIndex() {
      return MENU_SLOT_OUTPUT;
   }

   @Override
   public int getGridWidth() {
      return 1;
   }

   /**
    * Eleven input slots: the ten attachment slots and the blueprint.
    *
    * <p>The blueprint is listed as the recipe's last ingredient so the recipe book can ghost it, and
    * {@code PlaceRecipe} maps grid index {@code k} to menu slot {@code k} while stepping over the
    * result slot - with the output at slot 0 that puts index 10 on slot 11, the blueprint's slot.
    * {@code ServerPlaceRecipe} walks {@code gridWidth * gridHeight + 1} = 12 slots, which is exactly
    * the bench's own slot count (HANDOFF section 42.8). Declaring only the ten attachment slots left
    * the blueprint out of both the ghost and the automatic placement.</p>
    */
   @Override
   public int getGridHeight() {
      return GRID_SIZE + 1;
   }

   @Override
   public int getSize() {
      return GRID_SIZE + 1;
   }

   @Override
   public boolean shouldMoveToInventory(int slotIndex) {
      return slotIndex != MENU_SLOT_OUTPUT;
   }

   /**
    * Placing a recipe from the book.
    *
    * <p>Vanilla's {@link RecipeBookMenu#handlePlacement} runs {@code ServerPlaceRecipe}, which moves
    * the ingredients out of the player's inventory into the grid - now including the blueprint, since
    * it is the recipe's last input (see {@link #getGridHeight()}). {@link #placeBlueprint} stays as a
    * fallback for the case where the placer could not fill that slot itself; the auto-craft the
    * blueprint slot normally triggers is suppressed for this one round.</p>
    */
   @Override
   public void handlePlacement(boolean placeAll, RecipeHolder<?> recipe, ServerPlayer player) {
      this.placingFromBook = true;
      try {
         super.handlePlacement(placeAll, recipe, player);
         if (recipe.value() instanceof GunBenchRecipe gunBenchRecipe) {
            this.placeBlueprint(gunBenchRecipe);
         }
      } finally {
         this.placingFromBook = false;
      }

      this.slotsChanged(this.container);
   }

   /** Put the recipe's blueprint into its slot, taking it out of the player's inventory if needed. */
   private void placeBlueprint(GunBenchRecipe recipe) {
      ItemStack existing = this.container.getItem(SLOT_BLUEPRINT);
      if (!existing.isEmpty() && recipe.getBlueprint().test(existing)) {
         return;
      }

      if (!existing.isEmpty()) {
         this.returnContainerSlotToPlayer(SLOT_BLUEPRINT);
      }

      ItemStack found = this.findAndRemoveIngredientFromInventory(recipe.getBlueprint());
      if (!found.isEmpty()) {
         this.container.setItem(SLOT_BLUEPRINT, found);
      }
   }

   private void attemptAutoCrafting() {
      if (this.placingFromBook) {
         return;
      }
      this.containerAccess
         .execute(
            (level, pos) -> {
               if (!level.isClientSide) {
                  ItemStack blueprintStack = this.container.getItem(11);
                  if (!blueprintStack.isEmpty() && blueprintStack.getItem() instanceof BlueprintItem) {
                     ResourceLocation activeRecipeId = BlueprintScreen.getActiveRecipe(blueprintStack);
                     if (activeRecipeId != null) {
                        Optional<GunBenchRecipe> recipeOptional = level.getRecipeManager()
                           .getAllRecipesFor(GunBenchRecipe.Type.INSTANCE)
                           .stream()
                           .map(net.minecraft.world.item.crafting.RecipeHolder::value)
                           .filter(recipex -> recipex.getId().equals(activeRecipeId))
                           .findFirst();
                        if (!recipeOptional.isEmpty()) {
                           GunBenchRecipe recipe = recipeOptional.get();

                           for (int i = 0; i < 10; i++) {
                              ItemStack existing = this.container.getItem(i);
                              if (!existing.isEmpty()) {
                                 if (!this.player.getInventory().add(existing)) {
                                    this.player.drop(existing, false);
                                 }

                                 this.container.setItem(i, ItemStack.EMPTY);
                              }
                           }

                           NonNullList<Ingredient> ingredients = recipe.getModuleIngredients();

                           for (int slotIndex = 0; slotIndex < ingredients.size(); slotIndex++) {
                              Ingredient ingredient = (Ingredient)ingredients.get(slotIndex);
                              if (!ingredient.isEmpty()) {
                                 ItemStack foundItem = this.findAndRemoveIngredientFromInventory(ingredient);
                                 if (!foundItem.isEmpty()) {
                                    this.container.setItem(slotIndex, foundItem);
                                 }
                              }
                           }

                           this.slotsChanged(this.container);
                        }
                     }
                  }
               }
            }
         );
   }

   private ItemStack findAndRemoveIngredientFromInventory(Ingredient ingredient) {
      for (int i = 9; i < this.player.getInventory().getContainerSize(); i++) {
         ItemStack stack = this.player.getInventory().getItem(i);
         if (!stack.isEmpty() && ingredient.test(stack)) {
            ItemStack result = stack.copy();
            result.setCount(1);
            stack.shrink(1);
            return result;
         }
      }

      for (int ix = 0; ix < 9; ix++) {
         ItemStack stack = this.player.getInventory().getItem(ix);
         if (!stack.isEmpty() && ingredient.test(stack)) {
            ItemStack result = stack.copy();
            result.setCount(1);
            stack.shrink(1);
            return result;
         }
      }

      return ItemStack.EMPTY;
   }

   public void slotsChanged(Container container) {
      this.containerAccess.execute((level, pos) -> {
         if (!level.isClientSide) {
            SimpleContainer craftingContainer = new SimpleContainer(12);

            for (int i = 0; i < 12; i++) {
               craftingContainer.setItem(i, container.getItem(i));
            }

            ContainerRecipeInput recipeInput = new ContainerRecipeInput(craftingContainer);
            Optional<GunBenchRecipe> recipe = level.getRecipeManager().getRecipeFor(GunBenchRecipe.Type.INSTANCE, recipeInput, level).map(net.minecraft.world.item.crafting.RecipeHolder::value);
            if (recipe.isPresent()) {
               ItemStack result = recipe.get().assemble(recipeInput, level.registryAccess());
               container.setItem(10, result);
            } else {
               container.setItem(10, ItemStack.EMPTY);
            }
         }
      });
   }

   private void consumeIngredients() {
      this.containerAccess.execute((level, pos) -> {
         if (!level.isClientSide) {
            SimpleContainer craftingContainer = new SimpleContainer(12);

            for (int i = 0; i < 12; i++) {
               craftingContainer.setItem(i, this.container.getItem(i));
            }

            Optional<GunBenchRecipe> recipeOptional = level.getRecipeManager()
               .getRecipeFor(GunBenchRecipe.Type.INSTANCE, new ContainerRecipeInput(craftingContainer), level)
               .map(net.minecraft.world.item.crafting.RecipeHolder::value);
            if (recipeOptional.isPresent()) {
               GunBenchRecipe recipe = recipeOptional.get();
               // Container indices, not the book's input list: container slot 10 is the output and
               // slot 11 the blueprint, so the combined getIngredients() list would not line up.
               NonNullList<Ingredient> ingredients = recipe.getModuleIngredients();

               for (int i = 0; i < ingredients.size(); i++) {
                  Ingredient requiredIngredient = (Ingredient)ingredients.get(i);
                  ItemStack stackInSlot = this.container.getItem(i);
                  if (!requiredIngredient.isEmpty() && !stackInSlot.isEmpty()) {
                     stackInSlot.shrink(1);
                     this.container.setItem(i, stackInSlot);
                  }
               }
            }
         }
      });
   }

   public void broadcastChanges() {
      super.broadcastChanges();
      this.slotsChanged(this.container);
   }

   public boolean stillValid(Player player) {
      return this.container.stillValid(player);
   }

   @NotNull
   public ItemStack quickMoveStack(Player player, int index) {
      ItemStack itemstack = ItemStack.EMPTY;
      Slot slot = (Slot)this.slots.get(index);
      if (slot.hasItem()) {
         ItemStack itemstack1 = slot.getItem();
         itemstack = itemstack1.copy();
         if (index >= this.container.getContainerSize()) {
            if (itemstack1.getItem() instanceof BlueprintItem) {
               boolean moveSuccess = this.moveItemStackTo(itemstack1, MENU_SLOT_BLUEPRINT, MENU_SLOT_BLUEPRINT + 1, false);
               if (!moveSuccess && !this.moveItemStackTo(itemstack1, MENU_SLOT_GRID_START, MENU_SLOT_GRID_START + GRID_SIZE, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (!this.moveItemStackTo(itemstack1, 0, this.container.getContainerSize(), false)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo(itemstack1, this.container.getContainerSize(), this.slots.size(), true)) {
            return ItemStack.EMPTY;
         }

         if (itemstack1.isEmpty()) {
            slot.set(ItemStack.EMPTY);
         } else {
            slot.setChanged();
         }

         if (itemstack1.getCount() == itemstack.getCount()) {
            return ItemStack.EMPTY;
         }

         slot.onTake(player, itemstack1);
      }

      return itemstack;
   }
}
