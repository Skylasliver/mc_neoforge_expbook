package dev.skylasliver.expbook.util;

import dev.skylasliver.expbook.item.ExperienceBookItem;
import dev.skylasliver.expbook.registry.ModComponents;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Reconcile a creative inventory transaction against server-owned stacks, not client NBT. */
public final class CreativeBookEdits {
    public record Edit(int slot, ItemStack stack) {}
    public static final int MAX_EDITS = 256;
    // Creative cursor pickup has no vanilla server cursor packet. Reserve removed originals
    // until they are placed/dropped; copies never get a second claim to that reservation.
    private static final Map<ServerPlayer, LinkedHashMap<UUID, ItemStack>> CURSORS = new WeakHashMap<>();
    private static final Map<ServerPlayer, List<ItemStack>> UNBOUND_CURSORS = new WeakHashMap<>();
    private CreativeBookEdits() {}

    private static UUID id(ItemStack stack) {
        return stack.getItem() instanceof ExperienceBookItem ? stack.get(ModComponents.BOOK_ID.get()) : null;
    }

    public static void forget(ServerPlayer player) { CURSORS.remove(player); UNBOUND_CURSORS.remove(player); }

    public static void apply(ServerPlayer player, List<Edit> edits) {
        if (!player.gameMode.isCreative() || edits.size() > MAX_EDITS) return;
        LinkedHashMap<UUID, ItemStack> reserved = CURSORS.computeIfAbsent(player, ignored -> new LinkedHashMap<>());
        List<ItemStack> unbound = new ArrayList<>(UNBOUND_CURSORS.getOrDefault(player, List.of()));
        ItemStack[] before = new ItemStack[46];
        ItemStack[] after = new ItemStack[46];
        Map<UUID, ItemStack> originals = new LinkedHashMap<>(reserved);
        for (int slot = 1; slot <= 45; slot++) {
            ItemStack actual = player.inventoryMenu.getSlot(slot).getItem();
            if (actual.getItem() instanceof ExperienceBookItem) BookIdentity.validate(player.serverLevel(), actual);
            before[slot] = actual.copy();
            after[slot] = actual.copy();
            UUID id = id(actual);
            if (id != null) originals.put(id, actual.copy());
            else if (actual.getItem() instanceof ExperienceBookItem) unbound.add(actual.copy());
        }
        List<ItemStack> drops = new ArrayList<>();
        Set<Integer> changed = new HashSet<>();
        for (Edit edit : edits) {
            ItemStack stack = edit.stack();
            if (!stack.isItemEnabled(player.level().enabledFeatures())
                    || (!stack.isEmpty() && stack.getCount() > stack.getMaxStackSize())) continue;
            if (edit.slot() >= 1 && edit.slot() <= 45) {
                after[edit.slot()] = stack.copy();
                changed.add(edit.slot());
            } else if (edit.slot() < 0 && drops.size() < 10) {
                drops.add(stack.copy());
            }
        }
        Set<UUID> claimed = new HashSet<>();
        boolean[] retained = new boolean[46];
        // An unchanged original slot wins over every extra copy, regardless of slot order.
        for (int slot = 1; slot <= 45; slot++) {
            UUID identity = id(before[slot]);
            if (identity != null && identity.equals(id(after[slot])) && claimed.add(identity)) {
                after[slot] = before[slot].copy();
                retained[slot] = true;
            } else if (identity == null && before[slot].getItem() instanceof ExperienceBookItem
                    && ItemStack.isSameItemSameComponents(before[slot], after[slot])) {
                after[slot] = takeUnbound(unbound, before[slot]);
                retained[slot] = true;
            }
        }
        for (int slot = 1; slot <= 45; slot++) {
            if (!changed.contains(slot)) continue;
            if (!retained[slot]) after[slot] = resolve(player, after[slot], originals, claimed, unbound);
        }
        for (int slot : changed) {
            if (after[slot].isEmpty() || after[slot].getItem() instanceof ExperienceBookItem)
                player.inventoryMenu.getSlot(slot).setByPlayer(after[slot]);
            else player.connection.handleSetCreativeModeSlot(
                    new net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket(slot, after[slot]));
        }

        for (ItemStack drop : drops) {
            ItemStack actual = resolve(player, drop, originals, claimed, unbound);
            if (!actual.isEmpty()) {
                if (actual.getItem() instanceof ExperienceBookItem) player.drop(actual, true);
                else player.connection.handleSetCreativeModeSlot(
                        new net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket(-1, actual));
            }
        }
        // Keep only unclaimed removed books, never a second spendable copy of a live item.
        reserved.clear();
        for (var entry : originals.entrySet()) {
            if (!claimed.contains(entry.getKey()) && !BookLedger.isRevoked(player.serverLevel(), entry.getKey()))
                reserved.put(entry.getKey(), entry.getValue());
        }
        while (reserved.size() > 64) reserved.remove(reserved.keySet().iterator().next());
        UNBOUND_CURSORS.put(player, new ArrayList<>(unbound.subList(Math.max(0, unbound.size() - 64), unbound.size())));
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
    }

    private static ItemStack takeUnbound(List<ItemStack> unbound, ItemStack requested) {
        for (int i = 0; i < unbound.size(); i++)
            if (ItemStack.isSameItemSameComponents(unbound.get(i), requested)) return unbound.remove(i).copy();
        return ItemStack.EMPTY;
    }

    private static ItemStack resolve(ServerPlayer player, ItemStack requested, Map<UUID, ItemStack> originals, Set<UUID> claimed,
            List<ItemStack> unbound) {
        if (!(requested.getItem() instanceof ExperienceBookItem)) return requested;
        UUID identity = id(requested);
        if (identity == null) {
            ItemStack original = takeUnbound(unbound, requested);
            if (!original.isEmpty()) return original;
        }
        if (identity != null && !BookLedger.isRevoked(player.serverLevel(), identity)
                && originals.containsKey(identity) && claimed.add(identity)) return originals.get(identity).copy();
        return BookCloning.copy(player.serverLevel(), originals.getOrDefault(identity, requested));
    }

    /** Legacy/forged vanilla creative packets cannot introduce a second server identity. */
    public static ItemStack sanitizeSingle(ServerPlayer player, int slot, ItemStack requested) {
        if (!(requested.getItem() instanceof ExperienceBookItem) || !player.gameMode.isCreative()) return requested;
        if (slot >= 1 && slot <= 45) {
            ItemStack current = player.inventoryMenu.getSlot(slot).getItem();
            UUID identity = id(current);
            if (identity != null && identity.equals(id(requested)) && BookIdentity.validate(player.serverLevel(), current))
                return current.copy();
        }
        UUID sourceId = id(requested);
        if (sourceId != null) {
            for (var sourceSlot : player.inventoryMenu.slots) {
                ItemStack source = sourceSlot.getItem();
                if (sourceId.equals(id(source))) return BookCloning.copy(player.serverLevel(), source);
            }
            ItemStack snapshot = BookLedger.snapshot(player.serverLevel(), sourceId);
            if (!snapshot.isEmpty()) return BookCloning.copy(player.serverLevel(), snapshot);
        }
        return BookCloning.copy(player.serverLevel(), requested);
    }
}
