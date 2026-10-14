package dev.fasterplacement.client;

import dev.fasterplacement.FasterPlacementClient;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class PlacementKeyBindings {
    private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(
        Identifier.of(FasterPlacementClient.MOD_ID, "controls")
    );
    private static final String TOGGLE_KEY = "key." + FasterPlacementClient.MOD_ID + ".toggle";

    private static KeyBinding toggleBinding;

    private PlacementKeyBindings() {
    }

    public static void register() {
        toggleBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            TOGGLE_KEY,
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
        ));
    }

    public static KeyBinding toggleBinding() {
        return toggleBinding;
    }
}
