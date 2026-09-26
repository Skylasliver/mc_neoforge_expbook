package dev.skylasliver.expbook.client;

import dev.skylasliver.expbook.ExperienceBookMod;
import dev.skylasliver.expbook.client.screen.ExperienceBookScreen;
import dev.skylasliver.expbook.item.ExperienceBookItem;
import dev.skylasliver.expbook.menu.ExperienceBookMenu;
import dev.skylasliver.expbook.registry.ModKeyMappings;
import dev.skylasliver.expbook.registry.ModMenus;
import dev.skylasliver.expbook.util.BookLocator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Opens the book GUI on the G key, but only while a book is actually held. */
@EventBusSubscriber(modid = ExperienceBookMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientBookGui {

    private ClientBookGui() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.EXPERIENCE_BOOK.get(), ExperienceBookScreen::new);
        event.register(ModMenus.BOOK_SETTINGS.get(), dev.skylasliver.expbook.client.screen.BookSettingsScreen::new);
    }

    @EventBusSubscriber(modid = ExperienceBookMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static final class TickHandler {

        private TickHandler() {
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            Player player = minecraft.player;
            if (player == null || minecraft.screen != null) {
                return;
            }
            while (ModKeyMappings.OPEN_BOOK.consumeClick()) {
                ItemStack book = BookLocator.held(player);
                if (book.isEmpty()) {
                    continue;
                }
                openBook(book);
            }
        }

        private static void openBook(ItemStack book) {
            // The server-side handler lives with the container; the client simply asks to
            // open the menu and lets the container read the held stack.
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                    new dev.skylasliver.expbook.network.OpenBookPayload(
                            net.minecraft.client.gui.screens.Screen.hasShiftDown()));
        }
    }
}
