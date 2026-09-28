package dev.skylasliver.skylasliver_expbook.util;

import dev.skylasliver.skylasliver_expbook.registry.ModComponents;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

public final class BookLedger extends SavedData {
    public enum Delivery { OWNER, ADMIN }
    public record Entry(UUID id, UUID owner, String ownerName, String state, CompoundTag snapshot, UUID replacement) {}
    private final Map<UUID, Entry> entries = new LinkedHashMap<>();
    private final Map<UUID, List<CompoundTag>> pending = new LinkedHashMap<>();
    private static final String DATA_NAME = "skylasliver_expbook_ledger";

    private BookLedger() {}

    private static BookLedger load(CompoundTag tag, HolderLookup.Provider provider) {
        BookLedger ledger = new BookLedger();
        for (int i = 0; i < tag.getInt("count"); i++) {
            CompoundTag e = tag.getCompound("e" + i);
            try {
                UUID id = UUID.fromString(e.getString("id"));
                UUID replacement = e.contains("replacement") ? UUID.fromString(e.getString("replacement")) : null;
                ledger.entries.put(id, new Entry(id, UUID.fromString(e.getString("owner")), e.getString("name"),
                        e.getString("state"), "ACTIVE".equals(e.getString("state"))
                        ? e.getCompound("snapshot").copy() : new CompoundTag(), replacement));
            } catch (IllegalArgumentException ignored) { }
        }
        for (int i = 0; i < tag.getInt("pendingCount"); i++) {
            CompoundTag p = tag.getCompound("p" + i);
            try {
                ledger.pending.computeIfAbsent(UUID.fromString(p.getString("owner")), k -> new ArrayList<>())
                        .add(p.getCompound("stack").copy());
            } catch (IllegalArgumentException ignored) { }
        }
        return ledger;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putInt("count", entries.size());
        int index = 0;
        for (Entry e : entries.values()) {
            CompoundTag n = new CompoundTag();
            n.putString("id", e.id().toString());
            n.putString("owner", e.owner().toString());
            n.putString("name", e.ownerName());
            n.putString("state", e.state());
            if (!e.snapshot().isEmpty()) n.put("snapshot", e.snapshot().copy());
            if (e.replacement() != null) n.putString("replacement", e.replacement().toString());
            tag.put("e" + index++, n);
        }
        index = 0;
        for (var delivery : pending.entrySet()) for (CompoundTag stack : delivery.getValue()) {
            CompoundTag p = new CompoundTag();
            p.putString("owner", delivery.getKey().toString());
            p.put("stack", stack.copy());
            tag.put("p" + index++, p);
        }
        tag.putInt("pendingCount", index);
        return tag;
    }

