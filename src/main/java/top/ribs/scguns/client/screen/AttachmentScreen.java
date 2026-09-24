package top.ribs.scguns.client.screen;


import top.ribs.scguns.util.NbtHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import top.ribs.scguns.Config;
import top.ribs.scguns.client.handler.GunRenderingHandler;
import top.ribs.scguns.client.screen.widget.MiniButton;
import top.ribs.scguns.client.util.RenderUtil;
import top.ribs.scguns.common.FireMode;
import top.ribs.scguns.common.GripType;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.common.ReloadType;
import top.ribs.scguns.common.container.slot.AttachmentSlot;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.item.attachment.IAttachment;
import top.ribs.scguns.util.GunCompositeStatHelper;
import top.ribs.scguns.util.GunEnchantmentHelper;
import top.ribs.scguns.util.GunModifierHelper;

public class AttachmentScreen extends AbstractContainerScreen<AttachmentContainer> {
   private static final ResourceLocation GUI_TEXTURES = ResourceLocation.parse("scguns:textures/gui/attachments.png");
   private final Container weaponInventory;
   private int windowZoom = 10;
   private int windowX;
   private int windowY;
   private float windowRotationX;
   private float windowRotationY;
   private boolean mouseGrabbed;
   private int mouseGrabbedButton;
   private int mouseClickedX;
   private int mouseClickedY;
   private float momentumX = 0.0F;
   private float momentumY = 0.0F;
   private float lastDragX = 0.0F;
   private float lastDragY = 0.0F;
   private float targetRotationX = 0.0F;
   private float targetRotationY = 0.0F;
   private float prevRotationX = 0.0F;
   private float prevRotationY = 0.0F;
   private final List<Float> recentDragSpeedsX = new ArrayList<>();
   private final List<Float> recentDragSpeedsY = new ArrayList<>();
   private static final int VELOCITY_SAMPLES = 5;
   private float totalDragDistance = 0.0F;
   private static final float MOMENTUM_DAMPING = 0.92F;
   private static final float MIN_MOMENTUM = 0.15F;
   private static final float DRAG_THRESHOLD = 25.0F;
   private static final float MOMENTUM_SCALE = 1.4F;
   private static final float INTERPOLATION_SPEED = 0.5F;

   public AttachmentScreen(AttachmentContainer screenContainer, Inventory playerInventory, Component titleIn) {
      super(screenContainer, playerInventory, titleIn);
      this.weaponInventory = screenContainer.getWeaponInventory();
      this.imageHeight = 192;
      this.imageWidth = 188;
   }

   protected void init() {
      super.init();
      List<MiniButton> buttons = this.gatherButtons();

      for (int i = 0; i < buttons.size(); i++) {
         MiniButton button = buttons.get(i);
         switch ((ButtonAlignment)Config.CLIENT.buttonAlignment.get()) {
            case LEFT:
               assert this.minecraft != null;

               int titleWidth = this.minecraft.font.width(this.title);
               button.setX(this.leftPos + titleWidth + 8 + 3 + i * 13 - 6);
               break;
            case RIGHT:
               button.setX(this.leftPos + this.imageWidth - 7 - 10 - (buttons.size() - 1 - i) * 13 - 6);
         }

         button.setY(this.topPos + 5 + 19);
         this.addRenderableWidget(button);
      }
   }

   private List<MiniButton> gatherButtons() {
      List<MiniButton> buttons = new ArrayList<>();
      return buttons;
   }

   public void containerTick() {
      super.containerTick();
      if (this.minecraft != null && this.minecraft.player != null && !(this.minecraft.player.getMainHandItem().getItem() instanceof GunItem)) {
         Minecraft.getInstance().setScreen(null);
      }

      this.prevRotationX = this.windowRotationX;
      this.prevRotationY = this.windowRotationY;
      if (this.mouseGrabbed && this.mouseGrabbedButton == 1) {
         this.windowRotationX = this.windowRotationX + (this.targetRotationX - this.windowRotationX) * 0.5F;
         this.windowRotationY = this.windowRotationY + (this.targetRotationY - this.windowRotationY) * 0.5F;
      } else if (!this.mouseGrabbed && (Math.abs(this.momentumX) > 0.15F || Math.abs(this.momentumY) > 0.15F)) {
         this.windowRotationX = this.windowRotationX + this.momentumX;
         this.windowRotationY = this.windowRotationY + this.momentumY;
         this.targetRotationX = this.windowRotationX;
         this.targetRotationY = this.windowRotationY;
         this.momentumX *= 0.92F;
         this.momentumY *= 0.92F;
         if (Math.abs(this.momentumX) < 0.15F) {
            this.momentumX = 0.0F;
         }

         if (Math.abs(this.momentumY) < 0.15F) {
            this.momentumY = 0.0F;
         }
      }
   }

