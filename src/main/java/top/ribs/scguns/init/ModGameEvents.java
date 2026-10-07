package top.ribs.scguns.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModGameEvents {
   public static final DeferredRegister<GameEvent> REGISTER = DeferredRegister.create(Registries.GAME_EVENT, "scguns");

   /**
    * A silenced shot. It is not silent - it just carries less far, so this event uses half of vanilla's
    * 16 block radius (see GameEvent.register(String), whose bytecode hardcodes 16). Vanilla has no
    * short-range event that fits a gunshot: the only 10 block one is the jukebox.
    */
   public static final DeferredHolder<GameEvent, GameEvent> SILENCED_GUNSHOT =
      REGISTER.register("silenced_gunshot", () -> new GameEvent(8));
}
