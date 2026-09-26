package dev.skylasliver.expbook.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.skylasliver.expbook.ExperienceMath;
import dev.skylasliver.expbook.component.BookContents;
import dev.skylasliver.expbook.item.ExperienceBookItem;
import dev.skylasliver.expbook.menu.BookAdminMenu;
import dev.skylasliver.expbook.page.PageTier;
import dev.skylasliver.expbook.registry.ModComponents;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The same cloth panel, typography and entrance motion as the experience book. */
public final class BookAdminScreen extends AbstractContainerScreen<BookAdminMenu> {
    private static final int INK = 0xFF172722, GOLD = 0xFFC6AD78, PAPER = 0xFFEDE5CE;
    private static final int MUTED = 0xFFA2B1A0, JADE = 0xFF92C5A2;
    private static final ResourceLocation UI_FONT = ResourceLocation.withDefaultNamespace("uniform");
    private static final ResourceLocation PANEL = ResourceLocation.fromNamespaceAndPath(
            "skylasliver_expbook", "textures/gui/book_panel.png");
    private static final ResourceLocation COMPACT = ResourceLocation.fromNamespaceAndPath(
            "skylasliver_expbook", "textures/gui/book_panel_compact.png");
    private Button previous, next, recover, take;
    private int selected = -1, lastPage = -1;
    private int confirming = -1;
    private boolean wide;
    private long openedAt;

    public BookAdminScreen(BookAdminMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 280;
        imageHeight = 190;
    }

    private static Component label(String key, Object... args) {
        return Component.translatable("gui.skylasliver_expbook.admin." + key, args);
    }

    private static Component type(Component text) {
        return text.copy().withStyle(s -> s.withFont(UI_FONT).withBold(false).withItalic(false));
    }

    @Override protected void init() {
        wide = width >= 300;
        imageWidth = wide ? 280 : 176;
        super.init();
        if (openedAt == 0) openedAt = System.nanoTime();
        previous = addRenderableWidget(new LedgerButton(22, Component.literal("<"), b -> turnPage(BookAdminMenu.PREVIOUS)));
        next = addRenderableWidget(new LedgerButton(22, Component.literal(">"), b -> turnPage(BookAdminMenu.NEXT)));
        previous.setTooltip(Tooltip.create(label("previous")));
        next.setTooltip(Tooltip.create(label("next")));
        recover = addRenderableWidget(new LedgerButton(78, label("recover"), b -> recover(false)));
        take = addRenderableWidget(new LedgerButton(78, label("take"), b -> recover(true)));
        recover.setTooltip(Tooltip.create(label("recover_help")));
        take.setTooltip(Tooltip.create(label("take_help")));
        updateButtons();
    }

    private void recover(boolean toAdmin) {
        if (selected < 0 || !menu.getSlot(selected).hasItem()) return;
        int action = selected + (toAdmin ? BookAdminMenu.TAKE_BASE : 0);
        if (confirming != action) confirming = action;
        else { send(action); selected = -1; confirming = -1; }
    }

