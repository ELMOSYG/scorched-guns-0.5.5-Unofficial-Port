package top.ribs.scguns.mixin.client.compat.guardvillagers;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.item.GunItem;

/**
 * A guard holding a gun uses the <b>player's</b> gun holding animation (HANDOFF section 82.17).
 *
 * <h2>Why the generic mob pose never reached a guard</h2>
 *
 * <p>{@code MixinHumanoidModel} already poses any humanoid mob that holds a gun, but it injects into
 * {@code HumanoidModel.setupAnim(LivingEntity, ...)}, and Guard Villagers' {@code GuardModel} <b>overrides
 * that method</b> (it extends {@code HumanoidModel<Guard>} and declares its own
 * {@code setupAnim(Guard, ...)}, whose bridge overrides the {@code LivingEntity} one). Java calls the
 * override, so the generic pose never ran for a guard and a guard held its gun with whatever arm angles the
 * guard model's own animation had just left there.</p>
 *
 * <h2>Why it can simply borrow the player's</h2>
 *
 * <p>A guard's model is a {@code HumanoidModel} with the same skeleton the player model has - the same
 * {@code rightArm}/{@code leftArm}/{@code head} parts - so the player's own pose data applies directly. That
 * is what {@code IHeldAnimation#applyPlayerModelRotation} is, and section 82.17 widened its parameter from
 * {@code Player} to {@code LivingEntity} because the pose math only ever reads state a guard also has
 * (pitch, crouching, the off hand). The two rules that are genuinely about the local player - the melee pose
 * taken from the player's own swing - are now guarded by {@code instanceof Player} inside the poses
 * themselves, so a player swinging a sword cannot drag every armed guard into the melee pose.</p>
 *
 * <h2>What this deliberately does not do</h2>
 *
 * <p>It only poses the arms. The gun's placement in a mob's hand stays vanilla's
 * ({@code ItemInHandLayer#renderArmWithItem} with the item model's own transform), which is what
 * {@code ItemInHandLayerMixin} documents as the shipped 0.5.5 behaviour: an earlier revision of this port
 * positioned mobs' guns by hand and mobs stopped looking like they were holding them properly. The arm pose
 * carries the gun with it, because the item layer renders at the hand's transform.</p>
 *
 * <p>The aim progress is smoothed rather than switched: a guard with a target raises the gun over about
 * seven ticks instead of snapping between idle and aiming, which is what the player's own animation does
 * with {@code AimingHandler}'s progress value.</p>
 */
@Pseudo
@Mixin(
   targets = {"tallestegg.guardvillagers.client.models.GuardModel"},
   remap = false
)
public abstract class GuardModelGunPoseMixin {
   /** Per guard, how far into the aiming pose it is; weak keys, nothing here keeps an entity alive. */
   private static final Map<LivingEntity, Float> aimProgress = new WeakHashMap<>();

   /** Aiming progress per client tick, i.e. about seven ticks from idle to aiming. */
   private static final float AIM_STEP = 0.15F;

   @Inject(
      method = {"setupAnim(Ltallestegg/guardvillagers/common/entities/Guard;FFFFF)V"},
      at = {@At("TAIL")},
      remap = false
   )
   private void scguns$playerGunPose(
      @Coerce Object guardObject,
      float limbSwing,
      float limbSwingAmount,
      float ageInTicks,
      float netHeadYaw,
      float headPitch,
      CallbackInfo ci
   ) {
      if (!(guardObject instanceof LivingEntity guard)) {
         return;
      }

      ItemStack heldItem = guard.getMainHandItem();
      if (!(heldItem.getItem() instanceof GunItem gunItem)) {
         return;
      }

      Gun gun = gunItem.getModifiedGun(heldItem);
      if (gun == null || gun.getGeneral() == null) {
         return;
      }

      HumanoidModel<?> model = (HumanoidModel<?>)(Object)this;
      // A guard charging a crossbow hides an arm; a gun needs both.
      model.rightArm.visible = true;
      model.leftArm.visible = true;

      // The pose application also interpolates the arms' pivot offsets, which are the player's numbers. A
      // guard's arms must keep their own skeleton, so the pivots are put back and only the rotations stay.
      float rightX = model.rightArm.x;
      float rightY = model.rightArm.y;
      float rightZ = model.rightArm.z;
      float leftX = model.leftArm.x;
      float leftY = model.leftArm.y;
      float leftZ = model.leftArm.z;

      gun.getGeneral()
         .getGripType(heldItem)
         .heldAnimation()
         .applyPlayerModelRotation(
            guard, model.rightArm, model.leftArm, model.head, InteractionHand.MAIN_HAND, scguns$aimProgress(guard)
         );

      model.rightArm.x = rightX;
      model.rightArm.y = rightY;
      model.rightArm.z = rightZ;
      model.leftArm.x = leftX;
      model.leftArm.y = leftY;
      model.leftArm.z = leftZ;
   }

   /** How far this guard is into the aiming pose, eased towards 1 while it has a target. */
   private static float scguns$aimProgress(LivingEntity guard) {
      float wanted = guard instanceof Mob mob && mob.getTarget() != null ? 1.0F : 0.0F;
      Float previous = aimProgress.get(guard);
      float current = previous == null ? wanted : previous;
      if (current != wanted) {
         float delta = Minecraft.getInstance().getTimer().getGameTimeDeltaTicks();
         current += Mth.clamp(wanted - current, -delta * AIM_STEP, delta * AIM_STEP);
         aimProgress.put(guard, current);
      }

      return current;
   }
}