    private static BookLedger local(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(BookLedger::new, BookLedger::load, null), DATA_NAME);
    }

    private static BookLedger global(ServerLevel level) { return local(level.getServer().overworld()); }

    /** Include pre-upgrade dimension ledgers. A tombstone always wins over an active record. */
    public static Entry find(ServerLevel level, UUID id) {
        Entry found = global(level).entries.get(id);
        if (found != null && !"ACTIVE".equals(found.state())) return found;
        for (ServerLevel dimension : level.getServer().getAllLevels()) {
            Entry candidate = local(dimension).entries.get(id);
            if (candidate == null) continue;
            if (!"ACTIVE".equals(candidate.state())) return candidate;
            if (found == null) found = candidate;
        }
        return found;
    }

    public static Map<UUID, Entry> all(ServerLevel level) {
        Map<UUID, Entry> result = new LinkedHashMap<>(global(level).entries);
        for (ServerLevel dimension : level.getServer().getAllLevels())
            local(dimension).entries.forEach((id, entry) -> {
                if (!result.containsKey(id) || !"ACTIVE".equals(entry.state())) result.put(id, entry);
            });
        return result;
    }

    public static List<UUID> recordedIds(ServerLevel level) { return List.copyOf(local(level).entries.keySet()); }

    public static Map<UUID, Entry> activeFor(ServerLevel level, UUID owner) {
        Map<UUID, Entry> result = new LinkedHashMap<>();
        all(level).forEach((id, entry) -> {
            if (entry.owner().equals(owner) && "ACTIVE".equals(entry.state())) result.put(id, entry);
        });
        return result;
    }

    public static void record(ServerPlayer player, ItemStack stack, UUID id, UUID owner) {
        if (stack.isEmpty() || isRevoked(player.serverLevel(), id)) return;
        BookNumbers.sync(player.serverLevel(), stack, id);
        BookLedger ledger = global(player.serverLevel());
        String name = stack.getOrDefault(ModComponents.OWNER_NAME.get(), player.getGameProfile().getName());
        CompoundTag saved = (CompoundTag) stack.save(player.registryAccess());
        Entry next = new Entry(id, owner, name, "ACTIVE", saved, null);
        if (!next.equals(ledger.entries.get(id))) {
            ledger.entries.put(id, next);
            ledger.setDirty();
        }
    }

    public static ItemStack snapshot(ServerLevel level, UUID id) {
        Entry e = find(level, id);
        if (e == null || !"ACTIVE".equals(e.state())) return ItemStack.EMPTY;
        ItemStack stack = ItemStack.parse(level.registryAccess(), e.snapshot()).orElse(ItemStack.EMPTY);
        if (!stack.isEmpty()) BookNumbers.sync(level, stack, id);
        return stack;
    }

    public static boolean isActive(ServerLevel level, UUID id) {
        Entry e = find(level, id);
        return e != null && "ACTIVE".equals(e.state());
    }

    public static boolean isRevoked(ServerLevel level, UUID id) {
        Entry e = find(level, id);
        return e != null && !"ACTIVE".equals(e.state());
    }

    /** Server-thread transaction: snapshot, revoke every old identity, then deliver one unbound book. */
    public static ItemStack recover(ServerPlayer admin, UUID id, Delivery delivery) {
        if (!admin.hasPermissions(2)) return ItemStack.EMPTY;
        ServerLevel level = admin.serverLevel();
        Entry entry = find(level, id);
        if (entry == null || !"ACTIVE".equals(entry.state())) return ItemStack.EMPTY;
        // Prefer the current contents when an online player still holds the original.
        for (ServerPlayer player : admin.server.getPlayerList().getPlayers())
            BookIdentity.recordCarried(player, id);
        entry = find(level, id);
        ItemStack replacement = snapshot(level, id);
        if (replacement.isEmpty()) return ItemStack.EMPTY;

        UUID newId = UUID.randomUUID();
        replacement.set(ModComponents.BOOK_ID.get(), newId);
        replacement.remove(ModComponents.OWNER_UUID.get());
        replacement.remove(ModComponents.OWNER_NAME.get());
        BookNumbers.sync(level, replacement, newId);
        CompoundTag saved = (CompoundTag) replacement.save(level.registryAccess());
        Entry tombstone = new Entry(id, entry.owner(), entry.ownerName(), "REPLACED", new CompoundTag(), newId);
        BookLedger ledger = global(level);
        ledger.entries.put(id, tombstone);
        ledger.setDirty();
        // Remove old payload data even from legacy dimension records.
        for (ServerLevel dimension : admin.server.getAllLevels()) {
            BookLedger legacy = local(dimension);
            if (legacy.entries.containsKey(id)) {
                legacy.entries.put(id, tombstone);
                legacy.setDirty();
            }
        }
        for (ServerPlayer player : admin.server.getPlayerList().getPlayers())
            BookIdentity.purgeRevoked(player);

        ServerPlayer recipient = delivery == Delivery.ADMIN ? admin : admin.server.getPlayerList().getPlayer(entry.owner());
        if (recipient != null) give(recipient, replacement.copy());
        else {
            ledger.pending.computeIfAbsent(entry.owner(), ignored -> new ArrayList<>()).add(saved);
            ledger.setDirty();
        }
        // No owner/active ledger entry until somebody actually uses the replacement.
        return replacement;
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
    }

    public static int deliverPending(ServerPlayer player) {
        int delivered = 0;
        for (ServerLevel dimension : player.server.getAllLevels()) {
            BookLedger ledger = local(dimension);
            List<CompoundTag> list = ledger.pending.remove(player.getUUID());
            if (list == null) continue;
            ledger.setDirty();
            for (CompoundTag saved : list) {
                ItemStack stack = ItemStack.parse(player.registryAccess(), saved).orElse(ItemStack.EMPTY);
                if (stack.isEmpty() || !BookIdentity.validate(player.serverLevel(), stack)) continue;
                // Includes deliveries queued by older versions.
                stack.remove(ModComponents.OWNER_UUID.get());
                stack.remove(ModComponents.OWNER_NAME.get());
                UUID id = stack.get(ModComponents.BOOK_ID.get());
                if (id != null) {
                    for (ServerLevel oldDimension : player.server.getAllLevels()) {
                        BookLedger old = local(oldDimension);
                        if (old.entries.remove(id) != null) old.setDirty();
                    }
                }
                give(player, stack);
                delivered++;
            }
        }
        return delivered;
    }
}
