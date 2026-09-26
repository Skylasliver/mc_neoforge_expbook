package dev.skylasliver.expbook.util;

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

    public static final long COOLDOWN_MS = 150L;

    private static final Map<UUID, Long> LAST_ACTION = new WeakHashMap<>();

    private InteractionCooldown() {
    }

    /** @return true when the action may run, in which case the timestamp is refreshed. */
    public static boolean tryAcquire(UUID playerId) {
        long now = System.currentTimeMillis();
        Long last = LAST_ACTION.get(playerId);
        if (last != null && now - last < COOLDOWN_MS) {
            return false;
        }
        LAST_ACTION.put(playerId, now);
        return true;
    }

    /** @return true when the window has elapsed, without refreshing it. */
    public static boolean isReady(UUID playerId) {
        Long last = LAST_ACTION.get(playerId);
        return last == null || System.currentTimeMillis() - last >= COOLDOWN_MS;
    }

    /** Marks the window as freshly used, e.g. after a right-click store. */
    public static void touch(UUID playerId) {
        LAST_ACTION.put(playerId, System.currentTimeMillis());
    }

    public static void clear(UUID playerId) {
        LAST_ACTION.remove(playerId);
    }
}
