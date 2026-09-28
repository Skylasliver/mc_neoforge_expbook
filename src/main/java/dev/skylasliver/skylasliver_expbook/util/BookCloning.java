package dev.skylasliver.skylasliver_expbook.util;

import dev.skylasliver.skylasliver_expbook.item.ExperienceBookItem;
import dev.skylasliver.skylasliver_expbook.registry.ModComponents;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** Copies content while giving the new book an independent identity and no owner. */
public final class BookCloning {
    private BookCloning() {}
    /** Keep the source key for creative transaction matching until the server issues a new ID. */
    public static ItemStack preview(ItemStack source) {
        ItemStack result = source.copyWithCount(1);
        result.remove(ModComponents.OWNER_UUID.get());
        result.remove(ModComponents.OWNER_NAME.get());
        return result;
    }

    public static ItemStack copy(ServerLevel level, ItemStack source) {
        UUID sourceId = source.get(ModComponents.BOOK_ID.get());
        // An already-replaced original must not be resurrected by cloning stale client data.
        ItemStack result = sourceId != null && BookLedger.isRevoked(level, sourceId)
                ? ExperienceBookItem.createEmpty() : preview(source);
        UUID id = UUID.randomUUID();
        result.set(ModComponents.BOOK_ID.get(), id);
        BookNumbers.sync(level, result, id);
        return result;
    }
}
