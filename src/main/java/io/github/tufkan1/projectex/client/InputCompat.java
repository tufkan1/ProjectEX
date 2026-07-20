package io.github.tufkan1.projectex.client;

import com.mojang.blaze3d.platform.InputConstants;

/** Stable facade for input constants across the GLFW-to-Vulkan input transition. */
public final class InputCompat {
    public static final int KEY_LEFT = InputCompatLegacy.keyLeft();
    public static final int KEY_RIGHT = InputCompatLegacy.keyRight();
    public static final int KEY_UP = InputCompatLegacy.keyUp();
    public static final int KEY_DOWN = InputCompatLegacy.keyDown();
    public static final int KEY_ENTER = InputCompatLegacy.keyEnter();

    private InputCompat() {
    }

    static InputConstants.Type keyboardType() { return InputCompatLegacy.keyboardType(); }
    static int keyV() { return InputCompatLegacy.keyV(); }
    static int keyG() { return InputCompatLegacy.keyG(); }
    static int keyK() { return InputCompatLegacy.keyK(); }
}
