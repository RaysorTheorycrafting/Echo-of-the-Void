package com.eotv.echoofthevoid.client;

import com.eotv.echoofthevoid.dev.UncannyDevCatalog;
import com.eotv.echoofthevoid.dev.UncannyDevMetadataCatalog;
import com.eotv.echoofthevoid.dev.UncannyDevQueryEngine;
import com.eotv.echoofthevoid.network.UncannyDevMenuResultPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/** Search-first QA workbench. All actions remain server-authoritative and permission checked. */
public final class UncannyDevMenuScreen extends Screen {
    private static final Component TITLE = Component.literal("Echo of the Void — QA Workbench");
    private static final int COLOR_BACKGROUND = 0xE50C0D11;
    private static final int COLOR_PANEL = 0xE515171D;
    private static final int COLOR_PANEL_ALT = 0xE51A1D24;
    private static final int COLOR_BORDER = 0xFF343946;
    private static final int COLOR_TEXT = 0xFFE7E9EF;
    private static final int COLOR_MUTED = 0xFF9299A8;
    private static final int COLOR_ACCENT = 0xFF9E8CFF;
    private static final int COLOR_GRAY = 0xFF8B8F99;
    private static final int COLOR_ORANGE = 0xFFFFA33D;
    private static final int COLOR_GREEN = 0xFF57D87A;
    private static final int COLOR_RED = 0xFFFF6678;

    private UncannyDevCatalog.Category category = UncannyDevCatalog.Category.ALL;
    private UncannyDevQueryEngine.SortMode sortMode = UncannyDevQueryEngine.SortMode.NAME;
    private UncannyDevQueryEngine.QaFilter qaFilter = UncannyDevQueryEngine.QaFilter.ALL;
    private UncannyDevQueryEngine.Scope scope = UncannyDevQueryEngine.Scope.ALL;
    private UncannyDevMetadataCatalog.Danger dangerFilter;
    private int phaseFilter;
    private int spawnDistance = UncannyDevMenuClientState.preferredSpawnDistance();

    private EditBox searchBox;
    private EditBox targetBox;
    private EntryList entryList;
    private UncannyDevCatalog.Entry selectedEntry;
    private LivingEntity previewEntity;
    private float previewYaw = 25.0F;
    private float previewPitch = -8.0F;
    private float previewZoom = 1.0F;
    private boolean draggingPreview;
    private int previewLeft;
    private int previewTop;
    private int previewRight;
    private int previewBottom;
    private int detailScroll;
    private int maximumDetailScroll;

    private int listLeft;
    private int listTop;
    private int listBottom;
    private int listWidth;
    private int detailLeft;
    private int detailRight;
    private int detailTop;
    private int detailBottom;

    private Button categoryButton;
    private Button phaseButton;
    private Button dangerButton;
    private Button qaFilterButton;
    private Button scopeButton;
    private Button sortButton;
    private Button runButton;
    private Button repeatButton;
    private Button favoriteButton;
    private Button qaStatusButton;
    private Button previousVariantButton;
    private Button nextVariantButton;
    private Button distanceButton;

    private String lastClickedEntryId = "";
    private long lastClickMillis;

    public UncannyDevMenuScreen() {
        super(TITLE);
    }

