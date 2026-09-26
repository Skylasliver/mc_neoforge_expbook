package dev.skylasliver.expbook.menu;

import dev.skylasliver.expbook.component.BookContents;
import dev.skylasliver.expbook.item.ExperienceBookItem;
import dev.skylasliver.expbook.item.PageItem;
import dev.skylasliver.expbook.page.PageTier;
import java.util.ArrayList;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/** Stable inventory stacks with capacity checks before slot transactions. */
public class PageContainer extends SimpleContainer {
    private final ItemStack book;
    private boolean loading = true;

    public PageContainer(ItemStack book) {
        super(BookContents.MAX_PAGES);
        this.book = book;
        var saved = ExperienceBookItem.contents(book).pages();
        for (int i = 0; i < saved.size(); i++) super.setItem(i, new ItemStack(saved.get(i).item()));
        loading = false;
    }

    public ItemStack book() { return book; }

    public boolean canReplace(int slot, ItemStack replacement) {
        if (slot < 0 || slot >= getContainerSize() || book.isEmpty()) return false;
        if (!replacement.isEmpty() && !(replacement.getItem() instanceof PageItem)) return false;
        long capacity = 0;
        for (int i = 0; i < getContainerSize(); i++) {
            ItemStack item = i == slot ? replacement : getItem(i);
            if (item.getItem() instanceof PageItem page) capacity += page.tier().points();
        }
        var contents = ExperienceBookItem.contents(book);
        // A capacity reduction in server config must not prevent gradually adding pages.
        // Preserve an existing overflow, but never allow a transaction to make it worse.
        return capacity >= Math.min(contents.storedPoints(), contents.capacity());
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (loading || book.isEmpty()) return;
        var pages = new ArrayList<PageTier>();
        for (int i = 0; i < getContainerSize(); i++) {
            if (getItem(i).getItem() instanceof PageItem page) pages.add(page.tier());
        }
        ExperienceBookItem.setContents(book, ExperienceBookItem.contents(book).withPages(pages));
    }
}
