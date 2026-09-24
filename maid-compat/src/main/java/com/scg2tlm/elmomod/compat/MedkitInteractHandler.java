package com.scg2tlm.elmomod.compat;


import net.neoforged.fml.common.EventBusSubscriber;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.util.FakePlayer;

/**
 * 「潜行右键女仆 = 用手上的医疗包治疗她」（用户要求，与 TLM 喂金苹果同一套习惯）。
 *
 * <h2>为什么必须由我们接管</h2>
 * <ol>
 *   <li>scgextra 的 {@code MultiUseHealingItem.interactLivingEntity} 只对 <b>Player</b> 生效
 *       （{@code target instanceof Player}，不满足就 {@code return SUCCESS} 什么都不做）
 *       ⇒ 拿它治女仆本来就无效；</li>
 *   <li>而右键女仆会被 TLM 的 {@code EntityMaid#mobInteract}（开女仆界面）先吃掉 ——
 *       原版顺序是先 {@code entity.interact}，物品的 {@code interactLivingEntity} 只有前者
 *       未消费时才轮得到。所以想治女仆，只能在<b>更早</b>的事件里拦。</li>
 * </ol>
 * <p>Forge 的 {@code PlayerInteractEvent.EntityInteract} 正好在 {@code entity.interact} 之前触发，
 * 因此这里是唯一干净的入口。</p>
 *
 * <h2>行为约定</h2>
 * <ul>
 *   <li><b>潜行 + 右键</b>才是治疗；不潜行右键照旧开女仆界面（不改变原有习惯）；</li>
 *   <li>只对自己的女仆生效，且<b>只在真的受伤 / 带撕裂伤时</b>才拦 —— 满血时静默放行，
 *       免得玩家想开界面却被治疗动作吞掉；</li>
 *   <li>治疗数值与冷却复用 {@link SC2HealCompat#healMaidWithMedkit}
 *       （读物品字段，且与物品自身一样<b>不消耗</b>，只吃 100 刻冷却）。</li>
 * </ul>
 */
// Registered explicitly from ExampleMod, which is gated on Touhou Little Maid being installed
// (see the note on MaidProjectileHandler: @EventBusSubscriber would load TLM unconditionally).
public final class MedkitInteractHandler {

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof EntityMaid maid)) return;
        if (maid.level().isClientSide) return;                 // 只在服务端结算，避免双发
        Player player = event.getEntity();
        if (player instanceof FakePlayer) return;
        if (!player.isShiftKeyDown()) return;                   // 潜行右键才是治疗
        if (!maid.isOwnedBy(player)) return;                    // 只治自己的女仆

        ItemStack stack = event.getItemStack();
        if (!SC2HealCompat.isScgExtraMedkit(stack)) return;

        if (!SC2HealCompat.healMaidWithMedkit(stack, maid, player)) return;   // 没伤就没治，放行去开界面

        player.swing(event.getHand(), true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);                                // 拦下：不再触发女仆界面
    }
}
