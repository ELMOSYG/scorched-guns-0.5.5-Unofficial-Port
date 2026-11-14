package top.ribs.scguns.network.message;



import top.ribs.scguns.util.ScEnchants;
import net.minecraft.core.registries.BuiltInRegistries;
import com.mrcrayfish.framework.api.network.MessageContext;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.event.GunEventBus;
import top.ribs.scguns.init.ModEnchantments;
import top.ribs.scguns.item.GunItem;

public class C2SMessageEjectCasing {
   public C2SMessageEjectCasing() {
      super();
   }

   public void encode(C2SMessageEjectCasing message, FriendlyByteBuf buffer) {
   }

   public C2SMessageEjectCasing decode(FriendlyByteBuf buffer) {
      return new C2SMessageEjectCasing();
   }

   public void handle(C2SMessageEjectCasing message, MessageContext context) {
      context.execute(() -> {
         ServerPlayer player = context.getPlayer().map(p -> (ServerPlayer) p).orElse(null);
         if (player != null && !player.isSpectator()) {
            ItemStack heldItem = player.getMainHandItem();
            if (heldItem.getItem() instanceof GunItem) {
               if (!heldItem.getItem().getClass().getPackageName().startsWith("top.ribs.scguns")) {
                  return;
               }

               Gun gun = ((GunItem)heldItem.getItem()).getModifiedGun(heldItem);

               // This packet used to hand out a casing without checking that there was anything to eject, so
               // "eject, then start loading" produced free casings, and spamming eject produced them without limit.
               // Ejecting a round has to remove that round from the gun: no round, no casing.
               // Two different requests arrive through this packet, and they need different rules:
               //
               //  * pressing eject with rounds in the gun: a round physically leaves the chamber, so it must be spent;
               //  * the automatic eject during a reload: the gun is already empty (that is why it is being reloaded), so
               //    there is nothing to spend and the casing is just the spent case of the last shot. Ignoring this one
               //    made an emptied gun stop dropping its last casing.
               //
               // What must never happen is a casing without a matching round. Only guns that ask for it (ejectDuringReload)
               // get the automatic one, and it only fires on an empty chamber, so the loop that single-round reloads used
               // to fall into is still closed.
               boolean reloading = Boolean.TRUE.equals(
                  top.ribs.scguns.init.ModSyncedDataKeys.RELOADING.getValue(player));
               int chambered = Gun.getAmmoCount(heldItem);

               if (reloading) {
                  if (!gun.getProjectile().ejectDuringReload() || chambered > 0) {
                     context.setHandled(true);
                     return;
                  }

                  // The magazine was drained by the unload key, so every round in it was pulled out unfired - there is
                  // no spent case to drop for any of them. The flag is consumed here and also cleared when the reload
                  // finishes, so a normal (fired-empty) reload keeps its casing.
                  CompoundTag reloadTag = top.ribs.scguns.util.NbtHelper.getOrCreateTag(heldItem);
                  if (reloadTag.getBoolean("ScgunsUnloadedMag")) {
                     context.setHandled(true);
                     return;
                  }
               } else {
                  if (chambered <= 0) {
                     context.setHandled(true);
                     return;
                  }

                  // Gun has no setter; the count lives in the "AmmoCount" tag (see BeamWeaponHandler for the same pattern).
                  CompoundTag ejectTag = top.ribs.scguns.util.NbtHelper.getOrCreateTag(heldItem);
                  ejectTag.putInt("AmmoCount", chambered - 1);
                  // The round that just left the gun handed out a casing, so the slot it vacated must not hand out a
                  // second one when it is filled again. Load code consumes this flag.
                  ejectTag.putBoolean("ScgunsEjectedRound", true);
               }

               if (gun.getProjectile().casingType != null && !player.getAbilities().instabuild) {
                  ItemStack casingStack = new ItemStack(Objects.requireNonNull((Item)BuiltInRegistries.ITEM.get(gun.getProjectile().casingType)));
                  double baseChance = 0.4;
                  int enchantmentLevel = ScEnchants.level(heldItem, ModEnchantments.SHELL_CATCHER);
                  double finalChance = baseChance + (double)enchantmentLevel * 0.15;
                  double roll = Math.random();
                  if (roll < finalChance) {
                     if (enchantmentLevel > 0) {
                        boolean addedDirectly = GunEventBus.addCasingDirectly(player, casingStack);
                        if (!addedDirectly) {
                           GunEventBus.spawnCasingInWorld(player.level(), player, casingStack);
                        }
                     } else {
                        boolean addedToPouch = GunEventBus.addCasingToPouch(player, casingStack);
                        if (!addedToPouch) {
                           GunEventBus.spawnCasingInWorld(player.level(), player, casingStack);
                        }
                     }
                  }
               }
            }
         }

         context.setHandled(true);
      });
   }
}
