package io.github.tufkan1.projectex.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.content.component.MachineItemState;
import io.github.tufkan1.projectex.machine.MachineAccess;
import io.github.tufkan1.projectex.machine.MachineRedstoneMode;
import io.github.tufkan1.projectex.machine.MachineState;
import io.github.tufkan1.projectex.machine.MachineTier;
import io.netty.buffer.Unpooled;
import java.math.BigInteger;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

final class MachineItemStateTest {
    @Test
    void persistentAndNetworkFormatsRetainCompleteMachineState() {
        MachineState machine = new MachineState(
            MachineState.CURRENT_VERSION,
            MachineTier.RELAY_MK3,
            EmcValue.of(9_876_543),
            BigInteger.valueOf(17),
            new MachineAccess(Optional.of(UUID.randomUUID()), true),
            MachineRedstoneMode.REQUIRE_NO_SIGNAL
        );
        MachineItemState carried = MachineItemState.from(machine);
        var json = MachineItemState.CODEC.encodeStart(JsonOps.INSTANCE, carried).getOrThrow();
        assertEquals(carried, MachineItemState.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        assertEquals(machine, carried.toMachineState());

        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
            Unpooled.buffer(), RegistryAccess.EMPTY
        );
        try {
            MachineItemState.STREAM_CODEC.encode(buffer, carried);
            assertEquals(carried, MachineItemState.STREAM_CODEC.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    void malformedAndTierOverflowStatesAreRejected() {
        assertTrue(MachineItemState.CODEC.parse(
            JsonOps.INSTANCE,
            JsonParser.parseString("""
                {"version":1,"tier":"COLLECTOR_MK1","stored":"-1"}
                """)
        ).error().isPresent());
        assertTrue(MachineItemState.CODEC.parse(
            JsonOps.INSTANCE,
            JsonParser.parseString("""
                {"version":1,"tier":"COLLECTOR_MK1","stored":"10001"}
                """)
        ).error().isPresent());
    }

    @Test
    void expansionTierUsesExistingSchemaWithoutLoss() {
        MachineState machine = new MachineState(
            MachineState.CURRENT_VERSION,
            MachineTier.POWER_FLOWER_FINAL,
            new EmcValue(new BigInteger("1234567890123456789")),
            new BigInteger("19"),
            new MachineAccess(Optional.empty(), false),
            MachineRedstoneMode.IGNORED
        );
        MachineItemState carried = MachineItemState.from(machine);
        var encoded = MachineItemState.CODEC.encodeStart(JsonOps.INSTANCE, carried).getOrThrow();
        assertEquals(carried, MachineItemState.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
        assertEquals(machine, carried.toMachineState());
    }
}
