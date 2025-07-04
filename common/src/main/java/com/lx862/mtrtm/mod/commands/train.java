package com.lx862.mtrtm.mod.commands;

import com.lx862.mtrtm.mod.MtrUtil;
import com.lx862.mtrtm.mod.NeoTM;
import com.lx862.mtrtm.mod.Util;
import com.lx862.mtrtm.mod.data.ExposedTrainData;
import com.lx862.mtrtm.mod.data.TrainState;
import com.lx862.mtrtm.mod.mixin.SidingAccessorMixin;
import com.lx862.mtrtm.mod.mixin.TrainAccessorMixin;
import com.lx862.mtrtm.mod.mixin.TrainServerAccessorMixin;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import mtr.data.*;
import mtr.path.PathData;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class train {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("train")
                .requires(ctx -> ctx.hasPermission(2))
                .then(Commands.literal("halt")
                        .then(Commands.literal("dwell")
                                .executes(context -> haltDwell(context))
                        )
                        .then(Commands.literal("speed")
                                .executes(context -> haltSpeed(context))
                        )
                )
                .then(Commands.literal("toggleCollision")
                        .executes(context -> toggleCollision(context))
                )
                .then(Commands.literal("board")
                        .executes(context -> board(context))
                )
                .then(Commands.literal("ejectAllPassengers")
                        .executes(context -> ejectPassengers(context))
                )
                .then(Commands.literal("clear")
                        .executes(context -> clearNearestTrain(context))
                )
                .then(Commands.literal("deploy")
                        .executes(context -> deploy(context))
                )
                .then(Commands.literal("skipDwell")
                        .executes(context -> skipDwell(context))
                )
                .then(Commands.literal("jump")
                        .then(Commands.literal("siding")
                                .executes(context -> jump(context, true, false, false, false, true))
                        )
                        .then(Commands.literal("next")
                                .then(Commands.literal("platform")
                                        .executes(context -> jump(context, true, false, true, false, false))
                                )
                                .then(Commands.literal("path")
                                        .executes(context -> jump(context, true, true, false, false, false))
                                )
                                .then(Commands.literal("stopPosition")
                                        .executes(context -> jump(context, true, false, false, true, false))
                                )
                        )
                        .then(Commands.literal("previous")
                                .then(Commands.literal("platform")
                                        .executes(context -> jump(context, false, false, true, false, false))
                                )
                                .then(Commands.literal("path")
                                        .executes(context -> jump(context, false, true, false, false, false))
                                )
                        )
                )
                .executes(context -> getNearestTrain(context, Util.getPlayerFromContext(context)))
        );
    }

    private static int deploy(CommandContext<CommandSourceStack> context) {
        RailwayData data = RailwayData.getInstance(context.getSource().getLevel());
        ExposedTrainData nearestTrain = getNearestTrainOrError(context);

        List<Siding> trainSidings = data.sidings.stream().filter(siding -> siding.id == nearestTrain.train.sidingId).toList();
        Siding trainSiding;
        if(!trainSidings.isEmpty()) {
            trainSiding = trainSidings.get(0);
        } else {
            trainSiding = null;
        }

        nearestTrain.train.deployTrain();
        context.getSource().sendSuccess(() -> Component.literal("Deploying the nearest train (Siding " + trainSiding.name + ")...").withStyle(ChatFormatting.GREEN), false);
        context.getSource().sendSuccess(() -> Component.literal("Train ID: " + trainSiding.getTrainId()).withStyle(ChatFormatting.GREEN), false);

        if(nearestTrain.isManual) {
            context.getSource().sendSuccess(() -> Component.literal("NOTE: Train is currently in manual mode.").withStyle(ChatFormatting.YELLOW), false);
        }
        return 1;
    }

    private static int jump(CommandContext<CommandSourceStack> context, boolean next, boolean isPath, boolean isPlatform, boolean isNextStop, boolean isSiding) {
        ExposedTrainData trainData = getNearestTrainOrError(context);
        double currentRailProgress = trainData.train.getRailProgress();
        List<Double> distances = ((TrainAccessorMixin)trainData.train).getDistances();

        double targetDistance = -1;
        int pathIndex = -1;

        if(isNextStop) {
            targetDistance = distances.get(((TrainAccessorMixin)trainData.train).getNextStoppingIndex());
        } else if(isSiding) {
            targetDistance = distances.get(0);
        } else {
            boolean justOneMorePath = false;
            for(int i = 0; i < trainData.train.path.size(); i++) {
                int pIndex;
                if(next) {
                    pIndex = i;
                } else {
                    pIndex = trainData.train.path.size() - 1 - i;
                }

                PathData path = trainData.train.path.get(pIndex);

                boolean isStoppablePlatform = path.dwellTime > 0 && path.rail.railType == RailType.PLATFORM;

                if((isPlatform && isStoppablePlatform) || isPath) {
                    double dist = distances.get(pIndex);
                    if(next && dist > currentRailProgress) {
                        targetDistance = dist;
                        pathIndex = pIndex;
                        break;
                    }

                    if(!next && dist < currentRailProgress) {
                        if((trainData.train.getSpeed() == 0) || isPlatform || (trainData.train.getSpeed() > 0 && justOneMorePath) /* 1 more path if train is running */) {
                            targetDistance = dist;
                            pathIndex = pIndex;
                            break;
                        }

                        justOneMorePath = true;
                    }
                }
            }
        }

        if(targetDistance != -1) {
            ((TrainAccessorMixin)trainData.train).setNextStoppingIndex(Math.max(pathIndex, ((TrainAccessorMixin) trainData.train).getNextStoppingIndex()));
            ((TrainAccessorMixin)trainData.train).setRailProgress(targetDistance);
            MtrUtil.syncTrainToPlayers(trainData.train, context.getSource().getLevel().players());

            double finalTargetDistance = targetDistance;
            context.getSource().sendSuccess(() -> Component.literal("Jumped to distance " + Math.round(finalTargetDistance) + "m.").withStyle(ChatFormatting.GREEN), false);
        } else {
            throw new IllegalStateException("Cannot find the next path to stop to.");
        }
        return 1;
    }

    private static int ejectPassengers(CommandContext<CommandSourceStack> context) {
        ExposedTrainData nearestTrain = getNearestTrainOrError(context);

        ((TrainAccessorMixin)nearestTrain.train).getRidingEntities().clear();
        MtrUtil.syncTrainToPlayers(nearestTrain.train, context.getSource().getLevel().players());
        context.getSource().sendSuccess(() -> Component.literal("All passengers cleared from train!").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int board(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ExposedTrainData nearestTrain = getNearestTrainOrError(context);

        ((TrainAccessorMixin)nearestTrain.train).getRidingEntities().add(context.getSource().getPlayerOrException().getUUID());
        MtrUtil.syncTrainToPlayers(nearestTrain.train, context.getSource().getLevel().players());
        context.getSource().sendSuccess(() -> Component.literal("Train boarded!").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int clearNearestTrain(CommandContext<CommandSourceStack> context) {
        ExposedTrainData nearestTrain = getNearestTrainOrError(context);
        RailwayData railwayData = RailwayData.getInstance(context.getSource().getLevel());

        long sidingId = nearestTrain.train.sidingId;
        Siding trainSiding = railwayData.dataCache.sidingIdMap.get(sidingId);

        ((SidingAccessorMixin)trainSiding).getTrains().removeIf(train -> train.id == nearestTrain.train.id);
        context.getSource().sendSuccess(() -> Component.literal("Siding cleared!").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int skipDwell(CommandContext<CommandSourceStack> context) {
        ExposedTrainData nearestTrain = getNearestTrainOrError(context);
        ((TrainAccessorMixin)nearestTrain.train).setElapsedDwellTicks(nearestTrain.train.getTotalDwellTicks());
        MtrUtil.syncTrainToPlayers(nearestTrain.train, context.getSource().getLevel().players());

        context.getSource().sendSuccess(() -> Component.literal("Dwell time skipped!").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int haltDwell(CommandContext<CommandSourceStack> context) {
        ExposedTrainData nearestTrain = getNearestTrainOrError(context);
        boolean halted = NeoTM.getTrainState(nearestTrain.train.id, TrainState.HALT_DWELL);
        NeoTM.setTrainState(nearestTrain.train.id, TrainState.HALT_DWELL, !halted);

        context.getSource().sendSuccess(() -> Component.literal("Dwell timer for the nearest train has been " + (!halted ? "paused" : "resumed")).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int haltSpeed(CommandContext<CommandSourceStack> context) {
        ExposedTrainData nearestTrain = getNearestTrainOrError(context);
        boolean halted = NeoTM.getTrainState(nearestTrain.train.id, TrainState.HALT_SPEED);
        NeoTM.setTrainState(nearestTrain.train.id, TrainState.HALT_SPEED, !halted);

        context.getSource().sendSuccess(() -> Component.literal("The nearest train has " + (!halted ? "been brought to a halt" : "resumed it's operation")).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int toggleCollision(CommandContext<CommandSourceStack> context) {
        ExposedTrainData nearestTrain = getNearestTrainOrError(context);
        boolean skipCollision = NeoTM.getTrainState(nearestTrain.train.id, TrainState.SKIP_COLLISION);
        NeoTM.setTrainState(nearestTrain.train.id, TrainState.SKIP_COLLISION, !skipCollision);

        context.getSource().sendSuccess(() -> Component.literal("Collision detection for the nearest train is now " + (!skipCollision ? "bypassed" : "reset to normal")).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static ExposedTrainData getNearestTrainOrError(CommandContext<CommandSourceStack> context) {
        ExposedTrainData trainData;
        ServerPlayer player = Util.getPlayerFromContext(context);

        if (player != null) {
            trainData = MtrUtil.getNearestTrain(context.getSource().getLevel(), player, player.getEyePosition());
        } else {
            trainData = MtrUtil.getNearestTrain(context.getSource().getLevel(), null, context.getSource().getPosition());
        }

        if(trainData == null) {
            throw new IllegalStateException("Cannot find any nearest train!");
        }
        return trainData;
    }

    // From whattrain
    public static int getNearestTrain(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        RailwayData data = RailwayData.getInstance(context.getSource().getLevel());
        ExposedTrainData trainData = MtrUtil.getNearestTrain(context.getSource().getLevel(), player, player.getEyePosition());

        if(trainData == null) {
            context.getSource().sendSuccess(() -> Component.literal("Cannot find any train.").withStyle(ChatFormatting.RED), false);
            return 1;
        }

        if(trainData.isManual) {
            trainData.isCurrentlyManual = ((TrainAccessorMixin)trainData.train).getIsCurrentlyManual();
            if(trainData.isCurrentlyManual) {
                trainData.accelerationSign = ((TrainAccessorMixin) trainData.train).getManualNotch();
                trainData.manualCooldown = ((TrainServerAccessorMixin)trainData.train).getManualCoolDown();
                trainData.manualToAutomaticTime = ((TrainAccessorMixin) trainData.train).getManualToAutomaticTime();
            }
        }

        long sidingId = trainData.train.sidingId;
        List<Siding> sidingList = data.sidings.stream().filter(sd -> sd.id == sidingId).toList();

        if(sidingList.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("Cannot find corresponding siding.").withStyle(ChatFormatting.RED), false);
            return 1;
        }

        Siding siding = sidingList.get(0);
        Depot sidingDepot = null;

        for(Depot depot : data.depots) {
            if(depot.inArea(siding.getMidPos().getX(), siding.getMidPos().getZ())) {
                sidingDepot = depot;
                break;
            }
        }

        if(sidingDepot == null) {
            context.getSource().sendSuccess(() -> Component.literal("No depot associated with that siding.").withStyle(ChatFormatting.RED), false);
            return 1;
        }

        List<Route> trainRoutes = data.routes.stream().filter(rt -> rt.id == trainData.routeId).toList();
        String currentRouteName = "N/A";
        final int currentRouteColor;
        String currentRouteDestination = null;
        String dwellString;

        double remainingDwell = (trainData.train.getTotalDwellTicks() - trainData.train.getElapsedDwellTicks()) / SharedConstants.TICKS_PER_SECOND;
        int displayedDwell = (int)Math.round(remainingDwell);
        if(remainingDwell < 0) {
            dwellString = "0s (" + Math.abs(displayedDwell) + " overdue)";
        } else {
            dwellString = Util.getReadableTimeMs(displayedDwell * 1000L);
        }

        Platform lastPlatformInRoute = null;

        if(!trainRoutes.isEmpty()) {
            Route runningRoute = trainRoutes.get(0);
            currentRouteColor = runningRoute.color;
            currentRouteName = IGui.formatStationName(runningRoute.name);

            long lastPlatformId = runningRoute.getLastPlatformId();
            Station lastStation = data.dataCache.platformIdToStation.get(lastPlatformId);
            lastPlatformInRoute = data.dataCache.platformIdMap.get(lastPlatformId);
            if(lastStation == null) {
                BlockPos midPos = lastPlatformInRoute.getMidPos();
                currentRouteDestination = "Platform " + lastPlatformInRoute.name + " (" + midPos.getX() + ", " + midPos.getY() + ", " + midPos.getZ()  + ")";
            } else {
                currentRouteDestination = IGui.formatStationName(lastStation.name) + " (" + lastPlatformInRoute.name + ")";
            }
        } else {
            currentRouteColor = 0;
        }

        final int depotColor = sidingDepot.color;
        final String depotSidingName = IGui.formatStationName(sidingDepot.name)  + " (Siding " + siding.name + ")";

        MutableComponent depotName = Component.literal(depotSidingName).withStyle(style -> style.withColor(depotColor));
        MutableComponent routeName = Component.literal(currentRouteName).withStyle(style -> style.withColor(currentRouteColor));
        MutableComponent destinationName = currentRouteDestination == null ? null : Component.literal(currentRouteDestination).withStyle(ChatFormatting.GREEN);
        String title = IGui.formatStationName(trainData.train.trainId) + " (" + trainData.train.trainCars + "-cars)";
        MutableComponent pos = Component.literal(String.format("%d, %d, %d", Math.round(trainData.positions[0].x()), Math.round(trainData.positions[0].y()), Math.round(trainData.positions[0].z()))).withStyle(ChatFormatting.GREEN);
        MutableComponent dwell = Component.literal(dwellString).withStyle(ChatFormatting.GREEN);
        MutableComponent isManual = Component.literal(trainData.isManual ? trainData.isCurrentlyManual ? "Manual (Currently Manual)" : "Manual (Current ATO)" : "ATO").withStyle(ChatFormatting.GREEN);

        MutableComponent trainNotch = Component.literal(trainData.accelerationSign == -2 ? "B2" : trainData.accelerationSign == -1 ? "B1" : trainData.accelerationSign == 0 ? "N" : trainData.accelerationSign == 1 ? "P1" : "P2").withStyle(ChatFormatting.GREEN);

        int manualToAutoTimeMs = (trainData.manualToAutomaticTime * 10) * 50;
        int manualCooldownMs = trainData.manualCooldown * 50;
        MutableComponent PMLeft = Component.literal(Util.getReadableTimeMs(manualToAutoTimeMs - manualCooldownMs)).withStyle(ChatFormatting.GREEN);

        Set<UUID> ridingEntities = ((TrainAccessorMixin)trainData.train).getRidingEntities();
        StringBuilder ridingEntitiesStr = new StringBuilder();
        for (UUID uuid : ((TrainAccessorMixin)trainData.train).getRidingEntities()) {
            ServerPlayer ridingPlayer = context.getSource().getServer().getPlayerList().getPlayer(uuid);
            if (ridingPlayer == null) continue;

            ridingEntitiesStr.append(ridingPlayer.getGameProfile().getName()).append("\n");
        }


        boolean hasEffectApplied = false;
        MutableComponent effectText = Component.literal("");

        for(TrainState state : TrainState.values()) {
            boolean enabled = NeoTM.getTrainState(trainData.train.id, state);
            if(enabled) {
                hasEffectApplied = true;
                MutableComponent thisEffectText = Component.literal(state.getName()).withStyle(ChatFormatting.GREEN).withStyle(ChatFormatting.UNDERLINE);
                effectText.append(thisEffectText).append(" ");
            }
        }

        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(ridingEntitiesStr.toString()).withStyle(ChatFormatting.GREEN));
        ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/traininv " + siding.id);

        context.getSource().sendSuccess(() -> Component.literal("===== " + title + " =====").withStyle(ChatFormatting.GREEN), false);
        sendKeyValueFeedback(context, Component.literal("Mode: "), isManual);
        sendKeyValueFeedback(context, Component.literal("Depot/Siding: "), teleportToSavedRailText(depotName, siding));
        sendKeyValueFeedback(context, Component.literal("Position: "), pos);
        sendKeyValueFeedback(context, Component.literal("Running Route: "), routeName);
        if(trainData.train.getSpeed() == 0 && trainData.train.getTotalDwellTicks() > 0) {
            sendKeyValueFeedback(context, Component.literal("Dwell left: "), dwell);
        }

        if(hasEffectApplied) {
            sendKeyValueFeedback(context, Component.literal("Effect applied: "), effectText);
        }

        if(destinationName != null) {
            sendKeyValueFeedback(context, Component.literal("Destination: "), teleportToSavedRailText(destinationName, lastPlatformInRoute));
        }

        if(trainData.isManual && trainData.isCurrentlyManual) {
            sendKeyValueFeedback(context, Component.literal("Train Notch: "), trainNotch);
            sendKeyValueFeedback(context, Component.literal("Switching to ATO in: "), PMLeft);
        }

        if(!ridingEntities.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("Riding players: (Hover Here)").withStyle(ChatFormatting.GOLD).withStyle(style -> style.withHoverEvent(hoverEvent)), false);
        }
        return 1;
    }

    private static void sendKeyValueFeedback(CommandContext<CommandSourceStack> context, MutableComponent key, MutableComponent value) {
        context.getSource().sendSuccess(() -> key.withStyle(ChatFormatting.GOLD).append(value), false);
    }

    private static MutableComponent teleportToSavedRailText(MutableComponent originalText, SavedRailBase savedRail) {
        if(savedRail == null) return originalText;
        BlockPos midPos = savedRail.getMidPos();
        HoverEvent hoverEventTp = new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to teleport").withStyle(ChatFormatting.GREEN));
        ClickEvent clickEventTp = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp " + midPos.getX() + " " + midPos.getY() + " " + midPos.getZ());
        return originalText.withStyle(ChatFormatting.UNDERLINE).withStyle(e -> e.withHoverEvent(hoverEventTp).withClickEvent(clickEventTp));
    }
}
