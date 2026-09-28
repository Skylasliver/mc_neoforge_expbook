package dev.skylasliver.skylasliver_expbook.util;

import dev.skylasliver.skylasliver_expbook.item.ExperienceBookItem;
import dev.skylasliver.skylasliver_expbook.menu.BookAdminMenu;
import dev.skylasliver.skylasliver_expbook.menu.ExperienceBookMenu;
import dev.skylasliver.skylasliver_expbook.menu.BookSettingsMenu;
import dev.skylasliver.skylasliver_expbook.registry.ModComponents;
import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public final class BookIdentity {
    private BookIdentity() {}

    /** Destroy the original in place, including third-party components, names and enchantments. */
    public static void erase(ItemStack stack) {
        if (stack.isEmpty()) return;
        for (var type : new ArrayList<>(stack.getComponents().keySet())) stack.remove(type);
        stack.setCount(0);
    }

    public static boolean validate(ServerLevel level, ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ExperienceBookItem)) return false;
        UUID id = stack.get(ModComponents.BOOK_ID.get());
        if (id != null && BookLedger.isRevoked(level, id)) {
            erase(stack);
            return false;
        }
        return true;
    }

    public static UUID ensureBound(ServerPlayer player, ItemStack stack) {
        if (!validate(player.serverLevel(), stack)) return null;
        UUID id = stack.get(ModComponents.BOOK_ID.get());
        if (id == null) {
            id = UUID.randomUUID();
            stack.set(ModComponents.BOOK_ID.get(), id);
        }
        UUID owner = stack.get(ModComponents.OWNER_UUID.get());
        if (owner == null) {
            owner = player.getUUID();
            stack.set(ModComponents.OWNER_UUID.get(), owner);
            stack.set(ModComponents.OWNER_NAME.get(), player.getGameProfile().getName());
        }
        BookLedger.record(player, stack, id, owner);
        return id;
    }

    public static void recordIfBound(ServerPlayer player, ItemStack stack) {
        if (!validate(player.serverLevel(), stack)) return;
        UUID id = stack.get(ModComponents.BOOK_ID.get());
        UUID owner = stack.get(ModComponents.OWNER_UUID.get());
        if (id != null && owner != null) BookLedger.record(player, stack, id, owner);
    }

    public static boolean isUsable(ServerPlayer player, ItemStack stack) {
        return validate(player.serverLevel(), stack);
    }

    public static void recordCarried(ServerPlayer player, UUID id) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (id.equals(stack.get(ModComponents.BOOK_ID.get()))) recordIfBound(player, stack);
        }
        ItemStack cursor = player.containerMenu.getCarried();
        if (id.equals(cursor.get(ModComponents.BOOK_ID.get()))) recordIfBound(player, cursor);
    }

    private static boolean purge(ServerPlayer player, Container container) {
        boolean changed = false;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.getItem() instanceof ExperienceBookItem && !validate(player.serverLevel(), stack)) {
                container.setItem(i, ItemStack.EMPTY);
                changed = true;
            }
        }
        if (changed) container.setChanged();
        return changed;
    }

    public static void purgeRevoked(ServerPlayer player) {
        boolean changed = purge(player, player.getInventory()) | purge(player, player.getEnderChestInventory());
        ItemStack cursor = player.containerMenu.getCarried();
        if (cursor.getItem() instanceof ExperienceBookItem && !validate(player.serverLevel(), cursor)) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
            changed = true;
        }
        // An admin menu holds read-only snapshots; never treat those as physical books.
        if (!(player.containerMenu instanceof BookAdminMenu)) {
            for (var slot : player.containerMenu.slots) {
                ItemStack stack = slot.getItem();
                if (stack.getItem() instanceof ExperienceBookItem && !validate(player.serverLevel(), stack)) {
                    slot.set(ItemStack.EMPTY);
                    changed = true;
                }
            }
        }
        if ((player.containerMenu instanceof ExperienceBookMenu || player.containerMenu instanceof BookSettingsMenu)
                && !player.containerMenu.stillValid(player)) player.closeContainer();
        if (changed) {
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
            player.inventoryMenu.broadcastChanges();
        }
    }
}
