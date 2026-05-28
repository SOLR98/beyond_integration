package com.solr98.beyondintegration.command.network;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.solr98.beyondintegration.command.CommandLang;
import com.solr98.beyondintegration.command.util.*;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EnergyStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.FluidStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

public class NetworkInsertCommand {

    public static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> register(CommandBuildContext context) {
        return Commands.literal("insert")
                .then(buildItemInsertCommand(context))
                .then(buildFluidInsertCommand(context))
                .then(buildEnergyInsertCommand());
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildItemInsertCommand(CommandBuildContext context) {
        return Commands.literal("item")
                .executes(ctx -> { ctx.getSource().sendFailure(CommandLang.component("error.item_required")); return 0; })
                .then(Commands.argument("item", ItemArgument.item(context))
                        .executes(ctx -> executeInsertItemDefault(ctx, ItemArgument.getItem(ctx, "item").createItemStack(1, false), 1))
                        .then(Commands.argument("count", LongArgumentType.longArg(1, Long.MAX_VALUE))
                                .executes(ctx -> executeInsertItemDefault(ctx, ItemArgument.getItem(ctx, "item").createItemStack(1, false), LongArgumentType.getLong(ctx, "count")))
                                .then(Commands.argument("netId", IntegerArgumentType.integer(0, 9999))
                                        .executes(ctx -> executeInsertItem(ctx, IntegerArgumentType.getInteger(ctx, "netId"), ItemArgument.getItem(ctx, "item").createItemStack(1, false), LongArgumentType.getLong(ctx, "count"))))))
                .then(Commands.argument("netId", IntegerArgumentType.integer(0, 9999))
                        .executes(ctx -> { ctx.getSource().sendFailure(CommandLang.component("error.item_required")); return 0; })
                        .then(Commands.argument("item", ItemArgument.item(context))
                                .executes(ctx -> executeInsertItem(ctx, IntegerArgumentType.getInteger(ctx, "netId"), ItemArgument.getItem(ctx, "item").createItemStack(1, false), 1))
                                .then(Commands.argument("count", LongArgumentType.longArg(1, Long.MAX_VALUE))
                                        .executes(ctx -> executeInsertItem(ctx, IntegerArgumentType.getInteger(ctx, "netId"), ItemArgument.getItem(ctx, "item").createItemStack(1, false), LongArgumentType.getLong(ctx, "count"))))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildFluidInsertCommand(CommandBuildContext context) {
        return Commands.literal("fluid")
                .executes(ctx -> { ctx.getSource().sendFailure(CommandLang.component("error.fluid_required")); return 0; })
                .then(Commands.argument("fluid", ResourceArgument.resource(context, Registries.FLUID))
                        .executes(ctx -> { var h = ResourceArgument.getResource(ctx, "fluid", Registries.FLUID); return executeInsertFluidDefault(ctx, h.value(), 1000L); })
                        .then(Commands.argument("amount", LongArgumentType.longArg(1, Long.MAX_VALUE))
                                .executes(ctx -> { var h = ResourceArgument.getResource(ctx, "fluid", Registries.FLUID); return executeInsertFluidDefault(ctx, h.value(), LongArgumentType.getLong(ctx, "amount")); })
                                .then(Commands.argument("netId", IntegerArgumentType.integer(0, 9999))
                                        .executes(ctx -> { var h = ResourceArgument.getResource(ctx, "fluid", Registries.FLUID); return executeInsertFluid(ctx, IntegerArgumentType.getInteger(ctx, "netId"), h.value(), LongArgumentType.getLong(ctx, "amount")); }))))
                .then(Commands.argument("netId", IntegerArgumentType.integer(0, 9999))
                        .executes(ctx -> { ctx.getSource().sendFailure(CommandLang.component("error.fluid_required")); return 0; })
                        .then(Commands.argument("fluid", ResourceArgument.resource(context, Registries.FLUID))
                                .executes(ctx -> { var h = ResourceArgument.getResource(ctx, "fluid", Registries.FLUID); return executeInsertFluid(ctx, IntegerArgumentType.getInteger(ctx, "netId"), h.value(), 1000L); })
                                .then(Commands.argument("amount", LongArgumentType.longArg(1, Long.MAX_VALUE))
                                        .executes(ctx -> { var h = ResourceArgument.getResource(ctx, "fluid", Registries.FLUID); return executeInsertFluid(ctx, IntegerArgumentType.getInteger(ctx, "netId"), h.value(), LongArgumentType.getLong(ctx, "amount")); }))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildEnergyInsertCommand() {
        return Commands.literal("energy")
                .executes(ctx -> executeInsertEnergyDefault(ctx, 1000L))
                .then(Commands.argument("amount", LongArgumentType.longArg(1, Long.MAX_VALUE))
                        .executes(ctx -> executeInsertEnergyDefault(ctx, LongArgumentType.getLong(ctx, "amount")))
                        .then(Commands.argument("netId", IntegerArgumentType.integer(0, 9999))
                                .executes(ctx -> executeInsertEnergy(ctx, IntegerArgumentType.getInteger(ctx, "netId"), LongArgumentType.getLong(ctx, "amount")))))
                .then(Commands.argument("netId", IntegerArgumentType.integer(0, 9999))
                        .executes(ctx -> executeInsertEnergy(ctx, IntegerArgumentType.getInteger(ctx, "netId"), 1000L))
                        .then(Commands.argument("amount", LongArgumentType.longArg(1, Long.MAX_VALUE))
                                .executes(ctx -> executeInsertEnergy(ctx, IntegerArgumentType.getInteger(ctx, "netId"), LongArgumentType.getLong(ctx, "amount")))));
    }

    // ========== Item ==========

    private static int executeInsertItemDefault(CommandContext<CommandSourceStack> ctx, ItemStack stack, long count) {
        CommandSourceStack source = ctx.getSource();
        if (!PermissionChecker.checkOpPermission(source)) return 0;
        ServerPlayer executor = source.getPlayer();
        if (executor == null) { source.sendFailure(OutputFormatter.createError("error.player_required")); return 0; }
        DimensionsNet primaryNet = DimensionsNet.getPrimaryNetFromPlayer(executor);
        if (primaryNet == null) { source.sendFailure(OutputFormatter.createError("error.not_in_network")); return 0; }
        return executeInsertItemInternal(source, primaryNet, primaryNet.getId(), stack, count);
    }

    private static int executeInsertItem(CommandContext<CommandSourceStack> ctx, int netId, ItemStack stack, long count) {
        CommandSourceStack source = ctx.getSource();
        if (!PermissionChecker.checkOpPermission(source)) return 0;
        DimensionsNet net = PermissionChecker.checkNetworkExists(source, netId);
        if (net == null) return 0;
        return executeInsertItemInternal(source, net, netId, stack, count);
    }

    private static int executeInsertItemInternal(CommandSourceStack source, DimensionsNet net, int netId, ItemStack stack, long count) {
        if (!PermissionChecker.checkAmountPositive(source, count)) return 0;
        ItemStackKey key = new ItemStackKey(stack.copyWithCount(1));
        if (!NetworkUtils.hasEnoughStorageForItem(net, key, count)) { source.sendFailure(OutputFormatter.createError("network.transfer.insufficient_storage")); return 0; }
        KeyAmount remaining = net.getUnifiedStorage().insert(key, count, false);
        if (remaining.amount() > 0) { source.sendFailure(OutputFormatter.createError("error.insert_failed", remaining.amount())); return 0; }
        net.setDirty();
        String name = stack.getHoverName().getString();
        source.sendSuccess(() -> Component.literal(CommandLang.get("network.insert.item.success", count, name, netId)), false);
        return 1;
    }

    // ========== Fluid ==========

    private static int executeInsertFluidDefault(CommandContext<CommandSourceStack> ctx, Fluid fluid, long amount) {
        CommandSourceStack source = ctx.getSource();
        if (!PermissionChecker.checkOpPermission(source)) return 0;
        ServerPlayer executor = source.getPlayer();
        if (executor == null) { source.sendFailure(OutputFormatter.createError("error.player_required")); return 0; }
        DimensionsNet primaryNet = DimensionsNet.getPrimaryNetFromPlayer(executor);
        if (primaryNet == null) { source.sendFailure(OutputFormatter.createError("error.not_in_network")); return 0; }
        return executeInsertFluidInternal(source, primaryNet, primaryNet.getId(), fluid, amount);
    }

    private static int executeInsertFluid(CommandContext<CommandSourceStack> ctx, int netId, Fluid fluid, long amount) {
        CommandSourceStack source = ctx.getSource();
        if (!PermissionChecker.checkOpPermission(source)) return 0;
        DimensionsNet net = PermissionChecker.checkNetworkExists(source, netId);
        if (net == null) return 0;
        return executeInsertFluidInternal(source, net, netId, fluid, amount);
    }

    private static int executeInsertFluidInternal(CommandSourceStack source, DimensionsNet net, int netId, Fluid fluid, long amount) {
        if (!PermissionChecker.checkAmountPositive(source, amount)) return 0;
        FluidStack fs = new FluidStack(fluid, 1);
        FluidStackKey key = new FluidStackKey(fs);
        if (!NetworkUtils.hasEnoughStorageForFluid(net, key, amount)) { source.sendFailure(OutputFormatter.createError("network.transfer.insufficient_storage")); return 0; }
        KeyAmount remaining = net.getUnifiedStorage().insert(key, amount, false);
        if (remaining.amount() > 0) { source.sendFailure(OutputFormatter.createError("error.insert_failed", remaining.amount())); return 0; }
        net.setDirty();
        source.sendSuccess(() -> Component.literal(CommandLang.get("network.insert.fluid.success", amount, fluid.getFluidType().getDescription().getString(), netId)), false);
        return 1;
    }

    // ========== Energy ==========

    private static int executeInsertEnergyDefault(CommandContext<CommandSourceStack> ctx, long amount) {
        CommandSourceStack source = ctx.getSource();
        if (!PermissionChecker.checkOpPermission(source)) return 0;
        ServerPlayer executor = source.getPlayer();
        if (executor == null) { source.sendFailure(OutputFormatter.createError("error.player_required")); return 0; }
        DimensionsNet primaryNet = DimensionsNet.getPrimaryNetFromPlayer(executor);
        if (primaryNet == null) { source.sendFailure(OutputFormatter.createError("error.not_in_network")); return 0; }
        return executeInsertEnergyInternal(source, primaryNet, primaryNet.getId(), amount);
    }

    private static int executeInsertEnergy(CommandContext<CommandSourceStack> ctx, int netId, long amount) {
        CommandSourceStack source = ctx.getSource();
        if (!PermissionChecker.checkOpPermission(source)) return 0;
        DimensionsNet net = PermissionChecker.checkNetworkExists(source, netId);
        if (net == null) return 0;
        return executeInsertEnergyInternal(source, net, netId, amount);
    }

    private static int executeInsertEnergyInternal(CommandSourceStack source, DimensionsNet net, int netId, long amount) {
        if (!PermissionChecker.checkAmountPositive(source, amount)) return 0;
        if (!NetworkUtils.hasEnoughStorageForEnergy(net, EnergyStackKey.INSTANCE, amount)) { source.sendFailure(OutputFormatter.createError("network.transfer.insufficient_storage")); return 0; }
        KeyAmount remaining = net.getUnifiedStorage().insert(EnergyStackKey.INSTANCE, amount, false);
        if (remaining.amount() > 0) { source.sendFailure(OutputFormatter.createError("error.insert_failed", remaining.amount())); return 0; }
        net.setDirty();
        source.sendSuccess(() -> Component.literal(CommandLang.get("network.insert.energy.success", amount, netId)), false);
        return 1;
    }
}
