package top.ribs.scguns.mixin.common;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.ribs.scguns.config.GunFilter;

/**
 * Removes disabled structures from a structure set before the game picks one.
 *
 * <p>Worldgen entries cannot be unregistered, so the selection list is filtered instead: a structure whose key is
 * switched off in the config simply never gets chosen, while everything else keeps its weight and spacing. Filtering
 * the list (rather than the placement) means chunk generation is not affected at all - the structure is never
 * attempted in the first place.</p>
 */
@Mixin(StructureSet.class)
public abstract class StructureSetFilterMixin {

   @Inject(method = "structures", at = @At("RETURN"), cancellable = true)
   private void scguns$filterDisabledStructures(CallbackInfoReturnable<List<StructureSet.StructureSelectionEntry>> cir) {
      List<StructureSet.StructureSelectionEntry> original = cir.getReturnValue();
      if (original == null || original.isEmpty()) {
         return;
      }

      List<StructureSet.StructureSelectionEntry> kept = null;
      for (int i = 0; i < original.size(); i++) {
         StructureSet.StructureSelectionEntry entry = original.get(i);
         Holder<Structure> holder = entry.structure();
         boolean allowed = holder.unwrapKey()
            .map(key -> GunFilter.isStructureEnabled(key.location()))
            .orElse(true);

         if (!allowed && kept == null) {
            kept = new ArrayList<>(original.subList(0, i));
         } else if (allowed && kept != null) {
            kept.add(entry);
         }
      }

      if (kept != null) {
         cir.setReturnValue(kept);
      }
   }
}
