package top.ribs.scguns.item.attachment.impl;

import top.ribs.scguns.interfaces.IGunModifier;

public class Magazine extends Attachment {
   private Magazine(IGunModifier... modifier) {
      super(modifier);
   }

   public static Magazine create(IGunModifier... modifier) {
      return new Magazine(modifier);
   }
}
