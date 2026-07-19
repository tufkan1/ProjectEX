package io.github.tufkan1.projectex.api.emc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import org.junit.jupiter.api.Test;

class EmcValueTest {
    @Test
    void usesArbitraryPrecision() {
        EmcValue huge = new EmcValue(BigInteger.TEN.pow(100));
        assertEquals(BigInteger.TEN.pow(100).multiply(BigInteger.valueOf(2)), huge.multiply(2).amount());
    }

    @Test
    void rejectsNegativeValues() {
        assertThrows(IllegalArgumentException.class, () -> EmcValue.of(-1));
        assertThrows(IllegalArgumentException.class, () -> EmcValue.of(1).multiply(-1));
    }
}
