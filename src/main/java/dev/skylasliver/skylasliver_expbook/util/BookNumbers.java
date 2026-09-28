package dev.skylasliver.skylasliver_expbook.util;

import dev.skylasliver.skylasliver_expbook.registry.ModComponents;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

/** One sequence per save, shared by every player and dimension. UUIDs remain internal. */
public final class BookNumbers extends SavedData {
    private static final String DATA_NAME = "skylasliver_expbook_numbers";
    private BookNumberSequence sequence = new BookNumberSequence(Map.of(), 0);
    private boolean seeded;

    private static BookNumbers load(CompoundTag tag, HolderLookup.Provider provider) {
        BookNumbers data = new BookNumbers();
        Map<UUID, Long> saved = new LinkedHashMap<>();
        CompoundTag numbers = tag.getCompound("numbers");
        for (String key : numbers.getAllKeys()) {
            try { saved.put(UUID.fromString(key), numbers.getLong(key)); }
            catch (IllegalArgumentException ignored) { }
        }
        data.sequence = new BookNumberSequence(saved, tag.getLong("lastNumber"));
        return data;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag numbers = new CompoundTag();
        sequence.snapshot().forEach((id, number) -> numbers.putLong(id.toString(), number));
        tag.put("numbers", numbers);
        tag.putLong("lastNumber", sequence.lastNumber());
        return tag;
    }

    private long numberFor(UUID id) {
        long before = sequence.lastNumber();
        long number = sequence.numberFor(id);
        if (before != sequence.lastNumber()) setDirty();
        return number;
    }

    private static BookNumbers get(ServerLevel level) {
        BookNumbers data = level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(BookNumbers::new, BookNumbers::load, null), DATA_NAME);
        if (!data.seeded) {
            data.seeded = true;
            // Migrate saved ledger records only; no block, entity or container scanning.
            for (ServerLevel dimension : level.getServer().getAllLevels())
                for (UUID id : BookLedger.recordedIds(dimension)) data.numberFor(id);
        }
        return data;
    }

    public static void sync(ServerLevel level, ItemStack book, UUID id) {
        long number = get(level).numberFor(id);
        if (book.getOrDefault(ModComponents.BOOK_NUMBER.get(), 0L) != number)
            book.set(ModComponents.BOOK_NUMBER.get(), number);
    }
}
