package top.ribs.scguns.compat;

import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import top.ribs.scguns.ScorchedGuns;

/**
 * Lets a dispenser throw this mod's grenades and bombs the way it shoots vanilla arrows.
 *
 * <p>The throwing items all build their entity from a living thrower's aim, so the behaviour stands a fake player in
 * the dispenser and lets the item's own {@code use} code run. That keeps the fuse, the sound, the item consumption and
 * the explosion ownership exactly as they are when a player throws, instead of duplicating that logic here.</p>
 */
public final class DispenserThrowBehavior implements DispenseItemBehavior {

   private final Item item;

   public DispenserThrowBehavior(Item item) {
      this.item = item;
   }

   @Override
   public ItemStack dispense(BlockSource source, ItemStack stack) {
      Level level = source.level();
      if (!(level instanceof ServerLevel serverLevel)) {
         return stack;
      }

      Direction facing = source.state().getValue(DispenserBlock.FACING);
      var center = source.center();

      FakePlayer thrower = FakePlayerFactory.getMinecraft(serverLevel);
      double x = center.x - (double) facing.getStepX() * 0.3;
      double y = center.y - 0.2 + (double) facing.getStepY() * 0.3;
      double z = center.z - (double) facing.getStepZ() * 0.3;
      thrower.setPos(x, y, z);

      float yaw = (float) (Math.atan2((double) facing.getStepZ(), (double) facing.getStepX()) * (180.0 / Math.PI) - 90.0);
      float pitch = -((float) facing.getStepY() * 90.0F);
      thrower.setYRot(yaw);
      thrower.setXRot(pitch);
      thrower.setYHeadRot(yaw);

      thrower.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());

      int before = stack.getCount();
      try {
         stack.use(level, thrower, InteractionHand.MAIN_HAND);
         top.ribs.scguns.ScorchedGuns.LOGGER.info(
            "scguns: dispenser throw {} -> before={}, after={}", this.item, before, stack.getCount());
      } catch (Throwable failure) {
         ScorchedGuns.LOGGER.warn("Could not launch {} from a dispenser", this.item, failure);
         return stack;
      }

      return stack;
   }
}
