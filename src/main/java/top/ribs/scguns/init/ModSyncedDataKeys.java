package top.ribs.scguns.init;

import com.mrcrayfish.framework.api.sync.Serializers;
import com.mrcrayfish.framework.api.sync.SyncedClassKey;
import com.mrcrayfish.framework.api.sync.SyncedDataKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class ModSyncedDataKeys {
   public static final SyncedDataKey<Player, Boolean> AIMING = SyncedDataKey.builder(SyncedClassKey.PLAYER, Serializers.BOOLEAN)
      .id(ResourceLocation.fromNamespaceAndPath("scguns", "aiming"))
      .defaultValueSupplier(() -> false)
      .resetOnDeath()
      .build();
   public static final SyncedDataKey<Player, Boolean> SHOOTING = SyncedDataKey.builder(SyncedClassKey.PLAYER, Serializers.BOOLEAN)
      .id(ResourceLocation.fromNamespaceAndPath("scguns", "shooting"))
      .defaultValueSupplier(() -> false)
      .resetOnDeath()
      .build();
   public static final SyncedDataKey<Player, Integer> BURSTCOUNT = SyncedDataKey.builder(SyncedClassKey.PLAYER, Serializers.INTEGER)
      .id(ResourceLocation.fromNamespaceAndPath("scguns", "burstcount"))
      .defaultValueSupplier(() -> 0)
      .resetOnDeath()
      .build();
   public static final SyncedDataKey<Player, Boolean> ONBURSTCOOLDOWN = SyncedDataKey.builder(SyncedClassKey.PLAYER, Serializers.BOOLEAN)
      .id(ResourceLocation.fromNamespaceAndPath("scguns", "onburstcooldown"))
      .defaultValueSupplier(() -> false)
      .resetOnDeath()
      .build();
   public static final SyncedDataKey<Player, Boolean> RELOADING = SyncedDataKey.builder(SyncedClassKey.PLAYER, Serializers.BOOLEAN)
      .id(ResourceLocation.fromNamespaceAndPath("scguns", "reloading"))
      .defaultValueSupplier(() -> false)
      .resetOnDeath()
      .build();
   public static final SyncedDataKey<Player, Boolean> MELEE = SyncedDataKey.builder(SyncedClassKey.PLAYER, Serializers.BOOLEAN)
      .id(ResourceLocation.fromNamespaceAndPath("scguns", "melee"))
      .defaultValueSupplier(() -> false)
      .resetOnDeath()
      .build();
   /**
    * Whether a player is mid bayonet charge (HANDOFF section 82.22).
    *
    * <p>The charge is decided on the server ({@code MeleeAttackHandler.startBanzai}, reached from the melee
    * packet) and the client has to animate it, so it has to be synced. It used to be read from
    * {@code MeleeAttackHandler.isBanzaiActive()}, which is a static field only the server ever writes: that
    * happens to work in single player, where both sides share one JVM, and never works on a server - the
    * client's copy stays false, so the charge animation never plays there.</p>
    */
   public static final SyncedDataKey<Player, Boolean> BANZAI = SyncedDataKey.builder(SyncedClassKey.PLAYER, Serializers.BOOLEAN)
      .id(ResourceLocation.fromNamespaceAndPath("scguns", "banzai"))
      .defaultValueSupplier(() -> false)
      .resetOnDeath()
      .build();

   public ModSyncedDataKeys() {
      super();
   }
}
