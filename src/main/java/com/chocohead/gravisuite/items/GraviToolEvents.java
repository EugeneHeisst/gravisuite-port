package com.chocohead.gravisuite.items;

import com.chocohead.gravisuite.Gravisuite;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * onBlockStartBreak of the original tools. The break event comes first in ServerPlayerGameMode.destroyBlock, before the tool
 * is copied for the drops: Vajra adds its temporary silk touch here, the drill breaks the rest of its 3x3 through
 * destroyBlock (protection, drops, experience and EU like a normal break).
 */
@EventBusSubscriber(modid = Gravisuite.MODID)
public final class GraviToolEvents {
    private GraviToolEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void breakBlock(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer player) || player.isCreative()) return;
        var stack = player.getMainHandItem();
        if (stack.getItem() instanceof ItemVajra vajra) vajra.startAccurateBreak(stack, player.level());
        if (stack.getItem() instanceof ItemAdvancedDrill && ItemAdvancedDrill.readMode(stack) == ItemAdvancedDrill.DrillMode.BIG_HOLES)
            ItemAdvancedDrill.breakBigHole(player, stack, event.getPos());
    }
}
