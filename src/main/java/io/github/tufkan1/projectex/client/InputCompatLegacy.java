package io.github.tufkan1.projectex.client;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

/** Input constants used through 26.3 Snapshot 3. */
final class InputCompatLegacy {
    private InputCompatLegacy() {
    }

    static InputConstants.Type keyboardType() { return InputConstants.Type.KEYSYM; }
    static int keyV() { return GLFW.GLFW_KEY_V; }
    static int keyG() { return GLFW.GLFW_KEY_G; }
    static int keyK() { return GLFW.GLFW_KEY_K; }
    static int keyLeft() { return GLFW.GLFW_KEY_LEFT; }
    static int keyRight() { return GLFW.GLFW_KEY_RIGHT; }
    static int keyUp() { return GLFW.GLFW_KEY_UP; }
    static int keyDown() { return GLFW.GLFW_KEY_DOWN; }
    static int keyEnter() { return GLFW.GLFW_KEY_ENTER; }
}
