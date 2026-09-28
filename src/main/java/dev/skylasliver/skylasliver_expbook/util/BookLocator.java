package dev.skylasliver.skylasliver_expbook.util;

import dev.skylasliver.skylasliver_expbook.item.ExperienceBookItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Locates the experience book the player is currently acting with. */
public final class BookLocator {

    private BookLocator() {
    }

    /**
     * Returns the book in the main hand, or the off hand when the main hand is empty.
     * A player holding a book in each hand is resolved in favour of the main hand.
     */
    public static ItemStack held(Player player) {
        validateHands(player);
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof ExperienceBookItem) {
            return main;
        }
        ItemStack off = player.getOffhandItem();
        if (player.getMainHandItem().isEmpty() && off.getItem() instanceof ExperienceBookItem) {
            return off;
        }
        return ItemStack.EMPTY;
    }

    /**
     * The book considered by the enchanting table: the main hand first, then the off
     * hand, and between two books the one holding more points.
     */
    public static ItemStack forEnchanting(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return ItemStack.EMPTY;
        }
        validateHands(player);
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        boolean mainIsBook = main.getItem() instanceof ExperienceBookItem;
        boolean offIsBook = off.getItem() instanceof ExperienceBookItem;
        if (mainIsBook && offIsBook) {
            return ExperienceBookItem.storedPoints(main) >= ExperienceBookItem.storedPoints(off) ? main : off;
        }
        if (mainIsBook) {
            return main;
        }
        if (offIsBook) {
            return off;
        }
        return ItemStack.EMPTY;
    }

    private static void validateHands(Player player) {
        if (player instanceof net.minecraft.server.level.ServerPlayer server) {
            BookIdentity.validate(server.serverLevel(), player.getMainHandItem());
            BookIdentity.validate(server.serverLevel(), player.getOffhandItem());
        }
    }
}
