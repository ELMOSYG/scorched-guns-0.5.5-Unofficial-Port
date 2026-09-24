package top.ribs.scguns.entity.animations;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.animation.AnimationChannel.Interpolations;
import net.minecraft.client.animation.AnimationChannel.Targets;
import net.minecraft.client.animation.AnimationDefinition.Builder;

public class ModAnimationDefinitions {
   public static final AnimationDefinition COG_MINION_IDLE = Builder.withLength(1.5F)
      .looping()
      .addAnimation(
         "head",
         new AnimationChannel(
            Targets.POSITION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.625F, KeyframeAnimations.posVec(0.08F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.5F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "head",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.75F, KeyframeAnimations.degreeVec(0.0F, 0.5F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "Cog",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.8343334F, KeyframeAnimations.degreeVec(359.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "tread",
         new AnimationChannel(
            Targets.POSITION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.75F, KeyframeAnimations.posVec(-0.2F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.5F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .build();
   public static final AnimationDefinition COG_MINION_WALK = Builder.withLength(6.2F)
      .looping()
      .addAnimation(
         "Cog",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.8343334F, KeyframeAnimations.degreeVec(359.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(2.4F, KeyframeAnimations.degreeVec(359.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(3.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(3.9F, KeyframeAnimations.degreeVec(359.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(4.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(5.4F, KeyframeAnimations.degreeVec(359.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(6.2F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "tread",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -2.5F), Interpolations.LINEAR),
               new Keyframe(0.75F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -1.75F), Interpolations.LINEAR),
               new Keyframe(1.4583433F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -2.5F), Interpolations.LINEAR),
               new Keyframe(2.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -1.75F), Interpolations.LINEAR),
               new Keyframe(3.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -2.5F), Interpolations.LINEAR),
               new Keyframe(3.8F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -1.75F), Interpolations.LINEAR),
               new Keyframe(4.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -2.5F), Interpolations.LINEAR),
               new Keyframe(5.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -1.75F), Interpolations.LINEAR),
               new Keyframe(6.2F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -2.5F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "head",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(3.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, -359.9F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .build();
   public static final AnimationDefinition COG_MINION_ATTACK = Builder.withLength(1.2F)
      .looping()
      .addAnimation(
         "head",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.4F, KeyframeAnimations.degreeVec(5.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.7F, KeyframeAnimations.degreeVec(5.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "Cog",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.4F, KeyframeAnimations.degreeVec(359.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "hand",
         new AnimationChannel(
            Targets.POSITION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.2F, KeyframeAnimations.posVec(4.0F, 0.17F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "tread",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -5.0F), Interpolations.LINEAR),
               new Keyframe(0.8F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -5.0F), Interpolations.LINEAR),
               new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "CogMinion",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.4F, KeyframeAnimations.degreeVec(2.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.8F, KeyframeAnimations.degreeVec(2.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "bone",
         new AnimationChannel(
            Targets.POSITION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.2F, KeyframeAnimations.posVec(0.0F, 0.0F, -4.0F), Interpolations.LINEAR),
               new Keyframe(1.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .build();
   public static final AnimationDefinition THE_MERCHANT_IDLE = Builder.withLength(5.0F)
      .looping()
      .addAnimation(
         "LeftArm",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.CATMULLROM),
               new Keyframe(2.5F, KeyframeAnimations.degreeVec(2.5F, 0.0F, 0.0F), Interpolations.CATMULLROM),
               new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.CATMULLROM)
            }
         )
      )
      .addAnimation(
         "RightArm",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.CATMULLROM),
               new Keyframe(2.5F, KeyframeAnimations.degreeVec(2.5F, 0.0F, 0.0F), Interpolations.CATMULLROM),
               new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.CATMULLROM)
            }
         )
      )
      .addAnimation(
         "LeftLeg",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.CATMULLROM),
               new Keyframe(2.5F, KeyframeAnimations.degreeVec(-5.0F, 0.0F, 0.0F), Interpolations.CATMULLROM),
               new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.CATMULLROM)
            }
         )
      )
      .addAnimation(
         "RightLeg",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.CATMULLROM),
               new Keyframe(2.5F, KeyframeAnimations.degreeVec(-5.0F, 0.0F, 0.0F), Interpolations.CATMULLROM),
               new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.CATMULLROM)
            }
         )
      )
      .addAnimation(
         "Torso",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.CATMULLROM),
               new Keyframe(2.5F, KeyframeAnimations.degreeVec(2.5F, 0.0F, 0.0F), Interpolations.CATMULLROM),
               new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.CATMULLROM)
            }
         )
      )
      .addAnimation(
         "Bullets",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(2.5F, KeyframeAnimations.degreeVec(-1.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(4.9583F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "Lantern",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.CATMULLROM),
               new Keyframe(2.5F, KeyframeAnimations.degreeVec(-3.0F, 0.0F, 0.0F), Interpolations.CATMULLROM),
               new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.CATMULLROM)
            }
         )
      )
      .addAnimation(
         "Post",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.CATMULLROM),
               new Keyframe(2.5F, KeyframeAnimations.degreeVec(2.0F, 0.0F, 0.0F), Interpolations.CATMULLROM),
               new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.CATMULLROM)
            }
         )
      )
      .build();
   public static final AnimationDefinition THE_MERCHANT_WALK = Builder.withLength(1.0833F)
      .looping()
      .addAnimation(
         "Full",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(1.0F, 0.0F, 0.0F), Interpolations.CATMULLROM),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(-1.0F, 0.0F, 0.0F), Interpolations.CATMULLROM),
               new Keyframe(1.0833F, KeyframeAnimations.degreeVec(1.0F, 0.0F, 0.0F), Interpolations.CATMULLROM)
            }
         )
      )
      .addAnimation(
         "LeftArm",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(-7.5F, 0.0F, 0.0F), Interpolations.CATMULLROM),
               new Keyframe(0.5417F, KeyframeAnimations.degreeVec(12.5F, 0.0F, 0.0F), Interpolations.CATMULLROM),
               new Keyframe(1.0833F, KeyframeAnimations.degreeVec(-7.5F, 0.0F, 0.0F), Interpolations.CATMULLROM)
            }
         )
      )
      .addAnimation(
         "RightArm",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(12.5F, 0.0F, 0.0F), Interpolations.CATMULLROM),
               new Keyframe(0.5417F, KeyframeAnimations.degreeVec(-15.0F, 0.0F, 0.0F), Interpolations.CATMULLROM),
               new Keyframe(1.0833F, KeyframeAnimations.degreeVec(12.5F, 0.0F, 0.0F), Interpolations.CATMULLROM)
            }
         )
      )
      .addAnimation(
         "LeftLeg",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(22.5F, 0.0F, 0.0F), Interpolations.CATMULLROM),
               new Keyframe(0.5417F, KeyframeAnimations.degreeVec(-17.5F, 0.0F, 0.0F), Interpolations.CATMULLROM),
               new Keyframe(1.0833F, KeyframeAnimations.degreeVec(22.5F, 0.0F, 0.0F), Interpolations.CATMULLROM)
            }
         )
      )
      .addAnimation(
         "RightLeg",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(-22.5F, 0.0F, 0.0F), Interpolations.CATMULLROM),
               new Keyframe(0.5417F, KeyframeAnimations.degreeVec(22.5F, 0.0F, 0.0F), Interpolations.CATMULLROM),
               new Keyframe(1.0833F, KeyframeAnimations.degreeVec(-22.5F, 0.0F, 0.0F), Interpolations.CATMULLROM)
            }
         )
      )
      .addAnimation(
         "Gun2",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.CATMULLROM),
               new Keyframe(0.5417F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 0.0F), Interpolations.CATMULLROM),
               new Keyframe(1.0833F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.CATMULLROM)
            }
         )
      )
      .addAnimation(
         "Lantern",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.CATMULLROM),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(-12.5F, 0.0F, 0.0F), Interpolations.CATMULLROM),
               new Keyframe(1.0833F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.CATMULLROM)
            }
         )
      )
      .addAnimation(
         "Post",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.CATMULLROM),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(5.0F, 0.0F, 0.0F), Interpolations.CATMULLROM),
               new Keyframe(1.0833F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.CATMULLROM)
            }
         )
      )
      .build();
   public static final AnimationDefinition SIGNAL_BEACON_IDLE = Builder.withLength(1.5F)
      .looping()
      .addAnimation(
         "head",
         new AnimationChannel(
            Targets.SCALE,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.scaleVec(1.0, 1.0, 1.0), Interpolations.CATMULLROM),
               new Keyframe(0.2917F, KeyframeAnimations.scaleVec(1.1F, 1.1F, 1.1F), Interpolations.CATMULLROM),
               new Keyframe(0.5417F, KeyframeAnimations.scaleVec(1.0, 1.0, 1.0), Interpolations.CATMULLROM),
               new Keyframe(0.8333F, KeyframeAnimations.scaleVec(1.1F, 1.1F, 1.1F), Interpolations.CATMULLROM),
               new Keyframe(1.1667F, KeyframeAnimations.scaleVec(1.0, 1.0, 1.0), Interpolations.CATMULLROM)
            }
         )
      )
      .addAnimation(
         "body",
         new AnimationChannel(
            Targets.SCALE,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.scaleVec(1.0, 1.0, 1.0), Interpolations.LINEAR),
               new Keyframe(0.5833F, KeyframeAnimations.scaleVec(1.05F, 1.05F, 1.05F), Interpolations.LINEAR),
               new Keyframe(1.1667F, KeyframeAnimations.scaleVec(1.0, 1.0, 1.0), Interpolations.LINEAR)
            }
         )
      )
      .build();

   public ModAnimationDefinitions() {
      super();
   }
}
