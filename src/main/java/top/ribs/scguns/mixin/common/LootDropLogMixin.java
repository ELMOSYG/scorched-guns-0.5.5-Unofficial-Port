package top.ribs.scguns.mixin.common;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * TEMPORARY diagnostic: logs every item an entity drops, so a "one death drops three guns" report can be traced to
 * the code path that produced it. Remove once the duplication is found.
 */
@Mixin(LivingEntity.class)
public abstract class LootDropLogMixin {
   @Inject(method = "spawnAtLocation(Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;",
           at = @At("HEAD"))
   private void scguns$logDrop(ItemStack stack, float offset, CallbackInfoReturnable<?> cir) {
      LivingEntity self = (LivingEntity)(Object)this;

      String type = String.valueOf(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(self.getType()));

      if (!type.contains("guard")) {
         return;
      }

      org.slf4j.LoggerFactory.getLogger("scguns-loot").info(
         "[scguns-loot] {} dropped {} x{}", type,
         net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()), stack.getCount());
   }

   @Inject(method = "die", at = @At("HEAD"))
   private void scguns$logDie(net.minecraft.world.damagesource.DamageSource source, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
      LivingEntity self = (LivingEntity)(Object)this;
      String type = String.valueOf(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(self.getType()));

      if (!type.contains("guard")) {
         return;
      }

      org.slf4j.LoggerFactory.getLogger("scguns-loot").info(
         "[scguns-loot] {} die() called, source={} direct={}",
         type,
         source.typeHolder().unwrapKey().map(k -> k.location().toString()).orElse("?"),
         source.getDirectEntity() == null ? "null"
            : String.valueOf(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(source.getDirectEntity().getType())));
   }
}
