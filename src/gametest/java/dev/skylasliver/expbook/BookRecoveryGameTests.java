package dev.skylasliver.expbook;

import com.mojang.authlib.GameProfile;
import dev.skylasliver.expbook.component.BookContents;
import dev.skylasliver.expbook.item.BookActions;
import dev.skylasliver.expbook.item.BookAutomation;
import dev.skylasliver.expbook.item.ExperienceBookItem;
import dev.skylasliver.expbook.menu.ExperienceBookMenu;
import dev.skylasliver.expbook.page.PageTier;
import dev.skylasliver.expbook.registry.ModComponents;
import dev.skylasliver.expbook.util.BookIdentity;
import dev.skylasliver.expbook.util.BookLedger;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("skylasliver_expbook")
@PrefixGameTestTemplate(false)
public final class BookRecoveryGameTests {
    private static final class Admin extends FakePlayer {
        Admin(ServerLevel level) { super(level, new GameProfile(UUID.randomUUID(), "RecoveryTest")); }
        @Override public boolean hasPermissions(int level) { return true; }
    }

    /** Register only test actors, and always undo registration before the test returns. */
    private static final class Online implements AutoCloseable {
        private final Admin player;
        private final java.util.List<net.minecraft.server.level.ServerPlayer> players;
        private final java.util.Map<UUID, net.minecraft.server.level.ServerPlayer> byId;
        @SuppressWarnings("unchecked")
        Online(Admin player) throws Exception {
            this.player = player;
            var listField = net.minecraft.server.players.PlayerList.class.getDeclaredField("players");
            var mapField = net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");
            listField.setAccessible(true);
            mapField.setAccessible(true);
            players = (java.util.List<net.minecraft.server.level.ServerPlayer>) listField.get(player.server.getPlayerList());
            byId = (java.util.Map<UUID, net.minecraft.server.level.ServerPlayer>) mapField.get(player.server.getPlayerList());
            players.add(player);
            byId.put(player.getUUID(), player);
        }
        public void close() { players.remove(player); byId.remove(player.getUUID()); }
    }

    private static ItemStack book(Admin player) {
        ItemStack stack = ExperienceBookItem.createEmpty();
        ExperienceBookItem.setContents(stack, new BookContents(120, List.of(PageTier.values()[0])));
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Recovery payload"));
        stack.set(ModComponents.AUTOMATION.get(), 7);
        stack.set(ModComponents.REPAIR_CREDIT.get(), 1);
        player.getInventory().setItem(0, stack);
        BookIdentity.ensureBound(player, stack);
        return stack;
    }

    @GameTest(template = "empty")
    public static void adminTakeClearsOldAndBindsOnUse(GameTestHelper helper) throws Exception {
        Admin admin = new Admin(helper.getLevel());
        Online online = new Online(admin);
        try {
            ItemStack old = book(admin);
            UUID oldId = old.get(ModComponents.BOOK_ID.get());
            long oldNumber = old.get(ModComponents.BOOK_NUMBER.get());
            ItemStack hiddenCopy = old.copy();
            ItemStack netherCopy = old.copy();
            var openMenu = new ExperienceBookMenu(1, admin.getInventory(), old);
            ItemStack replacement = BookLedger.recover(admin, oldId, BookLedger.Delivery.ADMIN);
            helper.assertTrue(!replacement.isEmpty(), "Admin delivery must succeed");
            helper.assertTrue(old.isEmpty() && old.getComponents().keySet().isEmpty(), "All old components and item must be erased");
            helper.assertTrue(!openMenu.stillValid(admin), "An already-open old book menu must be invalid");
            helper.assertTrue(replacement.get(ModComponents.OWNER_UUID.get()) == null
                    && replacement.get(ModComponents.OWNER_NAME.get()) == null, "Receiving must not bind an owner");
            helper.assertTrue(ExperienceBookItem.storedPoints(replacement) == 120
                    && ExperienceBookItem.contents(replacement).pages().size() == 1
                    && replacement.get(DataComponents.CUSTOM_NAME) != null, "Replacement preserves contents and custom components");
            helper.assertTrue(replacement.get(ModComponents.BOOK_NUMBER.get()) > oldNumber, "Replacement gets a fresh numeric ID");
            helper.assertTrue(BookLedger.find(helper.getLevel(), oldId).snapshot().isEmpty(), "Tombstone must not retain the old payload");
            helper.assertTrue(BookLedger.recover(admin, oldId, BookLedger.Delivery.ADMIN).isEmpty(), "Repeated recovery must not duplicate");
            helper.assertTrue(BookActions.takeAll(admin, hiddenCopy) == 0 && hiddenCopy.isEmpty(), "Hidden original cannot withdraw XP");
            ServerLevel nether = admin.server.getLevel(net.minecraft.world.level.Level.NETHER);
            helper.assertTrue(nether != null && !BookIdentity.validate(nether, netherCopy)
                    && netherCopy.isEmpty(), "Revocation must apply across dimensions");
            UUID newId = replacement.get(ModComponents.BOOK_ID.get());
            helper.assertTrue(BookLedger.find(helper.getLevel(), newId) == null, "Unused replacement must not appear in an owner's ledger");
            Admin user = new Admin(helper.getLevel());
            user.getInventory().setItem(0, replacement);
            helper.assertTrue(BookActions.takeOneLevel(user, replacement) > 0, "First actual use works");
            helper.assertTrue(user.getUUID().equals(replacement.get(ModComponents.OWNER_UUID.get())), "First user becomes owner");
            helper.assertTrue(BookLedger.find(helper.getLevel(), newId).owner().equals(user.getUUID()), "First user owns the ledger record");
            helper.succeed();
        } finally {
            online.close();
        }
    }

