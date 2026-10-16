package com.tps.anchoroptimiser;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

import java.util.Optional;

/** Main menu: the list of disguises. Open it with K. */
public class RulesScreen extends Screen {
    private static final int W = 320;

    private final Screen parent;
    private int page = 0;

    public RulesScreen(Screen parent) {
        super(Component.literal("TP's Anchor Optimiser"));
        this.parent = parent;
    }

    private int perPage() {
        return Math.max(1, (this.height - 90) / 26);
    }

    private int maxPage() {
        return Math.max(0, (Rules.RULES.size() - 1) / perPage());
    }

    @Override
    protected void init() {
        int left = (this.width - W) / 2;
        int per = perPage();
        page = Math.min(page, maxPage());

        for (int i = 0; i < per; i++) {
            int idx = page * per + i;
            if (idx >= Rules.RULES.size()) {
                break;
            }
            Rules.Rule rule = Rules.RULES.get(idx);
            int y = 34 + i * 26;
            this.addRenderableWidget(Button.builder(Component.literal("Edit"),
                    b -> this.minecraft.setScreen(new EditRuleScreen(this, rule, false)))
                    .bounds(left + W - 112, y, 52, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal("Delete"),
                    b -> {
                        Rules.RULES.remove(rule);
                        Rules.saveAndApply();
                        this.rebuildWidgets();
                    })
                    .bounds(left + W - 56, y, 56, 20).build());
        }

        int by = this.height - 28;
        this.addRenderableWidget(Button.builder(Component.literal("+ Add disguise"),
                b -> this.minecraft.setScreen(new EditRuleScreen(this, new Rules.Rule(), true)))
                .bounds(left, by, 120, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            if (page > 0) {
                page--;
                this.rebuildWidgets();
            }
        }).bounds(left + 126, by, 24, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            if (page < maxPage()) {
                page++;
                this.rebuildWidgets();
            }
        }).bounds(left + 154, by, 24, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose())
                .bounds(left + W - 100, by, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        g.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFFFF);

        int left = (this.width - W) / 2;
        int per = perPage();

        if (Rules.RULES.isEmpty()) {
            g.drawCenteredString(this.font, "No disguises yet. Press \"+ Add disguise\".",
                    this.width / 2, 50, 0xFFAAAAAA);
        }

        for (int i = 0; i < per; i++) {
            int idx = page * per + i;
            if (idx >= Rules.RULES.size()) {
                break;
            }
            Rules.Rule r = Rules.RULES.get(idx);
            int y = 34 + i * 26;

            Optional<Item> src = Rules.item(r.source);
            Optional<Item> tgt = Rules.item(r.target);
            src.ifPresent(it -> UI.icon(g, it, left, y + 2));
            g.drawString(this.font, "->", left + 20, y + 6, 0xFFAAAAAA);
            tgt.ifPresent(it -> UI.icon(g, it, left + 34, y + 2));

            Component shown;
            if (tgt.isPresent()) {
                shown = Rules.makeName(r.name, tgt.get(), r.color);
            } else {
                shown = Component.literal("(look-alike not found in this version)");
            }
            g.drawString(this.font, shown, left + 58, y + 6, 0xFFFFFFFF);
        }

        g.drawCenteredString(this.font,
                "Page " + (page + 1) + "/" + (maxPage() + 1) + "   |   Press K in game to open this menu",
                this.width / 2, this.height - 42, 0xFF777777);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
