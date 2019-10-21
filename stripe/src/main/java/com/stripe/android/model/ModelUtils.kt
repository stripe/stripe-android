package com.stripe.android.model

import java.util.Calendar
import java.util.Locale

/**
 * Utilities function class for the models package.
 */
internal object ModelUtils {

    /**
     * Check to see whether the input string is a whole, positive number.
     *
     * @param value the input string to test
     * @return `true` if the input value consists entirely of integers
     */
    @JvmSynthetic
    internal fun isWholePositiveNumber(value: String?): Boolean {
        return value != null && isDigitsOnly(value)
    }

    /**
     * Returns whether the given CharSequence contains only digits.
     */
    private fun isDigitsOnly(str: CharSequence): Boolean {
        var i = 0
        while (i < str.length) {
            val cp = Character.codePointAt(str, i)
            if (!Character.isDigit(cp)) {
                return false
            }
            i += Character.charCount(cp)
        }
        return true
    }

    /**
     * Determines whether the input year-month pair has passed.
     *
     * @param year the input year, as a two or four-digit integer
     * @param month the input month
     * @param now the current time
     * @return `true` if the input time has passed the specified current time,
     * `false` otherwise.
     */
    @JvmSynthetic
    internal fun hasMonthPassed(year: Int, month: Int, now: Calendar): Boolean {
        return if (hasYearPassed(year, now)) {
            true
        } else {
            // Expires at end of specified month, Calendar month starts at 0
            normalizeYear(year, now) == now.get(Calendar.YEAR) &&
                month < now.get(Calendar.MONTH) + 1
        }
    }

    /**
     * Determines whether or not the input year has already passed.
     *
     * @param year the input year, as a two or four-digit integer
     * @param now, the current time
     * @return `true` if the input year has passed the year of the specified current time
     * `false` otherwise.
     */
    @JvmSynthetic
    internal fun hasYearPassed(year: Int, now: Calendar): Boolean {
        return normalizeYear(year, now) < now.get(Calendar.YEAR)
    }

    @JvmSynthetic
    internal fun normalizeYear(year: Int, now: Calendar): Int {
        return if (year in 0..99) {
            val currentYear = now.get(Calendar.YEAR).toString()
            val prefix = currentYear.substring(0, currentYear.length - 2)
            Integer.parseInt(String.format(Locale.US, "%s%02d", prefix, year))
        } else {
            year
        }
    }
}
