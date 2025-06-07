package com.lx862.mtrtm.loader;

import com.mojang.brigadier.CommandDispatcher;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.commands.CommandSourceStack;

import java.nio.file.Path;
import java.util.function.Consumer;

public class Loader {
    @ExpectPlatform
    public static void registerCommands(Consumer<CommandDispatcher<CommandSourceStack>> dispatcher) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static Path getConfigPath() {
        throw new AssertionError();
    }
}
