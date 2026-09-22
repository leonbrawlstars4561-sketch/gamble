package de.gamble.plugin;

import java.util.Locale;

/**
 * Formats large whole-number amounts of money into short human readable
 * strings such as "50m", "250m" or "1b". Since amounts in this plugin
 * always come from a fixed step, no decimal places are ever produced.
 */
public final class MoneyFormatter {

    private static final long THOUSAND = 1_000L;
    private static final long MILLION = 1_000_000L;
    private static final long BILLION = 1_000_000_000L;

    private MoneyFormatter() {
    }

    /**
     * Formats a whole amount of money without any decimal places.
     *
     * @param amount the amount to format, must be a non-negative whole number
     * @return a short formatted string, e.g. "50m", "1b", "999k", "500"
     */
    public static String format(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative: " + amount);
        }

        if (amount >= BILLION) {
            return formatUnit(amount, BILLION, "b");
        } else if (amount >= MILLION) {
            return formatUnit(amount, MILLION, "m");
        } else if (amount >= THOUSAND) {
            return formatUnit(amount, THOUSAND, "k");
        } else {
            return Long.toString(amount);
        }
    }

    private static String formatUnit(long amount, long unit, String suffix) {
        long whole = amount / unit;
        long remainder = amount % unit;

        if (remainder == 0) {
            return whole + suffix;
        }

        // Fallback for values that are not an exact multiple of the unit.
        // This should not normally happen given the plugin's step-based
        // amount generation, but we guard against it to avoid ugly output.
        double value = amount / (double) unit;
        return String.format(Locale.US, "%.2f%s", value, suffix);
    }
}