   public void render(GuiGraphics pGuiGraphics, int mouseX, int mouseY, float partialTicks) {
      super.render(pGuiGraphics, mouseX, mouseY, partialTicks);
      this.renderTooltip(pGuiGraphics, mouseX, mouseY);
      int startX = (this.width - this.imageWidth) / 2 - 6;
      int startY = (this.height - this.imageHeight) / 2 + 19;
      int numSlots = Math.min(5, IAttachment.Type.values().length);
      int centerX = 88 - numSlots * 18 / 2 + 18 - 5;

      for (int i = 0; i < numSlots; i++) {
         int x = centerX + i * 18;
         int y = 89;
         if (RenderUtil.isMouseWithin(mouseX, mouseY, startX + x, startY + y, 18, 18)) {
            IAttachment.Type type = IAttachment.Type.values()[i];
            if (!((AttachmentContainer)this.menu).getSlot(i).isActive()) {
               pGuiGraphics.renderComponentTooltip(
                  this.font,
                  Arrays.asList(
                     Component.translatable("slot.scguns.attachment." + type.getTranslationKey()), Component.translatable("slot.scguns.attachment.not_applicable")
                  ),
                  mouseX,
                  mouseY
               );
            } else {
               Slot var14 = ((AttachmentContainer)this.menu).getSlot(i);
               if (var14 instanceof AttachmentSlot) {
                  AttachmentSlot slot = (AttachmentSlot)var14;
                  if (slot.getItem().isEmpty() && !this.isCompatible(((AttachmentContainer)this.menu).getCarried(), slot)) {
                     pGuiGraphics.renderComponentTooltip(
                        this.font,
                        Arrays.asList(Component.translatable("slot.scguns.attachment.incompatible").withStyle(ChatFormatting.YELLOW)),
                        mouseX,
                        mouseY
                     );
                  }
               }
            }
         }
      }
   }

   protected void renderLabels(GuiGraphics pGuiGraphics, int mouseX, int mouseY) {
      Minecraft minecraft = Minecraft.getInstance();
      int left = (this.width - this.imageWidth) / 2 - 6;
      int top = (this.height - this.imageHeight) / 2 + 19;
      float partialTicks = minecraft.getTimer().getGameTimeDeltaPartialTick(false);
      float renderRotationX = this.prevRotationX + (this.windowRotationX - this.prevRotationX) * partialTicks;
      float renderRotationY = this.prevRotationY + (this.windowRotationY - this.prevRotationY) * partialTicks;
      pGuiGraphics.enableScissor(left + 8 - 14, top + 17 - 82, left + 8 + 176 + 14, top + 17 + 64);
      pGuiGraphics.pose().pushPose();
      pGuiGraphics.pose().translate(96.0F, 48.0F, 150.0F);
      pGuiGraphics.pose()
         .translate((float)(this.windowX + (this.mouseGrabbed && this.mouseGrabbedButton == 0 ? mouseX - this.mouseClickedX : 0)), 0.0F, 0.0F);
      pGuiGraphics.pose()
         .translate(0.0F, (float)(this.windowY + (this.mouseGrabbed && this.mouseGrabbedButton == 0 ? mouseY - this.mouseClickedY : 0)), 0.0F);
      pGuiGraphics.pose().mulPose(Axis.XP.rotationDegrees(-30.0F));
      pGuiGraphics.pose().mulPose(Axis.XP.rotationDegrees(renderRotationY));
      pGuiGraphics.pose().mulPose(Axis.YP.rotationDegrees(renderRotationX));
      pGuiGraphics.pose().mulPose(Axis.YP.rotationDegrees(150.0F));
      pGuiGraphics.pose().scale((float)this.windowZoom / 10.0F, (float)this.windowZoom / 10.0F, (float)this.windowZoom / 10.0F);
      pGuiGraphics.pose().mulPose(Axis.YP.rotationDegrees(90.0F));
      pGuiGraphics.pose().mulPose(new Matrix4f().scaling(1.0F, -1.0F, 1.0F));
      pGuiGraphics.pose().scale(90.0F, 90.0F, 90.0F);

      assert this.minecraft != null;

      BufferSource buffer = this.minecraft.renderBuffers().bufferSource();

      assert this.minecraft.player != null;

      // 0.5.5 pushed this transform onto RenderSystem's model-view stack and then
      // handed renderWeapon a brand new PoseStack, because in 1.20.1 the shader
      // read the model-view matrix from there. 1.21 removed that stack and takes
      // the matrix from the PoseStack passed to each draw call, so an empty
      // PoseStack renders the gun untransformed -- which is why the attachment
      // screen showed no gun model at all. Pass the pose that already carries the
      // transforms above instead.
      GunRenderingHandler.get()
         .renderWeapon(this.minecraft.player, this.minecraft.player.getMainHandItem(), ItemDisplayContext.GROUND, pGuiGraphics.pose(), buffer, 15728880, 0.0F);
      buffer.endBatch();
      pGuiGraphics.pose().popPose();
      pGuiGraphics.disableScissor();
      pGuiGraphics.flush();
      this.renderGunStats(pGuiGraphics);
   }

