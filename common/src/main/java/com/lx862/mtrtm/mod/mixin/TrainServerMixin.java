package com.lx862.mtrtm.mod.mixin;

import com.lx862.mtrtm.mod.NeoTM;
import com.lx862.mtrtm.mod.data.TrainState;
import mtr.data.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TrainServer.class, remap = false)
public class TrainServerMixin {

    /* Disable train collision */
    @Inject(method = "isRailBlocked", at = @At("HEAD"), cancellable = true)
    public void isRailBlocked(int checkIndex, CallbackInfoReturnable<Boolean> cir) {
        long trainId = ((Train)((TrainServer)(Object)this)).id;
        if(NeoTM.getTrainState(trainId, TrainState.SKIP_COLLISION)) {
            cir.setReturnValue(false);
        }
    }
}