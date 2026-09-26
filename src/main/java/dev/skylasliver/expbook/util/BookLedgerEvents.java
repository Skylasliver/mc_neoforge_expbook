package dev.skylasliver.expbook.util;

import dev.skylasliver.expbook.ExperienceBookMod;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Delivers books recovered while their owner was offline. */
@EventBusSubscriber(modid = ExperienceBookMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class BookLedgerEvents {
    private BookLedgerEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 20 == 0)
            BookIdentity.purgeRevoked(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) CreativeBookEdits.forget(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BookIdentity.purgeRevoked(player);
            BookLedger.deliverPending(player);
        }
    }
}
