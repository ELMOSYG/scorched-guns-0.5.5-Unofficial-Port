package top.ribs.scguns.debug.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.ContainerObjectSelectionList.Entry;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;
import top.ribs.scguns.debug.IDebugWidget;
import top.ribs.scguns.debug.IEditorMenu;
import top.ribs.scguns.util.ScreenUtil;

public class EditorScreen extends Screen {
   private static final ResourceLocation WINDOW_TEXTURE = ResourceLocation.fromNamespaceAndPath("scguns", "textures/gui/debug.png");
   private static final int WIDTH = 150;
   private final Screen parent;
   private final IEditorMenu menu;
   private EditorScreen.PropertyList list;
   private int windowWidth;
   private int windowHeight;
   private int windowLeft;
   private int windowTop;

   public EditorScreen(Screen parent, IEditorMenu menu) {
      super(menu.getEditorLabel());
      this.parent = parent;
      this.menu = menu;
   }

   protected void init() {
      List<Pair<Component, Supplier<IDebugWidget>>> widgets = new ArrayList<>();
      this.menu.getEditorWidgets(widgets);
      this.windowWidth = 150;
      this.windowHeight = widgets.size() * 34 + 20 + 10;
      this.windowLeft = 10;
      this.windowTop = (this.height - this.windowHeight) / 2;
      this.addRenderableWidget(
         Button.builder(Component.literal("<"), btn -> Minecraft.getInstance().setScreen(this.parent))
            .pos(this.windowLeft + 150 - 12 - 4, this.windowTop + 4)
            .size(12, 12)
            .build()
      );
      this.list = new EditorScreen.PropertyList();
      this.list.setX(this.windowLeft + 10);
      this.addWidget(this.list);
      widgets.forEach(pair -> {
         if (((Supplier)pair.getRight()).get() instanceof AbstractWidget widget) {
            this.list.addEntry(new EditorScreen.PropertyEntry((Component)pair.getLeft(), widget));
         }
      });
   }

   public void render(GuiGraphics pGuiGraphics, int mouseX, int mouseY, float partialTicks) {
      this.drawHeader(this.windowLeft, this.windowTop, this.windowWidth);
      this.drawBody(this.windowLeft + 4, this.windowTop + 20, this.windowWidth - 8, this.windowHeight - 20);
      this.list.render(pGuiGraphics, mouseX, mouseY, partialTicks);
      super.render(pGuiGraphics, mouseX, mouseY, partialTicks);
      pGuiGraphics.drawString(this.font, this.getTitle(), this.windowLeft + 5, this.windowTop + 6, 16777215);
   }

   public boolean isPauseScreen() {
      return false;
   }

   private void drawHeader(int x, int y, int width) {
      int height = 20;
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      RenderSystem.setShaderTexture(0, WINDOW_TEXTURE);
      this.drawTexturedRect(x, y, 0, 0, 2, 2, 2, 2);
      this.drawTexturedRect(x + width - 2, y, 3, 0, 2, 2, 2, 2);
      this.drawTexturedRect(x, y + height - 2, 0, 18, 2, 2, 2, 2);
      this.drawTexturedRect(x + width - 2, y + height - 2, 3, 18, 2, 2, 2, 2);
      this.drawTexturedRect(x + 2, y, 2, 0, width - 4, 2, 1, 2);
      this.drawTexturedRect(x + 2, y + height - 2, 2, 18, width - 4, 2, 1, 2);
      this.drawTexturedRect(x, y + 2, 0, 2, 2, height - 4, 2, 16);
      this.drawTexturedRect(x + width - 2, y + 2, 3, 2, 2, height - 4, 2, 16);
      this.drawTexturedRect(x + 2, y + 2, 2, 2, width - 4, height - 4, 1, 16);
   }

   private void drawBody(int x, int y, int width, int height) {
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      RenderSystem.setShaderTexture(0, WINDOW_TEXTURE);
      this.drawTexturedRect(x, y + height - 2, 5, 3, 2, 2, 2, 2);
      this.drawTexturedRect(x + width - 2, y + height - 2, 8, 3, 2, 2, 2, 2);
      this.drawTexturedRect(x + 2, y + height - 2, 7, 3, width - 4, 2, 1, 2);
      this.drawTexturedRect(x, y, 5, 2, 2, height - 2, 2, 1);
      this.drawTexturedRect(x + width - 2, y, 8, 2, 2, height - 2, 2, 1);
      this.drawTexturedRect(x + 2, y, 7, 2, width - 4, height - 2, 1, 1);
   }

   private void drawTexturedRect(int x, int y, int u, int v, int width, int height, int textureWidth, int textureHeight) {
      float uScale = 0.00390625F;
      float vScale = 0.00390625F;
      Tesselator tesselator = Tesselator.getInstance();
      BufferBuilder buffer = tesselator.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
      buffer.addVertex((float)x, (float)(y + height), 0.0F).setUv((float)u * uScale, (float)(v + textureHeight) * vScale);
      buffer.addVertex((float)(x + width), (float)(y + height), 0.0F).setUv((float)(u + textureWidth) * uScale, (float)(v + textureHeight) * vScale);
      buffer.addVertex((float)(x + width), (float)y, 0.0F).setUv((float)(u + textureWidth) * uScale, (float)v * vScale);
      buffer.addVertex((float)x, (float)y, 0.0F).setUv((float)u * uScale, (float)v * vScale);
      BufferUploader.drawWithShader(buffer.buildOrThrow());
   }

   private class PropertyEntry extends Entry<EditorScreen.PropertyEntry> {
      private final Component label;
      private final AbstractWidget widget;

      public PropertyEntry(Component label, AbstractWidget widget) {
         super();
         this.label = label;
         this.widget = widget;
      }

      public void render(
         GuiGraphics pGuiGraphics, int index, int top, int left, int rowWidth, int rowHeight, int mouseX, int mouseY, boolean hovered, float partialTicks
      ) {
         pGuiGraphics.drawString(EditorScreen.this.getMinecraft().font, this.label, left + 5, top, 16777215);
         this.widget.setX(left);
         this.widget.setY(top + 10);
         this.widget.setWidth(rowWidth);
         this.widget.render(pGuiGraphics, mouseX, mouseY, partialTicks);
      }

      public List<? extends NarratableEntry> narratables() {
         return List.of();
      }

      public List<? extends GuiEventListener> children() {
         return List.of(this.widget);
      }

      public boolean isMouseOver(double mouseX, double mouseY) {
         return ScreenUtil.isMouseWithin(
               EditorScreen.this.list.getRowLeft(),
               EditorScreen.this.list.getY(),
               EditorScreen.this.list.getRowWidth(),
               EditorScreen.this.list.getHeight(),
               (int)mouseX,
               (int)mouseY
            )
            && super.isMouseOver(mouseX, mouseY);
      }
   }

   private class PropertyList extends ContainerObjectSelectionList<EditorScreen.PropertyEntry> {
      public static final int ITEM_HEIGHT = 34;

      public PropertyList() {
         super(
            EditorScreen.this.minecraft,
            EditorScreen.this.windowWidth - 20,
            EditorScreen.this.windowHeight,
            EditorScreen.this.windowTop + 20,
            34
         );
      }

      public int addEntry(EditorScreen.PropertyEntry entry) {
         return super.addEntry(entry);
      }



      protected int getScrollbarPosition() {
         return this.getX() + this.width - 6;
      }

      public int getRowLeft() {
         return super.getRowLeft() - 2;
      }

      public int getRowWidth() {
         return EditorScreen.this.windowWidth - 20 - 2 - (this.getMaxScroll() > 0 ? 6 : 0);
      }


   }
}
