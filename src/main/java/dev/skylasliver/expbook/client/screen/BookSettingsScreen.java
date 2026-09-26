package dev.skylasliver.expbook.client.screen;

import dev.skylasliver.expbook.menu.BookSettingsMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class BookSettingsScreen extends AbstractContainerScreen<BookSettingsMenu> {
    private final Button[] toggles = new Button[3];
    private final Button[] levels = new Button[4];
    private static final String[] FEATURES = {"collect", "mend", "pump"};
    public BookSettingsScreen(BookSettingsMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 280;
        imageHeight = 218;
    }
    private static net.minecraft.network.chat.MutableComponent text(String key, Object... args) {
        return Component.translatable("gui.skylasliver_expbook." + key, args);
    }
    @Override
    protected void init() {
        super.init();
        for (int i = 0; i < 3; i++) {
            final int id = i;
            toggles[i] = addRenderableWidget(Button.builder(Component.empty(), b -> send(id))
                    .bounds(leftPos + 16, topPos + 36 + i * 30, 248, 22)
                    .tooltip(Tooltip.create(text(FEATURES[i] + "_help"))).build());
        }
        String[] labels = {"−10", "−1", "+1", "+10"};
        for (int i = 0; i < 4; i++) {
            final int id = i + 3;
            levels[i] = addRenderableWidget(Button.builder(Component.literal(labels[i]), b -> send(id))
                    .bounds(leftPos + 20 + i * 62, topPos + 149, 54, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(leftPos + 90, topPos + 184, 100, 20).build());
        updateButtons();
    }
    private void send(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }
    private void updateButtons() {
        for (int i = 0; i < 3; i++) {
            boolean allowed = (menu.allowed() & (1 << i)) != 0;
            toggles[i].active = allowed;
            toggles[i].setMessage(text(FEATURES[i]).append(": ").append(text(!allowed ? "disabled" : (menu.flags() & (1 << i)) != 0 ? "on" : "off")));
        }
        for (int i = 0; i < 4; i++) levels[i].active = (menu.allowed() & 4) != 0
                && (i < 2 ? menu.target() > 0 : menu.target() < menu.maximum());
    }
    @Override
    protected void containerTick() { super.containerTick(); updateButtons(); }
    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF20382C);
        graphics.fill(leftPos + 3, topPos + 3, leftPos + imageWidth - 3, topPos + imageHeight - 3, 0xFF304C3C);
    }
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, title, imageWidth / 2, 12, 0xF2EAD6);
        graphics.drawCenteredString(font, text("target", menu.target()), imageWidth / 2, 131, 0xF2EAD6);
    }
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
