package dev.skylasliver.expbook.menu;

import dev.skylasliver.expbook.BookConfig;
import dev.skylasliver.expbook.item.BookAutomation;
import dev.skylasliver.expbook.registry.ModComponents;
import dev.skylasliver.expbook.registry.ModMenus;
import dev.skylasliver.expbook.util.BookLocator;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

/** Button packets are bound to this live menu and the exact held stack. */
public final class BookSettingsMenu extends AbstractContainerMenu {
    private final ItemStack book;
    private final DataSlot flags = DataSlot.standalone();
    private final DataSlot target = DataSlot.standalone();
    private final DataSlot allowed = DataSlot.standalone();
    private final DataSlot maximum = DataSlot.standalone();

    public BookSettingsMenu(int id, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(id, inventory, ItemStack.STREAM_CODEC.decode(data));
    }
    public BookSettingsMenu(int id, Inventory inventory, ItemStack book) {
        super(ModMenus.BOOK_SETTINGS.get(), id);
        this.book = book;
        addDataSlot(flags);
        addDataSlot(target);
        addDataSlot(allowed);
        addDataSlot(maximum);
        refresh();
    }
    private void refresh() {
        flags.set(BookAutomation.flags(book));
        target.set(BookAutomation.target(book));
        allowed.set(BookConfig.allowedFlags());
        maximum.set(BookConfig.MAX_TARGET.get());
    }
    public int flags() { return flags.get(); }
    public int target() { return target.get(); }
    public int allowed() { return allowed.get(); }
    public int maximum() { return maximum.get(); }

    @Override
    public void broadcastChanges() {
        refresh();
        super.broadcastChanges();
    }
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player.level().isClientSide() || !stillValid(player)) return false;
        if (id >= 0 && id < 3) {
            int bit = 1 << id;
            if ((BookConfig.allowedFlags() & bit) == 0) return false;
            book.set(ModComponents.AUTOMATION.get(), BookAutomation.flags(book) ^ bit);
        } else if (id >= 3 && id <= 6 && BookConfig.PUMP.get()) {
            int delta = switch (id) { case 3 -> -10; case 4 -> -1; case 5 -> 1; default -> 10; };
            book.set(ModComponents.TARGET_LEVEL.get(), Math.clamp(BookAutomation.target(book) + delta, 0, BookConfig.MAX_TARGET.get()));
        } else return false;
        player.getInventory().setChanged();
        broadcastChanges();
        return true;
    }
    @Override
    public boolean stillValid(Player player) {
        return player.isAlive() && BookLocator.held(player) == book;
    }
    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}