    @Override
    protected void init() {
        super.init();
        String selectedId = this.selectedEntry == null ? "" : this.selectedEntry.id();
        this.clearWidgets();

        int margin = 10;
        int contentWidth = Math.max(1, this.width - margin * 2);
        this.listLeft = margin;
        this.listWidth = Mth.clamp((int) (contentWidth * 0.39F), 180, 340);
        this.listTop = 66;
        this.listBottom = Math.max(this.listTop + 24, this.height - 50);
        this.detailLeft = this.listLeft + this.listWidth + 7;
        this.detailRight = this.width - margin;
        this.detailTop = this.listTop;
        this.detailBottom = this.listBottom;

        this.searchBox = new EditBox(this.font, this.listLeft, 16, this.listWidth, 18, Component.literal("Global search"));
        this.searchBox.setHint(Component.literal("Search ID, behavior, sound, restriction…"));
        this.searchBox.setResponder(ignored -> rebuildEntries());
        this.addRenderableWidget(this.searchBox);

        int filterY = 40;
        int gap = 3;
        int filterWidth = Math.max(52, (contentWidth - gap * 5) / 6);
        int filterX = margin;
        this.categoryButton = addFilterButton(filterX, filterY, filterWidth, button -> {
            this.category = next(this.category, UncannyDevCatalog.Category.values());
            rebuildEntries();
        });
        filterX += filterWidth + gap;
        this.phaseButton = addFilterButton(filterX, filterY, filterWidth, button -> {
            this.phaseFilter = (this.phaseFilter + 1) % 5;
            rebuildEntries();
        });
        filterX += filterWidth + gap;
        this.dangerButton = addFilterButton(filterX, filterY, filterWidth, button -> {
            this.dangerFilter = nextDanger(this.dangerFilter);
            rebuildEntries();
        });
        filterX += filterWidth + gap;
        this.qaFilterButton = addFilterButton(filterX, filterY, filterWidth, button -> {
            this.qaFilter = next(this.qaFilter, UncannyDevQueryEngine.QaFilter.values());
            rebuildEntries();
        });
        filterX += filterWidth + gap;
        this.scopeButton = addFilterButton(filterX, filterY, filterWidth, button -> {
            this.scope = next(this.scope, UncannyDevQueryEngine.Scope.values());
            if (this.scope == UncannyDevQueryEngine.Scope.RECENT) {
                this.sortMode = UncannyDevQueryEngine.SortMode.RECENT;
            }
            rebuildEntries();
        });
        filterX += filterWidth + gap;
        this.sortButton = addFilterButton(filterX, filterY, this.width - margin - filterX, button -> {
            this.sortMode = next(this.sortMode, UncannyDevQueryEngine.SortMode.values());
            rebuildEntries();
        });

        this.entryList = new EntryList(this.minecraft, this.listWidth, this.listBottom - this.listTop, this.listTop, 25, this.listLeft, this.listWidth);
        this.entryList.setX(this.listLeft);
        this.addRenderableWidget(this.entryList);

        addActionControls();
        updateFilterLabels();
        rebuildEntries();
        if (!selectedId.isEmpty()) {
            selectEntry(UncannyDevCatalog.byId(selectedId));
        } else {
            String lastId = UncannyDevMenuClientState.lastEntryId();
            if (!lastId.isEmpty()) {
                selectEntry(UncannyDevCatalog.byId(lastId));
            }
        }
    }

    private Button addFilterButton(int x, int y, int width, java.util.function.Consumer<Button> action) {
        Button button = Button.builder(Component.empty(), action::accept)
                .bounds(x, y, Math.max(24, width), 18)
                .build();
        return this.addRenderableWidget(button);
    }

    private void addActionControls() {
        int margin = 10;
        int gap = 3;
        int contentWidth = Math.max(1, this.width - margin * 2);
        int rowOneWidth = Math.max(42, (contentWidth - gap * 5) / 6);
        int y1 = this.height - 43;
        int x = margin;

        this.runButton = addControl(x, y1, rowOneWidth, "Run", button -> runSelected());
        x += rowOneWidth + gap;
        this.repeatButton = addControl(x, y1, rowOneWidth, "Repeat last", button -> repeatLast());
        x += rowOneWidth + gap;
        this.favoriteButton = addControl(x, y1, rowOneWidth, "Favorite", button -> toggleFavorite());
        x += rowOneWidth + gap;
        this.qaStatusButton = addControl(x, y1, rowOneWidth, "Validate", button -> toggleQaStatus());
        x += rowOneWidth + gap;
        this.previousVariantButton = addControl(x, y1, rowOneWidth, "< Variant", button -> selectSibling(-1));
        x += rowOneWidth + gap;
        this.nextVariantButton = addControl(x, y1, this.width - margin - x, "Variant >", button -> selectSibling(1));

        int y2 = this.height - 22;
        int targetWidth = Math.max(80, Math.min(150, contentWidth / 5));
        this.targetBox = new EditBox(this.font, margin, y2, targetWidth, 18, Component.literal("Target player"));
        this.targetBox.setHint(Component.literal("Target (blank = self)"));
        this.targetBox.setMaxLength(32);
        this.targetBox.setValue(UncannyDevMenuClientState.lastTargetName());
        this.addRenderableWidget(this.targetBox);
        x = margin + targetWidth + gap;

        int remaining = this.width - margin - x;
        int secondWidth = Math.max(42, (remaining - gap * 3) / 4);
        this.distanceButton = addControl(x, y2, secondWidth, "Distance", button -> cycleDistance());
        x += secondWidth + gap;
        addControl(x, y2, secondWidth, "Cleanup entities", button -> runTool("tool_cleanup_entities"));
        x += secondWidth + gap;
        addControl(x, y2, secondWidth, "Reset transient", button -> runTool("tool_reset_transient"));
        x += secondWidth + gap;
        addControl(x, y2, this.width - margin - x, "Close", button -> onClose());
    }

