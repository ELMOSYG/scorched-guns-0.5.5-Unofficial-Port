package top.ribs.scguns.network.message;



import top.ribs.scguns.util.ScEnchants;
import net.minecraft.core.registries.BuiltInRegistries;
import com.mrcrayfish.framework.api.network.MessageContext;
import java.util.Objects;
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
               int chambered = Gun.getAmmoCount(heldItem);
               if (chambered <= 0) {
                  context.setHandled(true);
                  return;
               }

               // Gun has no setter; the count lives in the "AmmoCount" tag (see BeamWeaponHandler for the same pattern).
               top.ribs.scguns.util.NbtHelper.getOrCreateTag(heldItem).putInt("AmmoCount", chambered - 1);

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
