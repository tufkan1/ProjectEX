package io.github.tufkan1.projectex.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import org.junit.jupiter.api.Test;

final class EmcNumberFormatterTest {
    @Test
    void keepsSmallValuesExactAndGroupsThem() {
        assertEquals("999,999", EmcNumberFormatter.format(BigInteger.valueOf(999_999)));
        assertEquals("74,488,428", EmcNumberFormatter.group(BigInteger.valueOf(74_488_428)));
    }

    @Test
    void compactsMillionsWithoutRoundingTheDisplayedValueUp() {
        assertEquals("1M", EmcNumberFormatter.format(BigInteger.valueOf(1_000_000)));
        assertEquals("74.4M", EmcNumberFormatter.format(BigInteger.valueOf(74_488_428)));
        assertEquals("2.6B", EmcNumberFormatter.format(BigInteger.valueOf(2_699_999_999L)));
    }

    @Test
    void supportsValuesBeyondLongRange() {
        assertEquals("3.5Qi", EmcNumberFormatter.format(new BigInteger("3518437208883200000")));
    }
}
