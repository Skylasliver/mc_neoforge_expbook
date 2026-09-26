package dev.skylasliver.expbook.mixin;

import dev.skylasliver.expbook.item.ExperienceBookItem;
import dev.skylasliver.expbook.util.BookLocator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client-side mirror of the server escalation, so the three buttons light up according to
 * the book's level rather than the player's own level. The player fields are restored at
 * the end of the frame, which keeps the rest of the HUD honest.
 *
 * <p>This affects presentation only. Affordability and payment are decided on the server.
 */
@Mixin(EnchantmentScreen.class)
public abstract class EnchantmentScreenMixin {

    @Unique
    private int expbook$savedTotal;

    @Unique
    private int expbook$savedLevel;

    @Unique
    private float expbook$savedProgress;

    @Unique
    private boolean expbook$escalated;

    @Inject(method = "render", at = @At("HEAD"))
    private void expbook$escalateForDisplay(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY,
                                            float partialTick, CallbackInfo ci) {
        this.expbook$escalated = false;
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        int bookPoints = ExperienceBookItem.storedPoints(BookLocator.forEnchanting(player));
        if (BookLocator.forEnchanting(player).isEmpty()) {
            return;
        }
        this.expbook$savedTotal = player.totalExperience;
        this.expbook$savedLevel = player.experienceLevel;
        this.expbook$savedProgress = player.experienceProgress;
        this.expbook$escalated = true;
        long merged = bookPoints;
        player.experienceLevel = dev.skylasliver.expbook.ExperienceMath.levelForPoints(merged);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void expbook$restoreAfterDisplay(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY,
                                             float partialTick, CallbackInfo ci) {
        if (!this.expbook$escalated) {
            return;
        }
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        player.totalExperience = this.expbook$savedTotal;
        player.experienceLevel = this.expbook$savedLevel;
        player.experienceProgress = this.expbook$savedProgress;
        this.expbook$escalated = false;
    }
}
