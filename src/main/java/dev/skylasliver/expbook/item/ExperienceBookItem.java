package dev.skylasliver.expbook.item;

import dev.skylasliver.expbook.ExperienceMath;
import dev.skylasliver.expbook.component.BookContents;
import dev.skylasliver.expbook.page.PageTier;
import dev.skylasliver.expbook.registry.ModComponents;
import dev.skylasliver.expbook.registry.ModItems;
import dev.skylasliver.expbook.registry.ModKeyMappings;
import dev.skylasliver.expbook.util.BookLocator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * The experience book: a single-item container whose points and pages live in the
 * {@code book_contents} data component. Right click stores, a left-click air swing
 * takes back (see {@code TakeLevelPayload}). Right click always stores and always
 * wins over block interaction, so holding the book suppresses chests and placing.
 */
public class ExperienceBookItem extends Item {

    public ExperienceBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        // A styled child retains its color when vanilla applies rarity to the tooltip root.
        return Component.empty().append(super.getName(stack).copy()
                .withStyle(style -> style.withColor(0x75D995).withItalic(false).withBold(true)));
    }

    public static ItemStack createEmpty() {
        return new ItemStack(ModItems.EXPERIENCE_BOOK.get());
    }

    public static BookContents contents(ItemStack stack) {
        return stack.getOrDefault(ModComponents.BOOK_CONTENTS.get(), BookContents.EMPTY);
    }

    public static void setContents(ItemStack stack, BookContents contents) {
        stack.set(ModComponents.BOOK_CONTENTS.get(), contents);
        // The item model's overrides chain keys on the vanilla custom_model_data
        // predicate, so the fill level has to be written there, not only into the
        // mod's own FILL_LEVEL component.
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(fillLevel(contents)));
    }

    public static int storedPoints(ItemStack stack) {
        return contents(stack).storedPoints();
    }

    public static int capacity(ItemStack stack) {
        return contents(stack).capacity();
    }

    /** Capacity still available for new points. */
    public static int freeSpace(ItemStack stack) {
        BookContents contents = contents(stack);
        return Math.max(0, contents.capacity() - contents.storedPoints());
    }

    /** 0 = empty, 1 = below 25%, 2 = 25-75%, 3 = above 75% of capacity. */
    public static int fillLevel(BookContents contents) {
        if (contents.capacity() <= 0 || contents.storedPoints() <= 0) {
            return 0;
        }
        float ratio = (float) contents.storedPoints() / (float) contents.capacity();
        if (ratio < 0.25F) {
            return 1;
        }
        if (ratio <= 0.75F) {
            return 2;
        }
        return 3;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        store(level, player, stack);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        store(context.getLevel(), player, context.getItemInHand());
        return InteractionResult.SUCCESS;
    }

    private static void store(Level level, Player player, ItemStack stack) {
        int stored = player.isShiftKeyDown()
                ? BookActions.storeAll(player, stack)
                : BookActions.storeOneLevel(player, stack);
        if (stored > 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6F, 1.4F);
        } else {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.4F, 1.6F);
        }
    }

    /** Server-side entry point for the empty-swing packet. */
    public static void handleTake(ServerPlayer player, boolean all) {
        ItemStack book = BookLocator.held(player);
        if (book.isEmpty()) {
            return;
        }
        int taken = all ? BookActions.takeAll(player, book) : BookActions.takeOneLevel(player, book);
        if (taken > 0) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.4F, 1.8F);
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        BookContents contents = contents(stack);
        return contents.capacity() > 0 && contents.storedPoints() > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        BookContents contents = contents(stack);
        if (contents.capacity() <= 0) {
            return 0;
        }
        return Math.min(13, Math.round(13.0F * contents.storedPoints() / contents.capacity()));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x3D6FE0;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        BookContents contents = contents(stack);
        int level = ExperienceMath.levelForPoints(contents.storedPoints());
        tooltip.add(Component.translatable("tooltip.skylasliver_expbook.stored",
                        contents.storedPoints(), contents.capacity())
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.skylasliver_expbook.level", level)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.skylasliver_expbook.pages",
                        contents.pages().size(), BookContents.MAX_PAGES)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.skylasliver_expbook.take_hint")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.skylasliver_expbook.store_hint")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.skylasliver_expbook.gui_hint",
                        ModKeyMappings.OPEN_BOOK.getTranslatedKeyMessage())
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.skylasliver_expbook.settings_hint",
                        ModKeyMappings.OPEN_BOOK.getTranslatedKeyMessage())
                .withStyle(ChatFormatting.DARK_GRAY));
        if (contents.pages().isEmpty()) {
            tooltip.add(Component.translatable("tooltip.skylasliver_expbook.no_pages")
                    .withStyle(ChatFormatting.DARK_RED));
        } else {
            for (PageTier tier : PageTier.values()) {
                int count = 0;
                for (PageTier page : contents.pages()) {
                    if (page == tier) {
                        count++;
                    }
                }
                if (count > 0) {
                    tooltip.add(Component.literal("  " + count + "x ")
                            .append(tier.displayName())
                            .withStyle(ChatFormatting.DARK_GRAY));
                }
            }
        }
    }
}
