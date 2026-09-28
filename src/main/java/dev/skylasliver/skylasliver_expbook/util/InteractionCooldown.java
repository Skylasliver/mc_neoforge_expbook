package dev.skylasliver.skylasliver_expbook.util;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Spacing between repeated book transfers. The client re-samples the held key every
 * tick, so this window turns a long press into a steady repeat instead of one action;
 * it also keeps a single click from double firing. Kept client-side; the server bounds
 * every transfer by the points actually stored in the book.
 */
public final class InteractionCooldown {

    private static final long COOLDOWN_NANOS = 150_000_000L;

    private static final Map<UUID, Long> LAST_ACTION = new WeakHashMap<>();

    private InteractionCooldown() {
    }

    /** @return true when the action may run, in which case the timestamp is refreshed. */
    public static boolean tryAcquire(UUID playerId) {
        long now = System.nanoTime();
        Long last = LAST_ACTION.get(playerId);
        if (last != null && now - last < COOLDOWN_NANOS) {
            return false;
        }
        LAST_ACTION.put(playerId, now);
        return true;
    }

}
