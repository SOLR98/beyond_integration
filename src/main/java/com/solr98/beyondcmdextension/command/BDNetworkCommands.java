package com.solr98.beyondcmdextension.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;

import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EnergyStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.FluidStackKey;
import com.wintercogs.beyonddimensions.util.PlayerNameHelper;
import com.wintercogs.beyonddimensions.client.gui.NetMenuType;
import com.wintercogs.beyonddimensions.common.init.BDMenus;
import com.wintercogs.beyonddimensions.common.menu.DimensionsCraftMenu;
import com.wintercogs.beyonddimensions.common.menu.DimensionsNetMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;
import com.solr98.beyondcmdextension.Beyond_cmd_extension;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = Beyond_cmd_extension.MODID)
public final class BDNetworkCommands
{
    private BDNetworkCommands()
    {
    }

    private static final int OP_LEVEL = 2;

    private static String formatNumber(int number) {
        return formatNumberSI(number);
    }

    private static String formatNumber(long number) {
        return formatNumberSI(number);
    }

    private static String formatNumber(BigInteger number) {
        return formatNumberSI(number);
    }

    private static String formatNumberSI(long number) {
        if (number < 1000) {
            return String.valueOf(number);
        }

        String[] units = {"", "K", "M", "B", "T"};
        int unitIndex = 0;
        double value = number;

        while (value >= 1000 && unitIndex < units.length - 1) {
            value /= 1000.0;
            unitIndex++;
        }

        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(value) + units[unitIndex];
    }
    private static String formatNumberSI(BigInteger number)
    {
        if (number.compareTo(BigInteger.valueOf(1000)) < 0) {
            return number.toString();
        }

        String[] units = {"", "K", "M", "B", "T"};
        int unitIndex = 0;
        double value = number.doubleValue();

        while (value >= 1000 && unitIndex < units.length - 1) {
            value /= 1000.0;
            unitIndex++;
        }

        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(value) + units[unitIndex];
    }

    private static int getPlayerNetIdOrFail(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(CommandLang.component("error.player_required"));
            return -1;
        }

        DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
        if (net == null) {
            source.sendFailure(CommandLang.component("error.not_in_network"));
            return -1;
        }

