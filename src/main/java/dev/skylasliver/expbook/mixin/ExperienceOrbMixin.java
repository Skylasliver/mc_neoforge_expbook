package dev.skylasliver.expbook.mixin;

import dev.skylasliver.expbook.item.BookAutomation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Keep vanilla pickup, merged-orb counts, cooldown and remaining Mending intact. */
@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMixin {
    @ModifyVariable(method = "repairPlayerItems", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int expbook$collectBeforeMending(int points, ServerPlayer player, int originalPoints) {
        return BookAutomation.collect(player, points);
    }
}
