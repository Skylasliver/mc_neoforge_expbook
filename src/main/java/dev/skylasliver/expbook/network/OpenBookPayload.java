package dev.skylasliver.expbook.network;

import dev.skylasliver.expbook.ExperienceBookMod;
import dev.skylasliver.expbook.menu.ExperienceBookMenu;
import dev.skylasliver.expbook.util.BookLocator;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Asks the server to open the book menu. The server re-resolves the held book instead
 * of trusting a client-supplied stack, so the menu always edits the real item.
 */
public record OpenBookPayload(boolean settings) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenBookPayload> TYPE =
            new CustomPacketPayload.Type<>(ExperienceBookMod.id("open_book"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenBookPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> buffer.writeBoolean(payload.settings()),
                    buffer -> new OpenBookPayload(buffer.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenBookPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ItemStack book = BookLocator.held(player);
            if (book.isEmpty() || dev.skylasliver.expbook.util.BookIdentity.ensureBound(player, book) == null) {
                return;
            }
            player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                    (containerId, inventory, ignored) -> payload.settings()
                            ? new dev.skylasliver.expbook.menu.BookSettingsMenu(containerId, inventory, book)
                            : new ExperienceBookMenu(containerId, inventory, book),
                    Component.translatable(payload.settings() ? "gui.skylasliver_expbook.settings" : "gui.skylasliver_expbook.title")), buffer -> ItemStack.STREAM_CODEC.encode(buffer, book));
        });
    }
}
