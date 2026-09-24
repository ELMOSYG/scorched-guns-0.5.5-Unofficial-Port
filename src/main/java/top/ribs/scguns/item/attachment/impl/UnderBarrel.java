package top.ribs.scguns.item.attachment.impl;

import top.ribs.scguns.interfaces.IGunModifier;

public class UnderBarrel extends Attachment {
   private UnderBarrel(IGunModifier... modifier) {
      super(modifier);
   }

   public static UnderBarrel create(IGunModifier... modifier) {
      return new UnderBarrel(modifier);
   }

   public IGunModifier getModifier() {
      return this.modifiers[0];
   }
}