    private Button addControl(int x, int y, int width, String label, java.util.function.Consumer<Button> action) {
        return this.addRenderableWidget(Button.builder(Component.literal(label), action::accept)
                .bounds(x, y, Math.max(30, width), 18)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, this.width, this.height, COLOR_BACKGROUND);
        drawPanel(graphics, this.listLeft, this.listTop, this.listLeft + this.listWidth, this.listBottom, COLOR_PANEL);
        drawPanel(graphics, this.detailLeft, this.detailTop, this.detailRight, this.detailBottom, COLOR_PANEL_ALT);
        super.render(graphics, mouseX, mouseY, partialTick);

        // The selection list renders its own viewport. Keep the title, details and entity
        // preview above widget rendering so they cannot be dimmed by that viewport.
        graphics.drawString(this.font, TITLE, this.detailLeft, 19, COLOR_TEXT, false);
        renderDetail(graphics, mouseX, mouseY, partialTick);
    }

    private void renderDetail(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (this.selectedEntry == null) {
            graphics.drawCenteredString(this.font, Component.literal("Select an entry to inspect its exact QA contract."), (this.detailLeft + this.detailRight) / 2, this.detailTop + 24, COLOR_MUTED);
            return;
        }

        UncannyDevMetadataCatalog.Info info = UncannyDevMetadataCatalog.describe(this.selectedEntry);
        int x = this.detailLeft + 8;
        int width = Math.max(40, this.detailRight - this.detailLeft - 16);
        graphics.drawString(this.font, Component.literal(this.selectedEntry.groupLabel() + " — " + this.selectedEntry.label()), x, this.detailTop + 7, COLOR_TEXT, false);
        graphics.drawString(this.font, Component.literal(this.selectedEntry.id()), x, this.detailTop + 18, COLOR_ACCENT, false);

        UncannyDevMenuResultPayload result = UncannyDevMenuClientState.lastResult();
        if (result != null && result.entryId().equals(this.selectedEntry.id())) {
            int resultColor = result.success() ? COLOR_GREEN : COLOR_RED;
            String resultText = (result.success() ? "OK" : "FAILED") + " · " + result.targetName() + " · " + result.message();
            graphics.drawString(this.font, Component.literal(fit(resultText, width)), x, this.detailTop + 29, resultColor, false);
        } else {
            graphics.drawString(this.font, Component.literal(fit(summaryLine(info), width)), x, this.detailTop + 29, COLOR_MUTED, false);
        }

        boolean showPreview = info.entityPreview() && this.previewEntity != null && width >= 330;
        int previewWidth = showPreview ? Math.min(170, Math.max(110, width / 3)) : 0;
        int textWidth = Math.max(60, width - previewWidth - (showPreview ? 8 : 0));
        int contentTop = this.detailTop + 45;
        int y = contentTop - this.detailScroll;

        graphics.enableScissor(this.detailLeft + 1, contentTop, this.detailRight - 1, this.detailBottom - 1);
        y = drawWrappedField(graphics, "Description", info.description(), x, y, textWidth, COLOR_TEXT);
        y = drawWrappedField(graphics, "Availability", info.phase() + " · " + info.rarity().label() + " · " + info.danger().label(), x, y, textWidth, COLOR_TEXT);
        y = drawWrappedField(graphics, "Conditions", info.conditions(), x, y, textWidth, COLOR_TEXT);
        y = drawWrappedField(graphics, "Restrictions", info.restrictions(), x, y, textWidth, COLOR_TEXT);
        y = drawWrappedField(graphics, "Multiplayer / authority", info.multiplayer() + " " + info.authority().label(), x, y, textWidth, COLOR_TEXT);
        y = drawWrappedField(graphics, "Associated systems", info.associated(), x, y, textWidth, COLOR_TEXT);
        y = drawWrappedField(graphics, "Implementation / validation", info.implementation().label() + " · " + info.validation().label(), x, y, textWidth, COLOR_TEXT);
        y = drawWrappedField(graphics, "Known limitations", info.limitations(), x, y, textWidth, COLOR_ORANGE);
        graphics.disableScissor();

        int availableHeight = Math.max(1, this.detailBottom - contentTop - 4);
        int contentHeight = y + this.detailScroll - contentTop;
        this.maximumDetailScroll = Math.max(0, contentHeight - availableHeight);
        this.detailScroll = Mth.clamp(this.detailScroll, 0, this.maximumDetailScroll);
        if (showPreview) {
            renderPreview(graphics, previewWidth);
        } else {
            this.previewLeft = this.previewTop = this.previewRight = this.previewBottom = 0;
        }
    }

