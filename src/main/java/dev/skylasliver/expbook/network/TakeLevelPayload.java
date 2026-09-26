package dev.skylasliver.expbook.network;

import dev.skylasliver.expbook.ExperienceBookMod;
import dev.skylasliver.expbook.item.ExperienceBookItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent when the client sees an attack click that hit nothing. Vanilla reports that as a
 * client-only event, so the server only learns about it through this payload.
 *
 * <p>{@code all} separates a plain swing (take one level) from a sneaking swing
 * (take everything).
 */
public record TakeLevelPayload(boolean all) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TakeLevelPayload> TYPE =
            new CustomPacketPayload.Type<>(ExperienceBookMod.id("take_level"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TakeLevelPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> buffer.writeBoolean(payload.all()),
                    buffer -> new TakeLevelPayload(buffer.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TakeLevelPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                ExperienceBookItem.handleTake(serverPlayer, payload.all());
            }
        });
    }
}
