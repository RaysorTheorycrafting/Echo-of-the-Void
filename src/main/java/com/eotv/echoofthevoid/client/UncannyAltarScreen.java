package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.menu.UncannyAltarMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class UncannyAltarScreen extends AbstractContainerScreen<UncannyAltarMenu> {
    private static final int PANEL_WIDTH = 210;
    private static final int PANEL_HEIGHT = 130;
    private boolean purgeConfirmationArmed;
    private Button purgeButton;

    public UncannyAltarScreen(UncannyAltarMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = PANEL_WIDTH;
        this.imageHeight = PANEL_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        int left = this.leftPos;
        int top = this.topPos;
        int buttonWidth = 92;
        int buttonHeight = 20;
        int leftColumn = left + 12;
        int rightColumn = left + 106;
        int row1 = top + 24;
        int row2 = top + 48;
        int row3 = top + 72;

        purgeButton = Button.builder(
                        Component.translatable("gui.echoofthevoid.uncanny_altar.purge"),
                        button -> onPurgePressed())
                .bounds(leftColumn, row1, buttonWidth, buttonHeight)
                .build();
        addRenderableWidget(purgeButton);
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.echoofthevoid.uncanny_altar.phase1"),
                        button -> sendPhaseButton(UncannyAltarMenu.BUTTON_PHASE_1))
                .bounds(rightColumn, row1, buttonWidth, buttonHeight)
                .build());
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.echoofthevoid.uncanny_altar.phase2"),
                        button -> sendPhaseButton(UncannyAltarMenu.BUTTON_PHASE_2))
                .bounds(leftColumn, row2, buttonWidth, buttonHeight)
                .build());
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.echoofthevoid.uncanny_altar.phase3"),
                        button -> sendPhaseButton(UncannyAltarMenu.BUTTON_PHASE_3))
                .bounds(rightColumn, row2, buttonWidth, buttonHeight)
                .build());
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.echoofthevoid.uncanny_altar.phase4"),
                        button -> sendPhaseButton(UncannyAltarMenu.BUTTON_PHASE_4))
                .bounds(left + 59, row3, buttonWidth, buttonHeight)
                .build());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;
        guiGraphics.fill(left, top, left + this.imageWidth, top + this.imageHeight, 0xE0151515);
        guiGraphics.fill(left + 2, top + 2, left + this.imageWidth - 2, top + this.imageHeight - 2, 0xCC060606);
        guiGraphics.fill(left + 6, top + 18, left + this.imageWidth - 6, top + this.imageHeight - 6, 0x66101010);

        if (purgeConfirmationArmed) {
            guiGraphics.drawCenteredString(
                    this.font,
                    Component.translatable("gui.echoofthevoid.uncanny_altar.purge_warning_line1"),
                    left + this.imageWidth / 2,
                    top + this.imageHeight - 30,
                    0xFFCC5555);
            guiGraphics.drawCenteredString(
                    this.font,
                    Component.translatable("gui.echoofthevoid.uncanny_altar.purge_warning_line2"),
                    left + this.imageWidth / 2,
                    top + this.imageHeight - 20,
                    0xFFD3D3D3);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xE0E0E0, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void sendPhaseButton(int buttonId) {
        purgeConfirmationArmed = false;
        if (purgeButton != null) {
            purgeButton.setMessage(Component.translatable("gui.echoofthevoid.uncanny_altar.purge"));
        }
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
        }
    }

    private void onPurgePressed() {
        if (this.minecraft == null || this.minecraft.gameMode == null) {
            return;
        }

        if (!purgeConfirmationArmed) {
            purgeConfirmationArmed = true;
            if (purgeButton != null) {
                purgeButton.setMessage(Component.translatable("gui.echoofthevoid.uncanny_altar.purge_confirm"));
            }
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, UncannyAltarMenu.BUTTON_PURGE);
            return;
        }

        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, UncannyAltarMenu.BUTTON_PURGE_CONFIRM);
    }
}
