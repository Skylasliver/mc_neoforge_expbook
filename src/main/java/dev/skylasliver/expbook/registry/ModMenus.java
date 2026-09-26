package dev.skylasliver.expbook.registry;

import dev.skylasliver.expbook.ExperienceBookMod;
import dev.skylasliver.expbook.menu.ExperienceBookMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, ExperienceBookMod.MOD_ID);

    /** The book travels with the open packet, so no extra data slot is needed. */
    public static final DeferredHolder<MenuType<?>, MenuType<ExperienceBookMenu>> EXPERIENCE_BOOK =
            MENUS.register("experience_book", () -> IMenuTypeExtension.create(
                    (containerId, inventory, data) -> new ExperienceBookMenu(containerId, inventory, data)));

    public static final DeferredHolder<MenuType<?>, MenuType<dev.skylasliver.expbook.menu.BookSettingsMenu>> BOOK_SETTINGS =
            MENUS.register("book_settings", () -> IMenuTypeExtension.create(
                    (id, inventory, data) -> new dev.skylasliver.expbook.menu.BookSettingsMenu(id, inventory, data)));

    private ModMenus() {
    }
}
