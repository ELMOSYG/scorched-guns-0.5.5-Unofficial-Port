package top.ribs.scguns.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.TargetBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.fml.util.ObfuscationReflectionHelper;

public class ReflectionUtil {
   private static final Method updateRedstoneOutputMethod = ObfuscationReflectionHelper.findMethod(
      TargetBlock.class, "updateRedstoneOutput", new Class[]{LevelAccessor.class, BlockState.class, BlockHitResult.class, Entity.class}
   );

   public ReflectionUtil() {
      super();
   }

   public static int updateTargetBlock(TargetBlock block, LevelAccessor accessor, BlockState state, BlockHitResult result, Entity entity) {
      try {
         return (Integer)updateRedstoneOutputMethod.invoke(block, accessor, state, result, entity);
      } catch (InvocationTargetException | IllegalAccessException var6) {
         return 0;
      }
   }
}
