package top.ribs.scguns.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Keeps block particles out of the missing-texture look on copycat style blocks.
 *
 * <p>Copycat blocks (Create's and the standalone Copycats+ mod) render their surface through a block entity and do not
 * answer with a real particle icon. Anything that asks the block for its break particle - including vanilla's own
 * destroy path - then draws the magenta and black missing texture. Swapping in a plain neutral block for the particle
 * only keeps the effect looking like debris; the world is never touched.</p>
 */
public final class BlockParticleSafety {

   private BlockParticleSafety() {
   }

   /**
    * A stand-in state used when a block cannot provide a usable particle texture.
    *
    * <p>The first choice is a copycat block itself, in its undecorated form: that is what Create shows for its own
    * copycats, so the debris keeps the same material feel as the wall that was hit. Only when no copycat block is
    * present does this fall back to plain andesite.</p>
    */
   public static BlockState safeState(BlockState state) {
      return isCopycat(state) ? standIn() : state;
   }

   /** The same stand-in, as the integer id that level event 2001 carries. */
   public static int safeId(BlockState state) {
      return Block.getId(safeState(state));
   }

   private static BlockState standIn;

   private static BlockState standIn() {
      if (standIn == null) {
         standIn = resolveStandIn();
      }
      return standIn;
   }

   private static BlockState resolveStandIn() {
      // Preferred: an undecorated copycat panel, which is exactly what Create's own copycats fall back to.
      for (String candidate : new String[]{
         "create:copycat_panel", "copycats:copycat_panel", "copycatsplus:copycat_panel"}) {
         ResourceLocation id = ResourceLocation.tryParse(candidate);
         if (id == null) {
            continue;
         }
         Block block = BuiltInRegistries.BLOCK.get(id);
         if (block != null && block != Blocks.AIR) {
            return block.defaultBlockState();
         }
      }

      return Blocks.ANDESITE.defaultBlockState();
   }

   /** Whether this block is a copycat that renders through a block entity instead of a real model. */
   public static boolean isCopycat(BlockState state) {
      if (state == null) {
         return false;
      }

      // The stand-in is itself a copycat block, so it has to be recognised as "already safe" - otherwise remapping the
      // id would remap it again, and the event would recurse until the stack overflowed.
      if (standIn != null && state.getBlock() == standIn.getBlock()) {
         return false;
      }

      ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
      if (id == null) {
         return false;
      }

      String path = id.getPath();
      String namespace = id.getNamespace();
      return path.contains("copycat") || namespace.equals("copycats") || namespace.equals("copycatsplus");
   }
}
