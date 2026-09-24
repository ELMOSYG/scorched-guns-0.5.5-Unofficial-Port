package top.ribs.scguns.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem.Type;
import top.ribs.scguns.item.animated.ExoSuitItem;

public class ExoSuitScreen extends AbstractContainerScreen<ExoSuitMenu> {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("scguns", "textures/gui/exosuit_gui.png");

   public ExoSuitScreen(ExoSuitMenu menu, Inventory inv, Component title) {
      super(menu, inv, title);
      this.imageWidth = 176;
      this.imageHeight = 166;
   }

   protected void init() {
      super.init();
      this.titleLabelY = 6;
      this.inventoryLabelY = this.imageHeight - 94;
   }

   protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.setShaderTexture(0, TEXTURE);
      int x = (this.width - this.imageWidth) / 2;
      int y = (this.height - this.imageHeight) / 2;
      guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
      this.renderUpgradeSlots(guiGraphics, x, y);
      this.renderUpgradeInfo(guiGraphics, x, y);
   }

   private void renderUpgradeSlots(GuiGraphics guiGraphics, int x, int y) {
      ItemStack armorPiece = ((ExoSuitMenu)this.menu).getArmorPiece();
      int[][] slotPositions = new int[][]{{97, 25}, {115, 25}, {97, 43}, {115, 43}};

      for (int i = 0; i < 4; i++) {
         int slotX = x + slotPositions[i][0];
         int slotY = y + slotPositions[i][1];
         if (!((ExoSuitMenu)this.menu).isUpgradeSlotEnabled(1 + i)) {
            guiGraphics.fill(slotX, slotY, slotX + 18, slotY + 18, Integer.MIN_VALUE);
         } else {
            this.renderSlotTypeIndicator(guiGraphics, slotX, slotY, armorPiece, i);
         }
      }
   }

   private void renderSlotTypeIndicator(GuiGraphics guiGraphics, int slotX, int slotY, ItemStack armorPiece, int slotIndex) {
      if (!armorPiece.isEmpty() && armorPiece.getItem() instanceof ExoSuitItem exosuit) {
         int color = this.getSlotTypeColor(exosuit.getType(), slotIndex);
         if (color != 0) {
            guiGraphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, color);
         }
      }
   }

   private int getSlotTypeColor(Type armorType, int slotIndex) {
      return switch (armorType) {
         case HELMET -> {
            switch (slotIndex) {
               case 0:
                  yield 553582592;
               case 1:
                  yield 536936192;
               case 2:
                  yield 536871167;
               default:
                  yield 0;
            }
         }
         case CHESTPLATE -> {
            switch (slotIndex) {
               case 0:
                  yield 553582592;
               case 1:
                  yield 553647872;
               case 2:
                  yield 553582847;
               case 3:
                  yield 536936447;
               default:
                  throw new IllegalStateException("Unexpected value: " + slotIndex);
            }
         }
         case LEGGINGS -> {
            switch (slotIndex) {
               case 0:
                  yield 553582592;
               case 1:
                  yield 545292416;
               case 2:
                  yield 536936447;
               default:
                  yield 0;
            }
         }
         case BOOTS -> {
            switch (slotIndex) {
               case 0:
                  yield 553582592;
               case 1:
                  yield 536936192;
               default:
                  yield 0;
            }
         }
         default -> throw new IncompatibleClassChangeError();
      };
   }

   private void renderUpgradeInfo(GuiGraphics guiGraphics, int x, int y) {
      ItemStack armorPiece = ((ExoSuitMenu)this.menu).getArmorPiece();
      if (!armorPiece.isEmpty() && armorPiece.getItem() instanceof ExoSuitItem exosuit) {
         int centerX = x + 26 + 9;
         int textY = y + 20;
         int maxSlots = exosuit.getMaxUpgradeSlots();
         int usedSlots = exosuit.getCurrentUpgradeCount(armorPiece);
         String slotInfo = usedSlots + "/" + maxSlots + "Slots";
         int textWidth = this.font.width(slotInfo);
         int textX = centerX - textWidth / 2;
         guiGraphics.drawString(this.font, slotInfo, textX, textY, 4210752, false);
      }
   }

   protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
      super.renderTooltip(guiGraphics, x, y);
      this.renderUpgradeSlotTooltips(guiGraphics, x, y);
   }

   private void renderUpgradeSlotTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
      ItemStack armorPiece = ((ExoSuitMenu)this.menu).getArmorPiece();
      if (!armorPiece.isEmpty()) {
         int x = (this.width - this.imageWidth) / 2;
         int y = (this.height - this.imageHeight) / 2;
         int[][] slotPositions = new int[][]{{97, 25}, {115, 25}, {97, 43}, {115, 43}};

         for (int i = 0; i < 4; i++) {
            int slotX = x + slotPositions[i][0];
            int slotY = y + slotPositions[i][1];
            if (mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18) {
               Slot upgradeSlot = ((ExoSuitMenu)this.menu).getSlot(37 + i);
               if (!((ExoSuitMenu)this.menu).isUpgradeSlotEnabled(1 + i)) {
                  guiGraphics.renderTooltip(this.font, Component.literal("Slot not available"), mouseX, mouseY);
               } else if (upgradeSlot.getItem().isEmpty()) {
                  String slotType = this.getSlotTypeTooltip(((ExoSuitItem)armorPiece.getItem()).getType(), i);
                  guiGraphics.renderTooltip(this.font, Component.literal(slotType), mouseX, mouseY);
               }
               break;
            }
         }
      }
   }

   private String getSlotTypeTooltip(Type armorType, int slotIndex) {
      return switch (armorType) {
         case HELMET -> {
            switch (slotIndex) {
               case 0:
                  yield "Plating Slot";
               case 1:
                  yield "HUD Slot";
               case 2:
                  yield "Breathing Apparatus Slot";
               default:
                  yield "Upgrade Slot";
            }
         }
         case CHESTPLATE -> {
            switch (slotIndex) {
               case 0:
                  yield "Plating Slot";
               case 1:
                  yield "Pauldron Slot";
               case 2:
                  yield "Power Core Slot";
               case 3:
                  yield "Utility Slot";
               default:
                  throw new IllegalStateException("Unexpected value: " + slotIndex);
            }
         }
         case LEGGINGS -> {
            switch (slotIndex) {
               case 0:
                  yield "Plating Slot";
               case 1:
                  yield "Knee Guard / Plating Slot";
               case 2:
                  yield "Utility Slot";
               default:
                  yield "Upgrade Slot";
            }
         }
         case BOOTS -> {
            switch (slotIndex) {
               case 0:
                  yield "Plating Slot";
               case 1:
                  yield "Mobility Enhancement Slot";
               default:
                  yield "Upgrade Slot";
            }
         }
         default -> throw new IncompatibleClassChangeError();
      };
   }

   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
      super.render(guiGraphics, mouseX, mouseY, delta);
      this.renderTooltip(guiGraphics, mouseX, mouseY);
   }
}
