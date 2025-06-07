package com.lx862.mtrtm.mod.mixin;

import com.lx862.mtrtm.mod.NeoTM;
import mtr.data.RailwayDataPathGenerationModule;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(value = RailwayDataPathGenerationModule.class, remap = false)
public class RailwayDataPathGenerationModuleMixin {
    @Shadow @Final private Map<Long, Thread> generatingPathThreads;

    @Inject(method = "generatePath", at = @At("HEAD"), cancellable = true)
    public void generatePath(MinecraftServer minecraftServer, long depotId, CallbackInfo ci) {
        /* Abort path generation if requested */
        if(NeoTM.stopPathGenDepotList.contains(depotId)) {
            Thread thread = generatingPathThreads.get(depotId);
            if(thread != null && thread.isAlive()) {
                try {
                    thread.interrupt();
                } catch (Exception e) {
                    NeoTM.LOGGER.warn("[{}] Failed to abort path generation thread!", NeoTM.BRAND, e);
                }
            }

            NeoTM.stopPathGenDepotList.remove(depotId);
            generatingPathThreads.remove(depotId);
            ci.cancel();
        } else {
            NeoTM.pathGenerationTimer.put(depotId, System.currentTimeMillis());
        }
    }
}
