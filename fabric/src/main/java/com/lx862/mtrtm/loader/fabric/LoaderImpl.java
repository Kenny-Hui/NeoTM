package com.lx862.mtrtm.loader.fabric;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;

import java.nio.file.Path;
import java.util.function.Consumer;

public class LoaderImpl {
    public static void registerCommands(Consumer<CommandDispatcher<CommandSourceStack>> commandRegisterCallback) {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated, third) -> {
            commandRegisterCallback.accept(dispatcher);
        });
    }

    public static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
