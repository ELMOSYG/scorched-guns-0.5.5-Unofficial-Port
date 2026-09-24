package top.ribs.scguns.event;

import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import top.ribs.scguns.init.ModCommands;
import top.ribs.scguns.init.ModRecipeBookCommand;

@EventBusSubscriber(
   modid = "scguns",
   bus = Bus.GAME
)
public class ModCommandsRegister {
   public ModCommandsRegister() {
      super();
   }

   @SubscribeEvent
   public static void onRegisterCommands(RegisterCommandsEvent event) {
      ModCommands.register(event.getDispatcher());
      // Second root literal: Brigadier merges the two "scguns" nodes, and this keeps the recipe book
      // diagnostic out of ModCommands' deeply nested builder chain.
      ModRecipeBookCommand.register(event.getDispatcher());
   }
}