    private void renderPreview(GuiGraphics graphics, int width) {
        this.previewRight = this.detailRight - 7;
        this.previewLeft = this.previewRight - width;
        this.previewTop = this.detailTop + 48;
        this.previewBottom = Math.min(this.detailBottom - 8, this.previewTop + 145);
        graphics.fill(this.previewLeft, this.previewTop, this.previewRight, this.previewBottom, 0x9920232B);
        outline(graphics, this.previewLeft, this.previewTop, this.previewRight, this.previewBottom, COLOR_BORDER);
        int viewportHeight = Math.max(30, this.previewBottom - this.previewTop);
        float entityHeight = Math.max(0.8F, this.previewEntity.getBbHeight());
        int scale = Mth.clamp((int) (viewportHeight * 0.45F * this.previewZoom / entityHeight), 14, 70);
        InventoryScreen.renderEntityInInventoryFollowsAngle(
                graphics,
                this.previewLeft + 2,
                this.previewTop + 2,
                this.previewRight - 2,
                this.previewBottom - 12,
                scale,
                0.0F,
                this.previewPitch,
                this.previewYaw,
                this.previewEntity);
        graphics.drawCenteredString(this.font, Component.literal("Drag to rotate · wheel to zoom"), (this.previewLeft + this.previewRight) / 2, this.previewBottom - 10, COLOR_MUTED);
    }

    private int drawWrappedField(GuiGraphics graphics, String label, String value, int x, int y, int width, int valueColor) {
        graphics.drawString(this.font, Component.literal(label.toUpperCase(Locale.ROOT)), x, y, COLOR_ACCENT, false);
        y += 10;
        for (FormattedCharSequence line : this.font.split(Component.literal(value), Math.max(30, width))) {
            graphics.drawString(this.font, line, x, y, valueColor, false);
            y += 10;
        }
        return y + 5;
    }

    private void rebuildEntries() {
        if (this.entryList == null) {
            return;
        }
        String selectedId = this.selectedEntry == null ? "" : this.selectedEntry.id();
        UncannyDevQueryEngine.Query query = new UncannyDevQueryEngine.Query(
                this.searchBox == null ? "" : this.searchBox.getValue(),
                this.category,
                this.phaseFilter,
                this.dangerFilter,
                this.qaFilter,
                this.scope,
                this.sortMode);
        List<UncannyDevCatalog.Entry> entries = UncannyDevQueryEngine.query(
                UncannyDevCatalog.entries(),
                query,
                UncannyDevMenuClientState.favoriteIds(),
                UncannyDevMenuClientState.recentIds(),
                UncannyDevMenuClientState::statusOf);
        List<EntryRow> rows = new ArrayList<>();
        for (UncannyDevCatalog.Entry entry : entries) {
            rows.add(new EntryRow(entry));
        }
        this.entryList.replaceEntries(rows, selectedId);
        if (!selectedId.isEmpty() && entries.stream().noneMatch(entry -> entry.id().equals(selectedId))) {
            selectEntry(null);
        }
        updateFilterLabels();
        updateActionButtons();
    }

