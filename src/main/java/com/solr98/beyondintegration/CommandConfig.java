package com.solr98.beyondintegration;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import java.util.Arrays;
import java.util.List;

public class CommandConfig {
    public static final ModConfigSpec SERVER_SPEC;
    public static final ServerConfig SERVER;
    static {
        final Pair<ServerConfig, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(ServerConfig::new);
        SERVER_SPEC = specPair.getRight();
        SERVER = specPair.getLeft();
    }

    public enum Language { EN_US("en_us"), ZH_CN("zh_cn");
        private final String code;
        Language(String code) { this.code = code; }
        public String getCode() { return code; }
    }

    public static class ServerConfig {
        public final ModConfigSpec.EnumValue<Language> language;
        public final ModConfigSpec.IntValue maxNetworksPerPage;

        // Enchantment separation
        public final ModConfigSpec.BooleanValue enchantSeparation;
        public final ModConfigSpec.BooleanValue enchantItemSeparation;
        public final ModConfigSpec.DoubleValue enchantItemMult;
        public final ModConfigSpec.IntValue enchantBaseCost;
        public final ModConfigSpec.DoubleValue enchantLevelMult;
        public final ModConfigSpec.DoubleValue enchantDefaultMult;
        public final ModConfigSpec.ConfigValue<String> enchantFormula;
        public final ModConfigSpec.BooleanValue enchantUseFormula;
        public final ModConfigSpec.ConfigValue<List<? extends String>> enchantHighCostList;

        // Vehicle energy charge
        public final ModConfigSpec.IntValue swVehicleEnergyChargeRate;

        // Item blacklist
        public final ModConfigSpec.BooleanValue ENABLE_ITEM_BLACKLIST;
        public final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_BLACKLIST;

        public ServerConfig(ModConfigSpec.Builder builder) {
            builder.comment("General settings").push("general");
            language = builder.defineEnum("command_language", Language.EN_US);
            maxNetworksPerPage = builder.defineInRange("max_networks_per_page", 10, 1, 100);
            builder.pop();

            builder.comment("Enchantment separation settings").push("enchant");
            enchantSeparation = builder.define("separation", true);
            enchantItemSeparation = builder.define("itemSeparation", true);
            enchantItemMult = builder.defineInRange("itemMult", 2.0, 0.1, 100.0);
            enchantBaseCost = builder.defineInRange("base_cost", 5, 0, 100);
            enchantLevelMult = builder.defineInRange("level_mult", 1.0, 0.0, 100.0);
            enchantDefaultMult = builder.defineInRange("default_mult", 1.0, 0.0, 100.0);
            enchantFormula = builder.define("formula", "base + level * level_mult");
            enchantUseFormula = builder.define("use_formula", false);
            enchantHighCostList = builder.defineList("high_cost",
                    Arrays.asList("minecraft:mending:3.0", "minecraft:frost_walker:3.0",
                            "minecraft:sharpness:1.2", "minecraft:protection:1.2"),
                    obj -> obj instanceof String);
            builder.pop();

            builder.comment("Vehicle settings").push("vehicle");
            swVehicleEnergyChargeRate = builder.defineInRange("energyChargeRate", 500000, 0, Integer.MAX_VALUE);
            builder.pop();

            builder.comment("Item blacklist").push("blacklist");
            ENABLE_ITEM_BLACKLIST = builder.define("enable", false);
            ITEM_BLACKLIST = builder.defineList("items",
                    Arrays.asList("minecraft:barrier", "minecraft:command_block"), obj -> obj instanceof String);
            builder.pop();
        }
    }

    public static Language getCommandLanguage() { return SERVER.language.get(); }
    public static int maxNetworksPerPage() { return SERVER.maxNetworksPerPage.get(); }
    public static boolean enableItemBlacklist() { return SERVER.ENABLE_ITEM_BLACKLIST.get(); }
    public static List<? extends String> itemBlacklist() { return SERVER.ITEM_BLACKLIST.get(); }
}
