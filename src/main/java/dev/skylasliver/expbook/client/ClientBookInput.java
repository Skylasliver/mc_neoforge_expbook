package dev.skylasliver.expbook.client;

import dev.skylasliver.expbook.network.TakeLevelPayload;
import dev.skylasliver.expbook.util.BookLocator;
import dev.skylasliver.expbook.util.InteractionCooldown;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Client half of the book interactions. A left-click that hits nothing is a client-only
 * event, so it is detected here from the held attack key and forwarded to the server.
 * The key is re-sampled every tick, so holding the button keeps withdrawing just like
 * holding the right button keeps storing; the shared cooldown spaces the transfers out
 * and keeps a single click from double firing.
 */
@EventBusSubscriber(modid = dev.skylasliver.expbook.ExperienceBookMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ClientBookInput {

    private ClientBookInput() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.screen != null) {
            return;
        }
        // Re-sample the attack key every tick: vanilla only reports the initial press
        // through its input event, so a held button has to be polled to repeat.
        if (!minecraft.options.keyAttack.isDown()) {
            return;
        }
        // A swing that hit a block or an entity is not a book action: mining and
        // attacking must keep working normally while holding the book.
        if (minecraft.hitResult != null && minecraft.hitResult.getType() != HitResult.Type.MISS) {
            return;
        }
        if (BookLocator.held(player).isEmpty()) {
            return;
        }
        if (!InteractionCooldown.tryAcquire(player.getUUID())) {
            return;
        }
        PacketDistributor.sendToServer(new TakeLevelPayload(player.isShiftKeyDown()));
    }
}
