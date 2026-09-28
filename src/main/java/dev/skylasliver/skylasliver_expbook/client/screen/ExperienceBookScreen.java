package dev.skylasliver.skylasliver_expbook.client.screen;

import dev.skylasliver.skylasliver_expbook.ExperienceMath;
import dev.skylasliver.skylasliver_expbook.component.BookContents;
import dev.skylasliver.skylasliver_expbook.item.ExperienceBookItem;
import dev.skylasliver.skylasliver_expbook.menu.ExperienceBookMenu;
import net.minecraft.client.gui.GuiGraphics;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/** Ink-green cloth, brass rules and paper-white typography; no world dimming. */
public class ExperienceBookScreen extends AbstractContainerScreen<ExperienceBookMenu> {
    private static final int INK = 0xFF172722;
    private static final int GOLD = 0xFFC6AD78;
    private static final int PAPER = 0xFFEDE5CE;
    private static final int MUTED = 0xFFA2B1A0;
    private static final int JADE = 0xFF92C5A2;
    private static final int WARNING = 0xFFE8AD87;
    private static final ResourceLocation UI_FONT = ResourceLocation.fromNamespaceAndPath("minecraft", "uniform");
    private static final ResourceLocation PANEL = ResourceLocation.fromNamespaceAndPath(
            "skylasliver_expbook", "textures/gui/book_panel.png");
    private static final ResourceLocation PANEL_COMPACT = ResourceLocation.fromNamespaceAndPath(
            "skylasliver_expbook", "textures/gui/book_panel_compact.png");
    private long openedAt;
    private float progress;
    private float age;
    private boolean wide;

    public ExperienceBookScreen(ExperienceBookMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 280;
        imageHeight = 190;
    }

    @Override
    protected void init() {
        wide = width >= 300;
        imageWidth = wide ? 280 : 176;
        super.init();
        // Resizing must not restart the entrance animation.
        if (openedAt == 0L) openedAt = System.nanoTime();
    }

    /** AbstractContainerScreen calls this itself. Never draw its full-screen dim layer. */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBg(graphics, partialTick, mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        age = (System.nanoTime() - openedAt) / 1_000_000_000.0F;
        float t = Mth.clamp(age / 0.32F, 0.0F, 1.0F);
        progress = 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
        // Move the actual container origin, not its pose: slots, hitboxes and tooltips
        // stay aligned. Integer coordinates also avoid scaling/blurring the font.
        leftPos = (width - imageWidth) / 2;
        topPos = (height - imageHeight) / 2 + Math.round(12.0F * (1.0F - progress));
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        // Four physical texture pixels per GUI unit. Text/items remain independent
        // and crisp; the original slot coordinates and transparent corners are retained.
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(wide ? PANEL : PANEL_COMPACT, x, y, imageWidth, imageHeight,
                0.0F, 0.0F, imageWidth * 4, imageHeight * 4, imageWidth * 4, imageHeight * 4);
        RenderSystem.disableBlend();
        if (wide) {
            BookContents c = ExperienceBookItem.contents(menu.book());
            float ratio = c.capacity() <= 0 ? 0 : Mth.clamp((float)c.storedPoints() / c.capacity(), 0, 1);

            int filled = Math.round(78 * ratio * progress);
            if (filled > 0) g.fillGradient(x + 190, y + 104, x + 190 + filled, y + 108, 0xFFB2DDB5, 0xFF528566);
            // A slow, bounded highlight, independent of the completed opening timer.
            if (filled > 3) {
                int shine = Math.round((filled - 2) * (0.5F + 0.5F * Mth.sin(age * 1.3F)));
                g.fill(x + 190 + shine, y + 104, x + 192 + shine, y + 105, PAPER);
            }
        }
    }

    private Component type(Component text) {
        return text.copy().withStyle(style -> style.withFont(UI_FONT).withBold(false).withItalic(false));
    }

    private void text(GuiGraphics g, Component value, int x, int y, int color, int maxWidth) {
        // One font and one weight for every label; truncate long custom names safely.
        g.drawString(font, net.minecraft.locale.Language.getInstance().getVisualOrder(font.substrByWidth(type(value), maxWidth)), x, y, color, false);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        BookContents c = ExperienceBookItem.contents(menu.book());
        text(g, title, 8, 8, PAPER, wide ? 165 : 90);
        Component pages = type(Component.translatable("gui.skylasliver_expbook.pages", c.pages().size(), BookContents.MAX_PAGES));
        g.drawString(font, pages, imageWidth - 8 - font.width(pages), 9, GOLD, false);
        if (wide) text(g, Component.translatable("gui.skylasliver_expbook.ui.subtitle"), 8, 20, MUTED, imageWidth - 16);
        text(g, playerInventoryTitle, 8, 98, MUTED, 158);
        if (wide) {
            text(g, Component.translatable("gui.skylasliver_expbook.ui.reserve"), 190, 42, GOLD, 78);
            text(g, Component.translatable("gui.skylasliver_expbook.level", ExperienceMath.levelForPoints(c.storedPoints())), 190, 58, PAPER, 78);
            text(g, Component.literal(String.format(java.util.Locale.ROOT, "%,d XP", c.storedPoints())), 190, 78, JADE, 78);
            text(g, Component.literal("/ " + String.format(java.util.Locale.ROOT, "%,d", c.capacity())), 190, 91, MUTED, 78);
            text(g, Component.translatable("gui.skylasliver_expbook.ui.pages_hint"), 190, 122, PAPER, 78);
            text(g, Component.translatable("gui.skylasliver_expbook.ui.capacity_hint"), 190, 135, MUTED, 78);
            text(g, Component.translatable("gui.skylasliver_expbook.ui.close"), 190, 169, MUTED, 78);
        } else {
            text(g, Component.translatable("gui.skylasliver_expbook.stored", c.storedPoints(), c.capacity()), 8, 20, JADE, 160);
        }
        if (c.storedPoints() > c.capacity()) {
            // Replace the inventory label, rather than drawing over the page slots.
            g.fill(7, 97, 170, 108, INK);
            text(g, Component.translatable("gui.skylasliver_expbook.over_capacity"), 8, 98, WARNING, 160);
        }
    }
}



