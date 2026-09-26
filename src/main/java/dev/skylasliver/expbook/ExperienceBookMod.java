package dev.skylasliver.expbook;

import dev.skylasliver.expbook.registry.ModComponents;
import dev.skylasliver.expbook.registry.ModItems;
import dev.skylasliver.expbook.registry.ModMenus;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(ExperienceBookMod.MOD_ID)
public final class ExperienceBookMod {
    public static final String MOD_ID = "skylasliver_expbook";

    public ExperienceBookMod(IEventBus modBus, net.neoforged.fml.ModContainer container) {
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER, BookConfig.SPEC);
        ModItems.ITEMS.register(modBus);
        ModComponents.COMPONENTS.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModItems.CREATIVE_TABS.register(modBus);
        modBus.addListener(ModItems::addToCreativeTab);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
