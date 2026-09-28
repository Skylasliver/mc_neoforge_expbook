package dev.skylasliver.skylasliver_expbook.client;

import dev.skylasliver.skylasliver_expbook.ModMindEntry;
import dev.skylasliver.skylasliver_expbook.network.CreativeBookEditsPayload;
import dev.skylasliver.skylasliver_expbook.util.CreativeBookEdits;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Keep source-removal and destination-write in one server transaction. */
@EventBusSubscriber(modid = ModMindEntry.MOD_ID, value = Dist.CLIENT)
public final class CreativeBookPackets {
    private static final List<CreativeBookEdits.Edit> PENDING = new ArrayList<>();
    private static Object connection;
    private CreativeBookPackets() {}
    public static void enqueue(int slot, ItemStack stack) {
        var current = Minecraft.getInstance().getConnection();
        if (current != connection) { PENDING.clear(); connection = current; }
        if (current == null) return;
        if (PENDING.size() >= CreativeBookEdits.MAX_EDITS) flush();
        PENDING.add(new CreativeBookEdits.Edit(slot, stack.copy()));
    }
    public static void flush() {
        if (Minecraft.getInstance().getConnection() != connection || connection == null) { PENDING.clear(); return; }
        if (!PENDING.isEmpty()) {
            PacketDistributor.sendToServer(new CreativeBookEditsPayload(List.copyOf(PENDING)));
            PENDING.clear();
        }
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().getConnection() != connection || Minecraft.getInstance().player == null) {
            PENDING.clear();
            connection = Minecraft.getInstance().getConnection();
            return;
        }
        flush();
    }
}
