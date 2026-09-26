package dev.skylasliver.expbook.mixin;

import dev.skylasliver.expbook.client.CreativeBookPackets;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class CreativeInventoryPacketsMixin {
    @Inject(method = "handleCreativeModeItemAdd", at = @At("HEAD"), cancellable = true)
    private void expbook$batchSlot(ItemStack stack, int slot, CallbackInfo ci) {
        CreativeBookPackets.enqueue(slot, stack);
        ci.cancel();
    }
    @Inject(method = "handleCreativeModeItemDrop", at = @At("HEAD"), cancellable = true)
    private void expbook$batchDrop(ItemStack stack, CallbackInfo ci) {
        CreativeBookPackets.enqueue(-1, stack);
        ci.cancel();
    }
}
