package io.github.tufkan1.projectex.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.tufkan1.projectex.content.pedestal.DarkMatterPedestalBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

/** Displays the installed effect item above the Dark Matter Pedestal. */
@Environment(EnvType.CLIENT)
public final class DarkMatterPedestalRenderer implements
    BlockEntityRenderer<DarkMatterPedestalBlockEntity, DarkMatterPedestalRenderState> {
    private final ItemModelResolver itemModelResolver;

    public DarkMatterPedestalRenderer(BlockEntityRendererProvider.Context context) {
        itemModelResolver = context.itemModelResolver();
    }

    @Override public DarkMatterPedestalRenderState createRenderState() {
        return new DarkMatterPedestalRenderState();
    }

    @Override public void extractRenderState(DarkMatterPedestalBlockEntity pedestal,
        DarkMatterPedestalRenderState state, float partialTick, Vec3 cameraPosition,
        ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
            pedestal, state, partialTick, cameraPosition, breakProgress);
        int seed = (int) pedestal.getBlockPos().asLong();
        itemModelResolver.updateForTopItem(state.item, pedestal.item(), ItemDisplayContext.FIXED,
            pedestal.getLevel(), null, seed);
        state.rotationDegrees = pedestal.getLevel() == null ? 0.0F
            : (pedestal.getLevel().getGameTime() + partialTick) * 2.0F;
    }

    @Override public void submit(DarkMatterPedestalRenderState state, PoseStack poseStack,
        SubmitNodeCollector collector, CameraRenderState cameraState) {
        if (state.item.isEmpty()) return;
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.2F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.rotationDegrees));
        poseStack.scale(0.75F, 0.75F, 0.75F);
        state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}
