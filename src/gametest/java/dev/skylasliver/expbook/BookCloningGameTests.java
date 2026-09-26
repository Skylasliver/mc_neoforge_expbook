package dev.skylasliver.expbook;

import com.mojang.authlib.GameProfile;
import dev.skylasliver.expbook.component.BookContents;
import dev.skylasliver.expbook.item.ExperienceBookItem;
import dev.skylasliver.expbook.page.PageTier;
import dev.skylasliver.expbook.registry.ModComponents;
import dev.skylasliver.expbook.util.BookIdentity;
import dev.skylasliver.expbook.util.CreativeBookEdits;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("skylasliver_expbook")
@PrefixGameTestTemplate(false)
public final class BookCloningGameTests {
    private static final class CreativePlayer extends FakePlayer {
        ItemStack dropped = ItemStack.EMPTY;
        CreativePlayer(ServerLevel level) {
            super(level, new GameProfile(UUID.randomUUID(), "CloneTest"));
            setGameMode(GameType.CREATIVE);
        }
        @Override public ItemEntity drop(ItemStack stack, boolean scatter) {
            dropped = stack.copy();
            return null;
        }
    }

    private static ItemStack original(CreativePlayer player) {
        ItemStack book = ExperienceBookItem.createEmpty();
        ExperienceBookItem.setContents(book, new BookContents(123, List.of(PageTier.values()[0])));
        book.set(DataComponents.CUSTOM_NAME, Component.literal("Keep original"));
        book.set(ModComponents.AUTOMATION.get(), 7);
        book.enchant(player.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.MENDING), 1);
        player.getInventory().setItem(0, book);
        BookIdentity.ensureBound(player, book);
        return book;
    }

    private static void fresh(GameTestHelper helper, ItemStack copy, ItemStack original) {
        helper.assertTrue(!copy.isEmpty() && ExperienceBookItem.contents(copy).equals(ExperienceBookItem.contents(original)),
                "Clone must inherit XP and every page");
        helper.assertTrue(copy.get(DataComponents.ENCHANTMENTS).equals(original.get(DataComponents.ENCHANTMENTS)),
                "Clone must inherit enchantments");
        helper.assertTrue(java.util.Objects.equals(copy.get(DataComponents.CUSTOM_NAME), original.get(DataComponents.CUSTOM_NAME))
                && copy.getOrDefault(ModComponents.AUTOMATION.get(), 0).equals(original.getOrDefault(ModComponents.AUTOMATION.get(), 0)),
                "Copy preserves other content components");
        helper.assertTrue(copy.get(ModComponents.OWNER_UUID.get()) == null && copy.get(ModComponents.OWNER_NAME.get()) == null,
                "Clone remains unbound");
        helper.assertTrue(copy.get(ModComponents.BOOK_ID.get()) != null
                && !copy.get(ModComponents.BOOK_ID.get()).equals(original.get(ModComponents.BOOK_ID.get())), "Clone needs new UUID");
        helper.assertTrue(copy.get(ModComponents.BOOK_NUMBER.get()) > original.get(ModComponents.BOOK_NUMBER.get()), "Clone needs next numeric ID");
    }

    @GameTest(template = "empty")
    public static void middleClickCopiesContentWithoutOwner(GameTestHelper helper) {
        CreativePlayer player = new CreativePlayer(helper.getLevel());
        ItemStack original = original(player);
        ItemStack saved = original.copy();
        player.inventoryMenu.clicked(36, 2, ClickType.CLONE, player);
        ItemStack copy = player.inventoryMenu.getCarried();
        fresh(helper, copy, original);
        UUID copyId = copy.get(ModComponents.BOOK_ID.get());
        CreativePlayer firstUser = new CreativePlayer(helper.getLevel());
        BookIdentity.ensureBound(firstUser, copy);
        helper.assertTrue(firstUser.getUUID().equals(copy.get(ModComponents.OWNER_UUID.get()))
                && copyId.equals(copy.get(ModComponents.BOOK_ID.get())), "First user binds the clone without changing its ID");
        helper.assertTrue(ItemStack.isSameItemSameComponents(saved, player.getInventory().getItem(0)), "Cloning and binding must preserve original");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void creativeCopyAndForgedPacketCannotShareIdentity(GameTestHelper helper) {
        CreativePlayer player = new CreativePlayer(helper.getLevel());
        ItemStack original = original(player);
        ItemStack saved = original.copy();
        CreativeBookEdits.apply(player, List.of(new CreativeBookEdits.Edit(37, original.copy())));
        ItemStack copy = player.getInventory().getItem(1);
        fresh(helper, copy, saved);
        helper.assertTrue(ItemStack.isSameItemSameComponents(saved, player.getInventory().getItem(0)), "Original must survive creative copy");
        ItemStack forged = CreativeBookEdits.sanitizeSingle(player, 38, saved.copy());
        fresh(helper, forged, saved);
        helper.assertTrue(!forged.get(ModComponents.BOOK_ID.get()).equals(copy.get(ModComponents.BOOK_ID.get())), "Repeated clones need distinct IDs");
        CreativeBookEdits.forget(player);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void moveSwapAndCursorPreserveContents(GameTestHelper helper) {
        CreativePlayer player = new CreativePlayer(helper.getLevel());
        ItemStack a = original(player).copy();
        // Destination-first is the packet order that a naive duplicate check destroys.
        CreativeBookEdits.apply(player, List.of(new CreativeBookEdits.Edit(37, a.copy()), new CreativeBookEdits.Edit(36, ItemStack.EMPTY)));
        helper.assertTrue(player.getInventory().getItem(0).isEmpty()
                && ItemStack.isSameItemSameComponents(a, player.getInventory().getItem(1)), "Destination-first move keeps ID and contents");
        CreativeBookEdits.apply(player, List.of(new CreativeBookEdits.Edit(37, ItemStack.EMPTY)));
        CreativeBookEdits.apply(player, List.of(new CreativeBookEdits.Edit(38, a.copy())));
        helper.assertTrue(ItemStack.isSameItemSameComponents(a, player.getInventory().getItem(2)), "Cursor movement across transactions preserves original");
        ItemStack b = original(player).copy();
        CreativeBookEdits.apply(player, List.of(new CreativeBookEdits.Edit(36, a.copy()), new CreativeBookEdits.Edit(38, b.copy())));
        helper.assertTrue(ItemStack.isSameItemSameComponents(a, player.getInventory().getItem(0))
                && ItemStack.isSameItemSameComponents(b, player.getInventory().getItem(2)), "Swap preserves both originals");
        CreativeBookEdits.forget(player);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dropAndPickupPreserveOriginal(GameTestHelper helper) {
        CreativePlayer player = new CreativePlayer(helper.getLevel());
        ItemStack saved = original(player).copy();
        CreativeBookEdits.apply(player, List.of(new CreativeBookEdits.Edit(-1, saved.copy()), new CreativeBookEdits.Edit(36, ItemStack.EMPTY)));
        helper.assertTrue(ItemStack.isSameItemSameComponents(saved, player.dropped), "Drop preserves original data");
        player.getInventory().add(player.dropped.copy());
        BookIdentity.ensureBound(player, player.getInventory().getItem(0));
        helper.assertTrue(ItemStack.isSameItemSameComponents(saved, player.getInventory().getItem(0)), "Pickup does not renumber");
        CreativeBookEdits.apply(player, List.of(new CreativeBookEdits.Edit(-1, saved.copy())));
        fresh(helper, player.dropped, saved);
        CreativeBookEdits.forget(player);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void unusedBookMovesWithoutBinding(GameTestHelper helper) {
        CreativePlayer player = new CreativePlayer(helper.getLevel());
        ItemStack unused = ExperienceBookItem.createEmpty();
        ExperienceBookItem.setContents(unused, new BookContents(12, List.of(PageTier.values()[0])));
        player.getInventory().setItem(0, unused);
        CreativeBookEdits.apply(player, List.of(new CreativeBookEdits.Edit(36, ItemStack.EMPTY)));
        CreativeBookEdits.apply(player, List.of(new CreativeBookEdits.Edit(37, unused.copy())));
        helper.assertTrue(ItemStack.isSameItemSameComponents(unused, player.getInventory().getItem(1)), "Moving an unbound book preserves its data");
        helper.assertTrue(player.getInventory().getItem(1).get(ModComponents.OWNER_UUID.get()) == null, "Moving does not bind");
        CreativeBookEdits.forget(player);
        helper.succeed();
    }
}
