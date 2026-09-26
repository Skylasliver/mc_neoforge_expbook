package dev.skylasliver.expbook.registry;

import com.mojang.blaze3d.platform.InputConstants;
import dev.skylasliver.expbook.ExperienceBookMod;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = ExperienceBookMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModKeyMappings {

    /** Opens the page-management GUI. Rebindable under the mod's own category. */
    public static final KeyMapping OPEN_BOOK = new KeyMapping(
            "key.skylasliver_expbook.open_book",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.skylasliver_expbook");

    private ModKeyMappings() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_BOOK);
    }
}
