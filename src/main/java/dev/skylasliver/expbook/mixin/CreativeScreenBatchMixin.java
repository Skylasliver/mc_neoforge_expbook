package dev.skylasliver.expbook.mixin;

import dev.skylasliver.expbook.client.CreativeBookPackets;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Flush a complete click before another screen action or closing the creative inventory. */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeScreenBatchMixin {
    @Inject(method = "slotClicked", at = @At("RETURN"))
    private void expbook$finishClick(Slot slot, int slotId, int button, ClickType type, CallbackInfo ci) {
        CreativeBookPackets.flush();
    }
    @Inject(method = "removed", at = @At("RETURN"))
    private void expbook$finishScreen(CallbackInfo ci) {
        CreativeBookPackets.flush();
    }
}
