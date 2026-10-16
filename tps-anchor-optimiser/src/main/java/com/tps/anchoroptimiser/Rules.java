package com.tps.anchoroptimiser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Holds every disguise rule, saves/loads them, and answers the questions the mixins ask.
 */
public final class Rules {
    private static final Logger LOGGER = LoggerFactory.getLogger("tps_anchor_optimiser");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** One disguise: "source" looks and reads like "target". */
    public static final class Rule {
        public String source = "";
        public String target = "";
        /** Blank = use the real name of the look-alike item. */
        public String name = "";
        /** RGB colour of the name, or -1 for no colour override. */
        public int color = -1;
        /** Also apply to waxed / exposed / weathered / oxidised copper versions. */
        public boolean variants = true;
        /** Hide the item-id line shown by F3+H advanced tooltips. */
        public boolean hideAdvanced = true;

        public Rule copy() {
            Rule r = new Rule();
            r.source = source;
            r.target = target;
            r.name = name;
            r.color = color;
            r.variants = variants;
            r.hideAdvanced = hideAdvanced;
            return r;
        }
    }

    private static final class Config {
        int version = 1;
        List<Rule> rules = new ArrayList<>();
    }

    private record Resolved(Item target, String name, int color, boolean hideAdvanced) {}

    public static final List<Rule> RULES = new ArrayList<>();

    private static volatile Map<Item, Resolved> ITEM_MAP = Map.of();
    private static volatile Map<Block, BlockState> BLOCK_MAP = Map.of();

    /** Set by the menu while it draws item icons, so previews show the real item. */
    public static boolean bypass = false;

    private Rules() {}

