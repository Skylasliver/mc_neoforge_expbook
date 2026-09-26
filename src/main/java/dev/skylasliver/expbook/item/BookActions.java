package dev.skylasliver.expbook.item;

import dev.skylasliver.expbook.ExperienceMath;
import dev.skylasliver.expbook.component.BookContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The store/take operations shared by the instant interactions and the book GUI.
 * Every method works on raw points so no partial level is ever rounded away, and
 * the book is never consumed or damaged.
 */
public final class BookActions {

    private BookActions() {
    }

    /**
     * Withdraw exactly enough to complete the player's current level.
     * Withdraws what is available when the book cannot cover the gap.
     *
     * @return points actually transferred.
     */
    public static int takeOneLevel(Player player, ItemStack book) {
        if (player.level().isClientSide()) {
            return 0;
        }
        int wanted = player.getXpNeededForNextLevel() - Math.min(player.getXpNeededForNextLevel() - 1, Math.round(player.experienceProgress * player.getXpNeededForNextLevel()));
        int available = ExperienceBookItem.storedPoints(book);
        int taken = Math.min(wanted, available);
        if (taken <= 0) {
            return 0;
        }
        ExperienceBookItem.setContents(book, ExperienceBookItem.contents(book)
                .withStoredPoints(available - taken));
        player.giveExperiencePoints(taken);
        return taken;
    }

    /**
     * Withdraw the entire book. The book itself is kept.
     *
     * @return points actually transferred.
     */
    public static int takeAll(Player player, ItemStack book) {
        if (player.level().isClientSide()) {
            return 0;
        }
        int available = ExperienceBookItem.storedPoints(book);
        if (available <= 0) {
            return 0;
        }
        ExperienceBookItem.setContents(book, ExperienceBookItem.contents(book).withStoredPoints(0));
        player.giveExperiencePoints(available);
        return available;
    }

    /**
     * Deposit one whole level: every point the player holds above the floor of the
     * previous level, the partial progress included, so a single right click always
     * drops them by exactly one level. Below level 1 there is no whole level to hand
     * over, so the little they have is deposited instead; deposits always stop at the
     * book's capacity and never take more than the player actually holds.
     *
     * <p>This is the counterpart of {@link #takeOneLevel}, which hands back exactly the
     * points needed to complete the level the player is standing on.
     *
     * @return points actually transferred.
     */
    public static int storeOneLevel(Player player, ItemStack book) {
        if (player.level().isClientSide()) {
            return 0;
        }
        int wanted;
        if (player.experienceLevel > 0) {
            int floor = ExperienceMath.totalPointsForLevel(player.experienceLevel - 1);
            wanted = visiblePoints(player) - floor;
        } else {
            wanted = visiblePoints(player);
        }
        return storePoints(player, book, wanted);
    }

    /**
     * Deposit the player's whole experience bar, stopping at capacity.
     *
     * @return points actually transferred.
     */
    public static int storeAll(Player player, ItemStack book) {
        if (player.level().isClientSide()) {
            return 0;
        }
        return storePoints(player, book, visiblePoints(player));
    }

    private static int storePoints(Player player, ItemStack book, int wanted) {
        int space = ExperienceBookItem.freeSpace(book);
        int moved = Math.min(Math.min(wanted, space), visiblePoints(player));
        if (moved <= 0) {
            return 0;
        }
        BookContents contents = ExperienceBookItem.contents(book);
        ExperienceBookItem.setContents(book, contents.withStoredPoints(contents.storedPoints() + moved));
        removePlayerPoints(player, moved);
        return moved;
    }

    /**
     * Removes raw points without the level-change side effects of
     * {@code giveExperienceLevels(-n)}, so partial progress survives.
     */
    public static int visiblePoints(Player player) {
        return (int) Math.min(Integer.MAX_VALUE, (long) ExperienceMath.totalPointsForLevel(player.experienceLevel)
                + Math.round(player.experienceProgress * player.getXpNeededForNextLevel()));
    }

    private static void removePlayerPoints(Player player, int points) {
        int remaining = Math.max(0, visiblePoints(player) - points);
        player.totalExperience = remaining;
        player.experienceLevel = ExperienceMath.levelForPoints(remaining);
        player.experienceProgress = ExperienceMath.progress(remaining);
    }
}
