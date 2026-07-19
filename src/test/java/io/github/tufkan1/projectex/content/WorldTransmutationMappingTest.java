package io.github.tufkan1.projectex.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.tufkan1.projectex.content.alchemy.WorldTransmutationService;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class WorldTransmutationMappingTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void forwardAndReverseMappingsAreDeterministic() {
        var stone = Blocks.STONE.defaultBlockState();
        var cobblestone = WorldTransmutationService.targetFor(stone, false);

        assertEquals(Blocks.COBBLESTONE, cobblestone.getBlock());
        assertEquals(
            Blocks.STONE,
            WorldTransmutationService.targetFor(cobblestone, true).getBlock()
        );
    }

    @Test
    void unsupportedBlocksNeverProduceTargets() {
        assertNull(WorldTransmutationService.targetFor(Blocks.OBSIDIAN.defaultBlockState(), false));
        assertNull(WorldTransmutationService.targetFor(Blocks.BEDROCK.defaultBlockState(), true));
    }
}