    @GameTest(template = "empty")
    public static void onlineOwnerDeliveryPurgesBeforeGiving(GameTestHelper helper) throws Exception {
        Admin owner = new Admin(helper.getLevel());
        Admin admin = new Admin(helper.getLevel());
        try (Online online = new Online(owner)) {
            ItemStack old = book(owner);
            UUID id = old.get(ModComponents.BOOK_ID.get());
            ItemStack newBook = BookLedger.recover(admin, id, BookLedger.Delivery.OWNER);
            helper.assertTrue(old.isEmpty(), "Online original must be erased during recovery");
            helper.assertTrue(admin.getInventory().isEmpty(), "Owner delivery must not also give the admin a copy");
            ItemStack received = owner.getInventory().getItem(0);
            helper.assertTrue(!received.isEmpty() && received.get(ModComponents.BOOK_ID.get()).equals(newBook.get(ModComponents.BOOK_ID.get())),
                    "Replacement must arrive in the owner's freed slot");
            helper.assertTrue(received.get(ModComponents.OWNER_UUID.get()) == null, "Online receiving is not ownership");
            helper.assertTrue(BookLedger.deliverPending(owner) == 0, "Online delivery must not queue a duplicate");
            helper.succeed();
        }
    }

    @GameTest(template = "empty")
    public static void offlineDeliveryIsUnboundAndSingleUse(GameTestHelper helper) {
        Admin owner = new Admin(helper.getLevel());
        ItemStack old = book(owner);
        UUID id = old.get(ModComponents.BOOK_ID.get());
        Admin admin = new Admin(helper.getLevel());
        ItemStack issued = BookLedger.recover(admin, id, BookLedger.Delivery.OWNER);
        helper.assertTrue(!issued.isEmpty(), "Offline reissue must succeed");
        BookIdentity.purgeRevoked(owner);
        helper.assertTrue(old.isEmpty(), "Login cleanup must erase the offline original");
        helper.assertTrue(BookLedger.deliverPending(owner) == 1, "Exactly one pending replacement must arrive");
        helper.assertTrue(BookLedger.deliverPending(owner) == 0, "Pending delivery cannot repeat");
        ItemStack delivered = owner.getInventory().getItem(0);
        helper.assertTrue(!delivered.isEmpty() && delivered.get(ModComponents.OWNER_UUID.get()) == null,
                "Offline delivery remains unbound");
        BookIdentity.ensureBound(owner, delivered);
        helper.assertTrue(owner.getUUID().equals(delivered.get(ModComponents.OWNER_UUID.get())), "First use binds delivered book");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void revokedAutomationAndForgedRecoveryAreBlocked(GameTestHelper helper) {
        Admin admin = new Admin(helper.getLevel());
        ItemStack old = book(admin);
        UUID id = old.get(ModComponents.BOOK_ID.get());
        ItemStack copy = old.copy();
        BookLedger.recover(admin, id, BookLedger.Delivery.ADMIN);
        // Put the old copy in the only occupied slot; collection must not write any XP to it.
        admin.getInventory().clearContent();
        admin.getInventory().setItem(0, copy);
        helper.assertTrue(BookAutomation.collect(admin, 10) == 10 && copy.isEmpty(), "Revoked auto-collection must not consume XP");
        ItemStack unbound = ExperienceBookItem.createEmpty();
        admin.getInventory().setItem(0, unbound);
        unbound.getItem().inventoryTick(unbound, helper.getLevel(), admin, 0, true);
        helper.assertTrue(unbound.get(ModComponents.OWNER_UUID.get()) == null, "Holding an unused book must not bind it");
        BookIdentity.ensureBound(admin, unbound);
        FakePlayer guest = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "GuestTest"));
        helper.assertTrue(BookLedger.recover(guest, unbound.get(ModComponents.BOOK_ID.get()), BookLedger.Delivery.ADMIN).isEmpty(),
                "Non-admin callers cannot recover books");
        helper.succeed();
    }
}
