package dev.skylasliver.expbook.mixin;

import dev.skylasliver.expbook.item.ExperienceBookItem;
import dev.skylasliver.expbook.util.BookCloning;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Covers middle-click in ordinary containers and the creative inventory player tab. */
@Mixin(AbstractContainerMenu.class)
public abstract class BookCloneMenuMixin {
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void expbook$independentClone(int slot, int button, ClickType type, Player player, CallbackInfo ci) {
        AbstractContainerMenu menu = (AbstractContainerMenu)(Object)this;
        if (type == ClickType.CLONE && player.hasInfiniteMaterials() && menu.getCarried().isEmpty()
                && slot >= 0 && slot < menu.slots.size()
                && menu.getSlot(slot).getItem().getItem() instanceof ExperienceBookItem) {
            menu.setCarried(player instanceof ServerPlayer server
                    ? BookCloning.copy(server.serverLevel(), menu.getSlot(slot).getItem())
                    : BookCloning.preview(menu.getSlot(slot).getItem()));
            ci.cancel();
        }
    }
}
