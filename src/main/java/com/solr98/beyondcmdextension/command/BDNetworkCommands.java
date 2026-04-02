package com.solr98.beyondcmdextension.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EnergyStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.FluidStackKey;
import com.wintercogs.beyonddimensions.util.PlayerNameHelper;
import com.wintercogs.beyonddimensions.client.gui.NetMenuType;
import com.wintercogs.beyonddimensions.common.init.BDMenus;
import com.wintercogs.beyonddimensions.common.menu.DimensionsCraftMenu;
import com.wintercogs.beyonddimensions.common.menu.DimensionsNetMenu;
import com.wintercogs.beyonddimensions.common.menu.NetControlMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.ResourceOrTagArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.SimpleMenuProvider;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;
import com.solr98.beyondcmdextension.Beyond_cmd_extension;
import com.solr98.beyondcmdextension.handler.NetworkTransferManager;
import com.solr98.beyondcmdextension.Config;

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
                                        .executes(ctx -> listNets(ctx.getSource(), 1, null))
                                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                                .executes(ctx -> listNets(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "page"), null))
                                        )
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> listNets(ctx.getSource(), 1, EntityArgument.getPlayer(ctx, "player")))
                                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> listNets(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "page"), EntityArgument.getPlayer(ctx, "player")))
                                                )
                                        )
                                )
                                .then(Commands.literal("info")
                                        .executes(ctx -> infoNet(ctx.getSource(), getPlayerNetIdOrFail(ctx.getSource())))
                                        .then(Commands.argument("netId", IntegerArgumentType.integer(0))
                                                .executes(ctx -> infoNet(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId")))
                                        )
                                )
                                .then(Commands.literal("insert")
                                        .then(Commands.literal("item")
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
                                        .then(Commands.literal("fluid")
                                                .then(Commands.argument("fluid", ResourceArgument.resource(context, Registries.FLUID))
                                                        .executes(ctx -> {
                                                            var fluidHolder = ResourceArgument.getResource(ctx, "fluid", Registries.FLUID);
                                                            return insertFluid(ctx.getSource(), getPlayerNetIdOrFail(ctx.getSource()), fluidHolder.value(), 1000L);
                                                        })
                                                        .then(Commands.argument("amount", LongArgumentType.longArg(1, Long.MAX_VALUE))
                                                                .executes(ctx -> {
                                                                    var fluidHolder = ResourceArgument.getResource(ctx, "fluid", Registries.FLUID);
                                                                    return insertFluid(ctx.getSource(), getPlayerNetIdOrFail(ctx.getSource()), fluidHolder.value(), LongArgumentType.getLong(ctx, "amount"));
                                                                })
                                                        )
                                                )
                                                .then(Commands.argument("netId", IntegerArgumentType.integer(0))
                                                        .then(Commands.argument("fluid", ResourceArgument.resource(context, Registries.FLUID))
                                                                .executes(ctx -> {
                                                                    var fluidHolder = ResourceArgument.getResource(ctx, "fluid", Registries.FLUID);
                                                                    return insertFluid(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), fluidHolder.value(), 1000L);
                                                                })
                                                                .then(Commands.argument("amount", LongArgumentType.longArg(1, Long.MAX_VALUE))
                                                                        .executes(ctx -> {
                                                                            var fluidHolder = ResourceArgument.getResource(ctx, "fluid", Registries.FLUID);
                                                                            return insertFluid(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), fluidHolder.value(), LongArgumentType.getLong(ctx, "amount"));
                                                                        })
                                                                )
                                                        )
                                                )
                                        )
                                        .then(Commands.literal("energy")
                                                .executes(ctx -> insertEnergy(ctx.getSource(), getPlayerNetIdOrFail(ctx.getSource()), 1000L))
                                                .then(Commands.argument("amount", LongArgumentType.longArg(1, Long.MAX_VALUE))
                                                        .executes(ctx -> insertEnergy(ctx.getSource(), getPlayerNetIdOrFail(ctx.getSource()), LongArgumentType.getLong(ctx, "amount")))
                                                )
                                                .then(Commands.argument("netId", IntegerArgumentType.integer(0))
                                                        .executes(ctx -> insertEnergy(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), 1000L))
                                                        .then(Commands.argument("amount", LongArgumentType.longArg(1, Long.MAX_VALUE))
                                                                .executes(ctx -> insertEnergy(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), LongArgumentType.getLong(ctx, "amount")))
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
                                 .then(Commands.literal("giveEnchantedBooks")
                                         .then(Commands.argument("player", EntityArgument.player())
                                                 .executes(ctx -> giveEnchantedBooks(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), 1, "random", 1, 3))
                                                 .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                         .executes(ctx -> giveEnchantedBooks(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "count"), "random", 1, 3))
                                                         .then(Commands.literal("random")
                                                                 .executes(ctx -> giveEnchantedBooks(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "count"), "random", 1, 3))
                                                                 .then(Commands.argument("minEnchants", IntegerArgumentType.integer(1))
                                                                         .executes(ctx -> giveEnchantedBooks(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "count"), "random", IntegerArgumentType.getInteger(ctx, "minEnchants"), 3))
                                                                         .then(Commands.argument("maxEnchants", IntegerArgumentType.integer(1))
                                                                                 .executes(ctx -> giveEnchantedBooks(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "count"), "random", IntegerArgumentType.getInteger(ctx, "minEnchants"), IntegerArgumentType.getInteger(ctx, "maxEnchants")))
                                                                         )
                                                                 )
                                                         )
                                                         .then(Commands.literal("all")
                                                                 .executes(ctx -> giveEnchantedBooks(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "count"), "all", 1, 1))
                                                         )
                                                 )
                                         )
                                 )
                                 .then(Commands.literal("batchCreate")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> batchCreateNets(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "count"), Long.MAX_VALUE, Integer.MAX_VALUE))
                                                        .then(Commands.argument("slotCapacity", LongArgumentType.longArg(1))
                                                                .executes(ctx -> batchCreateNets(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "count"), LongArgumentType.getLong(ctx, "slotCapacity"), Integer.MAX_VALUE))
                                                                .then(Commands.argument("slotMaxSize", IntegerArgumentType.integer(1))
                                                                        .executes(ctx -> batchCreateNets(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "count"), LongArgumentType.getLong(ctx, "slotCapacity"), IntegerArgumentType.getInteger(ctx, "slotMaxSize")))
                                                                )
                                                        )
                                                )
                                        )
                                )
                                // OP专用的open命令，可以打开任何网络
                                .then(Commands.literal("openAny")
                                        .executes(ctx -> openGui(ctx.getSource(), getPlayerNetIdOrFail(ctx.getSource()), NetMenuType.NET_MENU, null, false))
                                        .then(Commands.argument("netId", IntegerArgumentType.integer(0))
                                                .executes(ctx -> openGui(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), NetMenuType.NET_MENU, null, false))
                                                .then(Commands.literal("craft").executes(ctx -> {
                                                    ctx.getSource().sendFailure(CommandLang.component("error.crafting_disabled"));
                                                    return 0;
                                                }))
                                                .then(Commands.literal("terminal")
                                                        .executes(ctx -> openGui(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), NetMenuType.NET_CRAFT_TERMINAL, null, false))
                                                )
                                        )
                                        .then(Commands.literal("craft").executes(ctx -> {
                                            ctx.getSource().sendFailure(CommandLang.component("error.crafting_disabled"));
                                            return 0;
                                        }))
                                        .then(Commands.literal("terminal")
                                                .executes(ctx -> openGui(ctx.getSource(), getPlayerNetIdOrFail(ctx.getSource()), NetMenuType.NET_CRAFT_TERMINAL, null, false))
                                                .then(Commands.argument("netId", IntegerArgumentType.integer(0))
                                                        .executes(ctx -> openGui(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), NetMenuType.NET_CRAFT_TERMINAL, null, false))
                                                )
                                        )
                                        .then(Commands.literal("permission")
                                                .executes(ctx -> openPermissionControlGui(ctx.getSource(), getPlayerNetIdOrFail(ctx.getSource()), null, false))
                                        )
                                        .then(Commands.literal("control")
                                                .executes(ctx -> openPermissionControlGui(ctx.getSource(), getPlayerNetIdOrFail(ctx.getSource()), null, false))
                                          )
                                   )
                                    // 网络间物品传输（需要对方同意）- 根据配置启用
                                    .then(buildTransferCommand(context))
                             )
             );

        // 注册open命令，权限0可用，但需要检查网络权限
        event.getDispatcher().register(
                Commands.literal("bdtools")
                        .then(Commands.literal("open")
                                .executes(ctx -> openGui(ctx.getSource(), getPlayerNetIdOrFail(ctx.getSource()), NetMenuType.NET_MENU, null, true))
                                .then(Commands.argument("netId", IntegerArgumentType.integer(0))
                                        .executes(ctx -> openGui(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), NetMenuType.NET_MENU, null, true))
                                        .then(Commands.literal("craft").executes(ctx -> {
                                            ctx.getSource().sendFailure(CommandLang.component("error.crafting_disabled"));
                                            return 0;
                                        }))
                                        .then(Commands.literal("terminal")
                                                .executes(ctx -> openGui(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), NetMenuType.NET_CRAFT_TERMINAL, null, true))
                                        )
                                )
                                .then(Commands.literal("craft").executes(ctx -> {
                                    ctx.getSource().sendFailure(CommandLang.component("error.crafting_disabled"));
                                    return 0;
                                }))
                                .then(Commands.literal("terminal")
                                        .executes(ctx -> openGui(ctx.getSource(), getPlayerNetIdOrFail(ctx.getSource()), NetMenuType.NET_CRAFT_TERMINAL, null, true))
                                        .then(Commands.argument("netId", IntegerArgumentType.integer(0))
                                                .executes(ctx -> openGui(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), NetMenuType.NET_CRAFT_TERMINAL, null, true))
                                        )
                                )
                                .then(Commands.literal("permission")
                                        .executes(ctx -> openPermissionControlGui(ctx.getSource(), getPlayerNetIdOrFail(ctx.getSource()), null, true))
                                )
                                .then(Commands.literal("control")
                                        .executes(ctx -> openPermissionControlGui(ctx.getSource(), getPlayerNetIdOrFail(ctx.getSource()), null, true))
                                )
                        )
                         // 批量添加成员命令（普通玩家可用，需要权限检查）
                        .then(Commands.literal("addMembers")
                                // 向多个网络添加多个玩家（使用to参数区分）
                                .then(Commands.argument("players", EntityArgument.players())
                                        .then(Commands.literal("to")
                                                .then(Commands.argument("netId1", IntegerArgumentType.integer(0))
                                                        .executes(ctx -> batchAddPlayersToNetworks(ctx.getSource(),
                                                                EntityArgument.getPlayers(ctx, "players"),
                                                                new int[]{IntegerArgumentType.getInteger(ctx, "netId1")},
                                                                false))
                                                        .then(Commands.argument("netId2", IntegerArgumentType.integer(0))
                                                                .executes(ctx -> batchAddPlayersToNetworks(ctx.getSource(),
                                                                        EntityArgument.getPlayers(ctx, "players"),
                                                                        new int[]{IntegerArgumentType.getInteger(ctx, "netId1"),
                                                                                IntegerArgumentType.getInteger(ctx, "netId2")},
                                                                        false))
                                                                .then(Commands.argument("netId3", IntegerArgumentType.integer(0))
                                                                        .executes(ctx -> batchAddPlayersToNetworks(ctx.getSource(),
                                                                                EntityArgument.getPlayers(ctx, "players"),
                                                                                new int[]{IntegerArgumentType.getInteger(ctx, "netId1"),
                                                                                        IntegerArgumentType.getInteger(ctx, "netId2"),
                                                                                        IntegerArgumentType.getInteger(ctx, "netId3")},
                                                                                false))
                                                                        .then(Commands.argument("netId4", IntegerArgumentType.integer(0))
                                                                                .executes(ctx -> batchAddPlayersToNetworks(ctx.getSource(),
                                                                                        EntityArgument.getPlayers(ctx, "players"),
                                                                                        new int[]{IntegerArgumentType.getInteger(ctx, "netId1"),
                                                                                                IntegerArgumentType.getInteger(ctx, "netId2"),
                                                                                                IntegerArgumentType.getInteger(ctx, "netId3"),
                                                                                                IntegerArgumentType.getInteger(ctx, "netId4")},
                                                                                        false))
                                                                                .then(Commands.argument("netId5", IntegerArgumentType.integer(0))
                                                                                        .executes(ctx -> batchAddPlayersToNetworks(ctx.getSource(),
                                                                                                EntityArgument.getPlayers(ctx, "players"),
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
                        )
                        // 批量添加管理员命令（普通玩家可用，需要权限检查）
                        .then(Commands.literal("addManagers")
                                // 向多个网络添加多个玩家（使用to参数区分）
                                .then(Commands.argument("players", EntityArgument.players())
                                        .then(Commands.literal("to")
                                                .then(Commands.argument("netId1", IntegerArgumentType.integer(0))
                                                        .executes(ctx -> batchAddPlayersToNetworks(ctx.getSource(),
                                                                EntityArgument.getPlayers(ctx, "players"),
                                                                new int[]{IntegerArgumentType.getInteger(ctx, "netId1")},
                                                                true))
                                                        .then(Commands.argument("netId2", IntegerArgumentType.integer(0))
                                                                .executes(ctx -> batchAddPlayersToNetworks(ctx.getSource(),
                                                                        EntityArgument.getPlayers(ctx, "players"),
                                                                        new int[]{IntegerArgumentType.getInteger(ctx, "netId1"),
                                                                                IntegerArgumentType.getInteger(ctx, "netId2")},
                                                                        true))
                                                                .then(Commands.argument("netId3", IntegerArgumentType.integer(0))
                                                                        .executes(ctx -> batchAddPlayersToNetworks(ctx.getSource(),
                                                                                EntityArgument.getPlayers(ctx, "players"),
                                                                                new int[]{IntegerArgumentType.getInteger(ctx, "netId1"),
                                                                                        IntegerArgumentType.getInteger(ctx, "netId2"),
                                                                                        IntegerArgumentType.getInteger(ctx, "netId3")},
                                                                                true))
                                                                        .then(Commands.argument("netId4", IntegerArgumentType.integer(0))
                                                                                .executes(ctx -> batchAddPlayersToNetworks(ctx.getSource(),
                                                                                        EntityArgument.getPlayers(ctx, "players"),
                                                                                        new int[]{IntegerArgumentType.getInteger(ctx, "netId1"),
                                                                                                IntegerArgumentType.getInteger(ctx, "netId2"),
                                                                                                IntegerArgumentType.getInteger(ctx, "netId3"),
                                                                                                IntegerArgumentType.getInteger(ctx, "netId4")},
                                                                                        true))
                                                                                .then(Commands.argument("netId5", IntegerArgumentType.integer(0))
                                                                                        .executes(ctx -> batchAddPlayersToNetworks(ctx.getSource(),
                                                                                                EntityArgument.getPlayers(ctx, "players"),
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
                                // myNetworks list [页码] - 显示权限列表（只看执行者的网络）
                                .then(Commands.literal("list")
                                        .executes(ctx -> listPlayerNetworks(ctx.getSource(), null, 1))
                                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                                .executes(ctx -> listPlayerNetworks(ctx.getSource(), null, IntegerArgumentType.getInteger(ctx, "page")))
                                        )
                                )
                                 // myNetworks <netId> - 显示特定网络的详细信息（只看执行者的网络）
                                 .then(Commands.argument("netId", IntegerArgumentType.integer(0))
                                         .executes(ctx -> showNetworkInfoForPlayer(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "netId"), null))
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

    private static int listNets(CommandSourceStack source, int page, @Nullable ServerPlayer targetPlayer)
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
        {
            source.sendFailure(CommandLang.component("error.server_not_available"));
            return 0;
        }

        // 检查权限：只有OP可以查看其他玩家的网络
        boolean isOp = source.hasPermission(OP_LEVEL);
        if (targetPlayer != null && !isOp) {
            source.sendFailure(CommandLang.component("error.op_required_for_others"));
            return 0;
        }

        int maxPerPage = com.solr98.beyondcmdextension.CommandConfig.SERVER.maxNetworksPerPage.get();
        int startIndex = (page - 1) * maxPerPage;
        int endIndex = startIndex + maxPerPage;

        // 构建标题
        StringBuilder titleBuilder = new StringBuilder();
        if (targetPlayer != null) {
            titleBuilder.append(CommandLang.get("network.list.player_title", targetPlayer.getGameProfile().getName()));
        } else {
            titleBuilder.append(CommandLang.get("network.list.title"));
        }
        titleBuilder.append(CommandLang.get("network.list.page", page)).append(" ======");
        
        StringBuilder message = new StringBuilder().append(titleBuilder).append("\n");
        int count = 0;
        int totalCount = 0;
        int displayedCount = 0;

        for (int netId = 0; netId < 10000; netId++)
        {
            DimensionsNet net = server.overworld().getDataStorage().get(DimensionsNet::load, "BDNet_" + netId);
            if (net != null)
            {
                if (net.deleted) continue;
                
                // 如果指定了玩家，只显示该玩家所在的网络
                if (targetPlayer != null && !net.getPlayers().contains(targetPlayer.getUUID())) {
                    continue;
                }

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
                    
                    // 如果查看特定玩家的网络，显示玩家在该网络中的权限
                    String permissionInfo = "";
                    if (targetPlayer != null) {
                        String permissionLevel;
                        if (net.isOwner(targetPlayer)) {
                            permissionLevel = CommandLang.get("network.myNetworks.permission.owner");
                        } else if (net.isManager(targetPlayer)) {
                            permissionLevel = CommandLang.get("network.myNetworks.permission.manager");
                        } else {
                            permissionLevel = CommandLang.get("network.myNetworks.permission.member");
                        }
                        permissionInfo = " | " + CommandLang.get("network.list.permission") + ": " + permissionLevel;
                    }
                    
                    message.append("Net ID: ").append(netId)
                            .append(" | ").append(CommandLang.get("network.list.owner")).append(": ").append(ownerName)
                            .append(" | ").append(CommandLang.get("network.list.players")).append(": ").append(formatNumber(net.getPlayers().size()))
                            .append(" | ").append(CommandLang.get("network.list.managers")).append(": ").append(formatNumber(net.getManagers().size()))
                            .append(permissionInfo)
                            .append(net.deleted ? " | " + CommandLang.get("network.list.deleted_mark") + "\n" : "\n");
                    displayedCount++;
                }
                count++;
            }
        }

        if (displayedCount == 0)
        {
            source.sendSuccess(() -> Component.literal(CommandLang.get("network.list.none")), false);
        }
        else
        {
            int totalPages = (int) Math.ceil((double) totalCount / maxPerPage);

            MutableComponent navigation = Component.empty();

            if (page > 1)
            {
                String prevCommand = "/bdtools network list " + (page - 1);
                if (targetPlayer != null) prevCommand = "/bdtools network list " + targetPlayer.getGameProfile().getName() + " " + (page - 1);
                
                navigation = navigation.append(
                        Component.literal("[" + CommandLang.get("network.list.previous") + "]")
                                .withStyle(Style.EMPTY
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, prevCommand))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                Component.literal(CommandLang.get("pagination.click_to_page", page - 1))))
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
                String nextCommand = "/bdtools network list " + (page + 1);
                if (targetPlayer != null)  nextCommand = "/bdtools network list " + targetPlayer.getGameProfile().getName() + " " + (page + 1);
                
                navigation = navigation.append(Component.literal(" ")).append(
                        Component.literal("[" + CommandLang.get("network.list.next") + "]")
                                .withStyle(Style.EMPTY
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, nextCommand))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                Component.literal(CommandLang.get("pagination.click_to_page", page + 1))))
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
        int remainingTime = 0;
        try
        {
            // 获取结晶生成总时间（配置值 * 20转换为游戏刻）
            int crystalGenerateTime = com.wintercogs.beyonddimensions.config.ServerConfigRuntime.crystalGenerateTime;
            if (crystalGenerateTime <= 0) {
                remainingTime = -1; // 结晶生成已禁用
            } else {
                // 获取当前已过去的时间
                java.lang.reflect.Field field = DimensionsNet.class.getDeclaredField("currentTime");
                field.setAccessible(true);
                int elapsedTime = field.getInt(net);
                
                // 计算剩余时间：总时间 - 已过去时间
                int totalTime = crystalGenerateTime * 20;
                remainingTime = Math.max(0, totalTime - elapsedTime);
            }
        }
        catch (Exception e)
        {
            remainingTime = -1;
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
                .append(createHoverableTime(remainingTime))
                .append(Component.literal("\n"))
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

    private static int insertFluid(CommandSourceStack source, int netId, Fluid fluid, long amount)
    {
        DimensionsNet net = getNetOrFail(source, netId);
        if (net == null) return 0;

        FluidStack fluidStack = new FluidStack(fluid, 1);
        FluidStackKey key = new FluidStackKey(fluidStack);
        var remainder = net.getUnifiedStorage().insert(key, amount, false);

        long inserted = amount - remainder.amount();

        source.sendSuccess(
                () -> CommandLang.component("network.insert.fluid.success", inserted, BuiltInRegistries.FLUID.getKey(fluid).toString(), netId),
                false
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int insertEnergy(CommandSourceStack source, int netId, long amount)
    {
        DimensionsNet net = getNetOrFail(source, netId);
        if (net == null) return 0;

        var remainder = net.getUnifiedStorage().insert(EnergyStackKey.INSTANCE, amount, false);

        long inserted = amount - remainder.amount();

        source.sendSuccess(
                () -> CommandLang.component("network.insert.energy.success", inserted, netId),
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

        // 设置终端名称：所有者: xxx 的 x号网络
        String terminalName = CommandLang.get("network.giveTerminal.item_name", ownerName, netId);
        terminalStack.setHoverName(Component.literal(terminalName));

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
        ResourceGenerationResult result = new ResourceGenerationResult();
        
        switch (resourceType) {
            case "items":
                java.math.BigInteger[] itemTotalArr = new java.math.BigInteger[]{java.math.BigInteger.ZERO};
                result.itemTypes = generateItemsResources(source, net, typeCount, minAmount, maxAmount, withEnchantments, withNbt, random, itemTotalArr);
                result.itemTotal = itemTotalArr[0];
                break;
            case "fluids":
                java.math.BigInteger[] fluidTotalArr = new java.math.BigInteger[]{java.math.BigInteger.ZERO};
                result.fluidTypes = generateFluidsResources(source, net, typeCount, minAmount, maxAmount, random, fluidTotalArr);
                result.fluidTotal = fluidTotalArr[0];
                break;
            case "energy":
                java.math.BigInteger[] energyTotalArr = new java.math.BigInteger[]{java.math.BigInteger.ZERO};
                result.energyTypes = generateEnergyResources(source, net, typeCount, minAmount, maxAmount, random, energyTotalArr);
                result.energyTotal = energyTotalArr[0];
                break;
            case "mixed":
                // 对于mixed类型，我们分配类型数量给各种资源
                int itemCount = typeCount / 3;
                int fluidCount = typeCount / 3;
                int energyCount = typeCount - itemCount - fluidCount;
                
                if (itemCount > 0) {
                    java.math.BigInteger[] itemTotalArr2 = new java.math.BigInteger[]{java.math.BigInteger.ZERO};
                    result.itemTypes = generateItemsResources(source, net, itemCount, minAmount, maxAmount, withEnchantments, withNbt, random, itemTotalArr2);
                    result.itemTotal = itemTotalArr2[0];
                }
                if (fluidCount > 0) {
                    java.math.BigInteger[] fluidTotalArr2 = new java.math.BigInteger[]{java.math.BigInteger.ZERO};
                    result.fluidTypes = generateFluidsResources(source, net, fluidCount, minAmount, maxAmount, random, fluidTotalArr2);
                    result.fluidTotal = fluidTotalArr2[0];
                }
                if (energyCount > 0) {
                    java.math.BigInteger[] energyTotalArr2 = new java.math.BigInteger[]{java.math.BigInteger.ZERO};
                    result.energyTypes = generateEnergyResources(source, net, energyCount, minAmount, maxAmount, random, energyTotalArr2);
                    result.energyTotal = energyTotalArr2[0];
                }
                break;
            case "all":
                // 对于all类型，生成所有类型的资源
                java.math.BigInteger[] itemTotalArr3 = new java.math.BigInteger[]{java.math.BigInteger.ZERO};
                result.itemTypes = generateItemsResources(source, net, typeCount, minAmount, maxAmount, withEnchantments, withNbt, random, itemTotalArr3);
                result.itemTotal = itemTotalArr3[0];
                
                java.math.BigInteger[] fluidTotalArr3 = new java.math.BigInteger[]{java.math.BigInteger.ZERO};
                result.fluidTypes = generateFluidsResources(source, net, typeCount, minAmount, maxAmount, random, fluidTotalArr3);
                result.fluidTotal = fluidTotalArr3[0];
                
                java.math.BigInteger[] energyTotalArr3 = new java.math.BigInteger[]{java.math.BigInteger.ZERO};
                result.energyTypes = generateEnergyResources(source, net, typeCount, minAmount, maxAmount, random, energyTotalArr3);
                result.energyTotal = energyTotalArr3[0];
                break;
            default:
                source.sendFailure(CommandLang.component("error.invalid_resource_type", resourceType));
                return 0;
        }

        // 发送详细结果
        sendDetailedGenerationResult(source, netId, resourceType, result);
        return Command.SINGLE_SUCCESS;
    }

    private static int generateItemsResources(CommandSourceStack source, DimensionsNet net, int typeCount, int minAmount, int maxAmount, boolean withEnchantments, boolean withNbt, Random random, java.math.BigInteger[] totalInserted)
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
            totalInserted[0] = totalInserted[0].add(java.math.BigInteger.valueOf(amount - remainder.amount()));
        }

        return count;
    }

    private static int generateFluidsResources(CommandSourceStack source, DimensionsNet net, int typeCount, int minAmount, int maxAmount, Random random, java.math.BigInteger[] totalInserted)
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
            totalInserted[0] = totalInserted[0].add(java.math.BigInteger.valueOf(amount - remainder.amount()));
        }

        return count;
    }

    private static int generateEnergyResources(CommandSourceStack source, DimensionsNet net, int typeCount, int minAmount, int maxAmount, Random random, java.math.BigInteger[] totalInserted)
    {
        int count = Math.min(typeCount, 1); // 能量只有一种类型

        for (int i = 0; i < count; i++)
        {
            int amount = minAmount + random.nextInt(maxAmount - minAmount + 1);

            EnergyStackKey stack = EnergyStackKey.INSTANCE;
            var remainder = net.getUnifiedStorage().insert(stack, amount, false);
            totalInserted[0] = totalInserted[0].add(java.math.BigInteger.valueOf(amount - remainder.amount()));
        }

        return count;
    }

    private static int generateMixedResources(CommandSourceStack source, DimensionsNet net, int typeCount, int minAmount, int maxAmount, boolean withEnchantments, boolean withNbt, Random random, java.math.BigInteger[] totalInserted)
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
            totalInserted[0] = totalInserted[0].add(java.math.BigInteger.valueOf(amount - remainder.amount()));
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
                          totalInserted[0] = totalInserted[0].add(java.math.BigInteger.valueOf(amount - remainder.amount()));
                         actualCount++;
                    }
                    break;
                case 2: // 能量
                    int amount = minAmount + random.nextInt(maxAmount - minAmount + 1);

                     EnergyStackKey stack = EnergyStackKey.INSTANCE;
                     var remainder = net.getUnifiedStorage().insert(stack, amount, false);
                     totalInserted[0] = totalInserted[0].add(java.math.BigInteger.valueOf(amount - remainder.amount()));
                    actualCount++;
                    break;
            }
        }

        return actualCount;
    }

    private static int generateAllResources(CommandSourceStack source, DimensionsNet net, int typeCount, int minAmount, int maxAmount, boolean withEnchantments, boolean withNbt, Random random, java.math.BigInteger[] totalInserted)
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
                 totalInserted[0] = totalInserted[0].add(java.math.BigInteger.valueOf(amount - remainder.amount()));
                actualCount++;
            } else if (resource instanceof Fluid fluid) {
                 FluidStack fluidStack = new FluidStack(fluid, amount);
                 FluidStackKey stack = new FluidStackKey(fluidStack);
                 var remainder = net.getUnifiedStorage().insert(stack, amount, false);
                 totalInserted[0] = totalInserted[0].add(java.math.BigInteger.valueOf(amount - remainder.amount()));
                actualCount++;
            } else if (resource.equals("ENERGY")) {
                 EnergyStackKey stack = EnergyStackKey.INSTANCE;
                 var remainder = net.getUnifiedStorage().insert(stack, amount, false);
                 totalInserted[0] = totalInserted[0].add(java.math.BigInteger.valueOf(amount - remainder.amount()));
                actualCount++;
            }
        }

        return actualCount;
    }

    // 批量创建网络（使用官方API，为玩家创建网络并清除主网络）
    private static int batchCreateNets(CommandSourceStack source, ServerPlayer player, int count, long slotCapacity, int slotMaxSize)
    {
        if (player == null) {
            source.sendFailure(CommandLang.component("error.player_required"));
            return 0;
        }

        int createdCount = 0;
        List<Integer> createdNetIds = new java.util.ArrayList<>();

        for (int i = 0; i < count; i++)
        {
            try {
                // 使用官方API创建网络
                DimensionsNet newNet = DimensionsNet.createNewNetForPlayer(player, slotCapacity, slotMaxSize);
                
                if (newNet != null) {
                    // 获取创建的网络ID
                    int netId = newNet.getId();
                    
                    // 清除玩家的主网络（如果需要）
                    DimensionsNet.clearPrimaryNetForPlayer(player);
                    
                    createdCount++;
                    createdNetIds.add(netId);
                    
                    // 记录成功创建
                    final int createdIndex = i + 1;
                    final int createdNetId = netId;
                    source.sendSuccess(() -> CommandLang.component("network.create.success", createdIndex, createdNetId, player.getName().getString()), false);
                } else {
                    // 创建失败（可能玩家已经有网络）
                    final int failedIndex = i + 1;
                    source.sendSuccess(() -> CommandLang.component("network.create.warning.failed", failedIndex, player.getName().getString()), false);
                }
                
            } catch (Exception e) {
                // 创建单个网络失败，继续尝试创建其他网络
                final int networkIndex = i + 1;
                final String errorMsg = e.getMessage();
                source.sendSuccess(() -> CommandLang.component("network.create.warning.failed_with_error", networkIndex, player.getName().getString(), errorMsg), false);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(CommandLang.get("network.batchCreate.success", formatNumber(createdCount)));
        sb.append(" for player ").append(player.getName().getString());
        
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

    // 打开网络权限控制界面
    private static int openPermissionControlGui(CommandSourceStack source, int netId, ServerPlayer targetPlayer, boolean requirePermissionCheck) {
        ServerPlayer player = targetPlayer != null ? targetPlayer : source.getPlayer();
        if (player == null) {
            source.sendFailure(CommandLang.component("network.open.error.player_required"));
            return 0;
        }

        DimensionsNet net = DimensionsNet.getNetFromId(netId);
        if (net == null) {
            source.sendFailure(CommandLang.component("network.open.error.not_exist", netId));
            return 0;
        }

        // 检查玩家是否有权限访问该网络（权限控制界面需要所有者或管理员权限）
        boolean hasPermission = true;
        if (requirePermissionCheck) {
            // 如果玩家是OP2，则跳过权限检查
            if (source.hasPermission(OP_LEVEL)) {
                hasPermission = true;
            } else {
                // 非OP玩家需要检查是否是网络所有者或管理员
                hasPermission = net.isOwner(player) || net.isManager(player);
            }
        }

        if (!hasPermission) {
            source.sendFailure(CommandLang.component("network.open.error.no_permission_control", player.getGameProfile().getName(), netId));
            return 0;
        }

        // 检查指定的网络是否是玩家的主要网络
        DimensionsNet playerPrimaryNet = DimensionsNet.getNetFromPlayer(player);
        if (playerPrimaryNet == null) {
            source.sendFailure(CommandLang.component("network.open.error.no_primary_network", player.getGameProfile().getName()));
            return 0;
        }

        // 获取玩家主要网络的ID
        int playerPrimaryNetId = -1;
        try {
            java.lang.reflect.Field idField = DimensionsNet.class.getDeclaredField("id");
            idField.setAccessible(true);
            playerPrimaryNetId = idField.getInt(playerPrimaryNet);
        } catch (Exception e) {
            // 如果无法通过反射获取ID，回退到其他方式
            source.sendFailure(CommandLang.component("network.open.error.cannot_get_network_id"));
            return 0;
        }

        // 如果指定的网络不是玩家的主要网络，给出警告
        if (playerPrimaryNetId != netId) {
            source.sendFailure(CommandLang.component("network.open.error.not_primary_network", 
                    player.getGameProfile().getName(), netId, playerPrimaryNetId));
            return 0;
        }

        try {
            // 打开权限控制界面（注意：NetControlMenu总是打开玩家的主要网络）
            player.openMenu(new SimpleMenuProvider(
                    (containerId, playerInventory, _player) -> new NetControlMenu(containerId, playerInventory),
                    Component.translatable("menu.title.beyonddimensions.net_control_menu")
            ));

            // 根据命令类型决定是否广播：OP命令不广播，普通玩家命令也不广播（保持一致性）
            boolean broadcastToAll = false; // 所有命令都不广播给其他玩家
            source.sendSuccess(
                    () -> CommandLang.component("network.open.success", player.getGameProfile().getName(), netId, CommandLang.get("network.open.menu.permission")),
                    broadcastToAll
            );
            return Command.SINGLE_SUCCESS;

        } catch (Exception e) {
            source.sendFailure(CommandLang.component("network.open.error.general", e.getMessage()));
            return 0;
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

    // 批量添加多个玩家到多个网络
    private static int batchAddPlayersToNetworks(CommandSourceStack source, Collection<ServerPlayer> players, int[] netIds, boolean asManagers) {
        ServerPlayer executor = source.getPlayer();
        if (executor == null) {
            source.sendFailure(CommandLang.component("network.open.error.player_required"));
            return 0;
        }

        if (netIds == null || netIds.length == 0) {
            source.sendFailure(CommandLang.component("network.batchAddPlayer.error.no_valid_networks"));
            return 0;
        }

        if (players == null || players.isEmpty()) {
            source.sendFailure(CommandLang.component("network.batchAdd.no_players"));
            return 0;
        }

        boolean isOp = source.hasPermission(OP_LEVEL);
        String roleName = asManagers ? CommandLang.get("network.myNetworks.permission.manager") :
                CommandLang.get("network.myNetworks.permission.member");

        // 统计结果
        int totalAdded = 0;
        Map<Integer, List<String>> addedPlayersByNetwork = new HashMap<>();
        Map<Integer, List<String>> alreadyInNetworkByNetwork = new HashMap<>();
        Map<Integer, List<String>> noPermissionNetworks = new HashMap<>();
        List<String> notExistNetworks = new ArrayList<>();
        Map<Integer, List<String>> failedByNetwork = new HashMap<>();

        // 初始化映射
        for (int netId : netIds) {
            addedPlayersByNetwork.put(netId, new ArrayList<>());
            alreadyInNetworkByNetwork.put(netId, new ArrayList<>());
            failedByNetwork.put(netId, new ArrayList<>());
        }

        // 首先检查所有网络是否存在和权限
        for (int netId : netIds) {
            DimensionsNet net = DimensionsNet.getNetFromId(netId);
            if (net == null) {
                notExistNetworks.add(String.valueOf(netId));
                continue;
            }

            // 检查执行者权限（OP玩家跳过检查）
            boolean hasPermission = true;
            if (!isOp) {
                if (asManagers) {
                    // 添加管理员需要所有者权限
                    hasPermission = net.isOwner(executor);
                } else {
                    // 添加普通成员需要所有者或管理员权限
                    hasPermission = net.isOwner(executor) || net.isManager(executor);
                }
            }

            if (!hasPermission) {
                noPermissionNetworks.put(netId, new ArrayList<>());
            }
        }

        // 为每个玩家添加到每个网络
        for (ServerPlayer player : players) {
            String playerName = player.getGameProfile().getName();
            UUID playerUuid = player.getUUID();

            for (int netId : netIds) {
                // 跳过不存在的网络
                if (notExistNetworks.contains(String.valueOf(netId))) {
                    continue;
                }

                // 跳过没有权限的网络
                if (noPermissionNetworks.containsKey(netId)) {
                    noPermissionNetworks.get(netId).add(playerName);
                    continue;
                }

                DimensionsNet net = DimensionsNet.getNetFromId(netId);
                if (net == null) {
                    continue; // 应该不会发生，因为已经检查过了
                }

                try {
                    // 检查玩家是否已经在网络中
                    if (net.getPlayers().contains(playerUuid)) {
                        alreadyInNetworkByNetwork.get(netId).add(playerName);
                        continue;
                    }

                    // 添加玩家到网络
                    net.addPlayer(playerUuid);

                    // 如果添加为管理员，同时添加到管理员列表
                    if (asManagers) {
                        net.addManager(playerUuid);
                    }

                    // 标记网络为脏数据
                    net.setDirty();

                    totalAdded++;
                    addedPlayersByNetwork.get(netId).add(playerName);

                } catch (Exception e) {
                    failedByNetwork.get(netId).add(playerName + " (" + e.getMessage() + ")");
                }
            }
        }

        // 构建结果消息
        StringBuilder result = new StringBuilder();

        // 添加成功添加的统计
        if (totalAdded > 0) {
            result.append(CommandLang.get("network.batchAddToNetworks.success", totalAdded, roleName, netIds.length)).append("\n");
            
            // 显示每个网络的添加详情
            for (int netId : netIds) {
                List<String> addedPlayers = addedPlayersByNetwork.get(netId);
                if (!addedPlayers.isEmpty()) {
                    result.append("  网络 ").append(netId).append(": ").append(String.join(", ", addedPlayers)).append("\n");
                }
            }
        }

        // 添加已存在玩家的统计
        boolean hasAlreadyInNetwork = false;
        for (int netId : netIds) {
            List<String> alreadyInNetwork = alreadyInNetworkByNetwork.get(netId);
            if (!alreadyInNetwork.isEmpty()) {
                if (!hasAlreadyInNetwork) {
                    result.append(CommandLang.get("network.batchAddToNetworks.already_in_network")).append("\n");
                    hasAlreadyInNetwork = true;
                }
                result.append("  网络 ").append(netId).append(": ").append(String.join(", ", alreadyInNetwork)).append("\n");
            }
        }

        // 添加无权限网络的统计
        if (!noPermissionNetworks.isEmpty()) {
            result.append(CommandLang.get("network.batchAddToNetworks.no_permission")).append("\n");
            for (Map.Entry<Integer, List<String>> entry : noPermissionNetworks.entrySet()) {
                result.append("  网络 ").append(entry.getKey()).append(": ").append(String.join(", ", entry.getValue())).append("\n");
            }
        }

        // 添加不存在的网络
        if (!notExistNetworks.isEmpty()) {
            result.append(CommandLang.get("network.batchAddPlayer.not_exist",
                    String.join(", ", notExistNetworks))).append("\n");
        }

        // 添加失败的统计
        boolean hasFailed = false;
        for (int netId : netIds) {
            List<String> failed = failedByNetwork.get(netId);
            if (!failed.isEmpty()) {
                if (!hasFailed) {
                    result.append(CommandLang.get("network.batchAddToNetworks.failed")).append("\n");
                    hasFailed = true;
                }
                result.append("  网络 ").append(netId).append(": ").append(String.join(", ", failed)).append("\n");
            }
        }

        if (totalAdded == 0 && !hasAlreadyInNetwork && noPermissionNetworks.isEmpty() && 
                notExistNetworks.isEmpty() && !hasFailed) {
            result.append(CommandLang.get("network.batchAdd.no_players"));
        }

        source.sendSuccess(() -> Component.literal(result.toString()), false);
        return totalAdded;
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
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(CommandLang.component("network.open.error.player_required"));
            return 0;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            source.sendFailure(CommandLang.component("error.server_not_available"));
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
        String title = CommandLang.get("network.myNetworks.title.self");
        message.append("==== ").append(title).append(" ").append(CommandLang.get("network.list.page", page)).append("====\n");

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
                                                "/bdtools myNetworks list " + (page - 1) ))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                Component.literal(CommandLang.get("pagination.click_to_page", page - 1))))
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
                                                "/bdtools myNetworks list " + (page + 1)))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                Component.literal(CommandLang.get("pagination.click_to_page", page + 1))))
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
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(CommandLang.component("network.open.error.player_required"));
            return 0;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            source.sendFailure(CommandLang.component("error.server_not_available"));
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
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(CommandLang.component("network.open.error.player_required"));
            return 0;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            source.sendFailure(CommandLang.component("error.server_not_available"));
            return 0;
        }

        DimensionsNet net = server.overworld().getDataStorage().get(DimensionsNet::load, "BDNet_" + netId);
        if (net == null) {
            source.sendFailure(CommandLang.component("network.info.not_exist", netId));
            return 0;
        }

        // 检查玩家是否有权限查看这个网络
        if (!net.getPlayers().contains(player.getUUID())) {
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
        int remainingTime = 0;
        try {
            // 获取结晶生成总时间（配置值 * 20转换为游戏刻）
            int crystalGenerateTime = com.wintercogs.beyonddimensions.config.ServerConfigRuntime.crystalGenerateTime;
            if (crystalGenerateTime <= 0) {
                remainingTime = -1; // 结晶生成已禁用
            } else {
                // 获取当前已过去的时间
                java.lang.reflect.Field field = DimensionsNet.class.getDeclaredField("currentTime");
                field.setAccessible(true);
                int elapsedTime = field.getInt(net);
                
                // 计算剩余时间：总时间 - 已过去时间
                int totalTime = crystalGenerateTime * 20;
                remainingTime = Math.max(0, totalTime - elapsedTime);
            }
        } catch (Exception e) {
            remainingTime = -1;
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
        MutableComponent message = Component.literal(CommandLang.get("network.info.title", netId) + "\n")
                .append(createHoverableText(CommandLang.get("network.info.owner_label", ownerName), ""))
                .append(Component.literal(net.deleted ? CommandLang.get("network.info.status.deleted") : CommandLang.get("network.info.status.active"))
                        .withStyle(net.deleted ? ChatFormatting.RED : ChatFormatting.GREEN))
                .append(Component.literal("\n"))
                .append(createHoverableText(CommandLang.get("network.info.crystal_time"), ""))
                .append(createHoverableTime(remainingTime))
                .append(Component.literal("\n"))
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

        source.sendSuccess(() -> message, false);
        return 1;
    }
    
    /**
     * 请求能量传输（需要目标网络所有者同意）
     */
    private static int requestEnergyTransfer(CommandSourceStack source, int sourceNetId, int targetNetId, String energyType, long amount) {
        // 检查参数有效性
        if (sourceNetId == targetNetId) {
            source.sendFailure(CommandLang.component("network.transfer.same_network"));
            return 0;
        }
        
        if (amount <= 0) {
            source.sendFailure(CommandLang.component("error.amount_must_be_positive"));
            return 0;
        }
        
        // 获取源网络
        DimensionsNet sourceNet = getNetOrFail(source, sourceNetId);
        if (sourceNet == null) {
            source.sendFailure(CommandLang.component("network.transfer.invalid_network", sourceNetId));
            return 0;
        }
        
        // 获取目标网络
        DimensionsNet targetNet = getNetOrFail(source, targetNetId);
        if (targetNet == null) {
            source.sendFailure(CommandLang.component("network.transfer.invalid_network", targetNetId));
            return 0;
        }
        
        // 检查权限：源网络
        ServerPlayer player = source.getPlayer();
        if (player != null) {
            // 如果玩家是OP2，则跳过权限检查
            if (!source.hasPermission(OP_LEVEL)) {
                // 非OP玩家需要检查是否是源网络成员
                if (!sourceNet.getPlayers().contains(player.getUUID())) {
                    source.sendFailure(CommandLang.component("network.transfer.permission_denied", sourceNetId));
                    return 0;
                }
            }
        } else {
            source.sendFailure(CommandLang.component("error.player_required"));
            return 0;
        }
        
        // 检查源网络是否有足够的能量
        EnergyStackKey energyKey = EnergyStackKey.INSTANCE;
        long available = getAvailableEnergyCount(sourceNet, energyKey);
        if (available < amount) {
            source.sendFailure(CommandLang.component("network.transfer.insufficient_items", amount, energyType, available));
            return 0;
        }
        
        // 检查目标网络存储空间
        KeyAmount currentStack = targetNet.getUnifiedStorage().getStackByKey(energyKey);
        long currentAmount = currentStack.amount();
        long slotCapacity = targetNet.getUnifiedStorage().getSlotCapacity(0);
        if (slotCapacity <= 0) {
            slotCapacity = Long.MAX_VALUE;
        }
        
        if (currentAmount + amount > slotCapacity) {
            source.sendFailure(CommandLang.component("network.transfer.insufficient_storage"));
            return 0;
        }
        
        // 查找目标网络的所有者（需要同意）
        ServerPlayer targetPlayer = findNetworkOwner(targetNet, source.getServer());
        if (targetPlayer == null) {
            source.sendFailure(CommandLang.component("error.target_owner_not_found"));
            return 0;
        }
        
        // 如果目标玩家就是请求者自己，直接执行能量传输
        if (targetPlayer.getUUID().equals(player.getUUID())) {
            return executeEnergyTransfer(source, sourceNet, targetNet, energyType, amount, sourceNetId, targetNetId);
        }
        
        // 创建能量传输请求
        NetworkTransferManager.TransferRequest request = NetworkTransferManager.createEnergyRequest(
                player, targetPlayer, sourceNetId, targetNetId, energyType, amount);
        
        // 通知请求者
        MutableComponent requesterMessage = Component.literal("")
                .append(CommandLang.component("network.transfer.request.sent", targetPlayer.getGameProfile().getName()))
                .append(Component.literal("\n"))
                .append(createCancelButton());
        source.sendSuccess(() -> requesterMessage, false);
        
        // 通知目标玩家
        MutableComponent energyDisplay = getEnergyDisplayComponent(energyType, amount);
        MutableComponent message = buildTransferRequestMessage(
                player.getGameProfile().getName(), 
                sourceNetId, 
                targetNetId, 
                energyDisplay, 
                true
        );
        targetPlayer.sendSystemMessage(message);
        
        return 1;
    }
    
    /**
     * 获取网络中能量的可用数量
     */
    private static long getAvailableEnergyCount(DimensionsNet net, EnergyStackKey key) {
        com.wintercogs.beyonddimensions.api.storage.key.KeyAmount stack = net.getUnifiedStorage().getStackByKey(key);
        return stack.amount();
    }
    
    /**
     * 执行能量传输
     */
    private static int executeEnergyTransfer(CommandSourceStack source, DimensionsNet sourceNet, DimensionsNet targetNet, 
                                          String energyType, long amount, int sourceNetId, int targetNetId) {
        EnergyStackKey energyKey = EnergyStackKey.INSTANCE;
        
        // 再次检查源网络能量是否足够
        long available = getAvailableEnergyCount(sourceNet, energyKey);
        if (available < amount) {
            source.sendFailure(CommandLang.component("network.transfer.insufficient_items", amount, energyType, available));
            return 0;
        }
        
        // 从源网络提取能量
        KeyAmount extracted = sourceNet.getUnifiedStorage().extract(energyKey, amount, false, false);
        if (extracted.amount() < amount) {
            source.sendFailure(CommandLang.component("error.extract_energy_failed"));
            // 尝试回滚
            sourceNet.getUnifiedStorage().insert(energyKey, extracted.amount(), false);
            return 0;
        }
        
        // 插入到目标网络
        KeyAmount remaining = targetNet.getUnifiedStorage().insert(energyKey, amount, false);
        
        if (remaining.amount() > 0) {
            // 部分插入失败，回滚
            source.sendFailure(CommandLang.component("network.transfer.insufficient_storage"));
            targetNet.getUnifiedStorage().extract(energyKey, amount - remaining.amount(), false, false);
            sourceNet.getUnifiedStorage().insert(energyKey, amount, false);
            return 0;
        }
        
        // 传输成功
        MutableComponent energyDisplay = getEnergyDisplayComponent(energyType, amount);
        MutableComponent successMessage = buildTransferSuccessMessage(sourceNetId, targetNetId, energyDisplay);
        source.sendSuccess(() -> successMessage, true);
        
        return 1;
    }
    
    /**
     * 获取网络中物品的可用数量
     */
    private static long getAvailableCount(DimensionsNet net, ItemStackKey key) {
        com.wintercogs.beyonddimensions.api.storage.key.KeyAmount stack = net.getUnifiedStorage().getStackByKey(key);
        return stack.amount();
    }
    
    /**
     * 获取物品名称
     */
    private static String getItemName(ItemStack stack) {
        if (stack.isEmpty()) {
            return "空";
        }
        
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id != null) {
            return id.toString();
        }
        
        return stack.getDisplayName().getString();
    }
    
    /**
     * 请求物品传输（需要目标网络所有者同意）
     */
    private static int requestItemTransfer(CommandSourceStack source, int sourceNetId, int targetNetId, ItemStack item, long amount) {
        // 检查参数有效性
        if (sourceNetId == targetNetId) {
            source.sendFailure(CommandLang.component("network.transfer.same_network"));
            return 0;
        }
        
        if (amount <= 0) {
            source.sendFailure(CommandLang.component("error.amount_must_be_positive"));
            return 0;
        }
        
        // 获取源网络
        DimensionsNet sourceNet = getNetOrFail(source, sourceNetId);
        if (sourceNet == null) {
            source.sendFailure(CommandLang.component("network.transfer.invalid_network", sourceNetId));
            return 0;
        }
        
        // 获取目标网络
        DimensionsNet targetNet = getNetOrFail(source, targetNetId);
        if (targetNet == null) {
            source.sendFailure(CommandLang.component("network.transfer.invalid_network", targetNetId));
            return 0;
        }
        
        // 检查权限：源网络
        ServerPlayer player = source.getPlayer();
        if (player != null) {
            // 如果玩家是OP2，则跳过权限检查
            if (!source.hasPermission(OP_LEVEL)) {
                // 非OP玩家需要检查是否是源网络成员
                if (!sourceNet.getPlayers().contains(player.getUUID())) {
                    source.sendFailure(CommandLang.component("network.transfer.permission_denied", sourceNetId));
                    return 0;
                }
            }
        } else {
            source.sendFailure(CommandLang.component("error.player_required"));
            return 0;
        }
        
        // 检查源网络是否有足够的物品
        ItemStackKey itemKey = new ItemStackKey(item.copyWithCount(1));
        long available = getAvailableCount(sourceNet, itemKey);
        if (available < amount) {
            source.sendFailure(CommandLang.component("network.transfer.insufficient_items", amount, getItemName(item), available));
            return 0;
        }
        
        // 检查目标网络存储空间
        KeyAmount currentStack = targetNet.getUnifiedStorage().getStackByKey(itemKey);
        long currentAmount = currentStack.amount();
        long slotCapacity = targetNet.getUnifiedStorage().getSlotCapacity(0);
        if (slotCapacity <= 0) {
            slotCapacity = Long.MAX_VALUE;
        }
        
        if (currentAmount + amount > slotCapacity) {
            source.sendFailure(CommandLang.component("network.transfer.insufficient_storage"));
            return 0;
        }
        
        // 查找目标网络的所有者（需要同意）
        ServerPlayer targetPlayer = findNetworkOwner(targetNet, source.getServer());
        if (targetPlayer == null) {
            source.sendFailure(CommandLang.component("error.target_owner_not_found"));
            return 0;
        }
        
        // 如果目标玩家就是请求者自己，直接执行传输
        if (targetPlayer.getUUID().equals(player.getUUID())) {
            return executeTransfer(source, sourceNet, targetNet, item, amount, sourceNetId, targetNetId);
        }
        
        // 创建传输请求
        NetworkTransferManager.TransferRequest request = NetworkTransferManager.createItemRequest(
                player, targetPlayer, sourceNetId, targetNetId, item, amount);
        
        // 通知请求者
        MutableComponent requesterMessage = Component.literal("")
                .append(CommandLang.component("network.transfer.request.sent", targetPlayer.getGameProfile().getName()))
                .append(Component.literal("\n"))
                .append(createCancelButton());
        source.sendSuccess(() -> requesterMessage, false);
        
        // 通知目标玩家
        MutableComponent itemDisplay = getItemDisplayComponent(item, amount);
        MutableComponent message = Component.literal("")
                .append(Component.literal(player.getGameProfile().getName())
                        .withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" 想要从网络 "))
                .append(Component.literal(String.valueOf(sourceNetId))
                        .withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" 传输 "))
                .append(itemDisplay)
                .append(Component.literal(" 到你的网络 "))
                .append(Component.literal(String.valueOf(targetNetId))
                        .withStyle(ChatFormatting.GREEN))
                .append(Component.literal("。\n"))
                .append(createAcceptButton())
                .append(Component.literal(" "))
                .append(createDenyButton());
        targetPlayer.sendSystemMessage(message);
        
        return 1;
    }
    
    /**
     * 请求流体传输（需要目标网络所有者同意）
     */
    private static int requestFluidTransfer(CommandSourceStack source, int sourceNetId, int targetNetId, net.minecraft.world.level.material.Fluid fluid, long amount) {
        // 检查参数有效性
        if (sourceNetId == targetNetId) {
            source.sendFailure(CommandLang.component("network.transfer.same_network"));
            return 0;
        }
        
        if (amount <= 0) {
            source.sendFailure(CommandLang.component("error.amount_must_be_positive"));
            return 0;
        }
        
        // 获取源网络
        DimensionsNet sourceNet = getNetOrFail(source, sourceNetId);
        if (sourceNet == null) {
            source.sendFailure(CommandLang.component("network.transfer.invalid_network", sourceNetId));
            return 0;
        }
        
        // 获取目标网络
        DimensionsNet targetNet = getNetOrFail(source, targetNetId);
        if (targetNet == null) {
            source.sendFailure(CommandLang.component("network.transfer.invalid_network", targetNetId));
            return 0;
        }
        
        // 检查权限：源网络
        ServerPlayer player = source.getPlayer();
        if (player != null) {
            // 如果玩家是OP2，则跳过权限检查
            if (!source.hasPermission(OP_LEVEL)) {
                // 非OP玩家需要检查是否是源网络成员
                if (!sourceNet.getPlayers().contains(player.getUUID())) {
                    source.sendFailure(CommandLang.component("network.transfer.permission_denied", sourceNetId));
                    return 0;
                }
            }
        } else {
            source.sendFailure(CommandLang.component("error.player_required"));
            return 0;
        }
        
        // 检查源网络是否有足够的流体
        net.minecraftforge.fluids.FluidStack fluidStack = new net.minecraftforge.fluids.FluidStack(fluid, 1);
        FluidStackKey fluidKey = new FluidStackKey(fluidStack);
        long available = getAvailableFluidCount(sourceNet, fluidKey);
        if (available < amount) {
            source.sendFailure(CommandLang.component("network.transfer.insufficient_items", amount, getFluidName(fluid), available));
            return 0;
        }
        
        // 检查目标网络存储空间
        KeyAmount currentStack = targetNet.getUnifiedStorage().getStackByKey(fluidKey);
        long currentAmount = currentStack.amount();
        long slotCapacity = targetNet.getUnifiedStorage().getSlotCapacity(0);
        if (slotCapacity <= 0) {
            slotCapacity = Long.MAX_VALUE;
        }
        
        if (currentAmount + amount > slotCapacity) {
            source.sendFailure(CommandLang.component("network.transfer.insufficient_storage"));
            return 0;
        }
        
        // 查找目标网络的所有者（需要同意）
        ServerPlayer targetPlayer = findNetworkOwner(targetNet, source.getServer());
        if (targetPlayer == null) {
            source.sendFailure(CommandLang.component("error.target_owner_not_found"));
            return 0;
        }
        
        // 如果目标玩家就是请求者自己，直接执行流体传输
        if (targetPlayer.getUUID().equals(player.getUUID())) {
            return executeFluidTransfer(source, sourceNet, targetNet, fluid, amount, sourceNetId, targetNetId);
        }
        
        // 创建流体传输请求
        NetworkTransferManager.TransferRequest request = NetworkTransferManager.createFluidRequest(
                player, targetPlayer, sourceNetId, targetNetId, fluidStack, amount);
        
        // 通知请求者
        MutableComponent requesterMessage = Component.literal("")
                .append(CommandLang.component("network.transfer.request.sent", targetPlayer.getGameProfile().getName()))
                .append(Component.literal("\n"))
                .append(createCancelButton());
        source.sendSuccess(() -> requesterMessage, false);
        
        // 通知目标玩家
        MutableComponent fluidDisplay = getFluidDisplayComponent(fluid, amount);
        MutableComponent message = Component.literal("")
                .append(Component.literal(player.getGameProfile().getName())
                        .withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" 想要从网络 "))
                .append(Component.literal(String.valueOf(sourceNetId))
                        .withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" 传输 "))
                .append(fluidDisplay)
                .append(Component.literal(" 到你的网络 "))
                .append(Component.literal(String.valueOf(targetNetId))
                        .withStyle(ChatFormatting.GREEN))
                .append(Component.literal("。\n"))
                .append(createAcceptButton())
                .append(Component.literal(" "))
                .append(createDenyButton());
        targetPlayer.sendSystemMessage(message);
        
        return 1;
    }
    
    /**
     * 获取网络中流体的可用数量
     */
    private static long getAvailableFluidCount(DimensionsNet net, FluidStackKey key) {
        com.wintercogs.beyonddimensions.api.storage.key.KeyAmount stack = net.getUnifiedStorage().getStackByKey(key);
        return stack.amount();
    }
    
    /**
     * 获取流体名称
     */
    private static String getFluidName(net.minecraft.world.level.material.Fluid fluid) {
        net.minecraft.resources.ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid);
        if (id != null) {
            return id.toString();
        }
        
        return fluid.getFluidType().getDescriptionId();
    }
    
    /**
     * 执行流体传输
     */
    private static int executeFluidTransfer(CommandSourceStack source, DimensionsNet sourceNet, DimensionsNet targetNet, 
                                          net.minecraft.world.level.material.Fluid fluid, long amount, int sourceNetId, int targetNetId) {
        net.minecraftforge.fluids.FluidStack fluidStack = new net.minecraftforge.fluids.FluidStack(fluid, 1);
        FluidStackKey fluidKey = new FluidStackKey(fluidStack);
        
        // 再次检查源网络流体是否足够
        long available = getAvailableFluidCount(sourceNet, fluidKey);
        if (available < amount) {
            source.sendFailure(CommandLang.component("network.transfer.insufficient_items", amount, getFluidName(fluid), available));
            return 0;
        }
        
        // 从源网络提取流体
        KeyAmount extracted = sourceNet.getUnifiedStorage().extract(fluidKey, amount, false, false);
        if (extracted.amount() < amount) {
            source.sendFailure(CommandLang.component("error.extract_fluid_failed"));
            // 尝试回滚
            sourceNet.getUnifiedStorage().insert(fluidKey, extracted.amount(), false);
            return 0;
        }
        
        // 插入到目标网络
        KeyAmount remaining = targetNet.getUnifiedStorage().insert(fluidKey, amount, false);
        
        if (remaining.amount() > 0) {
            // 部分插入失败，回滚
            source.sendFailure(CommandLang.component("network.transfer.insufficient_storage"));
            targetNet.getUnifiedStorage().extract(fluidKey, amount - remaining.amount(), false, false);
            sourceNet.getUnifiedStorage().insert(fluidKey, amount, false);
            return 0;
        }
        
        // 传输成功
        MutableComponent fluidDisplay = getFluidDisplayComponent(fluid, amount);
        MutableComponent successMessage = Component.literal("")
                .append(Component.literal("成功从网络 "))
                .append(Component.literal(String.valueOf(sourceNetId))
                        .withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" 传输 "))
                .append(fluidDisplay)
                .append(Component.literal(" 到网络 "))
                .append(Component.literal(String.valueOf(targetNetId))
                        .withStyle(ChatFormatting.GREEN));
        source.sendSuccess(() -> successMessage, true);
        
        return 1;
    }
    
    /**
     * 接受传输请求
     */
    private static int acceptTransferRequest(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(CommandLang.component("error.player_required"));
            return 0;
        }
        
        // 获取待处理请求
        NetworkTransferManager.TransferRequest request = NetworkTransferManager.getPendingRequest(player);
        if (request == null) {
            source.sendFailure(CommandLang.component("network.transfer.no_pending"));
            return 0;
        }
        
        // 接受请求
        if (!NetworkTransferManager.acceptRequest(player)) {
            source.sendFailure(CommandLang.component("error.accept_request_failed"));
            return 0;
        }
        
        source.sendSuccess(() -> CommandLang.component("network.transfer.accept.success"), false);
        
        // 执行传输
        return executeAcceptedTransfer(request);
    }
    
    /**
     * 拒绝传输请求
     */
    private static int denyTransferRequest(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(CommandLang.component("error.player_required"));
            return 0;
        }
        
        // 获取待处理请求
        NetworkTransferManager.TransferRequest request = NetworkTransferManager.getPendingRequest(player);
        if (request == null) {
            source.sendFailure(CommandLang.component("network.transfer.no_pending"));
            return 0;
        }
        
        // 拒绝请求
        if (!NetworkTransferManager.denyRequest(player)) {
            source.sendFailure(CommandLang.component("error.deny_request_failed"));
            return 0;
        }
        
        source.sendSuccess(() -> CommandLang.component("network.transfer.deny.success"), false);
        
        // 通知请求者
        if (request.getRequester().isAlive()) {
            request.getRequester().sendSystemMessage(
                    Component.literal(CommandLang.get("error.request_denied")).withStyle(net.minecraft.ChatFormatting.RED));
        }
        
        return 1;
    }
    
    /**
     * 取消传输请求
     */
    private static int cancelTransferRequest(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(CommandLang.component("error.player_required"));
            return 0;
        }
        
        // 取消请求
        if (!NetworkTransferManager.cancelRequest(player)) {
            source.sendFailure(CommandLang.component("error.no_pending_request"));
            return 0;
        }
        
        source.sendSuccess(() -> CommandLang.component("network.transfer.cancelled"), false);
        return 1;
    }
    
    /**
     * 执行已接受的传输
     */
    private static int executeAcceptedTransfer(NetworkTransferManager.TransferRequest request) {
        // 获取网络
        DimensionsNet sourceNet = getNetById(request.getRequester().server, request.getSourceNetId());
        DimensionsNet targetNet = getNetById(request.getRequester().server, request.getTargetNetId());
        
        if (sourceNet == null || targetNet == null) {
            // 通知请求者
            if (request.getRequester().isAlive()) {
                request.getRequester().sendSystemMessage(
                        Component.literal(CommandLang.get("error.transfer_failed_network")).withStyle(net.minecraft.ChatFormatting.RED));
            }
            NetworkTransferManager.completeRequest(request.getRequestId());
            return 0;
        }
        
        // 执行传输
        return executeTransfer(request.getRequester().createCommandSourceStack(), 
                sourceNet, targetNet, request.getItem(), request.getAmount(), 
                request.getSourceNetId(), request.getTargetNetId());
    }
    
    /**
     * 执行实际传输
     */
    private static int executeTransfer(CommandSourceStack source, DimensionsNet sourceNet, DimensionsNet targetNet, 
                                      ItemStack item, long amount, int sourceNetId, int targetNetId) {
        ItemStackKey itemKey = new ItemStackKey(item.copyWithCount(1));
        
        // 再次检查源网络物品是否足够
        long available = getAvailableCount(sourceNet, itemKey);
        if (available < amount) {
            source.sendFailure(CommandLang.component("network.transfer.insufficient_items", amount, getItemName(item), available));
            return 0;
        }
        
        // 从源网络提取物品
        KeyAmount extracted = sourceNet.getUnifiedStorage().extract(itemKey, amount, false, false);
        if (extracted.amount() < amount) {
            source.sendFailure(CommandLang.component("error.extract_item_failed"));
            // 尝试回滚
            sourceNet.getUnifiedStorage().insert(itemKey, extracted.amount(), false);
            return 0;
        }
        
        // 插入到目标网络
        KeyAmount remaining = targetNet.getUnifiedStorage().insert(itemKey, amount, false);
        
        if (remaining.amount() > 0) {
            // 部分插入失败，回滚
            source.sendFailure(CommandLang.component("network.transfer.insufficient_storage"));
            targetNet.getUnifiedStorage().extract(itemKey, amount - remaining.amount(), false, false);
            sourceNet.getUnifiedStorage().insert(itemKey, amount, false);
            return 0;
        }
        
        // 传输成功
        MutableComponent itemDisplay = getItemDisplayComponent(item, amount);
        MutableComponent successMessage = Component.literal("")
                .append(Component.literal("成功从网络 "))
                .append(Component.literal(String.valueOf(sourceNetId))
                        .withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" 传输 "))
                .append(itemDisplay)
                .append(Component.literal(" 到网络 "))
                .append(Component.literal(String.valueOf(targetNetId))
                        .withStyle(ChatFormatting.GREEN));
        source.sendSuccess(() -> successMessage, true);
        
        // 如果是请求-响应模式，完成请求
        NetworkTransferManager.completeRequest(null); // 这里需要传递请求ID，但在这个简单实现中我们直接完成
        
        return 1;
    }
    
    /**
     * 查找网络的所有者玩家
     */
    private static ServerPlayer findNetworkOwner(DimensionsNet net, MinecraftServer server) {
        UUID ownerId = net.getOwner();
        if (ownerId == null) {
            return null;
        }
        
        return server.getPlayerList().getPlayer(ownerId);
    }
    
    /**
     * 通过ID获取网络（不进行权限检查）
     */
    private static DimensionsNet getNetById(MinecraftServer server, int netId) {
        try {
            // 这里需要调用Beyond Dimensions的API来获取网络
            // 由于API限制，我们使用现有的getNetOrFail方法，但创建一个虚拟的CommandSourceStack
            CommandSourceStack dummySource = server.createCommandSourceStack()
                    .withPermission(OP_LEVEL) // 给予OP权限绕过检查
                    .withSuppressedOutput();
            
            return getNetOrFail(dummySource, netId);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 创建接受按钮组件
     */
    private static MutableComponent createAcceptButton() {
        return Component.literal(CommandLang.get("button.accept"))
                .withStyle(Style.EMPTY
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bdtools transfer accept"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                                Component.literal(CommandLang.get("button.hover.accept")).withStyle(ChatFormatting.GREEN)))
                        .withColor(ChatFormatting.GREEN)
                        .withBold(true));
    }
    
    /**
     * 创建拒绝按钮组件
     */
    private static MutableComponent createDenyButton() {
        return Component.literal(CommandLang.get("button.deny"))
                .withStyle(Style.EMPTY
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bdtools transfer deny"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                                Component.literal(CommandLang.get("button.hover.deny")).withStyle(ChatFormatting.RED)))
                        .withColor(ChatFormatting.RED)
                        .withBold(true));
    }
    
    /**
     * 创建取消按钮组件（给请求者）
     */
    private static MutableComponent createCancelButton() {
        return Component.literal(CommandLang.get("button.cancel"))
                .withStyle(Style.EMPTY
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bdtools transfer cancel"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                                Component.literal(CommandLang.get("button.hover.cancel")).withStyle(ChatFormatting.GRAY)))
                        .withColor(ChatFormatting.GRAY)
                        .withBold(true));
    }
    
    /**
     * 获取物品的显示组件（带数量和样式）
     */
    private static MutableComponent getItemDisplayComponent(ItemStack itemStack, long amount) {
        ItemStack displayStack = itemStack.copy();
        displayStack.setCount((int) Math.min(amount, Integer.MAX_VALUE));
        
        return Component.literal("")
                .append(displayStack.getDisplayName())
                .append(Component.literal(" x" + amount)
                        .withStyle(ChatFormatting.GRAY));
    }
    
    /**
     * 获取流体的显示组件（带容量和样式）
     */
    private static MutableComponent getFluidDisplayComponent(net.minecraft.world.level.material.Fluid fluid, long amount) {
        net.minecraftforge.fluids.FluidStack fluidStack = new net.minecraftforge.fluids.FluidStack(fluid, (int) Math.min(amount, Integer.MAX_VALUE));
        
        return Component.literal("")
                .append(Component.literal(fluidStack.getDisplayName().getString()))
                .append(Component.literal(" " + amount + "mB")
                        .withStyle(ChatFormatting.BLUE));
    }
    
    /**
     * 构建传输请求消息
     */
    private static MutableComponent buildTransferRequestMessage(String requesterName, int sourceNetId, int targetNetId, 
                                                                MutableComponent resourceDisplay, boolean isToYourNetwork) {
        MutableComponent message = Component.literal("")
                .append(Component.literal(requesterName)
                        .withStyle(ChatFormatting.YELLOW))
                .append(CommandLang.component("network.transfer.request.wants_to_transfer"))
                .append(Component.literal(String.valueOf(sourceNetId))
                        .withStyle(ChatFormatting.GREEN))
                .append(CommandLang.component("network.transfer.request.from_network"))
                .append(resourceDisplay);
        
        if (isToYourNetwork) {
            message = message.append(CommandLang.component("network.transfer.request.to_your_network"))
                    .append(Component.literal(String.valueOf(targetNetId))
                            .withStyle(ChatFormatting.GREEN));
        } else {
            message = message.append(CommandLang.component("network.transfer.request.to_network"))
                    .append(Component.literal(String.valueOf(targetNetId))
                            .withStyle(ChatFormatting.GREEN));
        }
        
        message = message.append(Component.literal("。\n"))
                .append(createAcceptButton())
                .append(Component.literal(" "))
                .append(createDenyButton());
        
        return message;
    }
    
    /**
     * 构建传输成功消息
     */
    private static MutableComponent buildTransferSuccessMessage(int sourceNetId, int targetNetId, MutableComponent resourceDisplay) {
        return Component.literal("")
                .append(CommandLang.component("network.transfer.success.from_network"))
                .append(Component.literal(String.valueOf(sourceNetId))
                        .withStyle(ChatFormatting.GREEN))
                .append(CommandLang.component("network.transfer.success.transferred"))
                .append(resourceDisplay)
                .append(CommandLang.component("network.transfer.request.to_network"))
                .append(Component.literal(String.valueOf(targetNetId))
                        .withStyle(ChatFormatting.GREEN))
                .append(Component.literal("。"));
    }
    
    /**
     * 获取能量的显示组件（带数量和样式）
     */
    private static MutableComponent getEnergyDisplayComponent(String energyType, long amount) {
        return Component.literal("")
                .append(Component.literal(energyType)
                        .withStyle(ChatFormatting.GOLD))
                .append(Component.literal(" " + amount + "FE")
                        .withStyle(ChatFormatting.YELLOW));
    }
    
    /**
     * 解析网络ID参数（支持整数或玩家选择）
     * 格式：@p（当前玩家）、@s（自己）、玩家名、整数ID
     */
    private static int parseNetworkIdParameter(CommandSourceStack source, String param) {
        if (param == null || param.isEmpty()) {
            source.sendFailure(CommandLang.component("error.network_id_required"));
            throw new IllegalArgumentException("网络ID参数不能为空");
        }
        
        try {
            // 检查是否是玩家选择器
            if (param.startsWith("@")) {
                ServerPlayer player = null;
                
                if (param.equals("@p") || param.equals("@s")) {
                    // 当前玩家或自己
                    player = source.getPlayer();
                    if (player == null) {
                        source.sendFailure(CommandLang.component("error.player_required_for_command"));
                        throw new IllegalArgumentException("此命令必须由玩家执行");
                    }
                } else if (param.startsWith("@")) {
                    // 其他选择器，暂时不支持
                    source.sendFailure(Component.literal("不支持的玩家选择器: " + param));
                    throw new IllegalArgumentException("不支持的玩家选择器: " + param);
                }
                
                if (player != null) {
                    // 获取玩家的主要网络ID
                    return getPlayerPrimaryNetworkId(player);
                }
                // 如果player为null，应该已经抛出了异常，但为了安全起见，这里也抛出异常
                source.sendFailure(Component.literal("无法解析玩家选择器: " + param));
                throw new IllegalArgumentException("无法解析玩家选择器: " + param);
            }
            
            // 检查是否是玩家名
            ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(param);
            if (player != null) {
                // 获取玩家的主要网络ID
                return getPlayerPrimaryNetworkId(player);
            }
            
            // 尝试解析为整数
            try {
                return Integer.parseInt(param);
            } catch (NumberFormatException e) {
                source.sendFailure(Component.literal("无效的网络ID或玩家名: " + param));
                throw new IllegalArgumentException("无效的网络ID或玩家名: " + param);
            }
        } catch (IllegalArgumentException e) {
            // 重新抛出异常，让命令处理器捕获
            throw e;
        }
    }
    
    /**
     * 获取玩家的主要网络ID
     */
    private static int getPlayerPrimaryNetworkId(ServerPlayer player) {
        // 使用Beyond Dimensions的API获取玩家的网络
        DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
        if (net == null) {
            throw new IllegalArgumentException("玩家 " + player.getGameProfile().getName() + " 不在任何网络中");
        }
        
        return net.getId();
    }
    
    /**
     * 构建传输命令（根据配置决定是否启用）
     */
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildTransferCommand(CommandBuildContext context) {
        com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> transferCommand = 
                Commands.literal("transfer");
        
        // 如果配置中启用了网络传输功能，添加完整的命令
        if (Config.enableNetworkTransfer) {
            return transferCommand
                    // 物品传输命令（支持网络ID或玩家选择）
                    .then(Commands.literal("item")
                            .then(Commands.argument("source", StringArgumentType.word())
                                    .then(Commands.argument("target", StringArgumentType.word())
                                            .then(Commands.argument("item", ItemArgument.item(context))
                                                    .executes(ctx -> {
                                                        int sourceNetId = parseNetworkIdParameter(ctx.getSource(), StringArgumentType.getString(ctx, "source"));
                                                        int targetNetId = parseNetworkIdParameter(ctx.getSource(), StringArgumentType.getString(ctx, "target"));
                                                        return requestItemTransfer(ctx.getSource(), 
                                                                sourceNetId,
                                                                targetNetId,
                                                                ItemArgument.getItem(ctx, "item").createItemStack(1, false), 1);
                                                    })
                                                    .then(Commands.argument("amount", LongArgumentType.longArg(1, Long.MAX_VALUE))
                                                            .executes(ctx -> {
                                                                int sourceNetId = parseNetworkIdParameter(ctx.getSource(), StringArgumentType.getString(ctx, "source"));
                                                                int targetNetId = parseNetworkIdParameter(ctx.getSource(), StringArgumentType.getString(ctx, "target"));
                                                                return requestItemTransfer(ctx.getSource(),
                                                                        sourceNetId,
                                                                        targetNetId,
                                                                        ItemArgument.getItem(ctx, "item").createItemStack(1, false),
                                                                        LongArgumentType.getLong(ctx, "amount"));
                                                            })
                                                    )
                                            )
                                    )
                            )
                    )
                    // 流体传输命令（支持网络ID或玩家选择）
                    .then(Commands.literal("fluid")
                            .then(Commands.argument("source", StringArgumentType.word())
                                    .then(Commands.argument("target", StringArgumentType.word())
                                             .then(Commands.argument("fluid", ResourceArgument.resource(context, Registries.FLUID))
                                                     .executes(ctx -> {
                                                         int sourceNetId = parseNetworkIdParameter(ctx.getSource(), StringArgumentType.getString(ctx, "source"));
                                                         int targetNetId = parseNetworkIdParameter(ctx.getSource(), StringArgumentType.getString(ctx, "target"));
                                                         var fluidHolder = ResourceArgument.getResource(ctx, "fluid", Registries.FLUID);
                                                         return requestFluidTransfer(ctx.getSource(),
                                                             sourceNetId,
                                                             targetNetId,
                                                             fluidHolder.value(), 1);
                                                     })
                                                     .then(Commands.argument("amount", LongArgumentType.longArg(1, Long.MAX_VALUE))
                                                             .executes(ctx -> {
                                                                 int sourceNetId = parseNetworkIdParameter(ctx.getSource(), StringArgumentType.getString(ctx, "source"));
                                                                 int targetNetId = parseNetworkIdParameter(ctx.getSource(), StringArgumentType.getString(ctx, "target"));
                                                                 var fluidHolder = ResourceArgument.getResource(ctx, "fluid", Registries.FLUID);
                                                                 return requestFluidTransfer(ctx.getSource(),
                                                                     sourceNetId,
                                                                     targetNetId,
                                                                     fluidHolder.value(),
                                                                     LongArgumentType.getLong(ctx, "amount"));
                                                             })
                                                     )
                                             )
                                    )
                            )
                    )
                    // 能量传输命令（简化版，使用字符串参数，默认类型为FE，支持网络ID或玩家选择）
                    .then(Commands.literal("energy")
                            .then(Commands.argument("source", StringArgumentType.word())
                                    .then(Commands.argument("target", StringArgumentType.word())
                                             .executes(ctx -> {
                                                 int sourceNetId = parseNetworkIdParameter(ctx.getSource(), StringArgumentType.getString(ctx, "source"));
                                                 int targetNetId = parseNetworkIdParameter(ctx.getSource(), StringArgumentType.getString(ctx, "target"));
                                                 return requestEnergyTransfer(ctx.getSource(),
                                                         sourceNetId,
                                                         targetNetId,
                                                         "FE", 1);
                                             })
                                             .then(Commands.argument("amount", LongArgumentType.longArg(1, Long.MAX_VALUE))
                                                     .executes(ctx -> {
                                                         int sourceNetId = parseNetworkIdParameter(ctx.getSource(), StringArgumentType.getString(ctx, "source"));
                                                         int targetNetId = parseNetworkIdParameter(ctx.getSource(), StringArgumentType.getString(ctx, "target"));
                                                         return requestEnergyTransfer(ctx.getSource(),
                                                                 sourceNetId,
                                                                 targetNetId,
                                                                 "FE",
                                                                 LongArgumentType.getLong(ctx, "amount"));
                                                     })
                                             )
                                             .then(Commands.argument("energyType", StringArgumentType.word())
                                                     .executes(ctx -> {
                                                         int sourceNetId = parseNetworkIdParameter(ctx.getSource(), StringArgumentType.getString(ctx, "source"));
                                                         int targetNetId = parseNetworkIdParameter(ctx.getSource(), StringArgumentType.getString(ctx, "target"));
                                                         return requestEnergyTransfer(ctx.getSource(),
                                                                 sourceNetId,
                                                                 targetNetId,
                                                                 StringArgumentType.getString(ctx, "energyType"), 1);
                                                     })
                                                     .then(Commands.argument("amount", LongArgumentType.longArg(1, Long.MAX_VALUE))
                                                             .executes(ctx -> {
                                                                 int sourceNetId = parseNetworkIdParameter(ctx.getSource(), StringArgumentType.getString(ctx, "source"));
                                                                 int targetNetId = parseNetworkIdParameter(ctx.getSource(), StringArgumentType.getString(ctx, "target"));
                                                                 return requestEnergyTransfer(ctx.getSource(),
                                                                         sourceNetId,
                                                                         targetNetId,
                                                                         StringArgumentType.getString(ctx, "energyType"),
                                                                         LongArgumentType.getLong(ctx, "amount"));
                                                             })
                                                     )
                                             )
                                    )
                            )
                    )
                    // 接受传输请求
                    .then(Commands.literal("accept")
                            .executes(ctx -> acceptTransferRequest(ctx.getSource()))
                    )
                    // 拒绝传输请求
                    .then(Commands.literal("deny")
                            .executes(ctx -> denyTransferRequest(ctx.getSource()))
                    )
                    // 取消传输请求
                    .then(Commands.literal("cancel")
                            .executes(ctx -> cancelTransferRequest(ctx.getSource()))
                    );
        } else {
            // 如果未启用，添加一个占位命令显示提示信息
            return transferCommand
                    .executes(ctx -> {
                        ctx.getSource().sendFailure(CommandLang.component("error.network_transfer_disabled"));
                        return 0;
                    })
                    .then(Commands.argument("sourceNetId", IntegerArgumentType.integer(0))
                            .executes(ctx -> {
                                ctx.getSource().sendFailure(CommandLang.component("error.network_transfer_disabled_simple"));
                                return 0;
                            })
                    );
        }
    }

    // 备份功能已移除（暂时不实现）
    
    /**
     * 创建可悬停文本组件
     */
    private static Component createHoverableText(String text, String hoverText) {
        return Component.literal(text).withStyle(style -> style
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hoverText)))
                .withColor(ChatFormatting.WHITE));
    }
    
    /**
     * 创建可悬停资源类型组件
     */
    private static Component createHoverableResourceType(int count, String resourceType) {
        ChatFormatting color = count > 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
        return Component.literal(String.valueOf(count)).withStyle(style -> style
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                    Component.literal(CommandLang.get("display.resource_type_count", resourceType, count))))
                .withColor(color));
    }
    
    /**
     * 创建可悬停物品数量组件
     */
    private static Component createHoverableItemCount(BigInteger count) {
        String displayText = formatBigNumber(count);
        return Component.literal(displayText).withStyle(style -> style
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                    Component.literal(CommandLang.get("display.total_items", count))))
                .withColor(ChatFormatting.YELLOW));
    }
    
    /**
     * 创建可悬停流体数量组件
     */
    private static Component createHoverableFluid(BigInteger amount) {
        String displayText = formatBigNumber(amount);
        return Component.literal(displayText).withStyle(style -> style
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                    Component.literal(CommandLang.get("display.fluid_total", amount))))
                .withColor(ChatFormatting.AQUA));
    }
    
    /**
     * 创建可悬停能量数量组件
     */
    private static Component createHoverableEnergy(BigInteger amount) {
        String displayText = formatBigNumber(amount);
        return Component.literal(displayText).withStyle(style -> style
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                    Component.literal(CommandLang.get("display.energy_total", amount))))
                .withColor(ChatFormatting.LIGHT_PURPLE));
    }
    
    /**
     * 创建可悬停数字组件
     */
    private static Component createHoverableNumber(int number, String description) {
        String formattedNumber = formatBigNumber(BigInteger.valueOf(number));
        return Component.literal(formattedNumber).withStyle(style -> style
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                    Component.literal(description + formattedNumber + " (" + number + ")")))
                .withColor(ChatFormatting.GOLD));
    }
    
    /**
     * 创建可悬停数字组件（支持long）
     */
    private static Component createHoverableNumber(long number, String description) {
        String formattedNumber = formatBigNumber(BigInteger.valueOf(number));
        return Component.literal(formattedNumber).withStyle(style -> style
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                    Component.literal(description + formattedNumber + " (" + number + ")")))
                .withColor(ChatFormatting.GOLD));
    }
    
    /**
     * 创建可悬停时间组件
     * @param ticks 剩余游戏刻数（-1表示结晶生成已禁用）
     */
    private static Component createHoverableTime(int ticks) {
        if (ticks < 0) {
            // 结晶生成已禁用
            return Component.literal(CommandLang.get("display.disabled")).withStyle(style -> style
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                        Component.literal(CommandLang.get("display.crystal_generation_disabled"))))
                    .withColor(ChatFormatting.GRAY));
        }
        
        int seconds = ticks / 20;
        int minutes = seconds / 60;
        int hours = minutes / 60;
        int days = hours / 24;
        
        // 格式化时间字符串
        String timeStr;
        if (days > 0) {
            timeStr = CommandLang.get("time.format.days_hours_minutes_seconds", 
                days, hours % 24, minutes % 60, seconds % 60);
        } else if (hours > 0) {
            timeStr = CommandLang.get("time.format.hours_minutes_seconds", 
                hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            timeStr = CommandLang.get("time.format.minutes_seconds", 
                minutes, seconds % 60);
        } else if (seconds > 0) {
            timeStr = CommandLang.get("time.format.seconds", seconds);
        } else {
            timeStr = CommandLang.get("time.format.less_than_second");
        }
        
        // 构建悬停文本
        String hoverText;
        if (ticks == 0) {
            hoverText = CommandLang.get("display.crystal_remaining_time");
        } else {
            hoverText = CommandLang.get("display.crystal_remaining_time", timeStr) + 
                       "\n" + CommandLang.get("display.crystal_time_tooltip") +
                       "\ntick: " + ticks;
        }
        
        // 根据剩余时间设置颜色
        ChatFormatting color;
        if (ticks == 0) {
            color = ChatFormatting.GOLD; // 即将生成，金色
        } else if (seconds < 30) {
            color = ChatFormatting.GREEN; // 少于30秒，绿色
        } else if (seconds < 300) {
            color = ChatFormatting.YELLOW; // 少于5分钟，黄色
        } else {
            color = ChatFormatting.AQUA; // 其他情况，青色
        }
        
        return Component.literal(timeStr).withStyle(style -> style
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                    Component.literal(hoverText)))
                .withColor(color));
    }
    
    /**
     * 格式化大数字为易读格式
     */
    private static String formatBigNumber(BigInteger number) {
        if (number.compareTo(BigInteger.valueOf(1_000_000_000L)) >= 0) {
            return number.divide(BigInteger.valueOf(1_000_000_000L)) + "B";
        } else if (number.compareTo(BigInteger.valueOf(1_000_000L)) >= 0) {
            return number.divide(BigInteger.valueOf(1_000_000L)) + "M";
        } else if (number.compareTo(BigInteger.valueOf(1_000L)) >= 0) {
            return number.divide(BigInteger.valueOf(1_000L)) + "K";
        } else {
            return number.toString();
        }
    }
    
    /**
     * 给予附魔书（测试/开发命令）
     */
    private static int giveEnchantedBooks(CommandSourceStack source, ServerPlayer player, int count, String type, int minEnchants, int maxEnchants) {
        source.sendFailure(CommandLang.component("error.enchantment_book_not_implemented"));
        return 0;
    }
    
    /**
     * 资源生成结果类
     */
    private static class ResourceGenerationResult {
        int itemTypes;
        int fluidTypes;
        int energyTypes;
        BigInteger itemTotal = BigInteger.ZERO;
        BigInteger fluidTotal = BigInteger.ZERO;
        BigInteger energyTotal = BigInteger.ZERO;
    }
    
    /**
     * 网络信息类（用于排序和显示）
     */
    private static class NetworkInfo {
        int netId;
        int permissionWeight;
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
    
    /**
     * 发送详细的生成结果
     */
    private static void sendDetailedGenerationResult(CommandSourceStack source, int netId, String resourceType, ResourceGenerationResult result) {
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(CommandLang.get("network.generate.result.title", netId, resourceType)).append("\n");
        
        if (result.itemTypes > 0) {
            messageBuilder.append(CommandLang.get("network.generate.result.items", result.itemTypes, result.itemTotal)).append("\n");
        }
        
        if (result.fluidTypes > 0) {
            messageBuilder.append(CommandLang.get("network.generate.result.fluids", result.fluidTypes, result.fluidTotal)).append("\n");
        }
        
        if (result.energyTypes > 0) {
            messageBuilder.append(CommandLang.get("network.generate.result.energy", result.energyTypes, result.energyTotal)).append("\n");
        }
        
        MutableComponent message = Component.literal(messageBuilder.toString())
                .withStyle(ChatFormatting.GOLD);
        
        source.sendSuccess(() -> message, false);
    }
}