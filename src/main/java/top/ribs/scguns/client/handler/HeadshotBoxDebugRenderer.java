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
 * <p><b>On "0.5.5 drew this in purple": it did not draw it at all.</b> The 0.5.5 jar has no
 * reference to {@code renderLineBox} or {@code renderVoxelShape} in any of its 1449 classes, and
 * neither does the 1.20.1 source branch (816 files) - so no Scorched Guns build for 1.20.1 could
 * draw a wireframe box of any colour, purple included. Its {@code BoundingBoxManager} carries
 * exactly the methods this port's does, with nothing that draws. The colour here is therefore this
 * renderer's own; purple is the player's choice, and it is one constant below if it needs
 * changing.</p>
 */
public final class HeadshotBoxDebugRenderer {
   private static final float COLOUR_R = 0.7F;
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
