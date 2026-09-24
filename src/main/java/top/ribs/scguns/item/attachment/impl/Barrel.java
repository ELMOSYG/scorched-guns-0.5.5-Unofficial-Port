package top.ribs.scguns.item.attachment.impl;

import top.ribs.scguns.interfaces.IGunModifier;

public class Barrel extends Attachment {
   private final float length;

   private Barrel(float length, IGunModifier... modifier) {
      super(modifier);
      this.length = length;
   }

   public float getLength() {
      return this.length;
   }

   public static Barrel create(float length, IGunModifier... modifiers) {
      return new Barrel(length, modifiers);
   }
}
