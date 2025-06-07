package com.lx862.mtrtm.mod.commands;

import com.lx862.mtrtm.mod.MtrUtil;
import com.lx862.mtrtm.mod.NeoTM;
import com.lx862.mtrtm.mod.Util;
import com.lx862.mtrtm.mod.mixin.RailwayDataPathGenerationModuleAccessorMixin;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import mtr.data.Depot;
import mtr.data.IGui;
import mtr.data.RailwayData;
import mtr.data.RailwayDataPathGenerationModule;
import mtr.packet.PacketTrainDataGuiServer;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;
import java.util.Map;

public class mtrpath {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mtrpath")
                .requires(ctx -> ctx.hasPermission(2))
                .then(Commands.literal("status")
                .executes(context -> {
                    RailwayData railwayData = RailwayData.getInstance(context.getSource().getLevel());
                    if(railwayData != null) {
                        RailwayDataPathGenerationModule module = railwayData.railwayDataPathGenerationModule;
                        Map<Long, Thread> generatingPathThreads = ((RailwayDataPathGenerationModuleAccessorMixin)module).getGeneratingPathThreads();
                        generatingPathThreads.keySet().removeIf(e -> !generatingPathThreads.get(e).isAlive());

                        context.getSource().sendSuccess(() -> Component.literal("===== Paths on " + generatingPathThreads.size() + " depot(s) are being refreshed =====").withStyle(ChatFormatting.GOLD), false);
                        for(Map.Entry<Long, Thread> entry : generatingPathThreads.entrySet()) {
                            Depot depot = railwayData.dataCache.depotIdMap.get(entry.getKey());
                            if(depot != null) {
                                MutableComponent text = Component.literal("- " + IGui.formatStationName(depot.name)).withStyle(ChatFormatting.GREEN);
                                MutableComponent time = Component.literal(" (" + Util.getReadableTimeMs(System.currentTimeMillis() - NeoTM.pathGenerationTimer.get(depot.id)) + " elapsed)").withStyle(ChatFormatting.YELLOW);
                                context.getSource().sendSuccess(() -> text.append(time), false);
                            }
                        }
                    }
                    return 1;
                }))
                .then(Commands.literal("refresh")
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
                                    mtr.generatePath(context);
                                    return 1;
                                })
                        ))
                .then(Commands.literal("abort")
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
                                    RailwayData railwayData = RailwayData.getInstance(context.getSource().getLevel());
                                    String depotName = StringArgumentType.getString(context, "depotName");
                                    MtrUtil.findDepots(depotName, context.getSource().getLevel()).forEach(depot -> {
                                        long id = depot.id;
                                        NeoTM.stopPathGenDepotList.add(id);
                                        RailwayDataPathGenerationModule pathGenerationModule = railwayData.railwayDataPathGenerationModule;
                                        pathGenerationModule.generatePath(context.getSource().getServer(), id);
                                        context.getSource().sendSuccess(() -> Component.literal("Path generation has been forcefully stopped.").withStyle(ChatFormatting.GREEN), false);
                                        PacketTrainDataGuiServer.generatePathS2C(context.getSource().getLevel(), id, 0);
                                    });
                                    return 1;
                                })
                        )
                )
            );
    }
}
