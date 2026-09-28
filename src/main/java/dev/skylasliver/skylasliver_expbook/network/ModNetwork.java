package dev.skylasliver.skylasliver_expbook.network;

import dev.skylasliver.skylasliver_expbook.ModMindEntry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = ModMindEntry.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ModNetwork {

    private ModNetwork() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("3");
        registrar.playToServer(CreativeBookEditsPayload.TYPE, CreativeBookEditsPayload.STREAM_CODEC, CreativeBookEditsPayload::handle);
        registrar.playToServer(
                TakeLevelPayload.TYPE,
                TakeLevelPayload.STREAM_CODEC,
                TakeLevelPayload::handle);
        registrar.playToServer(
                OpenBookPayload.TYPE,
                OpenBookPayload.STREAM_CODEC,
                OpenBookPayload::handle);
    }
}
