package io.github.tufkan1.projectex.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class StorageKindTest {
    @Test
    void condenserMk3PagesCoverEverySlotExactlyWithoutExposingTarget() {
        StorageKind kind = StorageKind.CONDENSER_MK3;
        boolean[] visited = new boolean[kind.inventorySlots()];

        for (int page = 0; page < kind.pageCount(); page++) {
            for (int visible = 0; visible < kind.pageSize(); visible++) {
                int slot = kind.storageSlot(page, visible);
                if (slot < 0) continue;
                assertFalse(visited[slot], "physical slot mapped more than once: " + slot);
                visited[slot] = true;
            }
        }

        assertFalse(visited[0], "target template was exposed in a paged inventory");
        for (int slot = kind.inputStart(); slot < kind.inventorySlots(); slot++) {
            assertTrue(visited[slot], "physical slot was not reachable: " + slot);
        }
        assertEquals(6, kind.pageCount());
        assertEquals(512, kind.inputBudget());
    }

    @Test
    void partialFinalPagesNeverMapBeyondTheInventory() {
        for (StorageKind kind : StorageKind.values()) {
            for (int page = 0; page < kind.pageCount(); page++) {
                for (int visible = 0; visible < kind.pageSize(); visible++) {
                    int slot = kind.storageSlot(page, visible);
                    assertTrue(slot == -1 || slot < kind.inventorySlots());
                }
            }
            assertEquals(-1, kind.storageSlot(kind.pageCount(), 0));
        }
    }
}
