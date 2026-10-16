package com.tps.anchoroptimiser;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;

/** Edit one disguise: what it is, what it looks like, its name and the name colour. */
public class EditRuleScreen extends Screen {
    private static final int W = 280;

    private final Screen parent;
    private final Rules.Rule original;
    private final Rules.Rule draft;
    private final boolean isNew;

    private EditBox nameBox;
    private EditBox hexBox;
    private String status = "";

    public EditRuleScreen(Screen parent, Rules.Rule original, boolean isNew) {
        super(Component.literal("Edit disguise"));
        this.parent = parent;
        this.original = original;
        this.draft = original.copy();
        this.isNew = isNew;
    }

    private static Component toggleText(String label, boolean on) {
        return Component.literal(label + ": " + (on ? "ON" : "OFF"));
    }

    @Override
    protected void init() {
        int left = this.width / 2 - W / 2;
        int y = 40;

        // 1. What is being disguised
        this.addRenderableWidget(Button.builder(
                Component.literal("Disguise: " + UI.label(draft.source)),
                b -> this.minecraft.setScreen(new ItemPickerScreen(this,
                        "Pick the item to disguise", item -> draft.source = Rules.id(item))))
                .bounds(left, y, 200, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Held item"), b -> {
            if (this.minecraft.player != null) {
                ItemStack held = this.minecraft.player.getMainHandItem();
                if (!held.isEmpty()) {
                    draft.source = Rules.id(held.getItem());
                    this.rebuildWidgets();
                }
            }
        }).bounds(left + 205, y, 75, 20).build());
        y += 22;

        // 2. What it should look like
        this.addRenderableWidget(Button.builder(
                Component.literal("Looks like: " + UI.label(draft.target)),
                b -> this.minecraft.setScreen(new ItemPickerScreen(this,
                        "Pick what it should look like", item -> draft.target = Rules.id(item))))
                .bounds(left, y, W, 20).build());
        y += 22;

        // 3. Name
        nameBox = new EditBox(this.font, left, y, W, 20, Component.literal("Name"));
        nameBox.setMaxLength(64);
        nameBox.setHint(Component.literal("Name (blank = the look-alike's real name)"));
        nameBox.setValue(draft.name == null ? "" : draft.name);
        nameBox.setResponder(v -> draft.name = v);
        this.addRenderableWidget(nameBox);
        y += 24;

        // 4. Name colour: the 16 chat colours, 8 per row
        for (int i = 0; i < UI.COLORS.length; i++) {
            final int rgb = UI.COLORS[i];
            int bx = left + (i % 8) * 35;
            int by = y + (i / 8) * 22;
            Component swatch = Component.literal("██")
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)));
            this.addRenderableWidget(Button.builder(swatch, b -> {
                draft.color = rgb;
                if (hexBox != null) {
                    hexBox.setValue(String.format("%06X", rgb));
                }
            }).bounds(bx, by, 32, 20).build());
        }
        y += 46;

        hexBox = new EditBox(this.font, left, y, 140, 20, Component.literal("Hex colour"));
        hexBox.setMaxLength(6);
        hexBox.setHint(Component.literal("Hex colour, e.g. FF55FF"));
        hexBox.setValue(draft.color >= 0 ? String.format("%06X", draft.color) : "");
        hexBox.setResponder(v -> {
            if (v.length() == 6) {
                try {
                    draft.color = Integer.parseInt(v, 16);
                } catch (NumberFormatException ignored) {
                    // keep the previous colour while the text is not valid hex
                }
            }
        });
        this.addRenderableWidget(hexBox);
        this.addRenderableWidget(Button.builder(Component.literal("No colour"), b -> {
            draft.color = -1;
            hexBox.setValue("");
        }).bounds(left + 145, y, 135, 20).build());
        y += 24;

        // 5. Options
        this.addRenderableWidget(Button.builder(toggleText("Waxed/oxidised too", draft.variants), b -> {
            draft.variants = !draft.variants;
            b.setMessage(toggleText("Waxed/oxidised too", draft.variants));
        }).bounds(left, y, 138, 20).build());
        this.addRenderableWidget(Button.builder(toggleText("Hide item ID tip", draft.hideAdvanced), b -> {
            draft.hideAdvanced = !draft.hideAdvanced;
            b.setMessage(toggleText("Hide item ID tip", draft.hideAdvanced));
        }).bounds(left + 142, y, 138, 20).build());
        y += 24;

        // 6. Save / cancel
        this.addRenderableWidget(Button.builder(Component.literal("Save"), b -> save())
                .bounds(left, y, 138, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> this.onClose())
                .bounds(left + 142, y, 138, 20).build());
    }

    private void save() {
        if (Rules.item(draft.source).isEmpty()) {
            status = "Pick the item to disguise first.";
            return;
        }
        if (Rules.item(draft.target).isEmpty()) {
            status = "Pick what it should look like first.";
            return;
        }
        if (draft.source.equals(draft.target)) {
            status = "Those are the same item.";
            return;
        }
        original.source = draft.source;
        original.target = draft.target;
        original.name = draft.name == null ? "" : draft.name;
        original.color = draft.color;
        original.variants = draft.variants;
        original.hideAdvanced = draft.hideAdvanced;
        if (isNew) {
            Rules.RULES.add(original);
        }
        Rules.saveAndApply();
        this.minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        int left = this.width / 2 - W / 2;
        g.drawCenteredString(this.font, this.title, this.width / 2, 4, 0xFFFFFFFF);

        // Live preview of the finished item name
        Rules.item(draft.target).ifPresent(it -> UI.icon(g, it, left, 16));
        Component preview = Rules.item(draft.target)
                .map(it -> (Component) Rules.makeName(draft.name, it, draft.color))
                .orElse(Component.literal("(pick a look-alike)"));
        g.drawString(this.font, preview, left + 22, 18, 0xFFFFFFFF);
        if (!status.isEmpty()) {
            g.drawString(this.font, status, left + 22, 29, 0xFFFF5555);
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
