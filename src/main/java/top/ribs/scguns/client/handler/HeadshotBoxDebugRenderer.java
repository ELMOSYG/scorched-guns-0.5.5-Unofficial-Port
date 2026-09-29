package top.ribs.scguns.client.handler;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import top.ribs.scguns.common.BoundingBoxManager;
import top.ribs.scguns.interfaces.IHeadshotBox;

/**
 * Draws the headshot box next to the entity box in the F3+B view.
 *
 * <p>The mod decides headshots with a box of its own ({@link IHeadshotBox}, registered per entity
 * type in {@link BoundingBoxManager}), and vanilla's F3+B only ever draws
 * {@code Entity#getBoundingBox()}. So the box that actually decides a headshot - usually a small
 * box on the head - was invisible: the only way to see it was to read the source and work out the
 * numbers, which is a poor way to tune a hitbox. This has never been drawn - 0.5.5 had exactly the
 * same five files touching it and no renderer either, so this is not a port regression.</p>
 *
 * <p><b>Where it is hooked, and why there.</b> Into {@code EntityRenderDispatcher.renderHitbox},
 * the private static method vanilla itself calls once per entity while F3+B is on. That means:
 * the mod's box appears and disappears with the key, is drawn with the same pose and vertex
 * consumer as the entity box, and costs nothing while the view is off. It is also the only place
 * that has already translated the pose to the entity, which is what the box is expressed in.</p>
 *
 * <p><b>The frame.</b> {@code IHeadshotBox} returns a box relative to the entity, and the hit test
 * anchors it with {@code move(boundingBox.getCenter().x, boundingBox.minY,
 * boundingBox.getCenter().z)} (see {@code ProjectileEntity#findHitLocation}). That anchor is
 * copied here rather than re-derived - if the two ever disagreed, the debug view would be drawing
 * a box that does not exist. Vanilla's own box is then made entity-relative by subtracting the
 * entity position, because that is the space the pose stack is in at this point.</p>
 *
 * <p>Drawn in purple to tell it from the white entity box, and deliberately not gated on the
 * {@code enableHeadShots} option: this is a debug view, and the useful moment to look at a
 * headshot box is while deciding whether to turn headshots on.</p>
 *
 * <p><b>Where the purple comes from, and who owns the box.</b> SCG Extra, the add-on, is what
 * draws a headshot box - {@code net.zincstudios.scgextra.debug.EntityHeadBoxDebug}, reading the
 * very same {@link BoundingBoxManager#getHeadshotBoxes} this renderer reads. Its colour, read out
 * of that class's bytecode, is 0.8 / 0.0 / 1.0 with alpha 1.0, so the value below matches it
 * exactly. (SCG Extra also has a second box system Scorched Guns has no equivalent of at all,
 * {@code WeakPointBoxManager}, drawn in a dimmer 0.6 / 0.0 / 1.0.)</p>
 *
 * <p><b>Scorched Guns itself never drew a headshot box on 1.20.1.</b> It has no reference to
 * {@code LevelRenderer.renderLineBox} in any of its 1449 classes - and note that a search for the
 * mapped name proves nothing on its own, because those jars carry the SRG name
 * {@code m_109646_}; the search has to use that. The 1.20.1 source branch agrees: 816 files, no
 * line-box call anywhere. So the purple the player remembers was SCG Extra's, and this renderer
 * deliberately adopts its colour so the two look the same.</p>
 */
public final class HeadshotBoxDebugRenderer {
   private static final float COLOUR_R = 0.8F;
   private static final float COLOUR_G = 0.0F;
   private static final float COLOUR_B = 1.0F;
   private static final float ALPHA = 1.0F;

   private HeadshotBoxDebugRenderer() {
   }

   /**
    * Draw one entity's headshot box. Called once per entity from
    * {@code EntityRenderDispatcherMixin}, already inside vanilla's hitbox pass.
    *
    * @param box the entity box vanilla just drew, used for the anchor - passed in rather than
    *            re-read so the two cannot come from different frames
    */
   public static void render(PoseStack stack, VertexConsumer consumer, Entity entity, AABB box) {
      if (!(entity instanceof LivingEntity living)) {
         return;
      }

      IHeadshotBox<LivingEntity> headshotBox = BoundingBoxManager.getHeadshotBoxes(entity.getType());
      if (headshotBox == null) {
         return;
      }

      AABB local = headshotBox.getHeadshotBox(living);
      if (local == null) {
         return;
      }

      AABB world = local.move(box.getCenter().x, box.minY, box.getCenter().z);
      AABB relative = world.move(-entity.getX(), -entity.getY(), -entity.getZ());
      // 1.21.1's AABB has no isEmpty(); getSize() is 0 exactly when the box has no volume, and
      // renderLineBox on a degenerate box draws nothing useful while still pushing vertices.
      if (relative.getSize() == 0.0) {
         return;
      }

      LevelRenderer.renderLineBox(stack, consumer, relative, COLOUR_R, COLOUR_G, COLOUR_B, ALPHA);
   }
}
