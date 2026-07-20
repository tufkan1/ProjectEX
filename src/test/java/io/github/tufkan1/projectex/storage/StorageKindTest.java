package io.github.tufkan1.projectex.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class StorageKindTest {
    @Test
    void sourceLayoutsHaveExactCapacitiesWithoutPagination() {
        assertEquals(92, StorageKind.CONDENSER_MK1.inventorySlots());
        assertEquals(85, StorageKind.CONDENSER_MK2.inventorySlots());
        assertEquals(272, StorageKind.CONDENSER_MK3.inventorySlots());
        assertEquals(104, StorageKind.ALCHEMICAL_CHEST.inventorySlots());
        assertEquals(104, StorageKind.ADVANCED_ALCHEMICAL_CHEST.inventorySlots());
        assertEquals(104, StorageKind.ALCHEMICAL_BAG.inventorySlots());
    }

    @Test
    void condenserRangesAreBoundedAndMk1SharesItsStorageForOutput() {
        for (StorageKind kind : StorageKind.values()) {
            assertTrue(kind.inputStart() >= 0);
            assertTrue(kind.inputEnd() <= kind.inventorySlots());
            assertTrue(kind.outputStart() >= 0);
            assertTrue(kind.outputEnd() <= kind.inventorySlots());
        }
        assertTrue(StorageKind.CONDENSER_MK1.sharedOutput());
        assertEquals(StorageKind.CONDENSER_MK1.inputStart(),
            StorageKind.CONDENSER_MK1.outputStart());
        assertFalse(StorageKind.CONDENSER_MK2.sharedOutput());
        assertEquals(512, StorageKind.CONDENSER_MK3.inputBudget());
    }
}
