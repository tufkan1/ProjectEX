package io.github.tufkan1.projectex.client;

import com.mojang.blaze3d.platform.InputConstants;

/** Vulkan-era input constants introduced by 26.3 Snapshot 4. */
final class InputCompatLegacy {
    private InputCompatLegacy() {
    }

    static InputConstants.Type keyboardType() { return InputConstants.Type.KEYBOARD; }
    static int keyV() { return InputConstants.KEY_V; }
    static int keyG() { return InputConstants.KEY_G; }
    static int keyK() { return InputConstants.KEY_K; }
    static int keyLeft() { return InputConstants.KEY_LEFT; }
    static int keyRight() { return InputConstants.KEY_RIGHT; }
    static int keyUp() { return InputConstants.KEY_UP; }
    static int keyDown() { return InputConstants.KEY_DOWN; }
    static int keyEnter() { return InputConstants.KEY_RETURN; }
}
