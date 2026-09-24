package top.ribs.scguns.common;


import top.ribs.scguns.util.NbtHelper;
import com.mrcrayfish.framework.api.network.LevelLocation;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationController;
import top.ribs.scguns.Config;
import top.ribs.scguns.attributes.SCAttributes;
import top.ribs.scguns.client.handler.ReloadHandler;
import top.ribs.scguns.common.exosuit.ExoSuitAmmoHelper;
import top.ribs.scguns.common.network.ServerPlayHandler;
import top.ribs.scguns.event.GunEventBus;
import top.ribs.scguns.init.ModSyncedDataKeys;
import top.ribs.scguns.item.AmmoBoxItem;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.item.ammo_boxes.CreativeAmmoBoxItem;
import top.ribs.scguns.item.animated.AnimatedGunItem;
import top.ribs.scguns.network.PacketHandler;
import top.ribs.scguns.network.message.S2CMessageGunSound;
import top.ribs.scguns.network.message.S2CMessageStopReload;
import top.ribs.scguns.util.GunEnchantmentHelper;
import top.ribs.scguns.util.GunModifierHelper;
import top.theillusivec4.curios.api.CuriosApi;

@EventBusSubscriber(
   modid = "scguns",
   bus = Bus.GAME
)
public class ReloadTracker {
   private static final Map<Player, ReloadTracker> RELOAD_TRACKER_MAP = new WeakHashMap<>();

   /**
    * How long an item may keep a stop/reload NBT state while no reload is running before it is
    * treated as stale (HANDOFF section 63). The stop animation lasts about a second, so five
    * seconds is far beyond anything legitimate.
    */
   private static final int STALE_RELOAD_STATE_TICKS = 100;
   private static final Map<Player, Integer> STALE_RELOAD_STATE_COUNTER = new WeakHashMap<>();
   private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("scguns-reload");

   private final int startTick;
   private final int slot;
   private final ItemStack stack;
   private final Gun gun;
   private long pendingManualStopSince;

   public ReloadTracker(Player player) {
      super();
      this.startTick = player.tickCount;
      this.slot = player.getInventory().selected;
      this.stack = player.getInventory().getSelected();
      this.gun = ((GunItem)this.stack.getItem()).getModifiedGun(this.stack);
   }

   private void handleReloadByproduct(Player player) {
      if (this.gun.getReloads().getReloadType() == ReloadType.SINGLE_ITEM) {
         if (this.gun.getReloads().shouldGiveByproduct(player.level().getRandom(), this.stack)) {
            Item byproduct = this.gun.getReloads().getReloadByproduct();
            if (byproduct != null) {
               ItemStack byproductStack = new ItemStack(byproduct);
               if (GunEventBus.addCasingDirectly(player, byproductStack)) {
                  return;
               }

               boolean added = player.getInventory().add(byproductStack);
               if (!added) {
                  Level level = player.level();
                  double x = player.getX();
                  double y = player.getY();
                  double z = player.getZ();
                  ItemEntity itemEntity = new ItemEntity(level, x, y, z, byproductStack);
                  itemEntity.setDeltaMovement(level.random.nextDouble() * 0.2 - 0.1, 0.2, level.random.nextDouble() * 0.2 - 0.1);
                  level.addFreshEntity(itemEntity);
               }
            }
         }
      }
   }

   public boolean isWeaponFull(Player player) {
      ItemStack currentStack = player.getMainHandItem();
      CompoundTag tag = NbtHelper.getOrCreateTag(currentStack);
      int currentAmmo = tag.getInt("AmmoCount");
      int maxAmmo = GunModifierHelper.getModifiedAmmoCapacity(currentStack, this.gun);
      return currentAmmo >= maxAmmo;
   }

   private boolean isWeaponEmpty() {
      CompoundTag tag = NbtHelper.getOrCreateTag(this.stack);
      return tag.getInt("AmmoCount") == 0;
   }

