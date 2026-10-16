package com.tps.anchoroptimiser;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** A searchable grid of every item in the game. Click one to choose it. */
public class ItemPickerScreen extends Screen {
    private static final int CELL = 20;

    private final Screen parent;
    private final Consumer<Item> onPick;
    private final List<Item> all = new ArrayList<>();
    private List<Item> filtered;

    private EditBox search;
    private String query = "";
    private int page = 0;
    private int cols = 1;
    private int rows = 1;
    private int gridX = 0;
    private int gridY = 56;

    public ItemPickerScreen(Screen parent, String title, Consumer<Item> onPick) {
        super(Component.literal(title));
        this.parent = parent;
        this.onPick = onPick;
        for (Item item : BuiltInRegistries.ITEM) {
            if (item != Items.AIR) {
                all.add(item);
            }
        }
        this.filtered = all;
    }

    private int perPage() {
        return cols * rows;
    }

    private int maxPage() {
        return Math.max(0, (filtered.size() - 1) / perPage());
    }

    private void refilter() {
        String q = query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            filtered = all;
        } else {
            List<Item> out = new ArrayList<>();
            for (Item item : all) {
                String name = I18n.get(item.getDescriptionId()).toLowerCase(Locale.ROOT);
                if (name.contains(q) || Rules.id(item).contains(q)) {
                    out.add(item);
                }
            }
            filtered = out;
        }
        page = Math.min(page, maxPage());
    }

    @Override
    protected void init() {
        cols = Math.max(1, Math.min(18, (this.width - 20) / CELL));
        rows = Math.max(1, (this.height - 100) / CELL);
        gridX = (this.width - cols * CELL) / 2;
        gridY = 56;

        search = new EditBox(this.font, this.width / 2 - 100, 28, 200, 18, Component.literal("Search"));
        search.setHint(Component.literal("Search by name or id..."));
        search.setValue(query);
        search.setResponder(v -> {
            query = v;
            page = 0;
            refilter();
        });
        this.addRenderableWidget(search);
        this.setInitialFocus(search);
        refilter();

        int by = this.height - 26;
        this.addRenderableWidget(Button.builder(Component.literal("< Prev"), b -> {
            if (page > 0) {
                page--;
            }
        }).bounds(this.width / 2 - 154, by, 70, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Back"), b -> this.onClose())
                .bounds(this.width / 2 - 40, by, 80, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Next >"), b -> {
            if (page < maxPage()) {
                page++;
            }
        }).bounds(this.width / 2 + 84, by, 70, 20).build());
    }

    private int cellAt(double mx, double my) {
        int col = (int) Math.floor((mx - gridX) / CELL);
        int row = (int) Math.floor((my - gridY) / CELL);
        if (mx < gridX || my < gridY || col < 0 || col >= cols || row < 0 || row >= rows) {
            return -1;
        }
        int idx = page * perPage() + row * cols + col;
        return idx < filtered.size() ? idx : -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        int idx = cellAt(mouseX, mouseY);
        if (idx >= 0) {
            onPick.accept(filtered.get(idx));
            this.minecraft.setScreen(parent);
            return true;
        }
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        g.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFFFF);

        int per = perPage();
        int start = page * per;
        Item hovered = null;
        for (int i = 0; i < per && start + i < filtered.size(); i++) {
            int cx = gridX + (i % cols) * CELL;
            int cy = gridY + (i / cols) * CELL;
            Item item = filtered.get(start + i);
            if (mouseX >= cx && mouseX < cx + CELL && mouseY >= cy && mouseY < cy + CELL) {
                g.fill(cx, cy, cx + CELL, cy + CELL, 0x80FFFFFF);
                hovered = item;
            }
            UI.icon(g, item, cx + 2, cy + 2);
        }

        String info;
        if (hovered != null) {
            info = I18n.get(hovered.getDescriptionId()) + "  (" + Rules.id(hovered) + ")";
        } else {
            info = "Page " + (page + 1) + "/" + (maxPage() + 1) + "  -  " + filtered.size() + " items";
        }
        g.drawCenteredString(this.font, info, this.width / 2, gridY + rows * CELL + 6, 0xFFAAAAAA);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
