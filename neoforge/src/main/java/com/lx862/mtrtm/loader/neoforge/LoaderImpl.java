package com.lx862.mtrtm.loader.neoforge;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class LoaderImpl {
    public static final List<Consumer<RegisterCommandsEvent>> commandsEventListeners = new ArrayList<>();

    public static void registerCommands(Consumer<CommandDispatcher<CommandSourceStack>> commandRegisterCallback) {
        commandsEventListeners.add((evt) -> commandRegisterCallback.accept(evt.getDispatcher()));
    }

    public static Path getConfigPath() {
        return FMLPaths.CONFIGDIR.get();
    }
}
