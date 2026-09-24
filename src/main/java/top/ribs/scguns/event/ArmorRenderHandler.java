package top.ribs.scguns.event;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot.Type;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLivingEvent.Post;
import net.neoforged.neoforge.client.event.RenderLivingEvent.Pre;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import top.ribs.scguns.item.animated.AdrienArmorItem;
import top.ribs.scguns.item.animated.AnthraliteArmorItem;
import top.ribs.scguns.item.animated.AnthraliteGasMaskArmorItem;
import top.ribs.scguns.item.animated.DiamondSteelArmorItem;
import top.ribs.scguns.item.animated.ExoSuitItem;
import top.ribs.scguns.item.animated.NetheriteGasMaskArmorItem;
import top.ribs.scguns.item.animated.RedcoatArmorItem;
import top.ribs.scguns.item.animated.ScrapArmorItem;
import top.ribs.scguns.item.animated.TreatedBrassArmorItem;

@EventBusSubscriber(
   modid = "scguns",
   bus = Bus.GAME,
   value = {Dist.CLIENT}
)
public class ArmorRenderHandler {
   public ArmorRenderHandler() {
      super();
   }

   @SubscribeEvent
   public static void onRenderLivingPre(Pre<?, ?> event) {
      if (event.getRenderer().getModel() instanceof PlayerModel<?> playerModel) {
         hideSecondLayerForCustomArmor(event.getEntity(), playerModel);
      }
   }

   @SubscribeEvent
   public static void onRenderLivingPost(Post<?, ?> event) {
      if (event.getRenderer().getModel() instanceof PlayerModel<?> playerModel) {
         restoreSecondLayer(playerModel);
      }
   }

   private static void hideSecondLayerForCustomArmor(LivingEntity entity, PlayerModel<?> playerModel) {
      for (EquipmentSlot slot : EquipmentSlot.values()) {
         if (slot.getType() == Type.HUMANOID_ARMOR) {
            ItemStack armorStack = entity.getItemBySlot(slot);
            if (armorStack.getItem() instanceof AdrienArmorItem) {
               hideSecondLayerForSlot(playerModel, slot);
            }

            if (armorStack.getItem() instanceof TreatedBrassArmorItem) {
               hideSecondLayerForSlot(playerModel, slot);
            }

            if (armorStack.getItem() instanceof AnthraliteArmorItem) {
               hideSecondLayerForSlot(playerModel, slot);
            }

            if (armorStack.getItem() instanceof AnthraliteGasMaskArmorItem) {
               hideSecondLayerForSlot(playerModel, slot);
            }

            if (armorStack.getItem() instanceof NetheriteGasMaskArmorItem) {
               hideSecondLayerForSlot(playerModel, slot);
            }

            if (armorStack.getItem() instanceof ExoSuitItem) {
               hideSecondLayerForSlot(playerModel, slot);
            }

            if (armorStack.getItem() instanceof DiamondSteelArmorItem) {
               hideSecondLayerForSlot(playerModel, slot);
            }

            if (armorStack.getItem() instanceof RedcoatArmorItem) {
               hideSecondLayerForSlot(playerModel, slot);
            }

            if (armorStack.getItem() instanceof ScrapArmorItem) {
               hideSecondLayerForSlot(playerModel, slot);
            }
         }
      }
   }

   private static void hideSecondLayerForSlot(PlayerModel<?> playerModel, EquipmentSlot slot) {
      switch (slot) {
         case HEAD:
            playerModel.hat.visible = false;
            break;
         case CHEST:
            playerModel.jacket.visible = false;
            playerModel.leftSleeve.visible = false;
            playerModel.rightSleeve.visible = false;
            break;
         case LEGS:
            playerModel.leftPants.visible = false;
            playerModel.rightPants.visible = false;
      }
   }

   private static void restoreSecondLayer(PlayerModel<?> playerModel) {
      playerModel.hat.visible = true;
      playerModel.jacket.visible = true;
      playerModel.leftSleeve.visible = true;
      playerModel.rightSleeve.visible = true;
      playerModel.leftPants.visible = true;
      playerModel.rightPants.visible = true;
   }
}
