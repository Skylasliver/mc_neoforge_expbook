package dev.skylasliver.skylasliver_expbook.registry;

import dev.skylasliver.skylasliver_expbook.ModMindEntry;
import dev.skylasliver.skylasliver_expbook.item.ExperienceBookItem;
import dev.skylasliver.skylasliver_expbook.item.PageItem;
import dev.skylasliver.skylasliver_expbook.page.PageTier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(ModMindEntry.MOD_ID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ModMindEntry.MOD_ID);

    public static final DeferredItem<ExperienceBookItem> EXPERIENCE_BOOK =
            ITEMS.register("experience_book", () -> new ExperienceBookItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<PageItem> LEATHER_PAGE = page("leather_page", PageTier.LEATHER);
    public static final DeferredItem<PageItem> GOLD_PAGE = page("gold_page", PageTier.GOLD);
    public static final DeferredItem<PageItem> DIAMOND_PAGE = page("diamond_page", PageTier.DIAMOND);
    public static final DeferredItem<PageItem> NETHERITE_PAGE = page("netherite_page", PageTier.NETHERITE);
    public static final DeferredItem<PageItem> NETHER_STAR_PAGE = page("nether_star_page", PageTier.NETHER_STAR);

    private static DeferredItem<PageItem> page(String name, PageTier tier) {
        return ITEMS.register(name, () -> new PageItem(tier, new Item.Properties().stacksTo(16)));
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = CREATIVE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.skylasliver_expbook"))
                    .icon(() -> new ItemStack(EXPERIENCE_BOOK.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ExperienceBookItem.createEmpty());
                        for (PageTier tier : PageTier.values()) {
                            output.accept(new ItemStack(tier.item()));
                        }
                    })
                    .build());

    private ModItems() {
    }
}