    private void selectEntry(UncannyDevCatalog.Entry entry) {
        this.selectedEntry = entry;
        this.previewEntity = entry == null ? null : UncannyDevEntityPreview.create(entry);
        this.previewYaw = 25.0F;
        this.previewPitch = -8.0F;
        this.previewZoom = 1.0F;
        this.detailScroll = 0;
        if (this.entryList != null) {
            this.entryList.selectId(entry == null ? "" : entry.id());
        }
        updateActionButtons();
    }

    private void runSelected() {
        if (this.selectedEntry == null) {
            return;
        }
        UncannyDevMenuClientState.requestRun(this.selectedEntry.id(), this.targetBox == null ? "" : this.targetBox.getValue(), this.spawnDistance);
        updateActionButtons();
    }

    private void repeatLast() {
        String entryId = UncannyDevMenuClientState.lastEntryId();
        if (UncannyDevCatalog.byId(entryId) == null) {
            return;
        }
        UncannyDevMenuClientState.requestRun(entryId, this.targetBox == null ? "" : this.targetBox.getValue(), this.spawnDistance);
        selectEntry(UncannyDevCatalog.byId(entryId));
    }

    private void runTool(String entryId) {
        UncannyDevMenuClientState.requestRun(entryId, this.targetBox == null ? "" : this.targetBox.getValue(), this.spawnDistance);
    }

    private void toggleFavorite() {
        if (this.selectedEntry != null) {
            UncannyDevMenuClientState.toggleFavorite(this.selectedEntry.id());
            rebuildEntries();
        }
    }

    private void toggleQaStatus() {
        if (this.selectedEntry == null) {
            return;
        }
        boolean green = UncannyDevMenuClientState.statusOf(this.selectedEntry.id()) == UncannyDevCatalog.QaStatus.GREEN;
        UncannyDevMenuClientState.requestSetGreen(this.selectedEntry.id(), !green);
    }

    private void selectSibling(int direction) {
        if (this.selectedEntry == null) {
            return;
        }
        List<UncannyDevCatalog.Entry> siblings = UncannyDevCatalog.entries(this.selectedEntry.category(), this.selectedEntry.groupKey());
        if (siblings.size() < 2) {
            return;
        }
        int index = 0;
        for (int i = 0; i < siblings.size(); i++) {
            if (siblings.get(i).id().equals(this.selectedEntry.id())) {
                index = i;
                break;
            }
        }
        selectEntry(siblings.get(Math.floorMod(index + direction, siblings.size())));
    }

    private void cycleDistance() {
        int[] distances = {4, 8, 12, 16, 24};
        int index = 0;
        for (int i = 0; i < distances.length; i++) {
            if (distances[i] == this.spawnDistance) {
                index = i;
                break;
            }
        }
        this.spawnDistance = distances[(index + 1) % distances.length];
        UncannyDevMenuClientState.storeTargetAndDistance(this.targetBox == null ? "" : this.targetBox.getValue(), this.spawnDistance);
        updateActionButtons();
    }

