package top.ribs.scguns.blockentity;


import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import top.ribs.scguns.block.ChargedAmethystRelayBlock;
import top.ribs.scguns.init.ModBlockEntities;

public class ChargedAmethystRelayBlockEntity extends BlockEntity {
   private NoteBlockInstrument tunedInstrument = NoteBlockInstrument.HARP;
   private boolean isActive = false;
   private int activationTimer = 0;
   private static final int ACTIVATION_DURATION = 20;

   public ChargedAmethystRelayBlockEntity(BlockPos pos, BlockState blockState) {
      super((BlockEntityType)ModBlockEntities.CHARGED_AMETHYST_RELAY.get(), pos, blockState);
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, ChargedAmethystRelayBlockEntity relay) {
      if (level instanceof ServerLevel) {
         if (relay.isActive && relay.activationTimer > 0) {
            relay.activationTimer--;
            if (relay.activationTimer <= 0) {
               relay.isActive = false;
               relay.setChanged();
            }
         }

         NoteBlockInstrument stateInstrument = (NoteBlockInstrument)state.getValue(ChargedAmethystRelayBlock.INSTRUMENT);
         if (stateInstrument != relay.tunedInstrument) {
            relay.tunedInstrument = stateInstrument;
            relay.setChanged();
         }
      }
   }

   public void onInstrumentTuned(NoteBlockInstrument instrument) {
      this.tunedInstrument = instrument;
      this.setChanged();
   }

   public void activateFromInstrument() {
      if (!this.isActive) {
         this.isActive = true;
         this.activationTimer = 20;

         assert this.level != null;

         this.level.playSound(null, this.worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.0F);
         this.setChanged();
      }
   }

   public void deactivate() {
      if (this.isActive) {
         this.isActive = false;
         this.activationTimer = 0;
         this.setChanged();
      }
   }

   public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.loadAdditional(tag, registries);
      this.tunedInstrument = NoteBlockInstrument.valueOf(tag.getString("TunedInstrument"));
      this.isActive = tag.getBoolean("IsActive");
      this.activationTimer = tag.getInt("ActivationTimer");
   }

   protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.saveAdditional(tag, registries);
      tag.putString("TunedInstrument", this.tunedInstrument.name());
      tag.putBoolean("IsActive", this.isActive);
      tag.putInt("ActivationTimer", this.activationTimer);
   }

   public boolean isActive() {
      return this.isActive;
   }
}
