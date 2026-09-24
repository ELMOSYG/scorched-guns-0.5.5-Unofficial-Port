package top.ribs.scguns.interfaces;

import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

public interface IHeadshotBox<T extends Entity> {
   @Nullable
   AABB getHeadshotBox(T var1);
}
