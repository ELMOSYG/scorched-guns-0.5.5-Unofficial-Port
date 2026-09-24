package top.ribs.scguns.init;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

public class ModTiers {
   public static final Tier ANTHRALITE = new Tier() {
      public int getUses() {
         return 600;
      }

      public float getSpeed() {
         return 7.0F;
      }

      public float getAttackDamageBonus() {
         return 2.5F;
      }

      public TagKey<Block> getIncorrectBlocksForDrops() {
         return BlockTags.INCORRECT_FOR_IRON_TOOL;
      }

      public int getLevel() {
         return 2;
      }

      public int getEnchantmentValue() {
         return 10;
      }

      public Ingredient getRepairIngredient() {
         return Ingredient.of(new ItemLike[]{(ItemLike)ModItems.ANTHRALITE_INGOT.get()});
      }
   };
   public static final Tier ANCIENT_BRASS = new Tier() {
      public int getUses() {
         return 250;
      }

      public float getSpeed() {
         return 6.0F;
      }

      public float getAttackDamageBonus() {
         return 3.0F;
      }

      public TagKey<Block> getIncorrectBlocksForDrops() {
         return BlockTags.INCORRECT_FOR_IRON_TOOL;
      }

      public int getLevel() {
         return 2;
      }

      public int getEnchantmentValue() {
         return 14;
      }

      public Ingredient getRepairIngredient() {
         return Ingredient.of(new ItemLike[]{(ItemLike)ModItems.ANCIENT_BRASS.get()});
      }
   };

   public ModTiers() {
      super();
   }
}