   public boolean hasNoAmmo(Player player) {
      boolean result;
      if (this.gun.getReloads().getReloadType() == ReloadType.SINGLE_ITEM) {
         result = Gun.findAmmo(player, this.gun.getReloads().getReloadItem()).stack().isEmpty();
      } else {
         result = Gun.findAmmo(player, this.gun.getProjectile().getItem()).stack().isEmpty();
      }

      return result;
   }

   private boolean canReload(Player player) {
      if (this.gun.getReloads().getReloadType() == ReloadType.MANUAL) {
         return true;
      } else {
         int deltaTicks = player.tickCount - this.startTick;
         double reloadSpeed = Objects.requireNonNull(player.getAttribute(SCAttributes.RELOAD_SPEED)).getValue();
         int interval = this.gun.getReloads().getReloadType() == ReloadType.SINGLE_ITEM
            ? (int)Math.ceil((double)GunEnchantmentHelper.getMagReloadSpeed(this.stack) / reloadSpeed)
            : (int)Math.ceil((double)GunEnchantmentHelper.getReloadInterval(this.stack) / reloadSpeed);
         return deltaTicks >= interval;
      }
   }

   public static int ammoInInventory(ItemStack[] ammoStack) {
      int result = 0;

      for (ItemStack x : ammoStack) {
         result += x.getCount();
      }

      return result;
   }

