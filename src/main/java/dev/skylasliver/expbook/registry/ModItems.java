package dev.skylasliver.expbook.registry;

import dev.skylasliver.expbook.ExperienceBookMod;
import dev.skylasliver.expbook.item.ExperienceBookItem;
import dev.skylasliver.expbook.item.PageItem;
import dev.skylasliver.expbook.page.PageTier;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(ExperienceBookMod.MOD_ID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ExperienceBookMod.MOD_ID);

    public static final DeferredItem<ExperienceBookItem> EXPERIENCE_BOOK =
            ITEMS.register("experience_book", () -> new ExperienceBookItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<PageItem> LEATHER_PAGE = page("leather_page", PageTier.LEATHER);
    public static final DeferredItem<PageItem> GOLD_PAGE = page("gold_page", PageTier.GOLD);
    public static final DeferredItem<PageItem> DIAMOND_PAGE = page("diamond_page", PageTier.DIAMOND);
    public static final DeferredItem<PageItem> NETHERITE_PAGE = page("netherite_page", PageTier.NETHERITE);
    public static final DeferredItem<PageItem> NETHER_STAR_PAGE = page("nether_star_page", PageTier.NETHER_STAR);

    private static final Map<PageTier, DeferredItem<PageItem>> PAGES = new EnumMap<>(PageTier.class);

    static {
        PAGES.put(PageTier.LEATHER, LEATHER_PAGE);
        PAGES.put(PageTier.GOLD, GOLD_PAGE);
        PAGES.put(PageTier.DIAMOND, DIAMOND_PAGE);
        PAGES.put(PageTier.NETHERITE, NETHERITE_PAGE);
        PAGES.put(PageTier.NETHER_STAR, NETHER_STAR_PAGE);
    }

    private static DeferredItem<PageItem> page(String name, PageTier tier) {
        return ITEMS.register(name, () -> new PageItem(tier, new Item.Properties().stacksTo(16)));
    }

    /** The registered page item for a tier. */
    public static Item pageFor(PageTier tier) {
        return PAGES.get(tier).get();
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = CREATIVE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.skylasliver_expbook"))
                    .icon(() -> new ItemStack(EXPERIENCE_BOOK.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ExperienceBookItem.createEmpty());
                        for (PageTier tier : PageTier.values()) {
                            output.accept(new ItemStack(pageFor(tier)));
                        }
                    })
                    .build());

    public static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        // Entries are contributed through the tab's own displayItems callback above.
    }

    private ModItems() {
    }
}