        return net.getId();
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event)
    {
        CommandBuildContext context = event.getBuildContext();
        // 注册需要OP权限的命令
        event.getDispatcher().register(
                Commands.literal("bdtools")
                        .requires(src -> src.hasPermission(OP_LEVEL))
                        .then(Commands.literal("network")
                                .then(Commands.literal("list")
                                        .executes(ctx -> listNets(ctx.getSource(), false, 1))
                                        .then(Commands.literal("all")
                                                .executes(ctx -> listNets(ctx.getSource(), true, 1))
                                        )
                                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                                .executes(ctx -> listNets(ctx.getSource(), false, IntegerArgumentType.getInteger(ctx, "page")))
                                                .then(Commands.literal("all")
                                                        .executes(ctx -> listNets(ctx.getSource(), true, IntegerArgumentType.getInteger(ctx, "page")))
                                                )
                                        )
                                )
                                .then(Commands.literal("info")
                                        .executes(ctx -> infoNet(ctx.getSource(), getPlayerNetIdOrFail(ctx.getSource())))
                                        .then(Commands.argument("netId", IntegerArgumentType.integer(0))
                                                .executes(ctx -> infoNet(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId")))
                                        )
                                )
                                .then(Commands.literal("restore")
                                        .then(Commands.argument("netId", IntegerArgumentType.integer(0))
                                                .executes(ctx -> restoreNet(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId")))
                                        )
                                )
                                .then(Commands.literal("insert")
                                        .then(Commands.argument("item", ItemArgument.item(context))
                                                .executes(ctx -> insertItem(ctx.getSource(), getPlayerNetIdOrFail(ctx.getSource()), ItemArgument.getItem(ctx, "item").createItemStack(1, false), 1L))
                                                .then(Commands.argument("count", LongArgumentType.longArg(1, Long.MAX_VALUE))
                                                        .executes(ctx -> insertItem(ctx.getSource(), getPlayerNetIdOrFail(ctx.getSource()), ItemArgument.getItem(ctx, "item").createItemStack(1, false), LongArgumentType.getLong(ctx, "count")))
                                                )
                                        )
                                        .then(Commands.argument("netId", IntegerArgumentType.integer(0))
                                                .then(Commands.argument("item", ItemArgument.item(context))
                                                        .executes(ctx -> insertItem(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), ItemArgument.getItem(ctx, "item").createItemStack(1, false), 1L))
                                                        .then(Commands.argument("count", LongArgumentType.longArg(1, Long.MAX_VALUE))
                                                                .executes(ctx -> insertItem(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), ItemArgument.getItem(ctx, "item").createItemStack(1, false), LongArgumentType.getLong(ctx, "count")))
                                                        )
                                                )
                                        )
                                )
                                .then(Commands.literal("giveTerminal")
                                        .executes(ctx -> giveTerminal(ctx.getSource(), getPlayerNetIdOrFail(ctx.getSource()), 1))
                                        .then(Commands.argument("netId", IntegerArgumentType.integer(0))
                                                .executes(ctx -> giveTerminal(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), 1))
                                                .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> giveTerminal(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), IntegerArgumentType.getInteger(ctx, "count")))
                                                )
                                        )
                                )
                                .then(Commands.literal("generateResources")
                                        .then(Commands.argument("netId", IntegerArgumentType.integer(0))
                                                .executes(ctx -> generateResources(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), 100, 100, 300, false, false, "mixed"))
                                                .then(Commands.argument("typeCount", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> generateResources(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), IntegerArgumentType.getInteger(ctx, "typeCount"), 100, 300, false, false, "mixed"))
                                                        .then(Commands.argument("minAmount", IntegerArgumentType.integer(1))
                                                                .executes(ctx -> generateResources(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), IntegerArgumentType.getInteger(ctx, "typeCount"), IntegerArgumentType.getInteger(ctx, "minAmount"), 300, false, false, "mixed"))
                                                                .then(Commands.argument("maxAmount", IntegerArgumentType.integer(1))
                                                                        .executes(ctx -> generateResources(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), IntegerArgumentType.getInteger(ctx, "typeCount"), IntegerArgumentType.getInteger(ctx, "minAmount"), IntegerArgumentType.getInteger(ctx, "maxAmount"), false, false, "mixed"))
                                                                        .then(Commands.literal("items")
                                                                                .executes(ctx -> generateResources(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), IntegerArgumentType.getInteger(ctx, "typeCount"), IntegerArgumentType.getInteger(ctx, "minAmount"), IntegerArgumentType.getInteger(ctx, "maxAmount"), false, false, "items"))
                                                                                .then(Commands.argument("withEnchantments", BoolArgumentType.bool())
                                                                                        .executes(ctx -> generateResources(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), IntegerArgumentType.getInteger(ctx, "typeCount"), IntegerArgumentType.getInteger(ctx, "minAmount"), IntegerArgumentType.getInteger(ctx, "maxAmount"), BoolArgumentType.getBool(ctx, "withEnchantments"), false, "items"))
                                                                                        .then(Commands.argument("withNbt", BoolArgumentType.bool())
                                                                                                .executes(ctx -> generateResources(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), IntegerArgumentType.getInteger(ctx, "typeCount"), IntegerArgumentType.getInteger(ctx, "minAmount"), IntegerArgumentType.getInteger(ctx, "maxAmount"), BoolArgumentType.getBool(ctx, "withEnchantments"), BoolArgumentType.getBool(ctx, "withNbt"), "items"))
                                                                                        )
                                                                                )
                                                                        )
                                                                        .then(Commands.literal("fluids")
                                                                                .executes(ctx -> generateResources(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), IntegerArgumentType.getInteger(ctx, "typeCount"), IntegerArgumentType.getInteger(ctx, "minAmount"), IntegerArgumentType.getInteger(ctx, "maxAmount"), false, false, "fluids"))
                                                                        )
                                                                        .then(Commands.literal("energy")
                                                                                .executes(ctx -> generateResources(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), IntegerArgumentType.getInteger(ctx, "typeCount"), IntegerArgumentType.getInteger(ctx, "minAmount"), IntegerArgumentType.getInteger(ctx, "maxAmount"), false, false, "energy"))
                                                                        )
                                                                        .then(Commands.literal("mixed")
                                                                                .executes(ctx -> generateResources(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), IntegerArgumentType.getInteger(ctx, "typeCount"), IntegerArgumentType.getInteger(ctx, "minAmount"), IntegerArgumentType.getInteger(ctx, "maxAmount"), false, false, "mixed"))
                                                                                .then(Commands.argument("withEnchantments", BoolArgumentType.bool())
                                                                                        .executes(ctx -> generateResources(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), IntegerArgumentType.getInteger(ctx, "typeCount"), IntegerArgumentType.getInteger(ctx, "minAmount"), IntegerArgumentType.getInteger(ctx, "maxAmount"), BoolArgumentType.getBool(ctx, "withEnchantments"), false, "mixed"))
                                                                                        .then(Commands.argument("withNbt", BoolArgumentType.bool())
                                                                                                .executes(ctx -> generateResources(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), IntegerArgumentType.getInteger(ctx, "typeCount"), IntegerArgumentType.getInteger(ctx, "minAmount"), IntegerArgumentType.getInteger(ctx, "maxAmount"), BoolArgumentType.getBool(ctx, "withEnchantments"), BoolArgumentType.getBool(ctx, "withNbt"), "mixed"))
                                                                                        )
                                                                                )
                                                                        )
                                                                        .then(Commands.literal("all")
                                                                                .executes(ctx -> generateResources(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), IntegerArgumentType.getInteger(ctx, "typeCount"), IntegerArgumentType.getInteger(ctx, "minAmount"), IntegerArgumentType.getInteger(ctx, "maxAmount"), false, false, "all"))
                                                                                .then(Commands.argument("withEnchantments", BoolArgumentType.bool())
                                                                                        .executes(ctx -> generateResources(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), IntegerArgumentType.getInteger(ctx, "typeCount"), IntegerArgumentType.getInteger(ctx, "minAmount"), IntegerArgumentType.getInteger(ctx, "maxAmount"), BoolArgumentType.getBool(ctx, "withEnchantments"), false, "all"))
                                                                                        .then(Commands.argument("withNbt", BoolArgumentType.bool())
                                                                                                .executes(ctx -> generateResources(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), IntegerArgumentType.getInteger(ctx, "typeCount"), IntegerArgumentType.getInteger(ctx, "minAmount"), IntegerArgumentType.getInteger(ctx, "maxAmount"), BoolArgumentType.getBool(ctx, "withEnchantments"), BoolArgumentType.getBool(ctx, "withNbt"), "all"))
                                                                                        )
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                                .then(Commands.literal("batchCreate")
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                .executes(ctx -> batchCreateNets(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "count"), Long.MAX_VALUE, Integer.MAX_VALUE))
                                                .then(Commands.argument("slotCapacity", LongArgumentType.longArg(1))
                                                        .executes(ctx -> batchCreateNets(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "count"), LongArgumentType.getLong(ctx, "slotCapacity"), Integer.MAX_VALUE))
                                                        .then(Commands.argument("slotMaxSize", IntegerArgumentType.integer(1))
                                                                .executes(ctx -> batchCreateNets(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "count"), LongArgumentType.getLong(ctx, "slotCapacity"), IntegerArgumentType.getInteger(ctx, "slotMaxSize")))
                                                        )
                                                )
                                        )
                                )
                                // OP专用的open命令，可以打开任何网络
                                .then(Commands.literal("openAny")
                                        .executes(ctx -> openGui(ctx.getSource(), getPlayerNetIdOrFail(ctx.getSource()), NetMenuType.NET_MENU, null, false))
                                        .then(Commands.argument("netId", IntegerArgumentType.integer(0))
                                                .executes(ctx -> openGui(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), NetMenuType.NET_MENU, null, false))
                                                .then(Commands.literal("craft")
                                                        .executes(ctx -> openGui(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), NetMenuType.NET_CRAFT_MENU, null, false))
                                                )
                                                .then(Commands.literal("terminal")
                                                        .executes(ctx -> openGui(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), NetMenuType.NET_CRAFT_TERMINAL, null, false))
                                                )
                                        )
                                        .then(Commands.literal("craft")
                                                .executes(ctx -> openGui(ctx.getSource(), getPlayerNetIdOrFail(ctx.getSource()), NetMenuType.NET_CRAFT_MENU, null, false))
                                        )
                                        .then(Commands.literal("terminal")
                                                .executes(ctx -> openGui(ctx.getSource(), getPlayerNetIdOrFail(ctx.getSource()), NetMenuType.NET_CRAFT_TERMINAL, null, false))
                                        )
                                )
                        )
        );

        // 注册open命令，权限0可用，但需要检查网络权限
        event.getDispatcher().register(
                Commands.literal("bdtools")
                        .then(Commands.literal("open")
                                .executes(ctx -> openGui(ctx.getSource(), getPlayerNetIdOrFail(ctx.getSource()), NetMenuType.NET_MENU, null, true))
                                .then(Commands.argument("netId", IntegerArgumentType.integer(0))
                                        .executes(ctx -> openGui(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), NetMenuType.NET_MENU, null, true))
                                        .then(Commands.literal("craft")
                                                .executes(ctx -> openGui(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), NetMenuType.NET_CRAFT_MENU, null, true))
                                        )
                                        .then(Commands.literal("terminal")
                                                .executes(ctx -> openGui(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), NetMenuType.NET_CRAFT_TERMINAL, null, true))
                                        )
                                )
                                .then(Commands.literal("craft")
                                        .executes(ctx -> openGui(ctx.getSource(), getPlayerNetIdOrFail(ctx.getSource()), NetMenuType.NET_CRAFT_MENU, null, true))
                                )
                                .then(Commands.literal("terminal")
                                        .executes(ctx -> openGui(ctx.getSource(), getPlayerNetIdOrFail(ctx.getSource()), NetMenuType.NET_CRAFT_TERMINAL, null, true))
                                )
                        )
                        // 批量添加成员命令（普通玩家可用，需要权限检查）
                        .then(Commands.literal("addMembers")
                                // 变体1：向单个网络添加多个玩家
                                .then(Commands.argument("netId", IntegerArgumentType.integer(0))
                                        .then(Commands.argument("players", EntityArgument.players())
                                                .executes(ctx -> batchAddPlayers(ctx.getSource(),
                                                        IntegerArgumentType.getInteger(ctx, "netId"),
                                                        EntityArgument.getPlayers(ctx, "players"),
                                                        false)) // false = 添加为普通成员
                                        )
                                )
                                // 变体2：向多个网络添加单个玩家（支持多个网络ID参数，每个都支持tab补全）
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("netId1", IntegerArgumentType.integer(0))
                                                .executes(ctx -> batchAddPlayerToNetworksSingle(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        new int[]{IntegerArgumentType.getInteger(ctx, "netId1")},
                                                        false))
                                                .then(Commands.argument("netId2", IntegerArgumentType.integer(0))
                                                        .executes(ctx -> batchAddPlayerToNetworksSingle(ctx.getSource(),
                                                                EntityArgument.getPlayer(ctx, "player"),
                                                                new int[]{IntegerArgumentType.getInteger(ctx, "netId1"),
                                                                        IntegerArgumentType.getInteger(ctx, "netId2")},
                                                                false))
                                                        .then(Commands.argument("netId3", IntegerArgumentType.integer(0))
                                                                .executes(ctx -> batchAddPlayerToNetworksSingle(ctx.getSource(),
                                                                        EntityArgument.getPlayer(ctx, "player"),
                                                                        new int[]{IntegerArgumentType.getInteger(ctx, "netId1"),
                                                                                IntegerArgumentType.getInteger(ctx, "netId2"),
                                                                                IntegerArgumentType.getInteger(ctx, "netId3")},
                                                                        false))
                                                                .then(Commands.argument("netId4", IntegerArgumentType.integer(0))
                                                                        .executes(ctx -> batchAddPlayerToNetworksSingle(ctx.getSource(),
                                                                                EntityArgument.getPlayer(ctx, "player"),
                                                                                new int[]{IntegerArgumentType.getInteger(ctx, "netId1"),
                                                                                        IntegerArgumentType.getInteger(ctx, "netId2"),
                                                                                        IntegerArgumentType.getInteger(ctx, "netId3"),
                                                                                        IntegerArgumentType.getInteger(ctx, "netId4")},
                                                                                false))
                                                                        .then(Commands.argument("netId5", IntegerArgumentType.integer(0))
                                                                                .executes(ctx -> batchAddPlayerToNetworksSingle(ctx.getSource(),
                                                                                        EntityArgument.getPlayer(ctx, "player"),
                                                                                        new int[]{IntegerArgumentType.getInteger(ctx, "netId1"),
                                                                                                IntegerArgumentType.getInteger(ctx, "netId2"),
                                                                                                IntegerArgumentType.getInteger(ctx, "netId3"),
                                                                                                IntegerArgumentType.getInteger(ctx, "netId4"),
                                                                                                IntegerArgumentType.getInteger(ctx, "netId5")},
                                                                                        false))
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                        // 批量添加管理员命令（普通玩家可用，需要权限检查）
                        .then(Commands.literal("addManagers")
                                // 变体1：向单个网络添加多个玩家
                                .then(Commands.argument("netId", IntegerArgumentType.integer(0))
                                        .then(Commands.argument("players", EntityArgument.players())
                                                .executes(ctx -> batchAddPlayers(ctx.getSource(),
                                                        IntegerArgumentType.getInteger(ctx, "netId"),
                                                        EntityArgument.getPlayers(ctx, "players"),
                                                        true)) // true = 添加为管理员
                                        )
                                )
                                // 变体2：向多个网络添加单个玩家（支持多个网络ID参数，每个都支持tab补全）
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("netId1", IntegerArgumentType.integer(0))
                                                .executes(ctx -> batchAddPlayerToNetworksSingle(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        new int[]{IntegerArgumentType.getInteger(ctx, "netId1")},
                                                        true))
                                                .then(Commands.argument("netId2", IntegerArgumentType.integer(0))
                                                        .executes(ctx -> batchAddPlayerToNetworksSingle(ctx.getSource(),
                                                                EntityArgument.getPlayer(ctx, "player"),
                                                                new int[]{IntegerArgumentType.getInteger(ctx, "netId1"),
                                                                        IntegerArgumentType.getInteger(ctx, "netId2")},
                                                                true))
                                                        .then(Commands.argument("netId3", IntegerArgumentType.integer(0))
                                                                .executes(ctx -> batchAddPlayerToNetworksSingle(ctx.getSource(),
                                                                        EntityArgument.getPlayer(ctx, "player"),
                                                                        new int[]{IntegerArgumentType.getInteger(ctx, "netId1"),
                                                                                IntegerArgumentType.getInteger(ctx, "netId2"),
                                                                                IntegerArgumentType.getInteger(ctx, "netId3")},
                                                                        true))
                                                                .then(Commands.argument("netId4", IntegerArgumentType.integer(0))
                                                                        .executes(ctx -> batchAddPlayerToNetworksSingle(ctx.getSource(),
                                                                                EntityArgument.getPlayer(ctx, "player"),
                                                                                new int[]{IntegerArgumentType.getInteger(ctx, "netId1"),
                                                                                        IntegerArgumentType.getInteger(ctx, "netId2"),
                                                                                        IntegerArgumentType.getInteger(ctx, "netId3"),
                                                                                        IntegerArgumentType.getInteger(ctx, "netId4")},
                                                                                true))
                                                                        .then(Commands.argument("netId5", IntegerArgumentType.integer(0))
                                                                                .executes(ctx -> batchAddPlayerToNetworksSingle(ctx.getSource(),
                                                                                        EntityArgument.getPlayer(ctx, "player"),
                                                                                        new int[]{IntegerArgumentType.getInteger(ctx, "netId1"),
                                                                                                IntegerArgumentType.getInteger(ctx, "netId2"),
                                                                                                IntegerArgumentType.getInteger(ctx, "netId3"),
                                                                                                IntegerArgumentType.getInteger(ctx, "netId4"),
                                                                                                IntegerArgumentType.getInteger(ctx, "netId5")},
                                                                                        true))
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                        // 批量删除玩家命令（普通玩家可用，需要权限检查）
                        .then(Commands.literal("removePlayers")
                                .then(Commands.argument("netId", IntegerArgumentType.integer(0))
                                        .then(Commands.argument("players", EntityArgument.players())
                                                .executes(ctx -> batchRemovePlayers(ctx.getSource(),
                                                        IntegerArgumentType.getInteger(ctx, "netId"),
                                                        EntityArgument.getPlayers(ctx, "players")))
                                        )
                                )
                        )
                        // 查看自己拥有权限的网络信息（普通玩家可用）
                        .then(Commands.literal("myNetworks")
                                // 默认：如果有默认网络则显示默认网络信息，否则显示网络列表
                                .executes(ctx -> showDefaultNetworkOrList(ctx.getSource(), null))
                                // myNetworks list [页码] [玩家] - 显示权限列表
                                .then(Commands.literal("list")
                                        .executes(ctx -> listPlayerNetworks(ctx.getSource(), null, 1))
                                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                                .executes(ctx -> listPlayerNetworks(ctx.getSource(), null, IntegerArgumentType.getInteger(ctx, "page")))
                                                .then(Commands.argument("player", EntityArgument.player())
                                                        .executes(ctx -> listPlayerNetworks(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "page")))
                                                )
                                        )
                                )
                                // myNetworks <netId> - 显示特定网络的详细信息
                                .then(Commands.argument("netId", IntegerArgumentType.integer(0))
                                        .executes(ctx -> showNetworkInfoForPlayer(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), null))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> showNetworkInfoForPlayer(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), EntityArgument.getPlayer(ctx, "player")))
                                        )
                                )
                          )
          );
    }

    private static DimensionsNet getNetOrFail(CommandSourceStack source, int netId)
    {
        DimensionsNet net = DimensionsNet.getNetFromId(netId);
        if (net == null)
        {
            source.sendFailure(CommandLang.component("error.network_not_found"));
        }
        return net;
    }

    private static int listNets(CommandSourceStack source, boolean showAll, int page)
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
        {
            source.sendFailure(CommandLang.component("error.server_not_available"));
            return 0;
        }

        int maxPerPage = com.solr98.beyondcmdextension.CommandConfig.SERVER.maxNetworksPerPage.get();
        int startIndex = (page - 1) * maxPerPage;
        int endIndex = startIndex + maxPerPage;

        String titleKey = showAll ? "network.list.all_title" : "network.list.title";
        StringBuilder message = new StringBuilder("========= " + CommandLang.get(titleKey) + " " + CommandLang.get("network.list.page", page) + " =========\n");
        int count = 0;
        int totalCount = 0;
        int displayedCount = 0;

        for (int netId = 0; netId < 10000; netId++)
        {
            DimensionsNet net = server.overworld().getDataStorage().get(DimensionsNet::load, "BDNet_" + netId);
            if (net != null)
            {
                if (!showAll && net.deleted) continue;

                totalCount++;

                if (totalCount > startIndex && displayedCount < maxPerPage)
                {
                    String ownerName = CommandLang.get("network.info.unknown");
                    if (net.getOwner() != null)
                    {
                        String playerName = PlayerNameHelper.getPlayerNameByUUID(net.getOwner(), server);
                        if (playerName != null && !playerName.isEmpty())
                        {
                            ownerName = playerName;
                        }
                    }
                    message.append("Net ID: ").append(netId)
                            .append(" | ").append(CommandLang.get("network.list.owner")).append(": ").append(ownerName)
                            .append(" | ").append(CommandLang.get("network.list.players")).append(": ").append(formatNumber(net.getPlayers().size()))
                            .append(" | ").append(CommandLang.get("network.list.managers")).append(": ").append(formatNumber(net.getManagers().size()))
                            .append(net.deleted ? " | " + CommandLang.get("network.list.deleted_mark") + "\n" : "\n");
                    displayedCount++;
                }
                count++;
            }
        }

        if (displayedCount == 0)
        {
            source.sendSuccess(() -> Component.literal(showAll ? CommandLang.get("network.list.none_all") : CommandLang.get("network.list.none")), false);
        }
        else
        {
            int totalPages = (int) Math.ceil((double) totalCount / maxPerPage);

            MutableComponent navigation = Component.empty();

            if (page > 1)
            {
                navigation = navigation.append(
                        Component.literal("[" + CommandLang.get("network.list.previous") + "]")
                                .withStyle(Style.EMPTY
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                                "/bdtools network list " + (page - 1) + (showAll ? " all" : "")))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                Component.literal("Click to go to page " + (page - 1))))
                                        .withColor(net.minecraft.ChatFormatting.GREEN)
                                )
                ).append(Component.literal(" "));
            }

            navigation = navigation.append(
                    Component.literal("[" + CommandLang.get("network.list.page_with_total", page, totalPages, totalCount) + "]")
                            .withStyle(Style.EMPTY
                                    .withColor(net.minecraft.ChatFormatting.YELLOW)
                            )
            );

            if (page < totalPages)
            {
                navigation = navigation.append(Component.literal(" ")).append(
                        Component.literal("[" + CommandLang.get("network.list.next") + "]")
                                .withStyle(Style.EMPTY
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                                "/bdtools network list " + (page + 1) + (showAll ? " all" : "")))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                Component.literal("Click to go to page " + (page + 1))))
                                        .withColor(net.minecraft.ChatFormatting.GREEN)
                                )
                );
            }

            MutableComponent finalMessage = Component.literal(message.toString())
                    .append(Component.literal("\n"))
                    .append(navigation);

            source.sendSuccess(() -> finalMessage, false);
        }
        return count;
    }

    private static int infoNet(CommandSourceStack source, int netId)
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
        {
            source.sendFailure(CommandLang.component("error.server_not_available"));
            return 0;
        }

        DimensionsNet net = server.overworld().getDataStorage().get(DimensionsNet::load, "BDNet_" + netId);
        if (net == null)
        {
            source.sendFailure(CommandLang.component("network.info.not_exist", netId));
            return 0;
        }

        String ownerName;
        if (net.getOwner() != null)
        {
            ownerName = PlayerNameHelper.getPlayerNameByUUID(net.getOwner(), server);
        }
        else
        {
            ownerName = CommandLang.get("network.info.unknown");
        }

        long slotCapacity = net.getUnifiedStorage().slotCapacity;
        int slotMaxSize = net.getUnifiedStorage().slotMaxSize;
        int currentTime = 0;
        try
        {
            java.lang.reflect.Field field = DimensionsNet.class.getDeclaredField("currentTime");
            field.setAccessible(true);
            currentTime = field.getInt(net);
        }
        catch (Exception e)
        {
            currentTime = -1;
        }

        int totalSlots = net.getUnifiedStorage().getSlots();

        // 统计不同类型的资源
        int itemTypesCount = 0;
        int fluidTypesCount = 0;
        int energyTypesCount = 0;
        java.math.BigInteger totalItems = java.math.BigInteger.ZERO;
        java.math.BigInteger totalFluids = java.math.BigInteger.ZERO;
        java.math.BigInteger totalEnergy = java.math.BigInteger.ZERO;

        for (com.wintercogs.beyonddimensions.api.storage.key.KeyAmount ka : net.getUnifiedStorage().getStorage()) {
            Object key = ka.key();
            long amount = ka.amount();

            if (key instanceof ItemStackKey) {
                itemTypesCount++;
                totalItems = totalItems.add(java.math.BigInteger.valueOf(amount));
            } else if (key instanceof FluidStackKey) {
                fluidTypesCount++;
                totalFluids = totalFluids.add(java.math.BigInteger.valueOf(amount));
            } else if (key instanceof EnergyStackKey) {
                energyTypesCount++;
                totalEnergy = totalEnergy.add(java.math.BigInteger.valueOf(amount));
            }
        }

        // 构建消息 - 使用与myNetworks一致的格式
        // 构建消息 - 使用带悬浮提示的组件（与myNetworks保持一致）
        MutableComponent message = Component.literal(CommandLang.get("network.info.title", netId) + "\n")
                .append(createHoverableText(CommandLang.get("network.info.owner_label", ownerName), ""))
                .append(Component.literal(net.deleted ? CommandLang.get("network.info.status.deleted") : CommandLang.get("network.info.status.active"))
                        .withStyle(net.deleted ? ChatFormatting.RED : ChatFormatting.GREEN))
                .append(Component.literal("\n"))
                .append(createHoverableText(CommandLang.get("network.info.crystal_time"), ""))
                .append(createHoverableTime(currentTime))
                .append(createHoverableText(" tick\n", ""))
                .append(createHoverableText(CommandLang.get("network.info.slot_capacity_label"), ""))
                .append(createHoverableNumber(slotCapacity, CommandLang.get("network.info.slot_capacity_label")))
                .append(createHoverableText(CommandLang.get("network.info.slot_count_label"), ""))
                .append(createHoverableNumber(slotMaxSize, CommandLang.get("network.info.slot_count_label")))
                .append(Component.literal("\n"))
                .append(createHoverableText(CommandLang.get("network.info.storage_stats") + "\n", ""))
                .append(createHoverableText(CommandLang.get("network.info.items_label"), ""))
                .append(createHoverableResourceType(itemTypesCount, CommandLang.get("network.info.items_label").trim()))
                .append(createHoverableText(CommandLang.get("network.info.types_suffix"), ""))
                .append(createHoverableItemCount(totalItems))
                .append(Component.literal("\n"))
                .append(createHoverableText(CommandLang.get("network.info.fluids_label"), ""))
                .append(createHoverableResourceType(fluidTypesCount, CommandLang.get("network.info.fluids_label").trim()))
                .append(createHoverableText(CommandLang.get("network.info.types_suffix"), ""))
                .append(createHoverableFluid(totalFluids))
                .append(createHoverableText(" mB\n", ""))
                .append(createHoverableText(CommandLang.get("network.info.energy_label"), ""))
                .append(createHoverableResourceType(energyTypesCount, CommandLang.get("network.info.energy_label").trim()))
                .append(createHoverableText(CommandLang.get("network.info.types_suffix"), ""))
                .append(createHoverableEnergy(totalEnergy))
                .append(createHoverableText(" FE\n", ""))
                .append(createHoverableText(CommandLang.get("network.info.player_count_label"), ""))
                .append(createHoverableNumber(net.getPlayers().size(), CommandLang.get("network.info.player_count_label")))
                .append(createHoverableText(CommandLang.get("network.info.manager_count_label"), ""))
                .append(createHoverableNumber(net.getManagers().size(), CommandLang.get("network.info.manager_count_label")))
                .append(Component.literal("\n"));

        // 玩家列表
        UUID ownerUuid = net.getOwner();

        // 添加玩家列表标题
        message.append(createHoverableText(CommandLang.get("network.info.player_list_label"), ""));

        // 构建玩家列表组件
        boolean hasPlayers = false;
        boolean firstPlayer = true;

        // 添加所有者
        if (ownerUuid != null) {
            String ownerPlayerName = PlayerNameHelper.getPlayerNameByUUID(ownerUuid, server);
            if (ownerPlayerName != null && !ownerPlayerName.isEmpty()) {
                if (!firstPlayer) {
                    message.append(createHoverableText(", ", ""));
                }
                message.append(Component.literal(ownerPlayerName)
                        .withStyle(ChatFormatting.RED)); // 红色
                hasPlayers = true;
                firstPlayer = false;
            }
        }

        // 添加管理员
        for (UUID managerUuid : net.getManagers()) {
            if (ownerUuid != null && managerUuid.equals(ownerUuid)) continue;

            String managerName = PlayerNameHelper.getPlayerNameByUUID(managerUuid, server);
            if (managerName != null && !managerName.isEmpty()) {
                if (!firstPlayer) {
                    message.append(createHoverableText(", ", ""));
                }
                message.append(Component.literal(managerName)
                        .withStyle(ChatFormatting.BLUE)); // 蓝色
                hasPlayers = true;
                firstPlayer = false;
            }
        }

        // 添加普通成员
        for (UUID playerUuid : net.getPlayers()) {
            if (ownerUuid != null && playerUuid.equals(ownerUuid)) continue;
            if (net.getManagers().contains(playerUuid)) continue;

            String playerName = PlayerNameHelper.getPlayerNameByUUID(playerUuid, server);
            if (playerName != null && !playerName.isEmpty()) {
                if (!firstPlayer) {
                    message.append(createHoverableText(", ", ""));
                }
                message.append(Component.literal(playerName)
                        .withStyle(ChatFormatting.GREEN)); // 绿色
                hasPlayers = true;
                firstPlayer = false;
            }
        }

        if (!hasPlayers) {
            message.append(createHoverableText(CommandLang.get("network.info.no_players"), ""));
        }
        message.append(Component.literal("\n"));

        MutableComponent finalMessage = message;

        source.sendSuccess(() -> finalMessage, false);
        return Command.SINGLE_SUCCESS;
    }

    private static int restoreNet(CommandSourceStack source, int netId)
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
        {
            source.sendFailure(CommandLang.component("error.server_not_available"));
            return 0;
        }

        // 使用兼容的方法获取网络（兼容旧版本）
        DimensionsNet net = server.overworld().getDataStorage().get(DimensionsNet::load, "BDNet_" + netId);
        if (net == null)
        {
            source.sendFailure(CommandLang.component("network.restore.not_exist", netId));
            return 0;
        }

        if (!net.deleted)
        {
            source.sendFailure(CommandLang.component("network.restore.not_deleted", netId));
            return 0;
        }

        // 恢复网络状态 - 使用兼容旧版本的方法
        net.deleted = false;

        // 尝试修复网络ID（如果被标记为-99）
        // 旧版本使用-99作为删除标记，新版本可能也使用
        try {
            // 使用反射安全地访问id字段
            java.lang.reflect.Field idField = DimensionsNet.class.getDeclaredField("id");
            idField.setAccessible(true);
            int currentId = idField.getInt(net);
            if (currentId == -99) {
                idField.setInt(net, netId);
            }
        } catch (Exception e) {
            // 如果反射失败，尝试使用setId方法（如果可用）
            try {
                java.lang.reflect.Method setIdMethod = DimensionsNet.class.getMethod("setId", int.class);
                setIdMethod.invoke(net, netId);
            } catch (Exception e2) {
                // 两种方法都失败，记录警告但继续
                source.sendSuccess(() -> CommandLang.component("network.restore.note"), false);
            }
        }

        net.setDirty();

        source.sendSuccess(
                () -> Component.literal(CommandLang.get("network.restore.success", netId)),
                false  // 仅对执行者可见，不广播给所有玩家
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int insertItem(CommandSourceStack source, int netId, ItemStack itemStack, long count)
    {
        DimensionsNet net = getNetOrFail(source, netId);
        if (net == null) return 0;

        ItemStackKey key = new ItemStackKey(itemStack);
        var remainder = net.getUnifiedStorage().insert(key, count, false);

        long inserted = count - remainder.amount();

        source.sendSuccess(
                () -> Component.literal(CommandLang.get("network.insert.success", inserted, itemStack.getHoverName().getString(), netId)),
                false
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int giveTerminal(CommandSourceStack source, int netId, int count)
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
        {
            source.sendFailure(CommandLang.component("error.server_not_available"));
            return 0;
        }

        DimensionsNet net = server.overworld().getDataStorage().get(DimensionsNet::load, "BDNet_" + netId);
        if (net == null)
        {
            source.sendFailure(CommandLang.component("network.giveTerminal.not_exist", netId));
            return 0;
        }

        ServerPlayer player = source.getPlayer();
        if (player == null)
        {
            source.sendFailure(CommandLang.component("error.player_required"));
            return 0;
        }

        Item portableTerminal = ForgeRegistries.ITEMS.getValue(
                ResourceLocation.fromNamespaceAndPath(BDConstants.MODID, "net_terminal_item")
        );
        if (portableTerminal == null)
        {
            source.sendFailure(CommandLang.component("error.item_not_found"));
            return 0;
        }

        ItemStack terminalStack = new ItemStack(portableTerminal, count);
        com.wintercogs.beyonddimensions.common.item.NetedItem.setNetId(terminalStack, netId);

        final String ownerName;
        if (net.getOwner() != null)
        {
            ownerName = PlayerNameHelper.getPlayerNameByUUID(net.getOwner(), server);
        }
        else
        {
            ownerName = "Unknown";
        }

        terminalStack.setHoverName(Component.literal(ownerName + " " + CommandLang.get("network.list.owner")));

        if (!player.getInventory().add(terminalStack))
        {
            player.drop(terminalStack, false);
        }

        source.sendSuccess(
                () -> Component.literal(CommandLang.get("network.giveTerminal.success", netId, ownerName, count)),
                false
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int generateItems(CommandSourceStack source, int netId, int typeCount, int minAmount, int maxAmount, boolean withEnchantments, boolean withNbt)
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
        {
            source.sendFailure(CommandLang.component("error.server_not_available"));
            return 0;
        }

        DimensionsNet net = server.overworld().getDataStorage().get(DimensionsNet::load, "BDNet_" + netId);
        if (net == null)
        {
            source.sendFailure(CommandLang.component("network.info.not_exist", netId));
            return 0;
        }

        List<Item> allItems = BuiltInRegistries.ITEM.stream()
                .filter(item -> item != Items.AIR)
                .collect(Collectors.toList());

        List<Enchantment> allEnchantments = BuiltInRegistries.ENCHANTMENT.stream()
                .collect(Collectors.toList());

        Random random = new Random();
        Collections.shuffle(allItems, random);

        int count = Math.min(typeCount, allItems.size());
        java.math.BigInteger totalInserted = java.math.BigInteger.ZERO;

        for (int i = 0; i < count; i++)
        {
            Item item = allItems.get(i);
            int amount = minAmount + random.nextInt(maxAmount - minAmount + 1);

            ItemStack itemStack = new ItemStack(item, amount);

            if (withEnchantments)
            {
                addRandomEnchantments(itemStack, allEnchantments, random);
            }

            if (withNbt)
            {
                addRandomNbt(itemStack, random);
            }

            ItemStackKey stack = new ItemStackKey(itemStack);
            var remainder = net.getUnifiedStorage().insert(stack, amount, false);
            totalInserted = totalInserted.add(java.math.BigInteger.valueOf(amount - remainder.amount()));
        }

        final java.math.BigInteger finalTotalInserted = totalInserted;
        source.sendSuccess(
                () -> Component.literal(CommandLang.get("network.generateItems.success", formatNumber(count), formatNumber(finalTotalInserted), netId)),
                false
        );
        return Command.SINGLE_SUCCESS;
    }

    private static void addRandomEnchantments(ItemStack itemStack, List<Enchantment> allEnchantments, Random random)
    {
        CompoundTag tag = itemStack.getOrCreateTag();
        ListTag enchantmentsList = new ListTag();

        int enchantmentCount = 1 + random.nextInt(4);

        for (int i = 0; i < enchantmentCount; i++)
        {
            Enchantment enchantment = allEnchantments.get(random.nextInt(allEnchantments.size()));
            int level = 1 + random.nextInt(enchantment.getMaxLevel());

            CompoundTag enchantmentTag = new CompoundTag();
            enchantmentTag.putString("id", BuiltInRegistries.ENCHANTMENT.getKey(enchantment).toString());
            enchantmentTag.putShort("lvl", (short) level);
            enchantmentsList.add(enchantmentTag);
        }

        tag.put("Enchantments", enchantmentsList);
        itemStack.setTag(tag);
    }

    private static void addRandomNbt(ItemStack itemStack, Random random)
    {
        CompoundTag tag = itemStack.getOrCreateTag();

        if (random.nextBoolean())
        {
            tag.putString("CustomName", "{\"text\":\"Random Item " + random.nextInt(1000) + "\"}");
        }

        if (random.nextBoolean())
        {
            tag.putBoolean("Unbreakable", true);
        }

        if (random.nextDouble() < 0.3)
        {
            CompoundTag display = tag.getCompound("display");
            int color = random.nextInt(16777216);
            display.putInt("color", color);
            tag.put("display", display);
        }

        if (random.nextDouble() < 0.2)
        {
            ListTag lore = new ListTag();
            lore.add(StringTag.valueOf("Lore line " + random.nextInt(10)));
            tag.put("Lore", lore);
        }

        itemStack.setTag(tag);
    }

    private static int generateResources(CommandSourceStack source, int netId, int typeCount, int minAmount, int maxAmount, boolean withEnchantments, boolean withNbt, String resourceType)
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
        {
            source.sendFailure(CommandLang.component("error.server_not_available"));
            return 0;
        }

        DimensionsNet net = server.overworld().getDataStorage().get(DimensionsNet::load, "BDNet_" + netId);
        if (net == null)
        {
            source.sendFailure(CommandLang.component("network.info.not_exist", netId));
            return 0;
        }

        Random random = new Random();
        java.math.BigInteger totalInserted = java.math.BigInteger.ZERO;
        final int[] actualCountHolder = new int[1];

        switch (resourceType) {
            case "items":
                actualCountHolder[0] = generateItemsResources(source, net, typeCount, minAmount, maxAmount, withEnchantments, withNbt, random, totalInserted);
                break;
            case "fluids":
                actualCountHolder[0] = generateFluidsResources(source, net, typeCount, minAmount, maxAmount, random, totalInserted);
                break;
            case "energy":
                actualCountHolder[0] = generateEnergyResources(source, net, typeCount, minAmount, maxAmount, random, totalInserted);
                break;
            case "mixed":
                actualCountHolder[0] = generateMixedResources(source, net, typeCount, minAmount, maxAmount, withEnchantments, withNbt, random, totalInserted);
                break;
            case "all":
                actualCountHolder[0] = generateAllResources(source, net, typeCount, minAmount, maxAmount, withEnchantments, withNbt, random, totalInserted);
                break;
            default:
                source.sendFailure(CommandLang.component("error.invalid_resource_type", resourceType));
                return 0;
        }

        final java.math.BigInteger finalTotalInserted = totalInserted;
        source.sendSuccess(
                () -> Component.literal(CommandLang.get("network.generateResources.success", formatNumber(actualCountHolder[0]), formatNumber(finalTotalInserted), netId, resourceType)),
                false
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int generateItemsResources(CommandSourceStack source, DimensionsNet net, int typeCount, int minAmount, int maxAmount, boolean withEnchantments, boolean withNbt, Random random, java.math.BigInteger totalInserted)
    {
        List<Item> allItems = BuiltInRegistries.ITEM.stream()
                .filter(item -> item != Items.AIR)
                .collect(Collectors.toList());

        List<Enchantment> allEnchantments = BuiltInRegistries.ENCHANTMENT.stream()
                .collect(Collectors.toList());

        Collections.shuffle(allItems, random);

        int count = Math.min(typeCount, allItems.size());

        for (int i = 0; i < count; i++)
        {
            Item item = allItems.get(i);
            int amount = minAmount + random.nextInt(maxAmount - minAmount + 1);

            ItemStack itemStack = new ItemStack(item, amount);

            if (withEnchantments)
            {
                addRandomEnchantments(itemStack, allEnchantments, random);
            }

            if (withNbt)
            {
                addRandomNbt(itemStack, random);
            }

            ItemStackKey stack = new ItemStackKey(itemStack);
            var remainder = net.getUnifiedStorage().insert(stack, amount, false);
            totalInserted = totalInserted.add(java.math.BigInteger.valueOf(amount - remainder.amount()));
        }

        return count;
    }

    private static int generateFluidsResources(CommandSourceStack source, DimensionsNet net, int typeCount, int minAmount, int maxAmount, Random random, java.math.BigInteger totalInserted)
    {
        List<Fluid> allFluids = BuiltInRegistries.FLUID.stream()
                .filter(fluid -> fluid != Fluids.EMPTY && fluid != Fluids.FLOWING_WATER && fluid != Fluids.FLOWING_LAVA)
                .collect(Collectors.toList());

        Collections.shuffle(allFluids, random);

        int count = Math.min(typeCount, allFluids.size());

        for (int i = 0; i < count; i++)
        {
            Fluid fluid = allFluids.get(i);
            int amount = minAmount + random.nextInt(maxAmount - minAmount + 1);

            FluidStack fluidStack = new FluidStack(fluid, amount);
            FluidStackKey stack = new FluidStackKey(fluidStack);
            var remainder = net.getUnifiedStorage().insert(stack, amount, false);
            totalInserted = totalInserted.add(java.math.BigInteger.valueOf(amount - remainder.amount()));
        }

        return count;
    }

    private static int generateEnergyResources(CommandSourceStack source, DimensionsNet net, int typeCount, int minAmount, int maxAmount, Random random, java.math.BigInteger totalInserted)
    {
        int count = Math.min(typeCount, 1); // 能量只有一种类型

        for (int i = 0; i < count; i++)
        {
            int amount = minAmount + random.nextInt(maxAmount - minAmount + 1);

            EnergyStackKey stack = EnergyStackKey.INSTANCE;
            var remainder = net.getUnifiedStorage().insert(stack, amount, false);
            totalInserted = totalInserted.add(java.math.BigInteger.valueOf(amount - remainder.amount()));
        }

        return count;
    }

    private static int generateMixedResources(CommandSourceStack source, DimensionsNet net, int typeCount, int minAmount, int maxAmount, boolean withEnchantments, boolean withNbt, Random random, java.math.BigInteger totalInserted)
    {
        List<Item> allItems = BuiltInRegistries.ITEM.stream()
                .filter(item -> item != Items.AIR)
                .collect(Collectors.toList());

        List<Fluid> allFluids = BuiltInRegistries.FLUID.stream()
                .filter(fluid -> fluid != Fluids.EMPTY && fluid != Fluids.FLOWING_WATER && fluid != Fluids.FLOWING_LAVA)
                .collect(Collectors.toList());

        List<Enchantment> allEnchantments = BuiltInRegistries.ENCHANTMENT.stream()
                .collect(Collectors.toList());

        Collections.shuffle(allItems, random);
        Collections.shuffle(allFluids, random);

        int count = Math.min(typeCount, allItems.size() + allFluids.size() + 1); // +1 为能量

        int actualCount = 0;
        int itemIndex = 0;
        int fluidIndex = 0;

        for (int i = 0; i < count; i++)
        {
            int resourceType = random.nextInt(3); // 0=物品, 1=流体, 2=能量

            switch (resourceType) {
                case 0: // 物品
                    if (itemIndex < allItems.size()) {
                        Item item = allItems.get(itemIndex++);
                        int amount = minAmount + random.nextInt(maxAmount - minAmount + 1);

                        ItemStack itemStack = new ItemStack(item, amount);

                        if (withEnchantments) {
                            addRandomEnchantments(itemStack, allEnchantments, random);
                        }

                        if (withNbt) {
                            addRandomNbt(itemStack, random);
                        }

                        ItemStackKey stack = new ItemStackKey(itemStack);
                        var remainder = net.getUnifiedStorage().insert(stack, amount, false);
                        totalInserted = totalInserted.add(java.math.BigInteger.valueOf(amount - remainder.amount()));
                        actualCount++;
                    }
                    break;
                case 1: // 流体
                    if (fluidIndex < allFluids.size()) {
                        Fluid fluid = allFluids.get(fluidIndex++);
                        int amount = minAmount + random.nextInt(maxAmount - minAmount + 1);

                        FluidStack fluidStack = new FluidStack(fluid, amount);
                        FluidStackKey stack = new FluidStackKey(fluidStack);
                        var remainder = net.getUnifiedStorage().insert(stack, amount, false);
                        totalInserted = totalInserted.add(java.math.BigInteger.valueOf(amount - remainder.amount()));
                        actualCount++;
                    }
                    break;
                case 2: // 能量
                    int amount = minAmount + random.nextInt(maxAmount - minAmount + 1);

                    EnergyStackKey stack = EnergyStackKey.INSTANCE;
                    var remainder = net.getUnifiedStorage().insert(stack, amount, false);
                    totalInserted = totalInserted.add(java.math.BigInteger.valueOf(amount - remainder.amount()));
                    actualCount++;
                    break;
            }
        }

        return actualCount;
    }

    private static int generateAllResources(CommandSourceStack source, DimensionsNet net, int typeCount, int minAmount, int maxAmount, boolean withEnchantments, boolean withNbt, Random random, java.math.BigInteger totalInserted)
    {
        // 收集所有资源类型
        List<Object> allResources = new ArrayList<>();

        // 添加物品
        List<Item> allItems = BuiltInRegistries.ITEM.stream()
                .filter(item -> item != Items.AIR)
                .collect(Collectors.toList());
        allResources.addAll(allItems);

        // 添加流体
        List<Fluid> allFluids = BuiltInRegistries.FLUID.stream()
                .filter(fluid -> fluid != Fluids.EMPTY && fluid != Fluids.FLOWING_WATER && fluid != Fluids.FLOWING_LAVA)
                .collect(Collectors.toList());
        allResources.addAll(allFluids);

        // 添加能量（用特殊标记表示）
        allResources.add("ENERGY");

        List<Enchantment> allEnchantments = BuiltInRegistries.ENCHANTMENT.stream()
                .collect(Collectors.toList());

        Collections.shuffle(allResources, random);

        int count = Math.min(typeCount, allResources.size());
        int actualCount = 0;

        for (int i = 0; i < count; i++)
        {
            Object resource = allResources.get(i);
            int amount = minAmount + random.nextInt(maxAmount - minAmount + 1);

            if (resource instanceof Item item) {
                ItemStack itemStack = new ItemStack(item, amount);

                if (withEnchantments) {
                    addRandomEnchantments(itemStack, allEnchantments, random);
                }

                if (withNbt) {
                    addRandomNbt(itemStack, random);
                }

                ItemStackKey stack = new ItemStackKey(itemStack);
                var remainder = net.getUnifiedStorage().insert(stack, amount, false);
                totalInserted = totalInserted.add(java.math.BigInteger.valueOf(amount - remainder.amount()));
                actualCount++;
            } else if (resource instanceof Fluid fluid) {
                FluidStack fluidStack = new FluidStack(fluid, amount);
                FluidStackKey stack = new FluidStackKey(fluidStack);
                var remainder = net.getUnifiedStorage().insert(stack, amount, false);
                totalInserted = totalInserted.add(java.math.BigInteger.valueOf(amount - remainder.amount()));
                actualCount++;
            } else if (resource.equals("ENERGY")) {
                EnergyStackKey stack = EnergyStackKey.INSTANCE;
                var remainder = net.getUnifiedStorage().insert(stack, amount, false);
                totalInserted = totalInserted.add(java.math.BigInteger.valueOf(amount - remainder.amount()));
                actualCount++;
            }
        }

        return actualCount;
    }

    private static int batchCreateNets(CommandSourceStack source, int count, long slotCapacity, int slotMaxSize)
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
        {
            source.sendFailure(CommandLang.component("error.server_not_available"));
            return 0;
        }

        int createdCount = 0;
        List<Integer> createdNetIds = new java.util.ArrayList<>();

        // 查找起始网络ID：从0开始找到第一个不存在的网络
        int startNetId = findFirstAvailableNetId(server);
        if (startNetId < 0) {
            source.sendFailure(CommandLang.component("error.network_id_limit"));
            return 0;
        }

        for (int i = 0; i < count; i++)
        {
            int netId = startNetId + i;
            String netIdStr = "BDNet_" + netId;

            try {
                // 检查网络ID是否已被占用（在批量创建过程中）
                if (server.overworld().getDataStorage().get(DimensionsNet::load, netIdStr) != null) {
                    // 网络ID已被占用，跳过这个ID
                    source.sendSuccess(() -> Component.literal("Warning: Network ID " + netId + " already exists, skipping"), false);
                    continue;
                }

                // 创建网络
                DimensionsNet newNet = server.overworld().getDataStorage().computeIfAbsent(DimensionsNet::load, DimensionsNet::create, netIdStr);

                // 设置网络ID
                try {
                    java.lang.reflect.Method setIdMethod = DimensionsNet.class.getMethod("setId", int.class);
                    setIdMethod.invoke(newNet, netId);
                } catch (Exception e) {
                    // 如果setId方法不可用，使用反射设置id字段
                    try {
                        java.lang.reflect.Field idField = DimensionsNet.class.getDeclaredField("id");
                        idField.setAccessible(true);
                        idField.setInt(newNet, netId);
                    } catch (Exception e2) {
                        // 如果两种方法都失败，跳过这个网络
                        source.sendSuccess(() -> Component.literal("Warning: Failed to set ID for network " + netId), false);
                        continue;
                    }
                }

                // 清空网络成员（新创建的网络应该是空的）
                newNet.getPlayers().clear();
                newNet.getManagers().clear();

                // 设置存储容量
                try {
                    // 尝试使用新版本的setSlotCapacity和setSlotMaxSize方法
                    java.lang.reflect.Method setSlotCapacityMethod = newNet.getUnifiedStorage().getClass().getMethod("setSlotCapacity", long.class);
                    java.lang.reflect.Method setSlotMaxSizeMethod = newNet.getUnifiedStorage().getClass().getMethod("setSlotMaxSize", int.class);
                    setSlotCapacityMethod.invoke(newNet.getUnifiedStorage(), slotCapacity);
                    setSlotMaxSizeMethod.invoke(newNet.getUnifiedStorage(), slotMaxSize);
                } catch (Exception e) {
                    // 如果方法不可用，尝试直接设置字段
                    try {
                        java.lang.reflect.Field slotCapacityField = newNet.getUnifiedStorage().getClass().getDeclaredField("slotCapacity");
                        java.lang.reflect.Field slotMaxSizeField = newNet.getUnifiedStorage().getClass().getDeclaredField("slotMaxSize");
                        slotCapacityField.setAccessible(true);
                        slotMaxSizeField.setAccessible(true);
                        slotCapacityField.setLong(newNet.getUnifiedStorage(), slotCapacity);
                        slotMaxSizeField.setInt(newNet.getUnifiedStorage(), slotMaxSize);
                    } catch (Exception e2) {
                        // 如果设置容量失败，使用默认容量
                        source.sendSuccess(() -> Component.literal("Note: Using default capacity for network " + netId), false);
                    }
                }

                // 标记为脏数据
                newNet.setDirty();

                // 尝试注册到NetRegistryIndex（如果可用）
                try {
                    Class<?> netRegistryIndexClass = Class.forName("com.wintercogs.beyonddimensions.api.dimensionnet.NetRegistryIndex");
                    java.lang.reflect.Method getMethod = netRegistryIndexClass.getMethod("get", MinecraftServer.class);
                    Object index = getMethod.invoke(null, server);
                    java.lang.reflect.Method registerNetMethod = netRegistryIndexClass.getMethod("registerNet", MinecraftServer.class, int.class);
                    registerNetMethod.invoke(index, server, netId);
                } catch (Exception e) {
                    // NetRegistryIndex不可用，跳过
                }

                createdCount++;
                createdNetIds.add(netId);

            } catch (Exception e) {
                // 创建单个网络失败，继续尝试创建其他网络
                final int networkIndex = i + 1;
                final String errorMsg = e.getMessage();
                source.sendSuccess(() -> Component.literal("Warning: Failed to create network #" + networkIndex + " (ID: " + netId + "): " + errorMsg), false);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(CommandLang.get("network.batchCreate.success", formatNumber(createdCount)));
        if (createdCount <= 10 && createdCount > 0)
        {
            sb.append(" (IDs: ");
            for (int i = 0; i < createdNetIds.size(); i++)
            {
                if (i > 0) sb.append(", ");
                sb.append(createdNetIds.get(i));
            }
            sb.append(")");
        }
        else if (createdCount > 10)
        {
            sb.append(" (IDs: ").append(createdNetIds.get(0)).append(" - ").append(createdNetIds.get(createdNetIds.size() - 1)).append(")");
        }

        if (createdCount < count) {
            sb.append("\n").append("Note: ").append(count - createdCount).append(" networks failed to create.");
        }

        source.sendSuccess(() -> Component.literal(sb.toString()), false);
        return createdCount;
    }

    // 查找第一个可用的网络ID（从0开始）
    private static int findFirstAvailableNetId(MinecraftServer server) {
        // 从0开始查找，最多到10000
        for (int netId = 0; netId < 10000; netId++) {
            if (server.overworld().getDataStorage().get(DimensionsNet::load, "BDNet_" + netId) == null) {
                return netId;
            }
        }
        return -1; // 没有可用的网络ID
    }

    private static int openGui(CommandSourceStack source, int netId, NetMenuType menuType, ServerPlayer targetPlayer, boolean requirePermissionCheck)
    {
        ServerPlayer player = targetPlayer != null ? targetPlayer : source.getPlayer();
        if (player == null)
        {
            source.sendFailure(CommandLang.component("network.open.error.player_required"));
            return 0;
        }

        DimensionsNet net = DimensionsNet.getNetFromId(netId);
        if (net == null)
        {
            source.sendFailure(CommandLang.component("network.open.error.not_exist", netId));
            return 0;
        }

        // 检查玩家是否有权限访问该网络
        boolean hasPermission = true;
        if (requirePermissionCheck)
        {
            // 如果玩家是OP2，则跳过权限检查
            if (source.hasPermission(OP_LEVEL))
            {
                hasPermission = true;
            }
            else
            {
                // 非OP玩家需要检查是否是网络成员
                hasPermission = net.getPlayers().contains(player.getUUID());
            }
        }

        if (!hasPermission)
        {
            source.sendFailure(CommandLang.component("network.open.error.no_permission", player.getGameProfile().getName(), netId));
            return 0;
        }

        try {
            switch (menuType) {
                case NET_MENU:
                    player.openMenu(new SimpleMenuProvider(
                            (containerId, playerInventory, _player) -> new DimensionsNetMenu(
                                    BDMenus.Dimensions_Net_Menu.get(),
                                    containerId,
                                    playerInventory,
                                    net.getUnifiedStorage()
                            ),
                            Component.translatable("menu.title.beyonddimensions.dimensionnetmenu")
                    ));
                    break;

                case NET_CRAFT_MENU:
                    player.openMenu(new SimpleMenuProvider(
                            (containerId, playerInventory, _player) -> new DimensionsCraftMenu(
                                    BDMenus.Dimensions_Craft_Menu.get(),
                                    containerId,
                                    playerInventory,
                                    net.getUnifiedStorage(),
                                    null,
                                    null
                            ),
                            Component.translatable("menu.title.beyonddimensions.dimensionnetmenu")
                    ));
                    break;

                case NET_CRAFT_TERMINAL:
                    // 终端需要手持网络终端物品，这里简化处理，直接打开普通网络界面
                    player.openMenu(new SimpleMenuProvider(
                            (containerId, playerInventory, _player) -> new DimensionsNetMenu(
                                    BDMenus.Dimensions_Net_Menu.get(),
                                    containerId,
                                    playerInventory,
                                    net.getUnifiedStorage()
                            ),
                            Component.translatable("menu.title.beyonddimensions.dimensionnetmenu")
                    ));
                    break;
            }

            String menuTypeName = getMenuTypeName(menuType);
            // 根据命令类型决定是否广播：OP命令不广播，普通玩家命令也不广播（保持一致性）
            boolean broadcastToAll = false; // 所有命令都不广播给其他玩家
            source.sendSuccess(
                    () -> CommandLang.component("network.open.success", player.getGameProfile().getName(), netId, menuTypeName),
                    broadcastToAll
            );
            return Command.SINGLE_SUCCESS;

        } catch (Exception e) {
            source.sendFailure(CommandLang.component("network.open.error.general", e.getMessage()));
            return 0;
        }
    }

    private static String getMenuTypeName(NetMenuType menuType) {
        switch (menuType) {
            case NET_MENU: return CommandLang.get("network.open.menu.storage");
            case NET_CRAFT_MENU: return CommandLang.get("network.open.menu.crafting");
            case NET_CRAFT_TERMINAL: return CommandLang.get("network.open.menu.terminal");
            default: return "Unknown";
        }
    }

    // 批量添加玩家到网络（成员或管理员）
    private static int batchAddPlayers(CommandSourceStack source, int netId, Collection<ServerPlayer> players, boolean asManagers) {
        ServerPlayer executor = source.getPlayer();
        if (executor == null) {
            source.sendFailure(CommandLang.component("network.open.error.player_required"));
            return 0;
        }

        DimensionsNet net = DimensionsNet.getNetFromId(netId);
        if (net == null) {
            source.sendFailure(CommandLang.component("network.open.error.not_exist", netId));
            return 0;
        }

        // 检查执行者权限（OP玩家跳过检查）
        boolean hasPermission = true;
        if (!source.hasPermission(OP_LEVEL)) {
            if (asManagers) {
                // 添加管理员需要所有者权限
                hasPermission = net.isOwner(executor);
            } else {
                // 添加普通成员需要所有者或管理员权限
                hasPermission = net.isOwner(executor) || net.isManager(executor);
            }
        }

        if (!hasPermission) {
            if (asManagers) {
                source.sendFailure(CommandLang.component("network.batchAdd.error.owner_required", netId));
            } else {
                source.sendFailure(CommandLang.component("network.batchAdd.error.owner_or_manager_required", netId));
            }
            return 0;
        }

        int addedCount = 0;
        List<String> addedPlayers = new ArrayList<>();
        List<String> alreadyInNetwork = new ArrayList<>();
        List<String> failedPlayers = new ArrayList<>();

        for (ServerPlayer player : players) {
            try {
                UUID playerUuid = player.getUUID();

                // 检查玩家是否已经在网络中
                if (net.getPlayers().contains(playerUuid)) {
                    alreadyInNetwork.add(player.getGameProfile().getName());
                    continue;
                }

                // 添加玩家到网络
                net.addPlayer(playerUuid);

                // 如果添加为管理员，同时添加到管理员列表
                if (asManagers) {
                    net.addManager(playerUuid);
                }

                addedCount++;
                addedPlayers.add(player.getGameProfile().getName());

            } catch (Exception e) {
                failedPlayers.add(player.getGameProfile().getName() + " (" + e.getMessage() + ")");
            }
        }

        // 标记网络为脏数据
        net.setDirty();

        // 构建结果消息
        StringBuilder result = new StringBuilder();
        String roleName = asManagers ? CommandLang.get("network.myNetworks.permission.manager") : CommandLang.get("network.myNetworks.permission.member");

        if (addedCount > 0) {
            result.append(CommandLang.get("network.batchAdd.success", addedCount, netId, roleName, String.join(", ", addedPlayers))).append("\n");
        }

        if (!alreadyInNetwork.isEmpty()) {
            result.append(CommandLang.get("network.batchAdd.already_in_network", String.join(", ", alreadyInNetwork))).append("\n");
        }

        if (!failedPlayers.isEmpty()) {
            result.append(CommandLang.get("network.batchAdd.failed", String.join(", ", failedPlayers)));
        }

        if (addedCount == 0 && alreadyInNetwork.isEmpty() && failedPlayers.isEmpty()) {
            result.append(CommandLang.get("network.batchAdd.no_players"));
        }

        source.sendSuccess(() -> Component.literal(result.toString()), false);
        return addedCount;
    }

    // 批量删除网络中的玩家
    private static int batchRemovePlayers(CommandSourceStack source, int netId, Collection<ServerPlayer> players) {
        ServerPlayer executor = source.getPlayer();
        if (executor == null) {
            source.sendFailure(CommandLang.component("network.open.error.player_required"));
            return 0;
        }

        DimensionsNet net = DimensionsNet.getNetFromId(netId);
        if (net == null) {
            source.sendFailure(CommandLang.component("network.open.error.not_exist", netId));
            return 0;
        }

        // 检查执行者权限（OP玩家跳过检查）
        boolean hasPermission = true;
        if (!source.hasPermission(OP_LEVEL)) {
            // 删除玩家需要所有者或管理员权限
            hasPermission = net.isOwner(executor) || net.isManager(executor);
        }

        if (!hasPermission) {
            source.sendFailure(CommandLang.component("network.batchAdd.error.owner_or_manager_required", netId));
            return 0;
        }

        int removedCount = 0;
        List<String> removedPlayers = new ArrayList<>();
        List<String> notInNetwork = new ArrayList<>();
        List<String> failedPlayers = new ArrayList<>();

        for (ServerPlayer player : players) {
            try {
                UUID playerUuid = player.getUUID();

                // 检查玩家是否在网络中
                if (!net.getPlayers().contains(playerUuid)) {
                    notInNetwork.add(player.getGameProfile().getName());
                    continue;
                }

                // 不能删除网络所有者
                if (net.isOwner(playerUuid)) {
                    failedPlayers.add(player.getGameProfile().getName() + " (不能删除网络所有者)");
                    continue;
                }

                // 从管理员列表中移除（如果是管理员）
                if (net.isManager(playerUuid)) {
                    net.removeManager(playerUuid);
                }

                // 从玩家列表中移除
                net.removePlayer(playerUuid);

                removedCount++;
                removedPlayers.add(player.getGameProfile().getName());

            } catch (Exception e) {
                failedPlayers.add(player.getGameProfile().getName() + " (" + e.getMessage() + ")");
            }
        }

        // 标记网络为脏数据
        net.setDirty();

        // 构建结果消息
        StringBuilder result = new StringBuilder();

        if (removedCount > 0) {
            result.append(CommandLang.get("network.batchRemove.success", removedCount, netId, String.join(", ", removedPlayers))).append("\n");
        }

        if (!notInNetwork.isEmpty()) {
            result.append(CommandLang.get("network.batchRemove.not_in_network", String.join(", ", notInNetwork))).append("\n");
        }

        if (!failedPlayers.isEmpty()) {
            result.append(CommandLang.get("network.batchRemove.failed", String.join(", ", failedPlayers)));
        }

        if (removedCount == 0 && notInNetwork.isEmpty() && failedPlayers.isEmpty()) {
            result.append(CommandLang.get("network.batchRemove.no_players"));
        }

        source.sendSuccess(() -> Component.literal(result.toString()), false);
        return removedCount;
    }

    // 列出玩家拥有权限的所有网络（带分页）
    private static int listPlayerNetworks(CommandSourceStack source, @Nullable ServerPlayer targetPlayer, int page) {
        ServerPlayer player = targetPlayer != null ? targetPlayer : source.getPlayer();
        if (player == null) {
            source.sendFailure(CommandLang.component("network.open.error.player_required"));
            return 0;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            source.sendFailure(CommandLang.component("error.server_not_available"));
            return 0;
        }

        // 如果是OP查看其他玩家的网络，或者查看自己的网络
        boolean isOp = source.hasPermission(OP_LEVEL);
        boolean isSelf = targetPlayer == null || targetPlayer == source.getPlayer();

        if (!isOp && !isSelf) {
            source.sendFailure(CommandLang.component("error.op_required_for_others"));
            return 0;
        }

        int maxPerPage = com.solr98.beyondcmdextension.CommandConfig.SERVER.maxNetworksPerPage.get();
        int startIndex = (page - 1) * maxPerPage;
        int endIndex = startIndex + maxPerPage;

        // 使用列表存储网络信息，以便排序
        List<NetworkInfo> allNetworks = new ArrayList<>();

        // 扫描所有网络（0-9999）
        for (int netId = 0; netId < 10000; netId++) {
            DimensionsNet net = server.overworld().getDataStorage().get(DimensionsNet::load, "BDNet_" + netId);
            if (net != null && !net.deleted) {
                // 检查玩家是否在网络中
                if (net.getPlayers().contains(player.getUUID())) {
                    // 获取玩家权限级别和排序权重
                    int permissionWeight;
                    String permissionLevel;
                    if (net.isOwner(player)) {
                        permissionWeight = 3; // 所有者最高优先级
                        permissionLevel = CommandLang.get("network.myNetworks.permission.owner");
                    } else if (net.isManager(player)) {
                        permissionWeight = 2; // 管理员中等优先级
                        permissionLevel = CommandLang.get("network.myNetworks.permission.manager");
                    } else {
                        permissionWeight = 1; // 成员最低优先级
                        permissionLevel = CommandLang.get("network.myNetworks.permission.member");
                    }

                    // 获取网络所有者名称
                    String ownerName = CommandLang.get("network.info.unknown");
                    if (net.getOwner() != null) {
                        String name = PlayerNameHelper.getPlayerNameByUUID(net.getOwner(), server);
                        if (name != null && !name.isEmpty()) {
                            ownerName = name;
                        }
                    }

                    // 统计网络信息
                    int playerCount = net.getPlayers().size();
                    int managerCount = net.getManagers().size();

                    allNetworks.add(new NetworkInfo(netId, permissionWeight, permissionLevel,
                            ownerName, playerCount, managerCount));
                }
            }
        }

        // 按权限级别降序排序（所有者 > 管理员 > 成员），相同权限按网络ID升序
        allNetworks.sort((a, b) -> {
            // 首先按权限权重降序排序
            int weightCompare = Integer.compare(b.permissionWeight, a.permissionWeight);
            if (weightCompare != 0) {
                return weightCompare;
            }
            // 相同权限按网络ID升序排序
            return Integer.compare(a.netId, b.netId);
        });

        int totalNetworks = allNetworks.size();
        int displayedCount = 0;
        List<String> networkInfos = new ArrayList<>();

        // 只显示当前页的数据
        for (int i = 0; i < totalNetworks; i++) {
            if (i >= startIndex && displayedCount < maxPerPage) {
                NetworkInfo info = allNetworks.get(i);
                networkInfos.add(CommandLang.get("network.myNetworks.info.format",
                        info.netId, info.permissionLevel, info.ownerName,
                        info.playerCount, info.managerCount));
                displayedCount++;
            }
        }

        // 构建输出消息
        StringBuilder message = new StringBuilder();
        String targetName = player.getGameProfile().getName();

        // 使用与list命令相似的格式：======== 标题 === 页码 ========
        String title;
        if (isSelf) {
            title = CommandLang.get("network.myNetworks.title.self");
        } else {
            title = CommandLang.get("network.myNetworks.title.other", targetName);
        }

        message.append("========= ").append(title).append(" ").append(CommandLang.get("network.list.page", page)).append(" =========\n");

        if (displayedCount > 0) {
            // 使用与list命令相似的格式显示网络信息
            for (String info : networkInfos) {
                message.append(info).append("\n");
            }

            // 添加分页导航
            int totalPages = (int) Math.ceil((double) totalNetworks / maxPerPage);

            MutableComponent navigation = Component.empty();

            if (page > 1) {
                navigation = navigation.append(
                        Component.literal("[" + CommandLang.get("network.list.previous") + "]")
                                .withStyle(Style.EMPTY
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                                "/bdtools myNetworks " + (page - 1) + (targetPlayer != null ? " " + targetName : "")))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                Component.literal("点击前往第 " + (page - 1) + " 页")))
                                        .withColor(net.minecraft.ChatFormatting.GREEN)
                                )
                ).append(Component.literal(" "));
            }

            navigation = navigation.append(
                    Component.literal("[" + CommandLang.get("network.list.page_with_total", page, totalPages, totalNetworks) + "]")
                            .withStyle(Style.EMPTY
                                    .withColor(net.minecraft.ChatFormatting.YELLOW)
                            )
            );

            if (page < totalPages) {
                navigation = navigation.append(Component.literal(" ")).append(
                        Component.literal("[" + CommandLang.get("network.list.next") + "]")
                                .withStyle(Style.EMPTY
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                                "/bdtools myNetworks " + (page + 1) + (targetPlayer != null ? " " + targetName : "")))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                Component.literal("点击前往第 " + (page + 1) + " 页")))
                                        .withColor(net.minecraft.ChatFormatting.GREEN)
                                )
                );
            }

            MutableComponent finalMessage = Component.literal(message.toString())
                    .append(Component.literal("\n"))
                    .append(navigation);

            source.sendSuccess(() -> finalMessage, false);
        } else {
            source.sendSuccess(() -> Component.literal(CommandLang.get("network.myNetworks.none")), false);
        }

        return totalNetworks;
    }

    // 显示玩家的默认网络信息或网络列表
    private static int showDefaultNetworkOrList(CommandSourceStack source, @Nullable ServerPlayer targetPlayer) {
        ServerPlayer player = targetPlayer != null ? targetPlayer : source.getPlayer();
        if (player == null) {
            source.sendFailure(CommandLang.component("network.open.error.player_required"));
            return 0;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            source.sendFailure(CommandLang.component("error.server_not_available"));
            return 0;
        }

        // 如果是OP查看其他玩家的网络，或者查看自己的网络
        boolean isOp = source.hasPermission(OP_LEVEL);
        boolean isSelf = targetPlayer == null || targetPlayer == source.getPlayer();

        if (!isOp && !isSelf) {
            source.sendFailure(CommandLang.component("error.op_required_for_others"));
            return 0;
        }

        // 优先使用玩家设定的主要网络
        DimensionsNet primaryNet = DimensionsNet.getPrimaryNetFromPlayer(player);
        if (primaryNet != null && !primaryNet.deleted && primaryNet.getPlayers().contains(player.getUUID())) {
            // 获取主要网络的ID
            int primaryNetId = -1;
            try {
                java.lang.reflect.Field idField = DimensionsNet.class.getDeclaredField("id");
                idField.setAccessible(true);
                primaryNetId = idField.getInt(primaryNet);
            } catch (Exception e) {
                // 如果无法通过反射获取ID，回退到扫描方式
                primaryNetId = -1;
            }

            if (primaryNetId != -1) {
                return showNetworkInfoForPlayer(source, primaryNetId, targetPlayer);
            }
        }

        // 如果没有主要网络或主要网络无效，按权限级别排序选择（所有者 > 管理员 > 成员）
        int defaultNetId = -1;
        int highestPermissionWeight = 0;
        int lowestNetIdForWeight = Integer.MAX_VALUE;

        for (int netId = 0; netId < 10000; netId++) {
            DimensionsNet net = server.overworld().getDataStorage().get(DimensionsNet::load, "BDNet_" + netId);
            if (net != null && !net.deleted && net.getPlayers().contains(player.getUUID())) {
                // 计算权限权重
                int permissionWeight;
                if (net.isOwner(player)) {
                    permissionWeight = 3; // 所有者
                } else if (net.isManager(player)) {
                    permissionWeight = 2; // 管理员
                } else {
                    permissionWeight = 1; // 成员
                }

                // 选择权限级别最高的网络，相同权限选择ID最小的
                if (permissionWeight > highestPermissionWeight ||
                        (permissionWeight == highestPermissionWeight && netId < lowestNetIdForWeight)) {
                    highestPermissionWeight = permissionWeight;
                    lowestNetIdForWeight = netId;
                    defaultNetId = netId;
                }
            }
        }

        if (defaultNetId != -1) {
            // 显示默认网络信息
            return showNetworkInfoForPlayer(source, defaultNetId, targetPlayer);
        } else {
            // 没有找到任何网络，显示空列表
            return listPlayerNetworks(source, targetPlayer, 1);
        }
    }

    // 为玩家显示网络详细信息（需要权限检查）
    private static int showNetworkInfoForPlayer(CommandSourceStack source, int netId, @Nullable ServerPlayer targetPlayer) {
        ServerPlayer player = targetPlayer != null ? targetPlayer : source.getPlayer();
        if (player == null) {
            source.sendFailure(CommandLang.component("network.open.error.player_required"));
            return 0;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            source.sendFailure(CommandLang.component("error.server_not_available"));
            return 0;
        }

        // 如果是OP查看其他玩家的网络，或者查看自己的网络
        boolean isOp = source.hasPermission(OP_LEVEL);
        boolean isSelf = targetPlayer == null || targetPlayer == source.getPlayer();

        if (!isOp && !isSelf) {
            source.sendFailure(CommandLang.component("error.op_required_for_others"));
            return 0;
        }

        DimensionsNet net = server.overworld().getDataStorage().get(DimensionsNet::load, "BDNet_" + netId);
        if (net == null) {
            source.sendFailure(CommandLang.component("network.info.not_exist", netId));
            return 0;
        }

        // 检查玩家是否有权限查看这个网络
        if (!net.getPlayers().contains(player.getUUID()) && !isOp) {
            source.sendFailure(CommandLang.component("network.open.error.no_permission", player.getGameProfile().getName(), netId));
            return 0;
        }

        // 获取玩家在这个网络中的权限级别
        String permissionLevel;
        if (net.isOwner(player)) {
            permissionLevel = CommandLang.get("network.myNetworks.permission.owner");
        } else if (net.isManager(player)) {
            permissionLevel = CommandLang.get("network.myNetworks.permission.manager");
        } else {
            permissionLevel = CommandLang.get("network.myNetworks.permission.member");
        }

        // 获取网络所有者名称
        String ownerName;
        if (net.getOwner() != null) {
            ownerName = PlayerNameHelper.getPlayerNameByUUID(net.getOwner(), server);
            if (ownerName == null || ownerName.isEmpty()) {
                ownerName = CommandLang.get("network.info.unknown");
            }
        } else {
            ownerName = CommandLang.get("network.info.unknown");
        }

        long slotCapacity = net.getUnifiedStorage().slotCapacity;
        int slotMaxSize = net.getUnifiedStorage().slotMaxSize;
        int currentTime = 0;
        try {
            java.lang.reflect.Field field = DimensionsNet.class.getDeclaredField("currentTime");
            field.setAccessible(true);
            currentTime = field.getInt(net);
        } catch (Exception e) {
            currentTime = -1;
        }

        int totalSlots = net.getUnifiedStorage().getSlots();

        // 统计不同类型的资源
        int itemTypesCount = 0;
        int fluidTypesCount = 0;
        int energyTypesCount = 0;
        java.math.BigInteger totalItems = java.math.BigInteger.ZERO;
        java.math.BigInteger totalFluids = java.math.BigInteger.ZERO;
        java.math.BigInteger totalEnergy = java.math.BigInteger.ZERO;

        for (com.wintercogs.beyonddimensions.api.storage.key.KeyAmount ka : net.getUnifiedStorage().getStorage()) {
            Object key = ka.key();
            long amount = ka.amount();

            if (key instanceof ItemStackKey) {
                itemTypesCount++;
                totalItems = totalItems.add(java.math.BigInteger.valueOf(amount));
            } else if (key instanceof FluidStackKey) {
                fluidTypesCount++;
                totalFluids = totalFluids.add(java.math.BigInteger.valueOf(amount));
            } else if (key instanceof EnergyStackKey) {
                energyTypesCount++;
                totalEnergy = totalEnergy.add(java.math.BigInteger.valueOf(amount));
            }
        }

        // 构建消息 - 使用带悬浮提示的组件
        MutableComponent message = Component.literal("========== 网络信息 (ID: " + netId + ") ==========\n")
                .append(createHoverableText("当前权限: " + permissionLevel + " | 所有者: " + ownerName + " | 状态: ", ""))
                .append(Component.literal(net.deleted ? "已删除" : "活跃")
                        .withStyle(net.deleted ? ChatFormatting.RED : ChatFormatting.GREEN))
                .append(Component.literal("\n"))
                .append(createHoverableText("结晶生成剩余时间: ", ""))
                .append(createHoverableTime(currentTime))
                .append(createHoverableText(" tick\n", ""))
                .append(createHoverableText("槽位容量: ", ""))
                .append(createHoverableNumber(slotCapacity, "槽位容量: "))
                .append(createHoverableText(" | 槽位数量: ", ""))
                .append(createHoverableNumber(slotMaxSize, "槽位数量: "))
                .append(Component.literal("\n"))
                .append(createHoverableText("存储统计:\n", ""))
                .append(createHoverableText("  物品: ", ""))
                .append(createHoverableResourceType(itemTypesCount, "物品"))
                .append(createHoverableText(" 种, 总量: ", ""))
                .append(createHoverableItemCount(totalItems))
                .append(Component.literal("\n"))
                .append(createHoverableText("  流体: ", ""))
                .append(createHoverableResourceType(fluidTypesCount, "流体"))
                .append(createHoverableText(" 种, 总量: ", ""))
                .append(createHoverableFluid(totalFluids))
                .append(createHoverableText(" mB\n", ""))
                .append(createHoverableText("  能量: ", ""))
                .append(createHoverableResourceType(energyTypesCount, "能量"))
                .append(createHoverableText(" 种, 总量: ", ""))
                .append(createHoverableEnergy(totalEnergy))
                .append(createHoverableText(" FE\n", ""))
                .append(createHoverableText("玩家数: ", ""))
                .append(createHoverableNumber(net.getPlayers().size(), "玩家数: "))
                .append(createHoverableText(" | 管理员: ", ""))
                .append(createHoverableNumber(net.getManagers().size(), "管理员数: "))
                .append(Component.literal("\n"));

        // 玩家列表
        UUID ownerUuid = net.getOwner();

        // 添加玩家列表标题
        message.append(createHoverableText(CommandLang.get("network.info.player_list_label"), ""));

        // 构建玩家列表组件
        boolean hasPlayers = false;
        boolean firstPlayer = true;

        // 添加所有者
        if (ownerUuid != null) {
            String ownerPlayerName = PlayerNameHelper.getPlayerNameByUUID(ownerUuid, server);
            if (ownerPlayerName != null && !ownerPlayerName.isEmpty()) {
                if (!firstPlayer) {
                    message.append(createHoverableText(", ", ""));
                }
                message.append(Component.literal(ownerPlayerName)
                        .withStyle(ChatFormatting.RED)); // 红色
                hasPlayers = true;
                firstPlayer = false;
            }
        }

        // 添加管理员
        for (UUID managerUuid : net.getManagers()) {
            if (ownerUuid != null && managerUuid.equals(ownerUuid)) continue;

            String managerName = PlayerNameHelper.getPlayerNameByUUID(managerUuid, server);
            if (managerName != null && !managerName.isEmpty()) {
                if (!firstPlayer) {
                    message.append(createHoverableText(", ", ""));
                }
                message.append(Component.literal(managerName)
                        .withStyle(ChatFormatting.BLUE)); // 蓝色
                hasPlayers = true;
                firstPlayer = false;
            }
        }

        // 添加普通成员
        for (UUID playerUuid : net.getPlayers()) {
            if (ownerUuid != null && playerUuid.equals(ownerUuid)) continue;
            if (net.getManagers().contains(playerUuid)) continue;

            String playerName = PlayerNameHelper.getPlayerNameByUUID(playerUuid, server);
            if (playerName != null && !playerName.isEmpty()) {
                if (!firstPlayer) {
                    message.append(createHoverableText(", ", ""));
                }
                message.append(Component.literal(playerName)
                        .withStyle(ChatFormatting.GREEN)); // 绿色
                hasPlayers = true;
                firstPlayer = false;
            }
        }

        if (!hasPlayers) {
            message.append(createHoverableText(CommandLang.get("network.info.no_players"), ""));
        }
        message.append(Component.literal("\n"));

        source.sendSuccess(() -> message, false);
        return 1;
    }

    // 创建带悬浮提示的文本组件
    private static MutableComponent createHoverableText(String text, String hoverText) {
        return Component.literal(text)
                .withStyle(Style.EMPTY
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.literal(hoverText)))
                        .withColor(net.minecraft.ChatFormatting.WHITE));
    }

    // 创建带悬浮提示的数字组件（显示格式化数字，悬浮显示完整数字）
    private static MutableComponent createHoverableNumber(long number, String prefix) {
        String formatted = formatNumber(number);
        String fullNumber = String.valueOf(number);
        return Component.literal(formatted)
                .withStyle(Style.EMPTY
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.literal(prefix + fullNumber)))
                        .withColor(net.minecraft.ChatFormatting.WHITE));
    }

    // 创建带悬浮提示的大数字组件
    private static MutableComponent createHoverableBigNumber(java.math.BigInteger number, String prefix) {
        String formatted = formatNumber(number);
        String fullNumber = number.toString();
        return Component.literal(formatted)
                .withStyle(Style.EMPTY
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.literal(prefix + fullNumber)))
                        .withColor(net.minecraft.ChatFormatting.WHITE));
    }

    // 创建带单位提示的时间组件（tick）
    private static MutableComponent createHoverableTime(long ticks) {
        String formatted = formatNumber(ticks);
        return Component.literal(formatted)
                .withStyle(Style.EMPTY
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.literal("结晶生成剩余时间: " + ticks + " tick")))
                        .withColor(net.minecraft.ChatFormatting.WHITE));
    }

    // 创建带单位提示的流体组件（mB）
    private static MutableComponent createHoverableFluid(java.math.BigInteger amount) {
        String formatted = formatNumber(amount);
        String fullAmount = amount.toString();
        return Component.literal(formatted)
                .withStyle(Style.EMPTY
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.literal("流体总量: " + fullAmount + " mB")))
                        .withColor(net.minecraft.ChatFormatting.WHITE));
    }

    // 创建带单位提示的能量组件（FE）
    private static MutableComponent createHoverableEnergy(java.math.BigInteger amount) {
        String formatted = formatNumber(amount);
        String fullAmount = amount.toString();
        return Component.literal(formatted)
                .withStyle(Style.EMPTY
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.literal("能量总量: " + fullAmount + " FE")))
                        .withColor(net.minecraft.ChatFormatting.WHITE));
    }

    // 创建带提示的物品数量组件
    private static MutableComponent createHoverableItemCount(java.math.BigInteger amount) {
        String formatted = formatNumber(amount);
        String fullAmount = amount.toString();
        return Component.literal(formatted)
                .withStyle(Style.EMPTY
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.literal("物品总量: " + fullAmount)))
                        .withColor(net.minecraft.ChatFormatting.WHITE));
    }

    // 创建带提示的资源种类组件
    private static MutableComponent createHoverableResourceType(int count, String resourceType) {
        String formatted = formatNumber(count);
        return Component.literal(formatted)
                .withStyle(Style.EMPTY
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.literal(resourceType + "种类: " + count + " 种")))
                        .withColor(net.minecraft.ChatFormatting.WHITE));
    }

    // 批量将玩家添加到多个网络
    private static int batchAddPlayerToNetworks(CommandSourceStack source, ServerPlayer player, String netIdsStr, boolean asManager) {
        if (player == null) {
            source.sendFailure(CommandLang.component("network.open.error.player_required"));
            return 0;
        }

        // 解析网络ID列表（支持逗号、空格分隔）
        String[] netIdParts = netIdsStr.split("[,\\s]+");
        List<Integer> netIds = new ArrayList<>();
        List<String> invalidNetIds = new ArrayList<>();

        for (String part : netIdParts) {
            part = part.trim();
            if (!part.isEmpty()) {
                try {
                    int netId = Integer.parseInt(part);
                    if (netId >= 0) {
                        netIds.add(netId);
                    } else {
                        invalidNetIds.add(part);
                    }
                } catch (NumberFormatException e) {
                    invalidNetIds.add(part);
                }
            }
        }

        if (netIds.isEmpty()) {
            source.sendFailure(CommandLang.component("network.batchAddPlayer.error.no_valid_networks"));
            return 0;
        }

        if (!invalidNetIds.isEmpty()) {
            source.sendFailure(CommandLang.component("network.batchAddPlayer.error.invalid_networks",
                    String.join(", ", invalidNetIds)));
            return 0;
        }

        ServerPlayer executor = source.getPlayer();
        if (executor == null) {
            source.sendFailure(CommandLang.component("network.open.error.player_required"));
            return 0;
        }

        boolean isOp = source.hasPermission(OP_LEVEL);
        String playerName = player.getGameProfile().getName();
        UUID playerUuid = player.getUUID();
        String roleName = asManager ? CommandLang.get("network.myNetworks.permission.manager") :
                CommandLang.get("network.myNetworks.permission.member");

        int addedCount = 0;
        List<String> addedNetworks = new ArrayList<>();
        List<String> alreadyInNetworks = new ArrayList<>();
        List<String> noPermissionNetworks = new ArrayList<>();
        List<String> notExistNetworks = new ArrayList<>();
        List<String> failedNetworks = new ArrayList<>();

        for (int netId : netIds) {
            try {
                DimensionsNet net = DimensionsNet.getNetFromId(netId);
                if (net == null) {
                    notExistNetworks.add(String.valueOf(netId));
                    continue;
                }

                // 检查玩家是否已经在网络中
                if (net.getPlayers().contains(playerUuid)) {
                    alreadyInNetworks.add(String.valueOf(netId));
                    continue;
                }

                // 检查执行者权限（OP玩家跳过检查）
                boolean hasPermission = true;
                if (!isOp) {
                    if (asManager) {
                        // 添加管理员需要所有者权限
                        hasPermission = net.isOwner(executor);
                    } else {
                        // 添加普通成员需要所有者或管理员权限
                        hasPermission = net.isOwner(executor) || net.isManager(executor);
                    }
                }

                if (!hasPermission) {
                    noPermissionNetworks.add(String.valueOf(netId));
                    continue;
                }

                // 添加玩家到网络
                net.addPlayer(playerUuid);

                // 如果添加为管理员，同时添加到管理员列表
                if (asManager) {
                    net.addManager(playerUuid);
                }

                // 标记网络为脏数据
                net.setDirty();

                addedCount++;
                addedNetworks.add(String.valueOf(netId));

            } catch (Exception e) {
                failedNetworks.add(netId + " (" + e.getMessage() + ")");
            }
        }

        // 构建结果消息
        StringBuilder result = new StringBuilder();

        if (addedCount > 0) {
            result.append(CommandLang.get("network.batchAddPlayer.success",
                    playerName, roleName, addedCount, String.join(", ", addedNetworks))).append("\n");
        }

        if (!alreadyInNetworks.isEmpty()) {
            result.append(CommandLang.get("network.batchAddPlayer.already_in_networks",
                    playerName, String.join(", ", alreadyInNetworks))).append("\n");
        }

        if (!noPermissionNetworks.isEmpty()) {
            result.append(CommandLang.get("network.batchAddPlayer.no_permission",
                    String.join(", ", noPermissionNetworks))).append("\n");
        }

        if (!notExistNetworks.isEmpty()) {
            result.append(CommandLang.get("network.batchAddPlayer.not_exist",
                    String.join(", ", notExistNetworks))).append("\n");
        }

        if (!failedNetworks.isEmpty()) {
            result.append(CommandLang.get("network.batchAddPlayer.failed",
                    String.join(", ", failedNetworks)));
        }

        if (addedCount == 0 && alreadyInNetworks.isEmpty() && noPermissionNetworks.isEmpty() &&
                notExistNetworks.isEmpty() && failedNetworks.isEmpty()) {
            result.append(CommandLang.get("network.batchAddPlayer.no_networks"));
        }

        source.sendSuccess(() -> Component.literal(result.toString()), false);
        return addedCount;
    }

    // 批量将玩家添加到多个网络（使用整数数组参数）
    private static int batchAddPlayerToNetworksSingle(CommandSourceStack source, ServerPlayer player, int[] netIds, boolean asManager) {
        if (player == null) {
            source.sendFailure(CommandLang.component("network.open.error.player_required"));
            return 0;
        }

        if (netIds == null || netIds.length == 0) {
            source.sendFailure(CommandLang.component("network.batchAddPlayer.error.no_valid_networks"));
            return 0;
        }

        ServerPlayer executor = source.getPlayer();
        if (executor == null) {
            source.sendFailure(CommandLang.component("network.open.error.player_required"));
            return 0;
        }

        boolean isOp = source.hasPermission(OP_LEVEL);
        String playerName = player.getGameProfile().getName();
        UUID playerUuid = player.getUUID();
        String roleName = asManager ? CommandLang.get("network.myNetworks.permission.manager") :
                CommandLang.get("network.myNetworks.permission.member");

        int addedCount = 0;
        List<String> addedNetworks = new ArrayList<>();
        List<String> alreadyInNetworks = new ArrayList<>();
        List<String> noPermissionNetworks = new ArrayList<>();
        List<String> notExistNetworks = new ArrayList<>();
        List<String> failedNetworks = new ArrayList<>();

        for (int netId : netIds) {
            try {
                DimensionsNet net = DimensionsNet.getNetFromId(netId);
                if (net == null) {
                    notExistNetworks.add(String.valueOf(netId));
                    continue;
                }

                // 检查玩家是否已经在网络中
                if (net.getPlayers().contains(playerUuid)) {
                    alreadyInNetworks.add(String.valueOf(netId));
                    continue;
                }

                // 检查执行者权限（OP玩家跳过检查）
                boolean hasPermission = true;
                if (!isOp) {
                    if (asManager) {
                        // 添加管理员需要所有者权限
                        hasPermission = net.isOwner(executor);
                    } else {
                        // 添加普通成员需要所有者或管理员权限
                        hasPermission = net.isOwner(executor) || net.isManager(executor);
                    }
                }

                if (!hasPermission) {
                    noPermissionNetworks.add(String.valueOf(netId));
                    continue;
                }

                // 添加玩家到网络
                net.addPlayer(playerUuid);

                // 如果添加为管理员，同时添加到管理员列表
                if (asManager) {
                    net.addManager(playerUuid);
                }

                // 标记网络为脏数据
                net.setDirty();

                addedCount++;
                addedNetworks.add(String.valueOf(netId));

            } catch (Exception e) {
                failedNetworks.add(netId + " (" + e.getMessage() + ")");
            }
        }

        // 构建结果消息
        StringBuilder result = new StringBuilder();

        if (addedCount > 0) {
            result.append(CommandLang.get("network.batchAddPlayer.success",
                    playerName, roleName, addedCount, String.join(", ", addedNetworks))).append("\n");
        }

        if (!alreadyInNetworks.isEmpty()) {
            result.append(CommandLang.get("network.batchAddPlayer.already_in_networks",
                    playerName, String.join(", ", alreadyInNetworks))).append("\n");
        }

        if (!noPermissionNetworks.isEmpty()) {
            result.append(CommandLang.get("network.batchAddPlayer.no_permission",
                    String.join(", ", noPermissionNetworks))).append("\n");
        }

        if (!notExistNetworks.isEmpty()) {
            result.append(CommandLang.get("network.batchAddPlayer.not_exist",
                    String.join(", ", notExistNetworks))).append("\n");
        }

        if (!failedNetworks.isEmpty()) {
            result.append(CommandLang.get("network.batchAddPlayer.failed",
                    String.join(", ", failedNetworks)));
        }

        if (addedCount == 0 && alreadyInNetworks.isEmpty() && noPermissionNetworks.isEmpty() &&
                notExistNetworks.isEmpty() && failedNetworks.isEmpty()) {
            result.append(CommandLang.get("network.batchAddPlayer.no_networks"));
        }

        source.sendSuccess(() -> Component.literal(result.toString()), false);
        return addedCount;
    }

    // 网络信息辅助类，用于排序
    private static class NetworkInfo {
        int netId;
        int permissionWeight; // 3=所有者, 2=管理员, 1=成员
        String permissionLevel;
        String ownerName;
        int playerCount;
        int managerCount;

        NetworkInfo(int netId, int permissionWeight, String permissionLevel,
                    String ownerName, int playerCount, int managerCount) {
            this.netId = netId;
            this.permissionWeight = permissionWeight;
            this.permissionLevel = permissionLevel;
            this.ownerName = ownerName;
            this.playerCount = playerCount;
            this.managerCount = managerCount;
        }
    }

    // 备份功能已移除（暂时不实现）
}