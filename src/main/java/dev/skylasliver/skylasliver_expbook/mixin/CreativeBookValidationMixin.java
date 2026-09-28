package dev.skylasliver.skylasliver_expbook.mixin;

import dev.skylasliver.skylasliver_expbook.util.CreativeBookEdits;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class CreativeBookValidationMixin {
    @Shadow public ServerPlayer player;
    @Redirect(method = "handleSetCreativeModeSlot", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/game/ServerboundSetCreativeModeSlotPacket;itemStack()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack expbook$validateCreativeBook(ServerboundSetCreativeModeSlotPacket packet) {
        return CreativeBookEdits.sanitizeSingle(player, packet.slotNum(), packet.itemStack());
    }
}
