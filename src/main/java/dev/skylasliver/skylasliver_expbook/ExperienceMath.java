package dev.skylasliver.skylasliver_expbook;

/**
 * Vanilla experience formulas expressed on the raw point scale, so partial levels
 * survive every conversion. Player balances use the visible level and progress.
 */
public final class ExperienceMath {

    /** Highest complete vanilla level representable with signed integer XP points. */
    public static final int LEVEL_CAP = 21863;

    private static final int[] CUMULATIVE = new int[LEVEL_CAP + 1];

    static {
        int total = 0;
        for (int level = 0; level < LEVEL_CAP; level++) {
            CUMULATIVE[level] = total;
            total += playerXpNeededForNextLevel(level);
        }
        CUMULATIVE[LEVEL_CAP] = total;
    }

    private ExperienceMath() {
    }

    /** Points required to advance from {@code level} to {@code level + 1}. */
    public static int playerXpNeededForNextLevel(int level) {
        if (level >= 30) {
            return 112 + (level - 30) * 9;
        }
        if (level >= 15) {
            return 37 + (level - 15) * 5;
        }
        return 7 + level * 2;
    }

    /** Total points held at exactly {@code level} with no partial progress. */
    public static int totalPointsForLevel(int level) {
        if (level <= 0) {
            return 0;
        }
        if (level >= LEVEL_CAP) {
            return CUMULATIVE[LEVEL_CAP];
        }
        return CUMULATIVE[level];
    }

    /** Highest level whose cumulative cost the point total satisfies. */
    public static int levelForPoints(long points) {
        if (points <= 0) {
            return 0;
        }
        int low = 0;
        int high = LEVEL_CAP;
        while (low < high) {
            int mid = (low + high + 1) >>> 1;
            if (totalPointsForLevel(mid) <= points) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return low;
    }

    /** Points already spent into the current, incomplete level. */
    public static int progressPoints(long points) {
        return (int) (points - totalPointsForLevel(levelForPoints(points)));
    }

    /** Fraction of the current level already filled, in {@code [0, 1)}. */
    public static float progress(long points) {
        int level = levelForPoints(points);
        int need = playerXpNeededForNextLevel(level);
        if (need <= 0) {
            return 0.0F;
        }
        return Math.min(0.999999F, (float) progressPoints(points) / (float) need);
    }

    /**
     * Points that complete the current level. "Take one level" withdraws that much;
     * deposits instead return the player to the previous level's floor.
     */
    public static int pointsToCompleteCurrentLevel(long points) {
        int level = levelForPoints(points);
        return playerXpNeededForNextLevel(level) - progressPoints(points);
    }
}
