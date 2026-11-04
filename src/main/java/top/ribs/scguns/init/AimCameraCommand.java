package top.ribs.scguns.init;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import com.mojang.brigadier.CommandDispatcher;

/**
 * Live iron-sight camera tuning, so an addon gun's aim camera can be dialled in without restarting the game.
 *
 * <p>The renderer reads the camera from {@code scguns:weapon -> ironSight -> camera} on the held stack, ahead of any
 * bone or zoom fallback, so writing that path takes effect on the next frame.</p>
 *
 * <pre>
 * /scguns aimcamera get
 * /scguns aimcamera set &lt;x&gt; &lt;y&gt; &lt;z&gt;
 * /scguns aimcamera add &lt;dx&gt; &lt;dy&gt; &lt;dz&gt;
 * /scguns aimcamera clear
 * </pre>
 */
public final class AimCameraCommand {

   private static final String WEAPON_TAG = "scguns:weapon";
   private static final String IRON_SIGHT_TAG = "ironSight";
   private static final String CAMERA_TAG = "camera";

   private AimCameraCommand() {
   }

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(Commands.literal("scguns")
         .then(Commands.literal("aimcamera")
            .then(Commands.literal("get").executes(AimCameraCommand::get))
            .then(Commands.literal("clear").executes(AimCameraCommand::clear))
            .then(Commands.literal("set")
               .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                  .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                     .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                        .executes(AimCameraCommand::set)))))
            .then(Commands.literal("add")
               .then(Commands.argument("dx", DoubleArgumentType.doubleArg())
                  .then(Commands.argument("dy", DoubleArgumentType.doubleArg())
                     .then(Commands.argument("dz", DoubleArgumentType.doubleArg())
                        .executes(AimCameraCommand::add)))))));
   }

   private static int get(CommandContext<CommandSourceStack> context) {
      ItemStack held = heldGun(context);
      if (held == null) {
         return 0;
      }

      ListTag camera = readCamera(held);
      if (camera == null) {
         context.getSource().sendSuccess(() -> Component.literal(
            "No ironSight.camera on this gun - the renderer is using a camera bone or the zoom offsets."), false);
         return 1;
      }

      context.getSource().sendSuccess(() -> Component.literal(String.format(
         "ironSight.camera = [%.3f, %.3f, %.3f]   (renderer adds y+0.6059, z-0.16, and +0.72 z for legacy sights)",
         camera.getDouble(0), camera.getDouble(1), camera.getDouble(2))), false);
      return 1;
   }

   private static int clear(CommandContext<CommandSourceStack> context) {
      ItemStack held = heldGun(context);
      if (held == null) {
         return 0;
      }

      CompoundTag root = copyData(held);
      CompoundTag weapon = root.getCompound(WEAPON_TAG);
      CompoundTag ironSight = weapon.getCompound(IRON_SIGHT_TAG);
      ironSight.remove(CAMERA_TAG);
      weapon.put(IRON_SIGHT_TAG, ironSight);
      root.put(WEAPON_TAG, weapon);
      held.set(DataComponents.CUSTOM_DATA, CustomData.of(root));

      context.getSource().sendSuccess(() -> Component.literal("Cleared ironSight.camera."), false);
      return 1;
   }

   private static int set(CommandContext<CommandSourceStack> context) {
      return write(context,
         DoubleArgumentType.getDouble(context, "x"),
         DoubleArgumentType.getDouble(context, "y"),
         DoubleArgumentType.getDouble(context, "z"), false);
   }

   private static int add(CommandContext<CommandSourceStack> context) {
      return write(context,
         DoubleArgumentType.getDouble(context, "dx"),
         DoubleArgumentType.getDouble(context, "dy"),
         DoubleArgumentType.getDouble(context, "dz"), true);
   }

   private static int write(CommandContext<CommandSourceStack> context, double x, double y, double z, boolean relative) {
      ItemStack held = heldGun(context);
      if (held == null) {
         return 0;
      }

      ListTag existing = readCamera(held);
      double nx = relative && existing != null ? existing.getDouble(0) + x : x;
      double ny = relative && existing != null ? existing.getDouble(1) + y : y;
      double nz = relative && existing != null ? existing.getDouble(2) + z : z;

      CompoundTag root = copyData(held);
      CompoundTag weapon = root.getCompound(WEAPON_TAG);
      CompoundTag ironSight = weapon.getCompound(IRON_SIGHT_TAG);

      ListTag camera = new ListTag();
      camera.add(DoubleTag.valueOf(nx));
      camera.add(DoubleTag.valueOf(ny));
      camera.add(DoubleTag.valueOf(nz));
      ironSight.put(CAMERA_TAG, camera);

      weapon.put(IRON_SIGHT_TAG, ironSight);
      root.put(WEAPON_TAG, weapon);
      held.set(DataComponents.CUSTOM_DATA, CustomData.of(root));

      context.getSource().sendSuccess(() -> Component.literal(String.format(
         "ironSight.camera = [%.3f, %.3f, %.3f]", nx, ny, nz)), true);
      return 1;
   }

   private static ItemStack heldGun(CommandContext<CommandSourceStack> context) {
      ItemStack held = context.getSource().getPlayer().getMainHandItem();
      if (!(held.getItem() instanceof top.ribs.scguns.item.GunItem)) {
         context.getSource().sendFailure(Component.literal("Hold a gun in your main hand first."));
         return null;
      }

      return held;
   }

   private static CompoundTag copyData(ItemStack stack) {
      return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
   }

   private static ListTag readCamera(ItemStack stack) {
      CompoundTag root = copyData(stack);
      if (!root.contains(WEAPON_TAG, 10)) {
         return null;
      }

      CompoundTag weapon = root.getCompound(WEAPON_TAG);
      if (!weapon.contains(IRON_SIGHT_TAG, 10)) {
         return null;
      }

      CompoundTag ironSight = weapon.getCompound(IRON_SIGHT_TAG);
      return ironSight.contains(CAMERA_TAG, 9) ? ironSight.getList(CAMERA_TAG, 6) : null;
   }
}
