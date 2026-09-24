package top.ribs.scguns.debug;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.tuple.Pair;

public interface IEditorMenu {
   Component getEditorLabel();

   void getEditorWidgets(List<Pair<Component, Supplier<IDebugWidget>>> var1);
}
