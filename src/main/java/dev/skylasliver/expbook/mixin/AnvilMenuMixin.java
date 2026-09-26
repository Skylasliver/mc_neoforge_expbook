package dev.skylasliver.expbook.mixin;

import dev.skylasliver.expbook.ExperienceMath;
import dev.skylasliver.expbook.item.ExperienceBookItem;
import dev.skylasliver.expbook.util.BookLocator;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Held books pay cumulative XP for the anvil's displayed level price. */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {
    @Redirect(method = "mayPickup", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Player;experienceLevel:I"))
    private int expbook$availableLevel(Player player) {
        ItemStack book = BookLocator.forEnchanting(player);
        return book.isEmpty() ? player.experienceLevel
                : ExperienceMath.levelForPoints(ExperienceBookItem.storedPoints(book));
    }

    @Redirect(method = "onTake", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;giveExperienceLevels(I)V"))
    private void expbook$pay(Player player, int levels) {
        ItemStack book = BookLocator.forEnchanting(player);
        if (book.isEmpty()) {
            player.giveExperienceLevels(levels);
        } else if (!player.level().isClientSide() && !player.getAbilities().instabuild) {
            int points = ExperienceMath.totalPointsForLevel(Math.max(0, -levels));
            ExperienceBookItem.setContents(book, ExperienceBookItem.contents(book)
                    .withStoredPoints(Math.max(0, ExperienceBookItem.storedPoints(book) - points)));
        }
    }
}
