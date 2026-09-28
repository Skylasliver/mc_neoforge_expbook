package dev.skylasliver.skylasliver_expbook;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server config is synchronized to clients, including page capacity tooltips. */
public final class BookConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue COLLECT, MEND, PUMP;
    public static final ModConfigSpec.IntValue TARGET, MAX_TARGET;
    public static final ModConfigSpec.IntValue LEATHER, GOLD, DIAMOND, NETHERITE, NETHER_STAR;
    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        String prefix = ModMindEntry.MOD_ID + ".configuration.";
        b.translation(prefix + "automation").push("automation");
        COLLECT = b.comment("Allow books to collect picked-up XP before vanilla Mending. Per-book switch defaults to off.")
                .translation(prefix + "automation.allowCollection").define("allowCollection", true);
        MEND = b.comment("Allow stored XP to repair carried Mending items. Per-book switch defaults to off.")
                .translation(prefix + "automation.allowMending").define("allowMending", true);
        PUMP = b.comment("Allow stored XP to maintain a player's target level. Per-book switch defaults to off.")
                .translation(prefix + "automation.allowLevelPump").define("allowLevelPump", true);
        TARGET = b.comment("Default target level for books without a saved target.")
                .translation(prefix + "automation.defaultTargetLevel").defineInRange("defaultTargetLevel", 30, 0, 100);
        MAX_TARGET = b.comment("Maximum target level selectable per book.")
                .translation(prefix + "automation.maxTargetLevel").defineInRange("maxTargetLevel", 100, 0, 100);
        b.pop();
        b.comment("Raw XP points per page. Lowering capacity preserves stored XP; overfull books cannot collect more.")
                .translation(prefix + "pageCapacity").push("pageCapacity");
        LEATHER = b.translation(prefix + "pageCapacity.leather").defineInRange("leather", 500, 0, 79000000);
        GOLD = b.translation(prefix + "pageCapacity.gold").defineInRange("gold", 2000, 0, 79000000);
        DIAMOND = b.translation(prefix + "pageCapacity.diamond").defineInRange("diamond", 5000, 0, 79000000);
        NETHERITE = b.translation(prefix + "pageCapacity.netherite").defineInRange("netherite", 20000, 0, 79000000);
        NETHER_STAR = b.translation(prefix + "pageCapacity.nether_star").defineInRange("nether_star", 100000, 0, 79000000);
        b.pop();
        SPEC = b.build();
    }
    public static int allowedFlags() {
        return (COLLECT.get() ? 1 : 0) | (MEND.get() ? 2 : 0) | (PUMP.get() ? 4 : 0);
    }
    private BookConfig() {}
}
