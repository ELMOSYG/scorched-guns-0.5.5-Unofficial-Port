package top.ribs.scguns.util;

public abstract class SuperBuilder<R, T extends SuperBuilder<R, T>> {
   public SuperBuilder() {
      super();
   }

   public abstract R build();

   protected final T self() {
      return (T)this;
   }
}
