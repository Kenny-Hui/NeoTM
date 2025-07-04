package com.lx862.mtrtm.mod.commands;

import com.lx862.mtrtm.mod.MtrUtil;
import com.lx862.mtrtm.mod.Util;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import mtr.data.*;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * MTR subcommands
 * It is (hopefully) expected that MTR 4 will use this root command in the future, that way TM users will have a smoother migration.
 * But I am not in charge of MTR Mod, don't go after me if it didn't end up happening :P
 */
public class mtr {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mtr")
            .then(Commands.literal("generatePath")
                    .requires(ctx -> ctx.hasPermission(2))
                    .executes(context -> {
                        generatePath(context);
                        return 1;
                    })
                    .then(Commands.argument("depotName", StringArgumentType.greedyString())
                            .suggests((context, suggestionsBuilder) -> {
                                RailwayData data = RailwayData.getInstance(context.getSource().getLevel());
                                if(data != null) {
                                    String target = suggestionsBuilder.getRemainingLowerCase();
                                    List<String> toBeAdded = Util.formulateMatchingString(target, data.depots.stream().map(e -> e.name).toList());
                                    for(String dp : toBeAdded) {
                                        suggestionsBuilder.suggest(dp);
                                    }
                                }
                                return suggestionsBuilder.buildFuture();
                            })
                            .executes(context -> {
                                generatePath(context);
                                return 1;
                            })
                    )
            )
            .then(Commands.literal("clearTrains")
                    .requires(ctx -> ctx.hasPermission(2))
                    .executes(context -> {
                        clearTrains(context);
                        return 1;
                    })
                    .then(Commands.argument("depotName", StringArgumentType.greedyString())
                            .suggests((context, suggestionsBuilder) -> {
                                RailwayData data = RailwayData.getInstance(context.getSource().getLevel());
                                if(data != null) {
                                    String target = suggestionsBuilder.getRemainingLowerCase();
                                    List<String> toBeAdded = Util.formulateMatchingString(target, data.depots.stream().map(e -> e.name).toList());
                                    for(String dp : toBeAdded) {
                                        suggestionsBuilder.suggest(dp);
                                    }
                                }
                                return suggestionsBuilder.buildFuture();
                            })
                            .executes(context -> {
                                clearTrains(context);
                                return 1;
                            })
                    )
            )
        );
    }

    public static void clearTrains(CommandContext<CommandSourceStack> context) {
        RailwayData railwayData = RailwayData.getInstance(context.getSource().getLevel());
        String depotName = null;

        try {
            depotName = StringArgumentType.getString(context, "depotName");
        } catch (Exception ignored) {}

        MtrUtil.findDepots(depotName, context.getSource().getLevel()).forEach(depot -> {
            int clearedCounter = 0;
            for(Siding siding : railwayData.sidings) {
                if(depot.inArea(siding.getMidPos().getX(), siding.getMidPos().getZ())) {
                    siding.clearTrains();
                    clearedCounter++;
                }
            }

            int finalClearedCounter = clearedCounter;
            context.getSource().sendSuccess(() -> Component.literal("Cleared " + finalClearedCounter + " train(s) in depot " + IGui.formatStationName(depot.name)).withStyle(ChatFormatting.GREEN), false);
        });
    }

    public static void generatePath(CommandContext<CommandSourceStack> context) {
        RailwayData railwayData = RailwayData.getInstance(context.getSource().getLevel());
        String depotName = null;

        try {
            depotName = StringArgumentType.getString(context, "depotName");
        } catch (Exception ignored) {}

        MtrUtil.findDepots(depotName, context.getSource().getLevel()).forEach(depot -> {
            RailwayDataPathGenerationModule pathGenerationModule = railwayData.railwayDataPathGenerationModule;
            pathGenerationModule.generatePath(context.getSource().getServer(), depot.id);
            context.getSource().sendSuccess(() -> Component.literal("Refreshing " + String.join(" ", depot.name) + " (" + depot.routeIds.size() + " Routes instructions)").withStyle(ChatFormatting.GREEN), false);
        });
    }
}