    // ------------------------------------------------------------------ config

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("tps_anchor_optimiser.json");
    }

    public static void load() {
        RULES.clear();
        boolean loaded = false;
        Path p = path();
        if (Files.exists(p)) {
            try (Reader r = Files.newBufferedReader(p)) {
                Config c = GSON.fromJson(r, Config.class);
                if (c != null && c.rules != null) {
                    RULES.addAll(c.rules);
                    loaded = true;
                }
            } catch (Exception e) {
                LOGGER.warn("Could not read {}: {}", p, e.toString());
            }
        }
        if (!loaded) {
            RULES.addAll(defaults());
            save();
        }
        rebuild();
    }

    public static void save() {
        Config c = new Config();
        c.rules = new ArrayList<>(RULES);
        try {
            Files.writeString(path(), GSON.toJson(c));
        } catch (Exception e) {
            LOGGER.warn("Could not write config: {}", e.toString());
        }
    }

    /** Save, rebuild lookups and re-mesh the world so block changes show immediately. */
    public static void saveAndApply() {
        save();
        rebuild();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            mc.levelRenderer.allChanged();
        }
    }

    private static Rule rule(String source, String target, String name, int color, boolean variants) {
        Rule r = new Rule();
        r.source = source;
        r.target = target;
        r.name = name;
        r.color = color;
        r.variants = variants;
        return r;
    }

    private static List<Rule> defaults() {
        List<Rule> l = new ArrayList<>();
        l.add(rule("minecraft:copper_block", "minecraft:netherite_block", "", -1, true));
        l.add(rule("minecraft:cut_copper_stairs", "minecraft:spawner", "Skeleton Spawner", 0xAA00AA, true));
        l.add(rule("minecraft:copper_bulb", "minecraft:gilded_blackstone", "", -1, true));
        l.add(rule("minecraft:golden_chestplate", "minecraft:elytra", "Elytra", 0xFF55FF, false));
        // Copper nuggets only exist in 1.21.9+, so this one is skipped automatically on 1.21.8.
        l.add(rule("minecraft:copper_nugget", "minecraft:netherite_ingot", "", -1, false));
        return l;
    }

    // ------------------------------------------------------------- registry bits

    public static Optional<Item> item(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        ResourceLocation rl = ResourceLocation.tryParse(id.trim());
        if (rl == null) {
            return Optional.empty();
        }
        return BuiltInRegistries.ITEM.getOptional(rl).filter(i -> i != Items.AIR);
    }

    public static String id(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    private static Optional<Item> byPath(String path) {
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.withDefaultNamespace(path));
    }

    /** The item itself plus (optionally) its waxed / exposed / weathered / oxidised copper versions. */
    private static List<Item> expand(Item src, boolean variants) {
        List<Item> out = new ArrayList<>();
        out.add(src);
        if (!variants) {
            return out;
        }
        String path = BuiltInRegistries.ITEM.getKey(src).getPath();
        if (!path.contains("copper")) {
            return out;
        }
        String base = path;
        if (base.startsWith("waxed_")) {
            base = base.substring("waxed_".length());
        }
        for (String prefix : new String[] {"exposed_", "weathered_", "oxidized_"}) {
            if (base.startsWith(prefix)) {
                base = base.substring(prefix.length());
                break;
            }
        }
        if (base.equals("copper")) {
            base = "copper_block";
        }
        List<String> stages = new ArrayList<>();
        if (base.equals("copper_block")) {
            stages.add("copper_block");
            stages.add("exposed_copper");
            stages.add("weathered_copper");
            stages.add("oxidized_copper");
        } else {
            stages.add(base);
            stages.add("exposed_" + base);
            stages.add("weathered_" + base);
            stages.add("oxidized_" + base);
        }
        for (String stage : stages) {
            for (String candidate : new String[] {stage, "waxed_" + stage}) {
                byPath(candidate).ifPresent(i -> {
                    if (!out.contains(i)) {
                        out.add(i);
                    }
                });
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ lookups

    public static void rebuild() {
        Map<Item, Resolved> items = new HashMap<>();
        Map<Block, BlockState> blocks = new HashMap<>();
        for (Rule r : RULES) {
            Optional<Item> src = item(r.source);
            Optional<Item> tgt = item(r.target);
            if (src.isEmpty() || tgt.isEmpty()) {
                continue;
            }
            Item target = tgt.get();
            for (Item s : expand(src.get(), r.variants)) {
                if (s == target) {
                    continue;
                }
                items.put(s, new Resolved(target, r.name == null ? "" : r.name.trim(), r.color, r.hideAdvanced));
                if (s instanceof BlockItem sb && target instanceof BlockItem tb) {
                    blocks.put(sb.getBlock(), tb.getBlock().defaultBlockState());
                }
            }
        }
        ITEM_MAP = items;
        BLOCK_MAP = blocks;
    }

    /** Builds a display name: custom text (or the look-alike's real name) with an optional colour. */
    public static MutableComponent makeName(String custom, Item target, int color) {
        MutableComponent c = (custom == null || custom.isBlank())
                ? Component.translatable(target.getDescriptionId())
                : Component.literal(custom.trim());
        if (color >= 0) {
            c = c.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)));
        }
        return c;
    }

    // -------------------------------------------------------------- mixin hooks

    /** Item model: if this stack is disguised, return a stack of the look-alike instead. */
    public static ItemStack swapStack(ItemStack stack) {
        if (bypass || stack.isEmpty()) {
            return stack;
        }
        Resolved r = ITEM_MAP.get(stack.getItem());
        if (r == null) {
            return stack;
        }
        ItemStack out = new ItemStack(r.target());
        if (stack.hasFoil()) {
            out.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, Boolean.TRUE);
        }
        return out;
    }

    /** Placed block: model and render layer of the look-alike. */
    public static BlockState swapState(BlockState state) {
        Map<Block, BlockState> m = BLOCK_MAP;
        if (m.isEmpty()) {
            return state;
        }
        BlockState t = m.get(state.getBlock());
        return t != null ? t : state;
    }

    /** Display name, or null to leave the real one alone. Anvil-renamed items keep their own name. */
    public static Component nameFor(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        Resolved r = ITEM_MAP.get(stack.getItem());
        if (r == null || stack.get(DataComponents.CUSTOM_NAME) != null) {
            return null;
        }
        return makeName(r.name(), r.target(), r.color());
    }

    /** Drops the advanced (F3+H) tooltip lines for disguised items so the real item id is not shown. */
    public static TooltipFlag tooltipFlag(ItemStack stack, TooltipFlag flag) {
        if (!flag.isAdvanced()) {
            return flag;
        }
        Resolved r = ITEM_MAP.get(stack.getItem());
        return (r != null && r.hideAdvanced()) ? TooltipFlag.NORMAL : flag;
    }
}
