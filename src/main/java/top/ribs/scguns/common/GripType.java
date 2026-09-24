package top.ribs.scguns.common;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import top.ribs.scguns.client.render.IHeldAnimation;
import top.ribs.scguns.client.render.pose.BazookaPose;
import top.ribs.scguns.client.render.pose.DualWieldPose;
import top.ribs.scguns.client.render.pose.MiniGun2Pose;
import top.ribs.scguns.client.render.pose.MiniGun3Pose;
import top.ribs.scguns.client.render.pose.MiniGun4Pose;
import top.ribs.scguns.client.render.pose.MiniGun5Pose;
import top.ribs.scguns.client.render.pose.MiniGunPose;
import top.ribs.scguns.client.render.pose.OneHanded2Pose;
import top.ribs.scguns.client.render.pose.OneHandedPose;
import top.ribs.scguns.client.render.pose.TwoHandedPose;
import top.ribs.scguns.client.render.pose.TwoHandedShotgunPose;
import top.ribs.scguns.client.render.pose.TwoHandedSmgPose;

public record GripType(ResourceLocation id, IHeldAnimation heldAnimation) {
   public static final GripType ONE_HANDED = new GripType(ResourceLocation.fromNamespaceAndPath("scguns", "one_handed"), new OneHandedPose());
   public static final GripType ONE_HANDED_2 = new GripType(ResourceLocation.fromNamespaceAndPath("scguns", "one_handed_2"), new OneHanded2Pose());
   public static final GripType TWO_HANDED = new GripType(ResourceLocation.fromNamespaceAndPath("scguns", "two_handed"), new TwoHandedPose());
   public static final GripType TWO_HANDED_SHOTGUN = new GripType(ResourceLocation.fromNamespaceAndPath("scguns", "two_handed_shotgun"), new TwoHandedShotgunPose());
   public static final GripType TWO_HANDED_SMG = new GripType(ResourceLocation.fromNamespaceAndPath("scguns", "two_handed_smg"), new TwoHandedSmgPose());
   public static final GripType DUAL_WIELD = new GripType(ResourceLocation.fromNamespaceAndPath("scguns", "dual_wield"), new DualWieldPose());
   public static final GripType MINI_GUN = new GripType(ResourceLocation.fromNamespaceAndPath("scguns", "mini_gun"), new MiniGunPose());
   public static final GripType MINI_GUN_2 = new GripType(ResourceLocation.fromNamespaceAndPath("scguns", "mini_gun_2"), new MiniGun2Pose());
   public static final GripType MINI_GUN_3 = new GripType(ResourceLocation.fromNamespaceAndPath("scguns", "mini_gun_3"), new MiniGun3Pose());
   public static final GripType MINI_GUN_4 = new GripType(ResourceLocation.fromNamespaceAndPath("scguns", "mini_gun_4"), new MiniGun4Pose());
   public static final GripType MINI_GUN_5 = new GripType(ResourceLocation.fromNamespaceAndPath("scguns", "mini_gun_5"), new MiniGun5Pose());
   public static final GripType BAZOOKA = new GripType(ResourceLocation.fromNamespaceAndPath("scguns", "bazooka"), new BazookaPose());
   private static final Map<ResourceLocation, GripType> gripTypeMap = new HashMap<>();

   public GripType(ResourceLocation id, IHeldAnimation heldAnimation) {
      this.id = id;
      this.heldAnimation = heldAnimation;
   }

   public static boolean applyBackTransforms(Player player, PoseStack poseStack) {
      if (player.getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
         return false;
      } else {
         poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
         poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
         if (player.isCrouching()) {
            poseStack.translate(0.0, -0.4375, -0.25);
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
         } else {
            poseStack.translate(0.0, -0.3125, -0.125);
         }

         if (!player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) {
            poseStack.translate(0.0, 0.0, -0.0625);
         }

         poseStack.mulPose(Axis.ZP.rotationDegrees(-45.0F));
         poseStack.scale(0.5F, 0.5F, 0.5F);
         return true;
      }
   }

   public static void registerType(GripType type) {
      gripTypeMap.putIfAbsent(type.id(), type);
   }

   public static GripType getType(ResourceLocation id) {
      return gripTypeMap.getOrDefault(id, ONE_HANDED);
   }

   static {
      registerType(ONE_HANDED);
      registerType(ONE_HANDED_2);
      registerType(TWO_HANDED);
      registerType(TWO_HANDED_SMG);
      registerType(TWO_HANDED_SHOTGUN);
      registerType(DUAL_WIELD);
      registerType(MINI_GUN);
      registerType(MINI_GUN_2);
      registerType(MINI_GUN_3);
      registerType(MINI_GUN_4);
      registerType(MINI_GUN_5);
      registerType(BAZOOKA);
   }
}
