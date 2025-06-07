package com.lx862.mtrtm.mod.commands;

import com.lx862.mtrtm.mod.config.Config;
import com.mojang.brigadier.CommandDispatcher;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class mtrtm {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
            dispatcher.register(Commands.literal("mtrtm")
                    .requires(ctx -> ctx.hasPermission(2))
                    .then(Commands.literal("reload")
                    .executes(context -> {
                        context.getSource().sendSuccess(() -> Component.literal("Reloading Config...").withStyle(ChatFormatting.GOLD), false);
                        List<String> error = reloadConfig(context.getSource().getServer().getServerDirectory().resolve("config").resolve("transitmanager"));
                        if(!error.isEmpty()) {
                            String failed = String.join(",", error);
                            context.getSource().sendFailure(Component.literal("Config Reloaded. " + failed + " failed to load.").withStyle(ChatFormatting.RED));
                            context.getSource().sendFailure(Component.literal("Please check whether the JSON syntax is correct!").withStyle(ChatFormatting.RED));
                        } else {
                            context.getSource().sendSuccess(() -> Component.literal("Config Reloaded!").withStyle(ChatFormatting.GREEN), false);
                        }
                        return 1;
                    }))
            );
    }

    public static List<String> reloadConfig(Path path) {
        List<String> error = new ArrayList<>();
        if(!Config.load(path)) {
            error.add("Main Config");
        }

        return error;
    }
}
