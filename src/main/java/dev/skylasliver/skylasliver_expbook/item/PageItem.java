package dev.skylasliver.skylasliver_expbook.item;

import dev.skylasliver.skylasliver_expbook.page.PageTier;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A page contributes capacity to whichever book it is inserted into. It carries no
 * experience of its own, so it stacks to 16 and cannot be used on its own.
 */
public class PageItem extends Item {

    private final PageTier tier;

    public PageItem(PageTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public PageTier tier() {
        return this.tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.skylasliver_expbook.page_capacity", this.tier.points())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.skylasliver_expbook.page_usage")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
