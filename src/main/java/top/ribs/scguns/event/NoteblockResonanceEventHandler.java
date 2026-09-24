package top.ribs.scguns.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.neoforged.neoforge.event.level.NoteBlockEvent.Play;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import top.ribs.scguns.block.ChargedAmethystRelayBlock;

@EventBusSubscriber(
   modid = "scguns",
   bus = Bus.GAME
)
public class NoteblockResonanceEventHandler {
   public NoteblockResonanceEventHandler() {
      super();
   }

   @SubscribeEvent
   public static void onNoteBlockPlay(Play event) {
      if (event.getLevel() instanceof ServerLevel serverLevel) {
         BlockPos var14 = event.getPos();
         NoteBlockInstrument playedInstrument = event.getInstrument();
         byte searchRadius = 20;
         int foundRelays = 0;

         for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -searchRadius; y <= searchRadius; y++) {
               for (int z = -searchRadius; z <= searchRadius; z++) {
                  double distance = Math.sqrt((double)(x * x + y * y + z * z));
                  if (distance <= (double)searchRadius) {
                     BlockPos relayPos = var14.offset(x, y, z);
                     if (serverLevel.getBlockState(relayPos).getBlock() instanceof ChargedAmethystRelayBlock relay) {
                        foundRelays++;
                        NoteBlockInstrument relayInstrument = (NoteBlockInstrument)serverLevel.getBlockState(relayPos).getValue(ChargedAmethystRelayBlock.INSTRUMENT);
                        relay.onInstrumentHeard(serverLevel, relayPos, playedInstrument);
                     }
                  }
               }
            }
         }
      }
   }
}
