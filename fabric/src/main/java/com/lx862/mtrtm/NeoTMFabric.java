package com.lx862.mtrtm;

import com.lx862.mtrtm.mod.NeoTM;
import net.fabricmc.api.ModInitializer;

public class NeoTMFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        NeoTM.init();
    }
}
