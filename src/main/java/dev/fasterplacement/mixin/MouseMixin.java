package dev.fasterplacement.mixin;

import dev.fasterplacement.FasterPlacementClient;
import dev.fasterplacement.core.PlacementEngine;
import net.minecraft.client.Mouse;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.MouseInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void fasterplacement$onMouseButton(long window, MouseInput mouseInput, int action, CallbackInfo ci) {
        if (mouseInput.button() != GLFW.GLFW_MOUSE_BUTTON_RIGHT || action != GLFW.GLFW_PRESS) {
            return;
        }

        PlacementEngine engine = FasterPlacementClient.engine();
        if (engine != null) {
            engine.onRawRightClickPress(MinecraftClient.getInstance());
        }
    }
}
