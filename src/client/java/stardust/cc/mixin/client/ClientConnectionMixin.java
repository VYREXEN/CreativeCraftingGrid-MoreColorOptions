package stardust.cc.mixin.client;

import stardust.cc.CreativeCrafting;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin {
    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof CloseHandledScreenC2SPacket p) {
            if (CreativeCrafting.isSticky() && p.getSyncId() == CreativeCrafting.mc.player.playerScreenHandler.syncId) {
                CreativeCrafting.logger.info("Creative Crafting: canceled CloseHandledScreenC2SPacket with syncId=" + p.getSyncId());
                ci.cancel();
            }
        }
    }
}