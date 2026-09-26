package dev.skylasliver.expbook.network;

import dev.skylasliver.expbook.ExperienceBookMod;
import dev.skylasliver.expbook.util.CreativeBookEdits;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CreativeBookEditsPayload(List<CreativeBookEdits.Edit> edits) implements CustomPacketPayload {
    public static final Type<CreativeBookEditsPayload> TYPE = new Type<>(ExperienceBookMod.id("creative_book_edits"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CreativeBookEditsPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.edits.size());
                for (var edit : payload.edits) {
                    buffer.writeInt(edit.slot());
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, edit.stack());
                }
            }, buffer -> {
                int count = buffer.readVarInt();
                if (count < 0 || count > CreativeBookEdits.MAX_EDITS) throw new IllegalArgumentException("Too many creative edits");
                List<CreativeBookEdits.Edit> edits = new ArrayList<>();
                for (int i = 0; i < count; i++) edits.add(new CreativeBookEdits.Edit(buffer.readInt(), ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer)));
                return new CreativeBookEditsPayload(List.copyOf(edits));
            });
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(CreativeBookEditsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) CreativeBookEdits.apply(player, payload.edits);
        });
    }
}
