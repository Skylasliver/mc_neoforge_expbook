package dev.skylasliver.skylasliver_expbook;

import dev.skylasliver.skylasliver_expbook.registry.ModComponents;
import dev.skylasliver.skylasliver_expbook.registry.ModItems;
import dev.skylasliver.skylasliver_expbook.registry.ModMenus;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import dev.skylasliver.skylasliver_expbook.command.ExperienceBookCommands;
import net.neoforged.neoforge.common.NeoForge;

@Mod(ModMindEntry.MOD_ID)
public final class ModMindEntry {
    public static final String MOD_ID = "skylasliver_expbook";

    public ModMindEntry(IEventBus modBus, net.neoforged.fml.ModContainer container) {
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER, BookConfig.SPEC);
        ModItems.ITEMS.register(modBus);
        ModComponents.COMPONENTS.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModItems.CREATIVE_TABS.register(modBus);
        NeoForge.EVENT_BUS.register(ExperienceBookCommands.class);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
