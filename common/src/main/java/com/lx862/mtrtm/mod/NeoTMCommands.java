package com.lx862.mtrtm.mod;

import com.lx862.mtrtm.mod.commands.*;
import com.mojang.brigadier.CommandDispatcher;

public class NeoTMCommands {
    public static void registerCommands(CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher) {
        mtr.register(dispatcher);
        mtrtm.register(dispatcher);
        train.register(dispatcher);
        platform.register(dispatcher);
        mtrpath.register(dispatcher);
        warpstn.register(dispatcher);
        warpdepot.register(dispatcher);
    }
}
