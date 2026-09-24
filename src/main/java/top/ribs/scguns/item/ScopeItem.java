package top.ribs.scguns.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import top.ribs.scguns.item.attachment.IScope;
import top.ribs.scguns.item.attachment.impl.Scope;

public class ScopeItem extends AttachmentItem implements IScope, IColored {
   private final Scope scope;
   private final boolean colored;

   public ScopeItem(Scope scope, Properties properties) {
      super(properties);
      this.scope = scope;
      this.colored = true;
   }

   public ScopeItem(Scope scope, Properties properties, boolean colored) {
      super(properties);
      this.scope = scope;
      this.colored = colored;
   }

   public Scope getProperties() {
      return this.scope;
   }

   @Override
   public boolean canColor(ItemStack stack) {
      return this.colored;
   }


}
