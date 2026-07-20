package io.github.tufkan1.projectex.client;

import java.math.BigInteger;

/** Compact, lossless-input EMC presentation for arbitrarily large server values. */
public final class EmcNumberFormatter {
    private static final BigInteger COMPACT_START = BigInteger.valueOf(1_000_000);
    private static final String[] SUFFIXES = {
        "", "K", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No", "Dc",
        "Ud", "Dd", "Td", "Qad", "Qid", "Sxd", "Spd", "Ocd", "Nod"
    };

    private EmcNumberFormatter() {
    }

    public static String format(String rawValue) {
        return format(new BigInteger(rawValue));
    }

    public static String format(BigInteger value) {
        if (value.abs().compareTo(COMPACT_START) < 0) return group(value);
        BigInteger magnitude = value.abs();
        int group = Math.min((magnitude.toString().length() - 1) / 3, SUFFIXES.length - 1);
        BigInteger divisor = BigInteger.TEN.pow(group * 3);
        BigInteger whole = magnitude.divide(divisor);
        int tenth = magnitude.remainder(divisor).multiply(BigInteger.TEN).divide(divisor).intValue();
        String sign = value.signum() < 0 ? "-" : "";
        return sign + whole + (tenth == 0 ? "" : "." + tenth) + SUFFIXES[group];
    }

    public static String group(BigInteger value) {
        String digits = value.abs().toString();
        StringBuilder result = new StringBuilder(digits.length() + digits.length() / 3);
        if (value.signum() < 0) result.append('-');
        int first = digits.length() % 3;
        if (first == 0) first = 3;
        result.append(digits, 0, first);
        for (int index = first; index < digits.length(); index += 3) {
            result.append(',').append(digits, index, index + 3);
        }
        return result.toString();
    }
}
