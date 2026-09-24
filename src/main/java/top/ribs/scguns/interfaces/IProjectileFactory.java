package top.ribs.scguns.interfaces;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.entity.projectile.ProjectileEntity;
import top.ribs.scguns.item.GunItem;

public interface IProjectileFactory {
   ProjectileEntity create(Level var1, LivingEntity var2, ItemStack var3, GunItem var4, Gun var5);
}
