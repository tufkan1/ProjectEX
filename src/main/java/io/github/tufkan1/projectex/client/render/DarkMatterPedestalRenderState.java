package io.github.tufkan1.projectex.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

/** Client snapshot used to render the pedestal item without retaining world state. */
@Environment(EnvType.CLIENT)
public final class DarkMatterPedestalRenderState extends BlockEntityRenderState {
    final ItemStackRenderState item = new ItemStackRenderState();
    float rotationDegrees;
}
