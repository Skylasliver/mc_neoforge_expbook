package dev.skylasliver.expbook;

import com.mojang.authlib.GameProfile;
import dev.skylasliver.expbook.component.BookContents;
import dev.skylasliver.expbook.item.BookActions;
import dev.skylasliver.expbook.item.ExperienceBookItem;
import dev.skylasliver.expbook.menu.ExperienceBookMenu;
import dev.skylasliver.expbook.page.PageTier;
import java.util.List;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("skylasliver_expbook")
@PrefixGameTestTemplate(false)
public final class BookMenuGameTests {
    private static FakePlayer player(GameTestHelper helper) {
        return new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "BookMenuTest"));
    }

    @GameTest(template = "empty")
    public static void overfullBookAcceptsGradualExpansion(GameTestHelper helper) {
        FakePlayer player = player(helper);
        ItemStack book = ExperienceBookItem.createEmpty();
        int capacity = PageTier.LEATHER.points();
        // Represents a saved book after an administrator reduced per-page capacity.
        ExperienceBookItem.setContents(book, new BookContents(capacity * 3, List.of(PageTier.LEATHER)));
        player.getInventory().setItem(0, book);
        ExperienceBookMenu menu = new ExperienceBookMenu(1, player.getInventory(), book);
        helper.assertTrue(!menu.getSlot(0).mayPickup(player), "Cannot remove capacity from an overfull book");
        menu.setCarried(new ItemStack(PageTier.LEATHER.item(), 3));
        menu.clicked(1, 0, ClickType.PICKUP, player);
        helper.assertTrue(ExperienceBookItem.contents(book).pages().size() == 2
                && menu.getCarried().getCount() == 2, "First page is accepted while book remains overfull");
        helper.assertTrue(ExperienceBookItem.storedPoints(book) == capacity * 3, "Expansion preserves every XP point");
        menu.clicked(2, 0, ClickType.PICKUP, player);
        helper.assertTrue(ExperienceBookItem.freeSpace(book) == 0
                && !menu.getSlot(0).mayPickup(player), "Exactly full books cannot lose a required page");
        menu.clicked(3, 0, ClickType.PICKUP, player);
        menu.clicked(3, 0, ClickType.PICKUP, player);
        helper.assertTrue(ExperienceBookItem.contents(book).pages().size() == 3
                && menu.getCarried().getCount() == 1, "Surplus page can be removed without changing XP");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void invalidMenuIndicesPreserveBook(GameTestHelper helper) {
        FakePlayer player = player(helper);
        ItemStack book = ExperienceBookItem.createEmpty();
        ExperienceBookItem.setContents(book, new BookContents(123, List.of(PageTier.LEATHER)));
        player.getInventory().setItem(0, book);
        ExperienceBookMenu menu = new ExperienceBookMenu(1, player.getInventory(), book);
        helper.assertTrue(menu.quickMoveStack(player, -1).isEmpty()
                && menu.quickMoveStack(player, menu.slots.size()).isEmpty(), "Invalid quick-move indices are rejected");
        menu.clicked(0, -1, ClickType.SWAP, player);
        menu.clicked(0, Integer.MAX_VALUE, ClickType.SWAP, player);
        menu.clicked(0, 0, ClickType.SWAP, player);
        helper.assertTrue(player.getMainHandItem() == book
                && ExperienceBookItem.storedPoints(book) == 123
                && ExperienceBookItem.contents(book).pages().size() == 1, "Malformed swaps and held-book swaps preserve contents");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void partialLevelTransfersConservePoints(GameTestHelper helper) {
        FakePlayer player = player(helper);
        ItemStack book = ExperienceBookItem.createEmpty();
        ExperienceBookItem.setContents(book, new BookContents(0, List.of(PageTier.NETHER_STAR)));
        player.getInventory().setItem(0, book);
        player.giveExperiencePoints(1500);
        int initial = BookActions.visiblePoints(player);
        int moved = BookActions.storeOneLevel(player, book);
        helper.assertTrue(moved > 0 && BookActions.visiblePoints(player)
                + ExperienceBookItem.storedPoints(book) == initial, "Deposit includes partial progress without losing XP");
        BookActions.takeOneLevel(player, book);
        helper.assertTrue(BookActions.visiblePoints(player) + ExperienceBookItem.storedPoints(book) == initial,
                "One-level withdrawal conserves XP");
        BookActions.takeAll(player, book);
        helper.assertTrue(BookActions.visiblePoints(player) == initial
                && ExperienceBookItem.storedPoints(book) == 0, "Complete withdrawal restores the original balance");
        helper.succeed();
    }
}
