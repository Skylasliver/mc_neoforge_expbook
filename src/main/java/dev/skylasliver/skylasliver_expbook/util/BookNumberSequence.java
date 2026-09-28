package dev.skylasliver.skylasliver_expbook.util;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Stable, never-reused public IDs. Independent of Minecraft for persistence regression tests. */
public final class BookNumberSequence {
    private final Map<UUID, Long> numbers = new LinkedHashMap<>();
    private long lastNumber;

    public BookNumberSequence(Map<UUID, Long> saved, long savedLastNumber) {
        lastNumber = Math.max(0, savedLastNumber);
        var used = new HashSet<Long>();
        saved.forEach((id, number) -> {
            if (number > 0 && used.add(number)) {
                numbers.put(id, number);
                lastNumber = Math.max(lastNumber, number);
            }
        });
    }

    public long numberFor(UUID id) {
        Long existing = numbers.get(id);
        if (existing != null) return existing;
        long next = Math.incrementExact(lastNumber);
        numbers.put(id, next);
        lastNumber = next;
        return next;
    }

    public long lastNumber() { return lastNumber; }
    public Map<UUID, Long> snapshot() { return new LinkedHashMap<>(numbers); }
}