   private void shrinkFromAmmoPool(ItemStack[] ammoStack, Player player, int shrinkAmount) {
      int[] shrinkAmt = new int[]{shrinkAmount};
      int exoSuitShrinkAmount = Math.min(shrinkAmt[0], ExoSuitAmmoHelper.getAmmoCountInExoSuit(player, this.gun.getProjectile().getItem()));
      if (exoSuitShrinkAmount > 0) {
         ExoSuitAmmoHelper.shrinkAmmoInExoSuit(player, this.gun.getProjectile().getItem(), exoSuitShrinkAmount);
         shrinkAmt[0] -= exoSuitShrinkAmount;
         if (shrinkAmt[0] == 0) {
            return;
         }
      }

      CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
         IItemHandlerModifiable curios = handler.getEquippedCurios();

         for (int i = 0; i < curios.getSlots(); i++) {
            ItemStack stack = curios.getStackInSlot(i);
            if (stack.getItem() instanceof AmmoBoxItem) {
               List<ItemStack> contentsx = AmmoBoxItem.getContents(stack).collect(Collectors.toList());

               for (ItemStack pouchAmmoStack : contentsx) {
                  if (!pouchAmmoStack.isEmpty() && pouchAmmoStack.getItem() == this.gun.getProjectile().getItem()) {
                     int maxx = Math.min(shrinkAmt[0], pouchAmmoStack.getCount());
                     pouchAmmoStack.shrink(maxx);
                     shrinkAmt[0] -= maxx;
                     if (shrinkAmt[0] == 0) {
                        this.updateAmmoPouchContents(stack, contentsx);
                        return;
                     }
                  }
               }

               this.updateAmmoPouchContents(stack, contentsx);
            }
         }
      });

      for (ItemStack itemStack : player.getInventory().items) {
         if (itemStack.getItem() instanceof AmmoBoxItem) {
            List<ItemStack> contents = AmmoBoxItem.getContents(itemStack).collect(Collectors.toList());

            for (ItemStack pouchAmmoStack : contents) {
               if (!pouchAmmoStack.isEmpty() && pouchAmmoStack.getItem() == this.gun.getProjectile().getItem()) {
                  int max = Math.min(shrinkAmt[0], pouchAmmoStack.getCount());
                  pouchAmmoStack.shrink(max);
                  shrinkAmt[0] -= max;
                  if (shrinkAmt[0] == 0) {
                     this.updateAmmoPouchContents(itemStack, contents);
                     return;
                  }
               }
            }

            this.updateAmmoPouchContents(itemStack, contents);
         }
      }

      for (ItemStack itemStackx : ammoStack) {
         if (shrinkAmt[0] > 0 && !itemStackx.isEmpty() && itemStackx.getItem() == this.gun.getProjectile().getItem()) {
            int max = Math.min(shrinkAmt[0], itemStackx.getCount());
            itemStackx.shrink(max);
            shrinkAmt[0] -= max;
         }
      }
   }

   private void shrinkFromAmmoPool(ItemStack ammoStack, Player player, int shrinkAmount) {
      int[] shrinkAmt = new int[]{shrinkAmount};
      int exoSuitShrinkAmount = Math.min(shrinkAmt[0], ExoSuitAmmoHelper.getAmmoCountInExoSuit(player, this.gun.getProjectile().getItem()));
      if (exoSuitShrinkAmount > 0) {
         ExoSuitAmmoHelper.shrinkAmmoInExoSuit(player, this.gun.getProjectile().getItem(), exoSuitShrinkAmount);
         shrinkAmt[0] -= exoSuitShrinkAmount;
         if (shrinkAmt[0] == 0) {
            return;
         }
      }

      CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
         IItemHandlerModifiable curios = handler.getEquippedCurios();

         for (int i = 0; i < curios.getSlots(); i++) {
            ItemStack stack = curios.getStackInSlot(i);
            if (stack.getItem() instanceof AmmoBoxItem) {
               List<ItemStack> contentsx = AmmoBoxItem.getContents(stack).collect(Collectors.toList());

               for (ItemStack pouchAmmoStackx : contentsx) {
                  if (!pouchAmmoStackx.isEmpty() && pouchAmmoStackx.getItem() == this.gun.getProjectile().getItem()) {
                     int maxx = Math.min(shrinkAmt[0], pouchAmmoStackx.getCount());
                     pouchAmmoStackx.shrink(maxx);
                     shrinkAmt[0] -= maxx;
                     if (shrinkAmt[0] == 0) {
                        this.updateAmmoPouchContents(stack, contentsx);
                        return;
                     }
                  }
               }

               this.updateAmmoPouchContents(stack, contentsx);
            }
         }
      });

      for (ItemStack itemStack : player.getInventory().items) {
         if (itemStack.getItem() instanceof AmmoBoxItem) {
            List<ItemStack> contents = AmmoBoxItem.getContents(itemStack).collect(Collectors.toList());

            for (ItemStack pouchAmmoStack : contents) {
               if (!pouchAmmoStack.isEmpty() && pouchAmmoStack.getItem() == this.gun.getProjectile().getItem()) {
                  int max = Math.min(shrinkAmt[0], pouchAmmoStack.getCount());
                  pouchAmmoStack.shrink(max);
                  shrinkAmt[0] -= max;
                  if (shrinkAmt[0] == 0) {
                     this.updateAmmoPouchContents(itemStack, contents);
                     return;
                  }
               }
            }

            this.updateAmmoPouchContents(itemStack, contents);
         }
      }

      if (shrinkAmt[0] > 0 && !ammoStack.isEmpty() && ammoStack.getItem() == this.gun.getProjectile().getItem()) {
         int max = Math.min(shrinkAmt[0], ammoStack.getCount());
         ammoStack.shrink(max);
         shrinkAmt[0] -= max;
      }
   }

   private void updateAmmoPouchContents(ItemStack ammoPouch, List<ItemStack> contents) {
      ListTag listTag = new ListTag();

      for (ItemStack stack : contents) {
         listTag.add(NbtHelper.tagFromItem(stack));
      }

      NbtHelper.getOrCreateTag(ammoPouch).put("Items", listTag);
   }

   public void increaseMagAmmo(Player player) {
      ItemStack[] ammoStack = Gun.findAmmoStack(player, this.gun.getProjectile().getItem());
      if (ammoStack.length > 0) {
         CompoundTag tag = NbtHelper.getTagForWrite(this.stack);
         if (tag != null) {
            int maxAmmo = GunModifierHelper.getModifiedAmmoCapacity(this.stack, this.gun);
            boolean hasCreativeBox = player.getInventory().items.stream().anyMatch(i -> i.getItem() instanceof CreativeAmmoBoxItem)
               || ServerPlayHandler.hasCreativeAmmoBoxInCurios((ServerPlayer)player);
            if (hasCreativeBox) {
               tag.putInt("AmmoCount", maxAmmo);
               return;
            }

            int currentAmmo = tag.getInt("AmmoCount");
            if (currentAmmo < 0 || currentAmmo > maxAmmo) {
               currentAmmo = 0;
            }

            int ammoAmount = Math.min(ammoInInventory(ammoStack), maxAmmo);
            int amount = maxAmmo - currentAmmo;
            if (ammoAmount < amount) {
               tag.putInt("AmmoCount", currentAmmo + ammoAmount);
               this.shrinkFromAmmoPool(ammoStack, player, ammoAmount);
            } else {
               tag.putInt("AmmoCount", maxAmmo);
               this.shrinkFromAmmoPool(ammoStack, player, amount);
            }
         }
      }

      this.playReloadSound(player);
   }

   public void reloadItem(Player player) {
      Item reloadItem = this.gun.getReloads().getReloadItem();
      ItemStack[] ammoStacks = Gun.findAmmoStack(player, reloadItem);
      if (ammoStacks.length > 0) {
         CompoundTag tag = NbtHelper.getTagForWrite(this.stack);
         if (tag != null) {
            int maxAmmo = GunModifierHelper.getModifiedAmmoCapacity(this.stack, this.gun);
            int currentAmmo = tag.getInt("AmmoCount");
            if (currentAmmo < maxAmmo) {
               tag.putInt("AmmoCount", maxAmmo);
               this.shrinkFromAmmoPool(ammoStacks, player, 1);
               this.handleReloadByproduct(player);
            }
         }

         this.playReloadSound(player);
      }
   }

   public void increaseAmmo(Player player) {
      AmmoContext context = Gun.findAmmo(player, this.gun.getProjectile().getItem());
      ItemStack ammo = context.stack();
      if (!ammo.isEmpty()) {
         int amount = Math.min(ammo.getCount(), this.gun.getReloads().getReloadAmount());
         ItemStack currentStack = player.getMainHandItem();
         CompoundTag tag = NbtHelper.getTagForWrite(currentStack);
         if (tag != null) {
            int maxAmmo = GunModifierHelper.getModifiedAmmoCapacity(currentStack, this.gun);
            int currentAmmo = tag.getInt("AmmoCount");
            amount = Math.min(amount, maxAmmo - currentAmmo);
            tag.putInt("AmmoCount", currentAmmo + amount);
         }

         this.shrinkFromAmmoPool(Gun.findAmmoStack(player, this.gun.getProjectile().getItem()), player, amount);
      }

      this.playReloadSound(player);
   }

   private void playReloadSound(Player player) {
      if (!(this.stack.getItem() instanceof AnimatedGunItem)) {
         ResourceLocation reloadSound = this.gun.getSounds().getReload();
         if (reloadSound != null) {
            double radius = (Double)Config.SERVER.reloadMaxDistance.get();
            double soundX = player.getX();
            double soundY = player.getY() + 1.0;
            double soundZ = player.getZ();
            S2CMessageGunSound message = new S2CMessageGunSound(
               reloadSound, SoundSource.PLAYERS, (float)soundX, (float)soundY, (float)soundZ, 1.0F, 1.0F, player.getId(), false, true
            );
            PacketHandler.getPlayChannel().sendToNearbyPlayers(() -> LevelLocation.create((ServerLevel) player.level(), soundX, soundY, soundZ, radius), message);
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent.Pre event) {
      {
         Player player = event.getEntity();
         if (!player.level().isClientSide) {
            if ((Boolean)ModSyncedDataKeys.RELOADING.getValue(player)) {
               ItemStack heldItem = player.getMainHandItem();
               if (heldItem.getItem() instanceof GunItem gunItem) {
                  Gun gun = gunItem.getModifiedGun(heldItem);
                  if (gun.getReloads().getReloadType() != ReloadType.MANUAL) {
                     CompoundTag tag = NbtHelper.getOrCreateTag(heldItem);
                  }

                  CompoundTag tag = NbtHelper.getOrCreateTag(heldItem);
                  if (!heldItem.getItem().getClass().getPackageName().startsWith("top.ribs.scguns")) {
                     return;
                  }

                  ReloadTracker tracker = RELOAD_TRACKER_MAP.get(player);
                  boolean needsNewTracker = false;
                  if (tracker == null) {
                     needsNewTracker = true;
                  } else {
                     ItemStack currentWeapon = player.getInventory().getSelected();
                     int currentSlot = player.getInventory().selected;
                     boolean isActivelyReloading = (Boolean)ModSyncedDataKeys.RELOADING.getValue(player);
                     boolean weaponChanged = tracker.slot != currentSlot
                        || currentWeapon.isEmpty()
                        || !currentWeapon.getItem().getClass().equals(tracker.stack.getItem().getClass());
                     if (weaponChanged) {
                        // A reload belongs to the gun it started on: end it, or the stuck state keeps the
                        // player-level RELOADING flag set and every gun animates as if reloading.
                        endReload(player, tracker);
                        needsNewTracker = true;
                        RELOAD_TRACKER_MAP.remove(player);
                     }
                  }

                  if (needsNewTracker) {
                     if (!(player.getInventory().getSelected().getItem() instanceof GunItem)) {
                        ModSyncedDataKeys.RELOADING.setValue(player, false);
                        return;
                     }

                     tracker = new ReloadTracker(player);
                     RELOAD_TRACKER_MAP.put(player, tracker);
                  }

                  boolean weaponFull = tracker.isWeaponFull(player);
                  boolean hasNoAmmo = tracker.hasNoAmmo(player);
                  if (weaponFull || hasNoAmmo) {
                     if (player.getMainHandItem().getItem() instanceof AnimatedGunItem && tracker.gun.getReloads().getReloadType() == ReloadType.MANUAL) {
                        if (!tag.getBoolean("scguns:ShouldStopAfterLoop")) {
                           tag.putBoolean("scguns:ShouldStopAfterLoop", true);
                           // Explicit write-back, as the upstream 1.21.1 port does: a probe showed this
                           // write never reaching the item, so the check above stayed true every tick
                           // and the reload never got past it (HANDOFF section 67).
                           NbtHelper.setTag(player.getMainHandItem(), tag);
                           return;
                        }

                        long stopTime = tracker.pendingManualStopSince;
                        if (stopTime == 0L) {
                           tracker.pendingManualStopSince = System.currentTimeMillis();
                           return;
                        }

                        if (System.currentTimeMillis() - stopTime < 100L) {
                           return;
                        }

                        tag.remove("scguns:ShouldStopAfterLoop");
                        tag.remove("scguns:StopAfterLoopTime");
                     }

                     RELOAD_TRACKER_MAP.remove(player);
                     ModSyncedDataKeys.RELOADING.setValue(player, false);
                     tag.remove("IsReloading");
                     tag.remove("scguns:IsReloading");
                     // A reload that ends here (weapon full, or out of ammo) must not leave the
                     // critical phase behind: the client forces aiming off every tick while it is set
                     // (AimingHandler.onClientTick), which is what made aiming impossible after a
                     // reload (HANDOFF section 63).
                     tag.remove("InCriticalReloadPhase");
                     if (player.getMainHandItem().getItem() instanceof AnimatedGunItem) {
                        tag.putString("scguns:ReloadState", "STOPPING");
                        tag.putBoolean("scguns:IsPlayingReloadStop", true);
                        PacketHandler.getPlayChannel().sendToPlayer(() -> (ServerPlayer)player, new S2CMessageStopReload());
                        // Write the tag back explicitly, the way the upstream 1.21.1 port does with its
                        // setCustomData() helper: a probe read the removals above and the STOPPING state
                        // back as absent, so without this the client never sees the stop state and the
                        // chambering animation is never triggered (HANDOFF section 67).
                        NbtHelper.setTag(player.getMainHandItem(), tag);
                     }
                  }
               } else {
                  // Holding something that is not a gun at all (put the gun away, switched to a
                  // block): the block above is skipped entirely, so without this the reload state
                  // stayed behind and RELOADING never cleared (HANDOFF section 66).
                  ReloadTracker staleReload = RELOAD_TRACKER_MAP.remove(player);
                  if (staleReload != null) {
                     endReload(player, staleReload);
                  } else {
                     ModSyncedDataKeys.RELOADING.setValue(player, false);
                  }
               }
            } else {
               RELOAD_TRACKER_MAP.remove(player);
               ItemStack held = player.getMainHandItem();
               CompoundTag tagx = NbtHelper.getTagForWrite(held);
               if (tagx != null) {
                  tagx.remove("IsReloading");
                  // Self-heal: the critical phase only ever means "a reload is running", and nothing
                  // sets it for manual reloads (C2SMessageReload and ReloadHandler both guard on
                  // != MANUAL), so any value left here while nothing reloads is stale (HANDOFF section
                  // 63). Without this the client keeps forcing aiming off every tick, which is what
                  // made aiming impossible after a reload.
                  tagx.remove("InCriticalReloadPhase");
               }

               tickStaleReloadStateWatchdog(player, held, tagx);
            }
         }
      }
   }

   /**
    * Clears an item that was left holding a reload/stop state while nothing is reloading.
    *
    * <p>Two situations used to leave the gun unaimable for good (HANDOFF section 63): a reload that
    * ended through the tracker's completion path, and a stop animation that never got its final
    * transition. The client refuses to aim while {@code scguns:ReloadState} is set to anything but
    * {@code NONE}, and it reads that state from the server's copy of the item, so the clean-up has to
    * happen here. The window is far longer than any legitimate stop animation, and every forced
    * clean-up is logged so the underlying cause can still be found from a log.</p>
    */
   private static void tickStaleReloadStateWatchdog(Player player, ItemStack held, CompoundTag tag) {
      String state = tag == null ? "" : tag.getString("scguns:ReloadState");
      boolean stale = tag != null
         && !state.isEmpty()
         && !state.equals("NONE")
         && (tag.getBoolean("scguns:IsPlayingReloadStop") || tag.getBoolean("scguns:ShouldStopAfterLoop"));
      if (!stale) {
         STALE_RELOAD_STATE_COUNTER.remove(player);
         return;
      }

      int ticks = STALE_RELOAD_STATE_COUNTER.merge(player, 1, Integer::sum);
      if (ticks < STALE_RELOAD_STATE_TICKS) {
         return;
      }

      STALE_RELOAD_STATE_COUNTER.remove(player);
      LOGGER.warn("Clearing a reload state that outlived its reload: item={} state={} after {} ticks",
         held.getItem(), state, ticks);
      tag.remove("scguns:ReloadState");
      tag.remove("scguns:IsPlayingReloadStop");
      tag.remove("scguns:ShouldStopAfterLoop");
      tag.remove("scguns:StopAfterLoopTime");
      tag.remove("IsReloading");
      tag.remove("scguns:IsReloading");
      tag.remove("InCriticalReloadPhase");
   }

   /**
    * Ends a reload that cannot continue any more - the player put the gun away, so the tracker would
    * otherwise keep judging the newly held item against the old gun's magazine, never seeing it full
    * nor out of ammo, and the player-level RELOADING flag would stay set for good. Every gun then
    * animated as if it were reloading (HANDOFF section 66).
    */
   private static void endReload(Player player, ReloadTracker tracker) {
      ModSyncedDataKeys.RELOADING.setValue(player, false);
      CompoundTag tag = NbtHelper.getTagForWrite(tracker.stack);
      if (tag != null) {
         // Clear, rather than setting STOPPING: this gun is no longer in hand, so its stop animation
         // can never play (that state machine only runs while the gun is held) and the flag would
         // simply sit there - which is what made that gun animate as if it were reloading the next
         // time it was held or looked at (HANDOFF section 68). The upstream 1.21.1 port clears the
         // old weapon's reload data in exactly this situation.
         tag.remove("IsReloading");
         tag.remove("scguns:IsReloading");
         tag.remove("InCriticalReloadPhase");
         tag.remove("scguns:ShouldStopAfterLoop");
         tag.remove("scguns:StopAfterLoopTime");
         tag.remove("scguns:ReloadState");
         tag.remove("scguns:IsPlayingReloadStop");
         tag.remove("InReloadLoop");
         tag.remove("LastReloadStateChange");
         NbtHelper.setTag(tracker.stack, tag);
      }

      tracker.pendingManualStopSince = 0L;
   }

   public static void loaded(Player player) {
      ItemStack heldItem = player.getMainHandItem();
      if (heldItem.getItem().getClass().getPackageName().startsWith("top.ribs.scguns")) {
         CompoundTag tag = NbtHelper.getTagForWrite(player.getMainHandItem());
         if (!(Boolean)ModSyncedDataKeys.RELOADING.getValue(player)) {
            RELOAD_TRACKER_MAP.remove(player);
            if (tag != null) {
               tag.remove("IsReloading");
            }
         } else {
            if (!RELOAD_TRACKER_MAP.containsKey(player)) {
               if (!(player.getInventory().getSelected().getItem() instanceof GunItem)) {
                  ModSyncedDataKeys.RELOADING.setValue(player, false);
                  return;
               }

               RELOAD_TRACKER_MAP.put(player, new ReloadTracker(player));
            }

            ReloadTracker tracker = RELOAD_TRACKER_MAP.get(player);
            boolean weaponChanged = false;
            ItemStack currentWeapon = player.getInventory().getSelected();
            int currentSlot = player.getInventory().selected;
            if (tracker.slot != currentSlot || !currentWeapon.getItem().equals(tracker.stack.getItem()) || currentWeapon.isEmpty()) {
               weaponChanged = true;
            }

            if (!weaponChanged
               && !tracker.hasNoAmmo(player)
               && (!tracker.isWeaponFull(player) || tracker.gun.getReloads().getReloadType() == ReloadType.MANUAL)) {
               Item item = player.getMainHandItem().getItem();
               if (item instanceof GunItem) {
                  Gun gun = tracker.gun;
                  ReloadType reloadType = gun.getReloads().getReloadType();
                  if (!(item instanceof AnimatedGunItem)) {
                     if (reloadType == ReloadType.MAG_FED) {
                        tracker.increaseMagAmmo(player);
                     } else if (reloadType == ReloadType.SINGLE_ITEM) {
                        tracker.reloadItem(player);
                     } else if (reloadType == ReloadType.MANUAL) {
                        tracker.increaseAmmo(player);
                     }

                     RELOAD_TRACKER_MAP.remove(player);
                     ModSyncedDataKeys.RELOADING.setValue(player, false);
                     if (tag != null) {
                        tag.remove("IsReloading");
                     }

                     return;
                  }

                  if (item instanceof AnimatedGunItem gunItem && item.getClass().getPackageName().startsWith("top.ribs.scguns")) {
                     if (reloadType == ReloadType.MANUAL) {
                        tracker.increaseAmmo(player);
                     }

                     if (tracker.isWeaponFull(player) || tracker.hasNoAmmo(player)) {
                        long id = GeoItem.getId(player.getMainHandItem());
                        AnimationController<GeoAnimatable> animationController = (AnimationController<GeoAnimatable>)gunItem.getAnimatableInstanceCache()
                           .getManagerForId(id)
                           .getAnimationControllers()
                           .get("controller");
                        // Null on a dedicated server: animation controllers are registered client-side
                        // only, and this tick handler runs on the server. The reload itself still has to
                        // finish, so only the animation is skipped.
                        if (animationController != null) {
                           animationController.setAnimationSpeed(1.0);
                           animationController.forceAnimationReset();
                        }

                        if (reloadType == ReloadType.MANUAL) {
                           ReloadHandler.loaded(player);
                        }

                        tracker.handleReloadByproduct(player);
                        RELOAD_TRACKER_MAP.remove(player);
                        ModSyncedDataKeys.RELOADING.setValue(player, false);
                        if (tag != null) {
                           tag.remove("IsReloading");
                        }
                     }
                  }
               }
            } else {
               RELOAD_TRACKER_MAP.remove(player);
               ModSyncedDataKeys.RELOADING.setValue(player, false);
               if (tag != null) {
                  tag.remove("IsReloading");
               }
            }
         }
      }
   }
}
