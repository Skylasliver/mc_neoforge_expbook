package dev.skylasliver.skylasliver_expbook.client;

import dev.skylasliver.skylasliver_expbook.ModMindEntry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Client-only entry point for NeoForge's mod-list configuration button. */
@Mod(value = ModMindEntry.MOD_ID, dist = Dist.CLIENT)
public final class ClientConfig {
    public ClientConfig(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