   private void renderGunStats(GuiGraphics pGuiGraphics) {
      if (this.minecraft != null && this.minecraft.player != null) {
         ItemStack gunStack = this.minecraft.player.getMainHandItem();
         if (gunStack.getItem() instanceof GunItem gunItem) {
            Gun modifiedGun = gunItem.getModifiedGun(gunStack);
            Gun.Projectile projectile = modifiedGun.getProjectile(gunStack);
            Gun.General general = modifiedGun.getGeneral();
            Gun.Reloads reloads = modifiedGun.getReloads();
            byte startX = -60;
            byte startY = 0;
            byte lineHeight = 7;
            float scale = 0.6F;
            pGuiGraphics.pose().pushPose();
            pGuiGraphics.pose().scale(scale, scale, scale);
            float currentY = (float)startY;
            String gunName = Component.translatable(gunStack.getDescriptionId()).getString();
            int gunNameWidth = this.font.width(gunName);
            pGuiGraphics.drawString(this.font, gunName, (int)((float)startX / scale), 0, 16777215, false);
            pGuiGraphics.fill(
               (int)((float)startX / scale),
               (int)((currentY + 6.0F) / scale),
               (int)(((float)startX + (float)gunNameWidth * scale) / scale),
               (int)((currentY + 7.0F) / scale),
               -1
            );
            currentY += (float)(lineHeight + 3);
            float baseDamage = projectile.getDamage();
            baseDamage = GunModifierHelper.getModifiedProjectileDamage(gunStack, baseDamage);
            baseDamage = GunEnchantmentHelper.getAcceleratorDamage(gunStack, baseDamage);
            baseDamage = GunEnchantmentHelper.getHeavyShotDamage(gunStack, baseDamage);
            baseDamage *= ((Double)Config.COMMON.gameplay.globalDamageMultiplier.get()).floatValue();
            float additionalDamage = GunModifierHelper.getAdditionalDamage(gunStack, false);
            CompoundTag tagCompound = NbtHelper.getTag(gunStack);
            if (tagCompound != null && tagCompound.contains("AdditionalDamage", 99)) {
               additionalDamage += tagCompound.getFloat("AdditionalDamage");
            }

            float totalDamage = baseDamage + additionalDamage;
            pGuiGraphics.drawString(
               this.font,
               Component.translatable("info.scguns.damage").getString() + ": " + String.format("%.1f", totalDamage),
               (int)((float)startX / scale),
               (int)(currentY / scale),
               16777215,
               false
            );
            currentY += (float)lineHeight;
            float critChance = GunModifierHelper.getCriticalChance(gunStack);
            if (critChance > 0.0F) {
               pGuiGraphics.drawString(
                  this.font,
                  Component.translatable("info.scguns.critical_chance").getString() + ": " + String.format("%.1f%%", critChance * 100.0F),
                  (int)((float)startX / scale),
                  (int)(currentY / scale),
                  16777215,
                  false
               );
               currentY += (float)lineHeight;
               pGuiGraphics.drawString(
                  this.font,
                  Component.translatable("info.scguns.critical_multiplier").getString() + ": " + String.format("%.1fx", projectile.getCritDamageMultiplier()),
                  (int)((float)startX / scale),
                  (int)(currentY / scale),
                  16777215,
                  false
               );
               currentY += (float)lineHeight;
            }

            if (projectile.getEnergyUse() > 0) {
               pGuiGraphics.drawString(
                  this.font,
                  Component.translatable("info.scguns.energy_use").getString() + ": " + projectile.getEnergyUse(),
                  (int)((float)startX / scale),
                  (int)(currentY / scale),
                  16777215,
                  false
               );
               currentY += (float)lineHeight;
            }

            float baseArmorPen = projectile.getArmorPen();
            float puncturingPen = GunEnchantmentHelper.getPuncturingArmorBypass(gunStack);
            float totalArmorPen = baseArmorPen + puncturingPen;
            if (totalArmorPen > 0.0F) {
               pGuiGraphics.drawString(
                  this.font,
                  Component.translatable("info.scguns.armor_penetration").getString() + ": " + String.format("%.1f", totalArmorPen),
                  (int)((float)startX / scale),
                  (int)(currentY / scale),
                  16777215,
                  false
               );
               currentY += (float)lineHeight;
            }

            FireMode fireMode = general.getFireMode();
            String fireModeKey = "fire_mode." + fireMode.id().toString();
            pGuiGraphics.drawString(
               this.font,
               Component.translatable("info.scguns.fire_mode").getString() + ": " + Component.translatable(fireModeKey).getString(),
               (int)((float)startX / scale),
               (int)(currentY / scale),
               16777215,
               false
            );
            currentY += (float)lineHeight;
            int modifiedRate = GunCompositeStatHelper.getCompositeRate(gunStack, modifiedGun);
            float rpm = 20.0F / (float)modifiedRate * 60.0F;
            pGuiGraphics.drawString(
               this.font,
               Component.translatable("info.scguns.fire_rate").getString() + ": " + String.format("%.0f", rpm) + " RPM",
               (int)((float)startX / scale),
               (int)(currentY / scale),
               16777215,
               false
            );
            currentY += (float)lineHeight;
            int modifiedAmmo = GunModifierHelper.getModifiedAmmoCapacity(gunStack, modifiedGun);
            pGuiGraphics.drawString(
               this.font,
               Component.translatable("info.scguns.max_ammo").getString() + ": " + modifiedAmmo,
               (int)((float)startX / scale),
               (int)(currentY / scale),
               16777215,
               false
            );
            currentY += (float)lineHeight;
            ReloadType reloadType = reloads.getReloadType();
            String reloadTypeKey = "reload_type." + reloadType.id().toString().replace(":", ".");
            pGuiGraphics.drawString(
               this.font,
               Component.translatable("info.scguns.reload_type").getString() + ": " + Component.translatable(reloadTypeKey).getString(),
               (int)((float)startX / scale),
               (int)(currentY / scale),
               16777215,
               false
            );
            currentY += (float)lineHeight;
            if (reloadType == ReloadType.SINGLE_ITEM && reloads.getReloadByproduct() != null) {
               pGuiGraphics.drawString(
                  this.font,
                  Component.translatable("info.scguns.reload_byproduct").getString()
                     + ": "
                     + Component.translatable(reloads.getReloadByproduct().getDescriptionId()).getString(),
                  (int)((float)startX / scale),
                  (int)(currentY / scale),
                  16777215,
                  false
               );
               currentY += (float)lineHeight;
               if (reloads.getByproductChance() > 0.0F) {
                  pGuiGraphics.drawString(
                     this.font,
                     Component.translatable("info.scguns.byproduct_chance").getString() + ": " + String.format("%.0f%%", reloads.getByproductChance() * 100.0F),
                     (int)((float)startX / scale),
                     (int)(currentY / scale),
                     16777215,
                     false
                  );
                  currentY += (float)lineHeight;
               }
            }

            GripType gripType = modifiedGun.determineGripType(gunStack);
            String gripKey = gripType == GripType.ONE_HANDED ? "info.scguns.grip_one_handed" : "info.scguns.grip_two_handed";
            pGuiGraphics.drawString(
               this.font,
               Component.translatable("info.scguns.grip_type").getString() + ": " + Component.translatable(gripKey).getString(),
               (int)((float)startX / scale),
               (int)(currentY / scale),
               16777215,
               false
            );
            currentY += (float)lineHeight;
            if (projectile.isAlwaysSpread()) {
               pGuiGraphics.drawString(
                  this.font,
                  Component.translatable("info.scguns.always_spread").getString(),
                  (int)((float)startX / scale),
                  (int)(currentY / scale),
                  16777215,
                  false
               );
               currentY += (float)lineHeight;
            }

            float modifiedSpread = GunModifierHelper.getModifiedSpread(gunStack, projectile.getSpread());
            pGuiGraphics.drawString(
               this.font,
               Component.translatable("info.scguns.spread").getString() + ": " + String.format("%.2f", modifiedSpread),
               (int)((float)startX / scale),
               (int)(currentY / scale),
               16777215,
               false
            );
            currentY += (float)lineHeight;
            pGuiGraphics.drawString(
               this.font,
               Component.translatable("info.scguns.recoil_angle").getString() + ": " + String.format("%.1f", projectile.getRecoilAngle()),
               (int)((float)startX / scale),
               (int)(currentY / scale),
               16777215,
               false
            );
            currentY += (float)lineHeight;
            float falloffStart = GunModifierHelper.getModifiedDamageFalloffStart(gunStack, projectile.getDamageFalloffStart());
            float falloffEnd = GunModifierHelper.getModifiedDamageFalloffEnd(gunStack, projectile.getDamageFalloffEnd());
            if (!(falloffStart > 0.0F) && !(falloffEnd > 0.0F)) {
               pGuiGraphics.drawString(
                  this.font,
                  Component.translatable("info.scguns.falloff_range").getString() + ": " + Component.translatable("info.scguns.falloff_none").getString(),
                  (int)((float)startX / scale),
                  (int)(currentY / scale),
                  16777215,
                  false
               );
            } else {
               pGuiGraphics.drawString(
                  this.font,
                  Component.translatable("info.scguns.falloff_range").getString()
                     + ": "
                     + String.format("%.0f", falloffStart)
                     + " - "
                     + String.format("%.0f", falloffEnd),
                  (int)((float)startX / scale),
                  (int)(currentY / scale),
                  16777215,
                  false
               );
            }

            pGuiGraphics.pose().popPose();
         }
      }
   }

