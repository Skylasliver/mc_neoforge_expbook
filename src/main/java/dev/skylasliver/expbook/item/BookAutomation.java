package dev.skylasliver.expbook.item;

import dev.skylasliver.expbook.BookConfig;
import dev.skylasliver.expbook.ExperienceMath;
import dev.skylasliver.expbook.ExperienceBookMod;
import dev.skylasliver.expbook.registry.ModComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ExperienceBookMod.MOD_ID)
public final class BookAutomation {
    public static int flags(ItemStack book) {
        return book.getOrDefault(ModComponents.AUTOMATION.get(), 0) & 7;
    }
    public static int target(ItemStack book) {
        return Math.clamp(book.getOrDefault(ModComponents.TARGET_LEVEL.get(), BookConfig.TARGET.get()), 0, BookConfig.MAX_TARGET.get());
    }
    /** Called only while processing an actual orb pickup, never on XP withdrawals or commands. */
    public static int collect(ServerPlayer player, int points) {
        if (!BookConfig.COLLECT.get() || points <= 0 || player.isSpectator()) return points;
        int remaining = points;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack book = player.getInventory().getItem(i);
            if (!(book.getItem() instanceof ExperienceBookItem) || (flags(book) & 1) == 0) continue;
            int moved = Math.min(remaining, ExperienceBookItem.freeSpace(book));
            if (moved > 0) {
                var contents = ExperienceBookItem.contents(book);
                ExperienceBookItem.setContents(book, contents.withStoredPoints(contents.storedPoints() + moved));
                remaining -= moved;
            }
        }
        return remaining;
    }
    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive()
                || player.isSpectator() || player.tickCount % 20 != 0) return;
        // Inventory includes main hand/hotbar, backpack, armor and offhand exactly once.
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack book = player.getInventory().getItem(i);
            if (!(book.getItem() instanceof ExperienceBookItem)) continue;
            int enabled = flags(book) & BookConfig.allowedFlags();
            int available = ExperienceBookItem.storedPoints(book);
            int remaining = available;
            int credit = book.getOrDefault(ModComponents.REPAIR_CREDIT.get(), 0);
            if ((enabled & 2) != 0 && (remaining > 0 || credit > 0)) {
                var mending = player.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.MENDING);
                for (int slot = 0; slot < player.getInventory().getContainerSize() && (remaining > 0 || credit > 0); slot++) {
                    ItemStack item = player.getInventory().getItem(slot);
                    if (!item.isDamaged() || item.getEnchantmentLevel(mending) <= 0) continue;
                    int repairPerPoint = EnchantmentHelper.modifyDurabilityToRepairFromXp(player.serverLevel(), item, 1);
                    if (repairPerPoint <= 0) continue;
                    var payment = dev.skylasliver.expbook.RepairBudget.spend(
                            item.getDamageValue(), remaining, credit, repairPerPoint);
                    credit = payment.credit();
                    item.setDamageValue(item.getDamageValue() - payment.repaired());
                    remaining -= payment.charged();
                }
            }
            if ((enabled & 4) != 0 && remaining > 0 && player.experienceLevel < target(book)) {
                // totalExperience is a lifetime counter after enchanting; use the visible level/bar.
                int current = ExperienceMath.totalPointsForLevel(player.experienceLevel)
                        + Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
                int moved = Math.min(remaining, Math.max(0, ExperienceMath.totalPointsForLevel(target(book)) - current));
                if (moved > 0) {
                    player.giveExperiencePoints(moved);
                    remaining -= moved;
                }
            }
            if (credit != book.getOrDefault(ModComponents.REPAIR_CREDIT.get(), 0)) {
                book.set(ModComponents.REPAIR_CREDIT.get(), credit);
            }
            if (remaining != available) {
                ExperienceBookItem.setContents(book, ExperienceBookItem.contents(book).withStoredPoints(remaining));
            }
        }
    }
    private BookAutomation() {}
}
