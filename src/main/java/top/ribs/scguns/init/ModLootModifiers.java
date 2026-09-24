package top.ribs.scguns.init;


import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import top.ribs.scguns.world.loot.AddItemModifier;
import top.ribs.scguns.world.loot.AddLootTableModifier;

public class ModLootModifiers {
   public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS = DeferredRegister.create(
      NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, "scguns"
   );
   public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<? extends IGlobalLootModifier>> ADD_ITEM = LOOT_MODIFIERS.register("add_item", AddItemModifier.CODEC);
   public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<? extends IGlobalLootModifier>> ADD_LOOT_TABLE = LOOT_MODIFIERS.register(
      "add_loot_table", AddLootTableModifier.CODEC
   );

   public ModLootModifiers() {
      super();
   }
}