   protected void renderBg(GuiGraphics pGuiGraphics, float partialTicks, int mouseX, int mouseY) {
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.setShaderTexture(0, GUI_TEXTURES);
      int left = (this.width - this.imageWidth) / 2 - 6;
      int top = (this.height - this.imageHeight) / 2 + 19;
      pGuiGraphics.blit(GUI_TEXTURES, left, top, 0, 0, this.imageWidth, this.imageHeight);
      int numSlots = Math.min(5, IAttachment.Type.values().length);
      int centerX = 88 - numSlots * 18 / 2 + 18 - 5;

      for (int i = 0; i < numSlots; i++) {
         int x = centerX + i * 18;
         int y = 89;
         if (!this.canPlaceAttachmentInSlot(((AttachmentContainer)this.menu).getCarried(), ((AttachmentContainer)this.menu).getSlot(i))) {
            pGuiGraphics.blit(GUI_TEXTURES, left + x, top + y, 192, 0, 16, 16);
         } else if (this.weaponInventory.getItem(i).isEmpty()) {
            pGuiGraphics.blit(GUI_TEXTURES, left + x, top + y, 192, 16 + i * 16, 16, 16);
         }
      }
   }

   private boolean canPlaceAttachmentInSlot(ItemStack stack, Slot slot) {
      if (!slot.isActive()) {
         return false;
      } else if (!slot.equals(this.getSlotUnderMouse())) {
         return true;
      } else if (!slot.getItem().isEmpty()) {
         return true;
      } else if (slot instanceof AttachmentSlot s) {
         if (stack.getItem() instanceof IAttachment<?> a) {
            return !s.getType().equals(a.getType()) ? true : s.mayPlace(stack);
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   private boolean isCompatible(ItemStack stack, AttachmentSlot slot) {
      if (stack.isEmpty()) {
         return true;
      } else if (stack.getItem() instanceof IAttachment<?> attachment) {
         if (!attachment.getType().equals(slot.getType())) {
            return true;
         } else {
            return !attachment.canAttachTo(stack) ? false : slot.mayPlace(stack);
         }
      } else {
         return false;
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
      int startX = (this.width - this.imageWidth) / 2 - 6;
      int startY = (this.height - this.imageHeight) / 2 + 19;
      if (RenderUtil.isMouseWithin((int)mouseX, (int)mouseY, startX + 8 - 14, startY + 17 - 82, 204, 146)) {
         if (scroll < 0.0 && this.windowZoom > 0) {
            this.windowZoom--;
         } else if (scroll > 0.0) {
            this.windowZoom++;
         }
      }

      return false;
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      int startX = (this.width - this.imageWidth) / 2 - 6;
      int startY = (this.height - this.imageHeight) / 2 + 19;
      if (RenderUtil.isMouseWithin((int)mouseX, (int)mouseY, startX + 8 - 14, startY + 17 - 82, 204, 146) && !this.mouseGrabbed && (button == 0 || button == 1)
         )
       {
         this.mouseGrabbed = true;
         this.mouseGrabbedButton = button == 1 ? 1 : 0;
         this.mouseClickedX = (int)mouseX;
         this.mouseClickedY = (int)mouseY;
         this.momentumX = 0.0F;
         this.momentumY = 0.0F;
         this.lastDragX = (float)mouseX;
         this.lastDragY = (float)mouseY;
         this.targetRotationX = this.windowRotationX;
         this.targetRotationY = this.windowRotationY;
         this.totalDragDistance = 0.0F;
         this.recentDragSpeedsX.clear();
         this.recentDragSpeedsY.clear();
         return true;
      } else {
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
      if (this.mouseGrabbed && this.mouseGrabbedButton == 1) {
         float deltaX = (float)(mouseX - (double)this.lastDragX);
         float deltaY = (float)(mouseY - (double)this.lastDragY);
         this.targetRotationX += deltaX;
         this.targetRotationY -= deltaY;
         float distance = (float)Math.sqrt((double)(deltaX * deltaX + deltaY * deltaY));
         this.totalDragDistance += distance;
         this.recentDragSpeedsX.add(deltaX);
         this.recentDragSpeedsY.add(-deltaY);
         if (this.recentDragSpeedsX.size() > 5) {
            this.recentDragSpeedsX.remove(0);
            this.recentDragSpeedsY.remove(0);
         }

         this.lastDragX = (float)mouseX;
         this.lastDragY = (float)mouseY;
      }

      return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (this.mouseGrabbed) {
         if (this.mouseGrabbedButton == 0 && button == 0) {
            this.mouseGrabbed = false;
            this.windowX = this.windowX + (int)(mouseX - (double)this.mouseClickedX - 1.0);
            this.windowY = this.windowY + (int)(mouseY - (double)this.mouseClickedY);
         } else if (this.mouseGrabbedButton == 1 && button == 1) {
            this.mouseGrabbed = false;
            if (this.totalDragDistance >= 25.0F && !this.recentDragSpeedsX.isEmpty()) {
               float avgSpeedX = 0.0F;
               float avgSpeedY = 0.0F;

               for (int i = 0; i < this.recentDragSpeedsX.size(); i++) {
                  avgSpeedX += this.recentDragSpeedsX.get(i);
                  avgSpeedY += this.recentDragSpeedsY.get(i);
               }

               avgSpeedX /= (float)this.recentDragSpeedsX.size();
               avgSpeedY /= (float)this.recentDragSpeedsY.size();
               this.momentumX = avgSpeedX * 1.4F;
               this.momentumY = avgSpeedY * 1.4F;
               float maxMomentum = 20.0F;
               this.momentumX = Math.max(-maxMomentum, Math.min(maxMomentum, this.momentumX));
               this.momentumY = Math.max(-maxMomentum, Math.min(maxMomentum, this.momentumY));
            } else {
               this.momentumX = 0.0F;
               this.momentumY = 0.0F;
            }

            this.recentDragSpeedsX.clear();
            this.recentDragSpeedsY.clear();
         }
      }

      return super.mouseReleased(mouseX, mouseY, button);
   }
}
