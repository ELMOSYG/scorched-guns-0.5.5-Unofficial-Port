package top.ribs.scguns.common;



import top.ribs.scguns.util.NbtHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import top.ribs.scguns.annotation.Ignored;

public class CustomGun  {
   @Ignored
   public ItemStack model;
   public Gun gun;

   public CustomGun() {
      super();
   }

   public ItemStack getModel() {
      return this.model;
   }

   public Gun getGun() {
      return this.gun;
   }

   public CompoundTag serializeNBT() {
      CompoundTag compound = new CompoundTag();
      compound.put("Model", top.ribs.scguns.util.NbtHelper.tagFromItem(this.model));
      compound.put("Gun", this.gun.serializeNBT());
      return compound;
   }

   public void deserializeNBT(CompoundTag compound) {
      this.model = top.ribs.scguns.util.NbtHelper.itemFromTag(compound.getCompound("Model"));
      this.gun = Gun.create(compound.getCompound("Gun"));
   }
}
