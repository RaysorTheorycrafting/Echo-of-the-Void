package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.dev.UncannyDevCatalog;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class UncannyDevMenuScreen extends Screen {
    private static final Component TITLE = Component.literal("Uncanny Dev Debug Menu");
    private static final int GROUPS_PER_PAGE = 12;
    private static final int COLOR_PANEL = 0xCC111111;
    private static final int COLOR_BORDER = 0xAA2A2A2A;
    private static final int COLOR_TEXT = 0xFFE0E0E0;
    private static final int COLOR_GRAY = 0xFF8B8B8B;
    private static final int COLOR_ORANGE = 0xFFFFA33D;
    private static final int COLOR_GREEN = 0xFF57D87A;

    private UncannyDevCatalog.Category selectedCategory = UncannyDevCatalog.Category.ENTITIES;
    private String selectedGroupKey;

    private EditBox searchBox;
    private RowList rowList;
    private Button backButton;
    private Button prevPageButton;
    private Button nextPageButton;
    private int groupPage;
    private int groupPages = 1;

    private final List<Button> categoryButtons = new ArrayList<>();
    private final List<UncannyDevCatalog.Category> categoryOrder = new ArrayList<>();

    public UncannyDevMenuScreen() {
        super(TITLE);
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        this.categoryButtons.clear();
        this.categoryOrder.clear();

        int panelLeft = 12;
        int panelTop = 12;
        int panelRight = this.width - 12;
        int panelBottom = this.height - 12;

        int searchWidth = Math.min(240, panelRight - panelLeft - 24);
        this.searchBox = new EditBox(this.font, panelLeft + 10, panelTop + 10, searchWidth, 18, Component.literal("Search"));
        updateSearchHint();
        this.searchBox.setResponder(value -> rebuildRows());
        this.addRenderableWidget(this.searchBox);

        int buttonX = panelLeft + 10 + searchWidth + 8;
        int buttonY = panelTop + 10;
        int buttonWidth = 88;
        int buttonHeight = 18;
        int index = 0;
        for (UncannyDevCatalog.Category category : UncannyDevCatalog.primaryCategories()) {
            int x = buttonX + (index % 3) * (buttonWidth + 4);
            int y = buttonY + (index / 3) * (buttonHeight + 3);
            Button button = Button.builder(Component.literal(category.label()), clicked -> {
                        this.selectedCategory = category;
                        this.selectedGroupKey = null;
                        this.groupPage = 0;
                        this.searchBox.setValue("");
                        updateSearchHint();
                        updateCategoryButtons();
                        updateBackButton();
                        rebuildRows();
                    })
                    .bounds(x, y, buttonWidth, buttonHeight)
                    .build();
            this.categoryButtons.add(button);
            this.categoryOrder.add(category);
            this.addRenderableWidget(button);
            index++;
        }
        updateCategoryButtons();

        int listTop = panelTop + 62;
        int listBottom = panelBottom - 28;
        this.rowList = new RowList(
                this.minecraft,
                this.width,
                this.height,
                listTop,
                listBottom,
                22,
                panelLeft + 10,
                panelRight - panelLeft - 20);
        this.addRenderableWidget(this.rowList);

        this.backButton = this.addRenderableWidget(Button.builder(Component.literal("Back"), button -> {
                    this.selectedGroupKey = null;
                    this.groupPage = 0;
                    this.searchBox.setValue("");
                    updateSearchHint();
                    updateBackButton();
                    rebuildRows();
                })
                .bounds(panelRight - 136, panelBottom - 22, 58, 14)
                .build());

        this.prevPageButton = this.addRenderableWidget(Button.builder(Component.literal("Prev"), button -> {
                    if (this.groupPage > 0) {
                        this.groupPage--;
                        rebuildRows();
                    }
                })
                .bounds(panelRight - 264, panelBottom - 22, 50, 14)
                .build());

        this.nextPageButton = this.addRenderableWidget(Button.builder(Component.literal("Next"), button -> {
                    if (this.groupPage + 1 < this.groupPages) {
                        this.groupPage++;
                        rebuildRows();
                    }
                })
                .bounds(panelRight - 208, panelBottom - 22, 50, 14)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Close"), button -> this.onClose())
                .bounds(panelRight - 70, panelBottom - 22, 58, 14)
                .build());

        updateBackButton();
        updatePaginationButtons();
        rebuildRows();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        int panelLeft = 12;
        int panelTop = 12;
        int panelRight = this.width - 12;
        int panelBottom = this.height - 12;

        guiGraphics.fill(panelLeft, panelTop, panelRight, panelBottom, COLOR_PANEL);
        guiGraphics.fill(panelLeft, panelTop, panelRight, panelTop + 1, COLOR_BORDER);
        guiGraphics.fill(panelLeft, panelBottom - 1, panelRight, panelBottom, COLOR_BORDER);
        guiGraphics.fill(panelLeft, panelTop, panelLeft + 1, panelBottom, COLOR_BORDER);
        guiGraphics.fill(panelRight - 1, panelTop, panelRight, panelBottom, COLOR_BORDER);

        guiGraphics.drawString(this.font, TITLE, panelLeft + 10, panelTop - 9, COLOR_TEXT, false);
        guiGraphics.drawString(this.font, Component.literal(currentPath()), panelLeft + 10, panelTop + 34, 0xFFBDBDBD, false);
        if (this.selectedGroupKey == null) {
            guiGraphics.drawString(
                    this.font,
                    Component.literal("Page " + (this.groupPage + 1) + "/" + this.groupPages),
                    panelRight - 124,
                    panelTop + 34,
                    0xFFBDBDBD,
                    false);
        }
        guiGraphics.drawString(
                this.font,
                Component.literal("LMB: open/trigger  |  RMB: green toggle  |  Shift+RMB: reset gray"),
                panelLeft + 10,
                panelBottom - 20,
                0xFF9F9F9F,
                false);

        int legendX = panelLeft + 10;
        int legendY = panelTop + 48;
        guiGraphics.drawString(this.font, Component.literal("N"), legendX, legendY, COLOR_GRAY, false);
        guiGraphics.drawString(this.font, Component.literal("Not tested"), legendX + 10, legendY, COLOR_TEXT, false);
        guiGraphics.drawString(this.font, Component.literal("R"), legendX + 90, legendY, COLOR_ORANGE, false);
        guiGraphics.drawString(this.font, Component.literal("Review"), legendX + 100, legendY, COLOR_TEXT, false);
        guiGraphics.drawString(this.font, Component.literal("G"), legendX + 156, legendY, COLOR_GREEN, false);
        guiGraphics.drawString(this.font, Component.literal("Validated"), legendX + 166, legendY, COLOR_TEXT, false);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.rowList != null && this.rowList.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void rebuildRows() {
        if (this.rowList == null) {
            return;
        }

        String search = this.searchBox == null ? "" : this.searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        List<MenuRow> rows = new ArrayList<>();

        if (this.selectedGroupKey == null) {
            List<UncannyDevCatalog.Group> matchingGroups = new ArrayList<>();
            for (UncannyDevCatalog.Group group : UncannyDevCatalog.groups(this.selectedCategory)) {
                if (!search.isEmpty()) {
                    String haystack = (group.label() + " " + group.key()).toLowerCase(Locale.ROOT);
                    if (!haystack.contains(search)) {
                        continue;
                    }
                }
                matchingGroups.add(group);
            }

            this.groupPages = Math.max(1, (matchingGroups.size() + GROUPS_PER_PAGE - 1) / GROUPS_PER_PAGE);
            this.groupPage = Math.max(0, Math.min(this.groupPage, this.groupPages - 1));

            int from = this.groupPage * GROUPS_PER_PAGE;
            int to = Math.min(matchingGroups.size(), from + GROUPS_PER_PAGE);
            for (int i = from; i < to; i++) {
                rows.add(new MenuRow(matchingGroups.get(i), null));
            }
        } else {
            this.groupPages = 1;
            this.groupPage = 0;
            for (UncannyDevCatalog.Entry entry : UncannyDevCatalog.entries(this.selectedCategory, this.selectedGroupKey)) {
                if (!search.isEmpty()) {
                    String haystack = (entry.label() + " " + entry.id()).toLowerCase(Locale.ROOT);
                    if (!haystack.contains(search)) {
                        continue;
                    }
                }
                rows.add(new MenuRow(null, entry));
            }
        }

        this.rowList.replaceEntries(rows);
        updatePaginationButtons();
    }

    private void updateCategoryButtons() {
        for (int i = 0; i < this.categoryButtons.size(); i++) {
            Button button = this.categoryButtons.get(i);
            UncannyDevCatalog.Category category = this.categoryOrder.get(i);
            button.active = category != this.selectedCategory;
        }
    }

    private void updateBackButton() {
        if (this.backButton != null) {
            this.backButton.active = this.selectedGroupKey != null;
        }
    }

    private void updatePaginationButtons() {
        if (this.prevPageButton == null || this.nextPageButton == null) {
            return;
        }
        boolean onGroups = this.selectedGroupKey == null;
        this.prevPageButton.visible = onGroups;
        this.nextPageButton.visible = onGroups;
        this.prevPageButton.active = onGroups && this.groupPage > 0;
        this.nextPageButton.active = onGroups && this.groupPage + 1 < this.groupPages;
    }

    private void updateSearchHint() {
        if (this.searchBox == null) {
            return;
        }
        this.searchBox.setHint(Component.literal(this.selectedGroupKey == null ? "Search groups..." : "Search variants/actions..."));
    }

    private String currentPath() {
        if (this.selectedGroupKey == null) {
            return this.selectedCategory.label() + " > Groups";
        }
        for (UncannyDevCatalog.Group group : UncannyDevCatalog.groups(this.selectedCategory)) {
            if (group.key().equals(this.selectedGroupKey)) {
                return this.selectedCategory.label() + " > " + group.label();
            }
        }
        return this.selectedCategory.label() + " > " + this.selectedGroupKey;
    }

    private final class RowList extends ObjectSelectionList<MenuRow> {
        private final int rowLeft;
        private final int rowWidth;

        private RowList(
                net.minecraft.client.Minecraft minecraft,
                int width,
                int height,
                int y0,
                int y1,
                int itemHeight,
                int rowLeft,
                int rowWidth) {
            super(minecraft, width, Math.max(1, y1 - y0), y0, itemHeight);
            this.rowLeft = rowLeft;
            this.rowWidth = rowWidth;
        }

        private void replaceEntries(List<MenuRow> rows) {
            this.clearEntries();
            for (MenuRow row : rows) {
                this.addEntry(row);
            }
        }

        @Override
        protected int getScrollbarPosition() {
            return this.rowLeft + this.rowWidth - 6;
        }

        @Override
        public int getRowLeft() {
            return this.rowLeft;
        }

        @Override
        public int getRowWidth() {
            return this.rowWidth - 12;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 1) {
                MenuRow row = this.getEntryAtPosition(mouseX, mouseY);
                if (row != null) {
                    return row.mouseClicked(mouseX, mouseY, button);
                }
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
    }

    private final class MenuRow extends ObjectSelectionList.Entry<MenuRow> {
        private final UncannyDevCatalog.Group group;
        private final UncannyDevCatalog.Entry entry;

        private MenuRow(UncannyDevCatalog.Group group, UncannyDevCatalog.Entry entry) {
            this.group = group;
            this.entry = entry;
        }

        @Override
        public void render(
                GuiGraphics guiGraphics,
                int index,
                int top,
                int left,
                int width,
                int height,
                int mouseX,
                int mouseY,
                boolean hovered,
                float partialTick) {
            int bgColor = hovered ? 0x442C2C2C : 0x221A1A1A;
            guiGraphics.fill(left, top, left + width, top + height - 1, bgColor);

            if (this.group != null) {
                renderGroup(guiGraphics, top, left);
            } else if (this.entry != null) {
                renderEntry(guiGraphics, top, left);
            }
        }

        private void renderGroup(GuiGraphics guiGraphics, int top, int left) {
            List<UncannyDevCatalog.Entry> entries = UncannyDevCatalog.entries(UncannyDevMenuScreen.this.selectedCategory, this.group.key());
            int total = entries.size();
            int green = 0;
            int review = 0;

            for (UncannyDevCatalog.Entry groupEntry : entries) {
                UncannyDevCatalog.QaStatus status = UncannyDevMenuClientState.statusOf(groupEntry.id());
                if (status == UncannyDevCatalog.QaStatus.GREEN) {
                    green++;
                } else if (status == UncannyDevCatalog.QaStatus.ORANGE) {
                    review++;
                }
            }

            int color = green == total && total > 0 ? COLOR_GREEN : (green > 0 || review > 0 ? COLOR_ORANGE : COLOR_GRAY);
            guiGraphics.drawString(UncannyDevMenuScreen.this.font, Component.literal(">"), left + 4, top + 7, color, false);
            guiGraphics.drawString(UncannyDevMenuScreen.this.font, this.group.label(), left + 18, top + 3, COLOR_TEXT, false);
            guiGraphics.drawString(
                    UncannyDevMenuScreen.this.font,
                    Component.literal(green + "/" + total + " validated"),
                    left + 18,
                    top + 13,
                    0xFF7B7B7B,
                    false);
        }

        private void renderEntry(GuiGraphics guiGraphics, int top, int left) {
            UncannyDevCatalog.QaStatus status = UncannyDevMenuClientState.statusOf(this.entry.id());
            int statusColor = switch (status) {
                case GREEN -> COLOR_GREEN;
                case ORANGE -> COLOR_ORANGE;
                case GRAY -> COLOR_GRAY;
            };
            String statusGlyph = switch (status) {
                case GREEN -> "G";
                case ORANGE -> "R";
                case GRAY -> "N";
            };

            guiGraphics.drawString(UncannyDevMenuScreen.this.font, statusGlyph, left + 4, top + 7, statusColor, false);
            guiGraphics.drawString(UncannyDevMenuScreen.this.font, this.entry.label(), left + 18, top + 3, statusColor, false);
            guiGraphics.drawString(UncannyDevMenuScreen.this.font, this.entry.id(), left + 18, top + 13, 0xFF7B7B7B, false);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            UncannyDevMenuScreen.this.rowList.setSelected(this);

            if (this.group != null) {
                if (button == 0) {
                    UncannyDevMenuScreen.this.selectedGroupKey = this.group.key();
                    UncannyDevMenuScreen.this.searchBox.setValue("");
                    UncannyDevMenuScreen.this.updateSearchHint();
                    UncannyDevMenuScreen.this.updateBackButton();
                    UncannyDevMenuScreen.this.rebuildRows();
                    return true;
                }
                return false;
            }

            if (this.entry == null) {
                return false;
            }

            if (button == 0) {
                UncannyDevMenuClientState.requestTrigger(this.entry.id());
                return true;
            }
            if (button == 1) {
                if (UncannyDevMenuScreen.hasShiftDown()) {
                    UncannyDevMenuClientState.requestSetGreen(this.entry.id(), false);
                } else {
                    boolean isGreen = UncannyDevMenuClientState.statusOf(this.entry.id()) == UncannyDevCatalog.QaStatus.GREEN;
                    UncannyDevMenuClientState.requestSetGreen(this.entry.id(), !isGreen);
                }
                return true;
            }
            return false;
        }

        @Override
        public Component getNarration() {
            if (this.group != null) {
                return Component.literal(this.group.label());
            }
            if (this.entry != null) {
                return Component.literal(this.entry.label());
            }
            return Component.literal("Dev row");
        }
    }
}
