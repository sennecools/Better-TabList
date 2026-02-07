//? if fabric {
package com.sennecools.tablist.fabric.mixin;

import com.sennecools.tablist.TabListVariables;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Inject(method = "getTabListDisplayName", at = @At("HEAD"), cancellable = true)
    private void tablist$getTabListDisplayName(CallbackInfoReturnable<Component> cir) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        String displayName = TabListVariables.resolveDisplayName(self);
        cir.setReturnValue(Component.literal(displayName));
    }
}
//?}