    public void onServerResult() {
        rebuildEntries();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && inside(mouseX, mouseY, this.previewLeft, this.previewTop, this.previewRight, this.previewBottom)) {
            this.draggingPreview = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.draggingPreview) {
            this.draggingPreview = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && this.draggingPreview) {
            this.previewYaw += (float) dragX * 2.0F;
            this.previewPitch = Mth.clamp(this.previewPitch + (float) dragY * 1.5F, -45.0F, 45.0F);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (inside(mouseX, mouseY, this.previewLeft, this.previewTop, this.previewRight, this.previewBottom) && this.previewEntity != null) {
            this.previewZoom = Mth.clamp(this.previewZoom + (float) scrollY * 0.08F, 0.55F, 1.8F);
            return true;
        }
        if (inside(mouseX, mouseY, this.detailLeft, this.detailTop, this.detailRight, this.detailBottom)) {
            this.detailScroll = Mth.clamp(this.detailScroll - (int) Math.signum(scrollY) * 18, 0, this.maximumDetailScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void removed() {
        UncannyDevMenuClientState.storeTargetAndDistance(this.targetBox == null ? "" : this.targetBox.getValue(), this.spawnDistance);
        this.previewEntity = null;
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    private void updateFilterLabels() {
        if (this.categoryButton == null) {
            return;
        }
        this.categoryButton.setMessage(Component.literal("Category: " + this.category.label()));
        this.phaseButton.setMessage(Component.literal(this.phaseFilter == 0 ? "Phase: All" : "Available: P" + this.phaseFilter));
        this.dangerButton.setMessage(Component.literal("Danger: " + (this.dangerFilter == null ? "All" : this.dangerFilter.label())));
        this.qaFilterButton.setMessage(Component.literal("QA: " + this.qaFilter.label()));
        this.scopeButton.setMessage(Component.literal("View: " + this.scope.label()));
        this.sortButton.setMessage(Component.literal("Sort: " + this.sortMode.label()));
    }

    private void updateActionButtons() {
        if (this.runButton == null) {
            return;
        }
        boolean selected = this.selectedEntry != null;
        this.runButton.active = selected;
        this.favoriteButton.active = selected;
        this.qaStatusButton.active = selected;
        this.repeatButton.active = UncannyDevCatalog.byId(UncannyDevMenuClientState.lastEntryId()) != null;
        this.favoriteButton.setMessage(Component.literal(selected && UncannyDevMenuClientState.isFavorite(this.selectedEntry.id()) ? "* Favorited" : "Favorite"));
        UncannyDevCatalog.QaStatus status = selected ? UncannyDevMenuClientState.statusOf(this.selectedEntry.id()) : UncannyDevCatalog.QaStatus.GRAY;
        this.qaStatusButton.setMessage(Component.literal(status == UncannyDevCatalog.QaStatus.GREEN ? "Reset QA" : "Validate green"));
        int siblings = selected ? UncannyDevCatalog.entries(this.selectedEntry.category(), this.selectedEntry.groupKey()).size() : 0;
        this.previousVariantButton.active = siblings > 1;
        this.nextVariantButton.active = siblings > 1;
        this.distanceButton.setMessage(Component.literal("Direct spawn: " + this.spawnDistance + "m"));
    }

    private static String summaryLine(UncannyDevMetadataCatalog.Info info) {
        return info.type().label() + " · " + info.phase() + " · " + info.rarity().label() + " · " + info.danger().label() + " · " + info.authority().label();
    }

    private String fit(String value, int width) {
        int maximumWidth = Math.max(20, width - 4);
        if (this.font.width(value) <= maximumWidth) {
            return value;
        }
        String suffix = "...";
        int prefixWidth = Math.max(0, maximumWidth - this.font.width(suffix));
        return this.font.plainSubstrByWidth(value, prefixWidth) + suffix;
    }

    private static <T> T next(T current, T[] values) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) {
                return values[(i + 1) % values.length];
            }
        }
        return values[0];
    }

    private static UncannyDevMetadataCatalog.Danger nextDanger(UncannyDevMetadataCatalog.Danger current) {
        if (current == null) {
            return UncannyDevMetadataCatalog.Danger.NONE;
        }
        UncannyDevMetadataCatalog.Danger[] values = UncannyDevMetadataCatalog.Danger.values();
        return current == values[values.length - 1] ? null : values[current.ordinal() + 1];
    }

    private static boolean inside(double x, double y, int left, int top, int right, int bottom) {
        return right > left && bottom > top && x >= left && x < right && y >= top && y < bottom;
    }

    private static void drawPanel(GuiGraphics graphics, int left, int top, int right, int bottom, int color) {
        graphics.fill(left, top, right, bottom, color);
        outline(graphics, left, top, right, bottom, COLOR_BORDER);
    }

    private static void outline(GuiGraphics graphics, int left, int top, int right, int bottom, int color) {
        graphics.fill(left, top, right, top + 1, color);
        graphics.fill(left, bottom - 1, right, bottom, color);
        graphics.fill(left, top, left + 1, bottom, color);
        graphics.fill(right - 1, top, right, bottom, color);
    }

    private final class EntryList extends ObjectSelectionList<EntryRow> {
        private final int rowLeft;
        private final int rowWidth;

        private EntryList(net.minecraft.client.Minecraft minecraft, int width, int height, int y, int itemHeight, int rowLeft, int rowWidth) {
            super(minecraft, width, Math.max(1, height), y, itemHeight);
            this.rowLeft = rowLeft;
            this.rowWidth = rowWidth;
        }

        private void replaceEntries(List<EntryRow> rows, String selectedId) {
            this.clearEntries();
            EntryRow retained = null;
            for (EntryRow row : rows) {
                this.addEntry(row);
                if (row.entry.id().equals(selectedId)) {
                    retained = row;
                }
            }
            this.setSelected(retained);
        }

        private void selectId(String entryId) {
            for (EntryRow row : this.children()) {
                if (row.entry.id().equals(entryId)) {
                    this.setSelected(row);
                    return;
                }
            }
            this.setSelected(null);
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
            return this.rowWidth - 10;
        }
    }

    private final class EntryRow extends ObjectSelectionList.Entry<EntryRow> {
        private final UncannyDevCatalog.Entry entry;

        private EntryRow(UncannyDevCatalog.Entry entry) {
            this.entry = entry;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            boolean selected = UncannyDevMenuScreen.this.selectedEntry != null && UncannyDevMenuScreen.this.selectedEntry.id().equals(this.entry.id());
            graphics.fill(left, top, left + width, top + height - 1, selected ? 0x88504788 : (hovered ? 0x55323742 : 0x221A1C22));

            UncannyDevCatalog.QaStatus status = UncannyDevMenuClientState.statusOf(this.entry.id());
            int statusColor = switch (status) {
                case GREEN -> COLOR_GREEN;
                case ORANGE -> COLOR_ORANGE;
                case GRAY -> COLOR_GRAY;
            };
            String marker = switch (status) {
                case GREEN -> "G";
                case ORANGE -> "R";
                case GRAY -> "N";
            };
            if (UncannyDevMenuClientState.isFavorite(this.entry.id())) {
                marker += "*";
            }
            UncannyDevMetadataCatalog.Info info = UncannyDevMetadataCatalog.describe(this.entry);
            graphics.drawString(UncannyDevMenuScreen.this.font, Component.literal(marker), left + 4, top + 8, statusColor, false);
            graphics.drawString(UncannyDevMenuScreen.this.font, Component.literal(fit(this.entry.groupLabel() + " · " + this.entry.label(), width - 56)), left + 24, top + 3, COLOR_TEXT, false);
            graphics.drawString(UncannyDevMenuScreen.this.font, Component.literal(fit(this.entry.id(), width - 74)), left + 24, top + 14, COLOR_MUTED, false);
            graphics.drawString(UncannyDevMenuScreen.this.font, Component.literal("P" + info.minimumPhase()), left + width - 24, top + 8, statusColor, false);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            UncannyDevMenuScreen.this.entryList.setSelected(this);
            if (button == 1) {
                boolean green = UncannyDevMenuClientState.statusOf(this.entry.id()) == UncannyDevCatalog.QaStatus.GREEN;
                UncannyDevMenuClientState.requestSetGreen(this.entry.id(), !green);
                return true;
            }
            if (button != 0) {
                return false;
            }
            long now = Util.getMillis();
            boolean doubleClick = this.entry.id().equals(UncannyDevMenuScreen.this.lastClickedEntryId) && now - UncannyDevMenuScreen.this.lastClickMillis <= 350L;
            UncannyDevMenuScreen.this.lastClickedEntryId = this.entry.id();
            UncannyDevMenuScreen.this.lastClickMillis = now;
            UncannyDevMenuScreen.this.selectEntry(this.entry);
            if (doubleClick) {
                UncannyDevMenuScreen.this.runSelected();
            }
            return true;
        }

        @Override
        public Component getNarration() {
            return Component.literal(this.entry.groupLabel() + ", " + this.entry.label() + ", " + this.entry.id());
        }
    }
}
