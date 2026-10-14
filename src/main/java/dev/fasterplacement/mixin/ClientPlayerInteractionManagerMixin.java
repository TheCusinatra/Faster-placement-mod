package dev.fasterplacement.mixin;

import dev.fasterplacement.bridge.PlacementMixinBridge;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {
    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void fasterplacement$onInteractBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult,
                                                 CallbackInfoReturnable<ActionResult> cir) {
        if (PlacementMixinBridge.shouldCancelVanillaDuplicate(
            player,
            hand,
            hitResult.getBlockPos().asLong(),
            hitResult.getSide().ordinal(),
            hitResult.isInsideBlock(),
            (float) hitResult.getPos().x,
            (float) hitResult.getPos().y,
            (float) hitResult.getPos().z
        )) {
            cir.setReturnValue(ActionResult.SUCCESS);
            return;
        }

        PlacementMixinBridge.onInteractBlockCall(player, hand);
    }
}
