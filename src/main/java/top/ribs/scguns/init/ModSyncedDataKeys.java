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

   public ModSyncedDataKeys() {
      super();
   }
}