    private void send(int action) {
        if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action);
    }

    private void turnPage(int action) {
        selected = -1;
        confirming = -1;
        send(action);
    }

    private void updateButtons() {
        if (menu.page() != lastPage) {
            selected = -1;
            confirming = -1;
            lastPage = menu.page();
        }
        if (selected >= 0 && !menu.getSlot(selected).hasItem()) { selected = -1; confirming = -1; }
        previous.active = menu.page() > 0;
        next.active = menu.page() + 1 < menu.pageCount();
        recover.active = selected >= 0;
        take.active = recover.active;
        recover.setMessage(type(label(confirming == selected && selected >= 0 ? "confirm" : "recover")));
        take.setMessage(type(label(selected >= 0 && confirming == selected + BookAdminMenu.TAKE_BASE ? "confirm_take" : "take")));
        previous.setPosition(leftPos + 8, topPos + 150);
        next.setPosition(leftPos + 146, topPos + 150);
        recover.setPosition(leftPos + (wide ? 190 : 8), topPos + (wide ? 143 : 168));
        take.setPosition(leftPos + (wide ? 190 : 90), topPos + (wide ? 164 : 168));
    }

    @Override public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBg(g, partialTick, mouseX, mouseY);
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float t = Mth.clamp((System.nanoTime() - openedAt) / 320_000_000.0F, 0, 1);
        leftPos = (width - imageWidth) / 2;
        topPos = (height - imageHeight) / 2 + Math.round(12 * (1 - t) * (1 - t) * (1 - t));
        updateButtons();
        super.render(g, mouseX, mouseY, partialTick);
        // Tooltips must be rendered after the container and widgets, in screen coordinates.
        if (hoveredSlot != null && hoveredSlot.hasItem())
            g.renderTooltip(font, details(hoveredSlot), Optional.empty(), mouseX, mouseY);
    }

    @Override protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(wide ? PANEL : COMPACT, leftPos, topPos, imageWidth, imageHeight,
                0, 0, imageWidth * 4, imageHeight * 4, imageWidth * 4, imageHeight * 4);
        RenderSystem.disableBlend();
        // Reuse the book frame/header, replacing its inventory area with six ledger rows.
        g.fill(leftPos + 6, topPos + 33, leftPos + imageWidth - 6, topPos + 186, 0xFF334A3C);
        for (Slot slot : menu.slots) {
            int x = leftPos + slot.x, y = topPos + slot.y;
            g.fill(x - 1, y - 1, x + 17, y + 17, slot.index == selected ? GOLD : INK);
            g.fill(x, y, x + 16, y + 16, slot.hasItem() ? 0xFF50614B : 0xFF3B5141);
        }
        if (wide) g.fill(leftPos + 181, topPos + 36, leftPos + 182, topPos + 183, 0xFF64775D);
    }

    private void text(GuiGraphics g, Component value, int x, int y, int color, int maxWidth) {
        g.drawString(font, net.minecraft.locale.Language.getInstance().getVisualOrder(
                font.substrByWidth(type(value), maxWidth)), x, y, color, false);
    }

    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        text(g, title, 8, 8, PAPER, imageWidth - 16);
        text(g, label("subtitle", menu.ids().size()), 8, 21, MUTED, imageWidth - 16);
        Component page = type(label("page", menu.page() + 1, menu.pageCount()));
        g.drawString(font, page, 88 - font.width(page) / 2, 155, GOLD, false);
        if (menu.ids().isEmpty()) {
            g.fill(7, 39, 170, 148, 0xFF334A3C);
            text(g, label("empty"), 14, 84, MUTED, 150);
        }
        if (!wide) return;
        Slot focus = hoveredSlot != null && hoveredSlot.hasItem() ? hoveredSlot
                : selected >= 0 ? menu.getSlot(selected) : null;
        text(g, label("details"), 190, 42, GOLD, 78);
        if (focus != null && focus.hasItem()) {
            ItemStack book = focus.getItem();
            BookContents contents = ExperienceBookItem.contents(book);
            text(g, book.getHoverName(), 190, 58, PAPER, 78);
            text(g, Component.literal(String.format(java.util.Locale.ROOT, "%,d XP", contents.storedPoints())),
                    190, 78, JADE, 78);
            text(g, Component.literal("/ " + String.format(java.util.Locale.ROOT, "%,d", contents.capacity())),
                    190, 91, MUTED, 78);
            text(g, Component.translatable("gui.skylasliver_expbook.pages", contents.pages().size(), BookContents.MAX_PAGES),
                    190, 112, PAPER, 78);
            text(g, label("id", book.getOrDefault(ModComponents.BOOK_NUMBER.get(), 0L)), 190, 128, GOLD, 78);
        } else {
            text(g, label("hover"), 190, 62, PAPER, 78);
            text(g, label("select"), 190, 78, MUTED, 78);
        }
        text(g, label("snapshot"), 8, 175, MUTED, 162);

    }

    private List<Component> details(Slot slot) {
        ItemStack book = slot.getItem();
        BookContents contents = ExperienceBookItem.contents(book);
        List<Component> lines = new ArrayList<>();
        lines.add(book.getHoverName().copy().withStyle(s -> s.withColor(0xEDE5CE)));
        long number = book.getOrDefault(ModComponents.BOOK_NUMBER.get(), 0L);
        lines.add(label("id", number > 0 ? Long.toString(number) : "—"));
        lines.add(label("owner", book.getOrDefault(ModComponents.OWNER_NAME.get(), menu.owner().toString())));
        lines.add(Component.translatable("gui.skylasliver_expbook.stored", contents.storedPoints(), contents.capacity()));
        lines.add(Component.translatable("gui.skylasliver_expbook.level", ExperienceMath.levelForPoints(contents.storedPoints())));
        lines.add(Component.translatable("gui.skylasliver_expbook.pages", contents.pages().size(), BookContents.MAX_PAGES));
        lines.add(label("contents"));
        if (contents.pages().isEmpty()) lines.add(Component.translatable("tooltip.skylasliver_expbook.no_pages"));
        for (PageTier tier : PageTier.values()) {
            long count = contents.pages().stream().filter(p -> p == tier).count();
            if (count > 0) lines.add(Component.literal("  ").append(tier.displayName()).append(" × " + count));
        }
        lines.add(label("snapshot").copy().withStyle(s -> s.withColor(0xA2B1A0)));
        return lines;
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (Slot slot : menu.slots) {
                if (isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY) && slot.hasItem()) {
                    selected = slot.index;
                    confirming = -1;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private final class LedgerButton extends Button {
        private LedgerButton(int width, Component message, OnPress onPress) {
            super(0, 0, width, 18, type(message), onPress, DEFAULT_NARRATION);
        }
        @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int edge = active && isHoveredOrFocused() ? GOLD : 0xFF64775D;
            g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), edge);
            g.fill(getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + getHeight() - 1,
                    active && isHoveredOrFocused() ? 0xFF50614B : 0xFF263C30);
            g.drawString(font, getMessage(), getX() + (getWidth() - font.width(getMessage())) / 2,
                    getY() + 5, active ? PAPER : MUTED, false);
        }
    }
}
