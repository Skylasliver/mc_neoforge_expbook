package dev.skylasliver.skylasliver_expbook.registry;

import dev.skylasliver.skylasliver_expbook.ModMindEntry;
import dev.skylasliver.skylasliver_expbook.menu.ExperienceBookMenu;
import dev.skylasliver.skylasliver_expbook.menu.BookAdminMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, ModMindEntry.MOD_ID);

    /** The book travels with the open packet, so no extra data slot is needed. */
    public static final DeferredHolder<MenuType<?>, MenuType<ExperienceBookMenu>> EXPERIENCE_BOOK =
            MENUS.register("experience_book", () -> IMenuTypeExtension.create(
                    (containerId, inventory, data) -> new ExperienceBookMenu(containerId, inventory, data)));

    public static final DeferredHolder<MenuType<?>, MenuType<dev.skylasliver.skylasliver_expbook.menu.BookSettingsMenu>> BOOK_SETTINGS =
            MENUS.register("book_settings", () -> IMenuTypeExtension.create(
                    (id, inventory, data) -> new dev.skylasliver.skylasliver_expbook.menu.BookSettingsMenu(id, inventory, data)));
    public static final DeferredHolder<MenuType<?>, MenuType<BookAdminMenu>> BOOK_ADMIN =
            MENUS.register("book_admin", () -> IMenuTypeExtension.create(BookAdminMenu::new));

    private ModMenus() {
    }
}
