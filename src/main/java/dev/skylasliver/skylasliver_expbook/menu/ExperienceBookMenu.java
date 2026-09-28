package dev.skylasliver.skylasliver_expbook.menu;

import dev.skylasliver.skylasliver_expbook.component.BookContents;
import dev.skylasliver.skylasliver_expbook.item.PageItem;
import dev.skylasliver.skylasliver_expbook.registry.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Server-authoritative page menu. Points never travel through this menu; only page
 * insertion and removal do, and the stored points stay inside the book's component.
 */
public class ExperienceBookMenu extends AbstractContainerMenu {

    private final PageContainer pages;

    /** Client-side constructor: the book arrives from the open-menu payload. */
    public ExperienceBookMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, ItemStack.STREAM_CODEC.decode(extraData));
    }

    public ExperienceBookMenu(int containerId, Inventory playerInventory, ItemStack book) {
        super(ModMenus.EXPERIENCE_BOOK.get(), containerId);
        this.pages = new PageContainer(book);
        addPageSlots();
        addPlayerSlots(playerInventory, 110);
    }

    public ItemStack book() {
        return this.pages.book();
    }

    private void addPageSlots() {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int index = row * 9 + column;
                addSlot(new Slot(this.pages, index, 8 + column * 18, 40 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return stack.getItem() instanceof PageItem && pages.canReplace(index, stack);
                    }

                    @Override
                    public boolean mayPickup(Player player) {
                        return pages.canReplace(index, ItemStack.EMPTY);
                    }

                    @Override
                    public int getMaxStackSize() {
                        return 1;
                    }
                });
            }
        }
    }

    private void addPlayerSlots(Inventory inventory, int top) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, top + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, top + 58));
        }
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (!player.level().isClientSide() && !stillValid(player)) return;
        if (slotId >= 0 && slotId < this.slots.size() && this.slots.get(slotId).getItem() == book()) return;
        if (clickType == ClickType.SWAP) {
            if ((button < 0 || button > 8) && button != 40) return;
            if ((button == 40 ? player.getOffhandItem() : player.getInventory().getItem(button)) == book()) return;
        }
        if (slotId >= 0 && slotId < BookContents.MAX_PAGES) {
            if (clickType == ClickType.QUICK_MOVE) {
                // Shift-clicking a page pulls it back into the inventory, subject to the
                // capacity check inside the container.
                Slot slot = this.slots.get(slotId);
                if (slot.hasItem() && slot.mayPickup(player)) {
                    ItemStack removed = this.pages.removeItem(slotId, 1);
                    if (!removed.isEmpty() && !player.getInventory().add(removed)) {
                        player.drop(removed, false);
                    }
                    record(player);
                    broadcastChanges();
                }
                return;
            }
        }
        super.clicked(slotId, button, clickType, player);
        record(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || !stillValid(player)) return ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem() || slot.getItem() == book() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < BookContents.MAX_PAGES) {
            if (!moveItemStackTo(stack, BookContents.MAX_PAGES, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof PageItem) {
            if (!moveItemStackTo(stack, 0, BookContents.MAX_PAGES, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        record(player);
        return original;
    }

    private void record(Player player) {
        if (player instanceof net.minecraft.server.level.ServerPlayer server)
            dev.skylasliver.skylasliver_expbook.util.BookIdentity.recordIfBound(server, book());
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive() && !book().isEmpty()
                && (!(player instanceof net.minecraft.server.level.ServerPlayer server)
                    || dev.skylasliver.skylasliver_expbook.util.BookIdentity.isUsable(server, book()))
                && dev.skylasliver.skylasliver_expbook.util.BookLocator.held(player) == book();
    }

}
