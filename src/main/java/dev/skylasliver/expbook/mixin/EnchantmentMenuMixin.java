package dev.skylasliver.expbook.mixin;

import dev.skylasliver.expbook.ExperienceMath;
import dev.skylasliver.expbook.item.ExperienceBookItem;
import dev.skylasliver.expbook.util.BookLocator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Held-book-only payment using the displayed cumulative level cost. */
@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin {

    @Unique
    private final Map<UUID, expbook$Stash> EXPBOOK_STASH = new HashMap<>();
    @Unique private boolean expbook$performed;

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;onEnchantmentPerformed(Lnet/minecraft/world/item/ItemStack;I)V"))
    private void expbook$recordPayment(Player player, ItemStack stack, int levels) {
        expbook$performed = true;
        player.onEnchantmentPerformed(stack, levels);
    }

    @Inject(method = "clickMenuButton", at = @At("HEAD"))
    private void expbook$escalateLevel(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
        expbook$performed = false;
        // The screen calls this menu locally before sending the button packet.
        // Both sides must pass the same held-book affordability check.
        if (id < 0 || id > 2) {
            return;
        }
        ItemStack book = BookLocator.forEnchanting(player);
        int bookPoints = ExperienceBookItem.storedPoints(book);
        if (book.isEmpty()) {
            return;
        }
        EXPBOOK_STASH.put(player.getUUID(), new expbook$Stash(
                player.totalExperience,
                player.experienceLevel,
                player.experienceProgress,
                bookPoints, book, ExperienceMath.totalPointsForLevel(((EnchantmentMenu)(Object)this).costs[id])));
        long merged = bookPoints;
        player.totalExperience = (int) Math.min(Integer.MAX_VALUE, merged);
        player.experienceLevel = ExperienceMath.levelForPoints(merged);
        player.experienceProgress = ExperienceMath.progress(merged);
    }

    @Inject(method = "clickMenuButton", at = @At("RETURN"))
    private void expbook$settle(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
        expbook$Stash stash = EXPBOOK_STASH.remove(player.getUUID());
        if (stash == null) {
            return;
        }
        if (!expbook$performed || player.level().isClientSide() || player.getAbilities().instabuild
                || cir.getReturnValue() == null || !cir.getReturnValue()) {
            // Client checks never charge XP; the server settles and syncs the result.
            // Each menu owns its stash so integrated client/server calls cannot race.
            player.totalExperience = stash.total();
            player.experienceLevel = stash.level();
            player.experienceProgress = stash.progress();
            return;
        }

        player.totalExperience = stash.total();
        player.experienceLevel = stash.level();
        player.experienceProgress = stash.progress();
        ExperienceBookItem.setContents(stash.book(), ExperienceBookItem.contents(stash.book())
                .withStoredPoints(stash.bookPoints() - stash.cost()));
    }
    /** Held-book-only payment using the displayed cumulative level cost. */
    @Unique
    private record expbook$Stash(int total, int level, float progress, int bookPoints, ItemStack book, int cost) {
    }
}
