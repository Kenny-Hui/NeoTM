package com.lx862.mtrtm.mod;

import com.lx862.mtrtm.mod.config.Config;
import com.lx862.mtrtm.mod.data.TrainState;
import com.lx862.mtrtm.loader.Loader;
import it.unimi.dsi.fastutil.longs.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NeoTM {
    public static final String BRAND = "NeoTM";
    public static final String MOD_ID = "mtrtm";

    public static final Logger LOGGER = LogManager.getLogger(BRAND);
    public static final LongList stopPathGenDepotList = new LongArrayList();
    public static final Long2IntOpenHashMap trainStateList = new Long2IntOpenHashMap();
    public static final Long2LongArrayMap pathGenerationTimer = new Long2LongArrayMap();

    public static void init() {
        LOGGER.info("[{}] TransitManager initialized \\(＾▽＾)/", BRAND);
        Config.load(Loader.getConfigPath().resolve("transitmanager"));
        Loader.registerCommands(NeoTMCommands::registerCommands);
    }

    public static boolean getTrainState(long trainId, TrainState trainState) {
        int state = trainStateList.get(trainId);
        int pos = trainState.getPos();

        return ((state >> pos) & 1) == 1;
    }

    public static void setTrainState(long trainId, TrainState trainState, boolean value) {
        int state = trainStateList.getOrDefault(trainId, 0);
        int pos = trainState.getPos();
        if(value) {
            state = state | (1 << pos);
        } else {
            state = state & ~(1 << pos);
        }


        trainStateList.put(trainId, state);
    }
}
