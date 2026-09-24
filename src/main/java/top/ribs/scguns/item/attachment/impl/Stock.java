package top.ribs.scguns.item.attachment.impl;

import top.ribs.scguns.interfaces.IGunModifier;

public class Stock extends Attachment {
   private Stock(IGunModifier... modifier) {
      super(modifier);
   }

   public static Stock create(IGunModifier... modifier) {
      return new Stock(modifier);
   }
}
