package top.ribs.scguns.client.screen;

import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import top.ribs.scguns.entity.monster.SupplyScampEntity;

public class SupplyScampMenuProvider implements MenuProvider {
   private final SupplyScampEntity supplyScamp;

   public SupplyScampMenuProvider(SupplyScampEntity supplyScamp) {
      super();
      this.supplyScamp = supplyScamp;
   }

   public Component getDisplayName() {
      return this.supplyScamp.getDisplayName();
   }

   @Nullable
   public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
      return new ChestMenu((MenuType)ModMenuTypes.SUPPLY_SCAMP_MENU.get(), id, playerInventory, this.supplyScamp.inventory, 3);
   }
}
