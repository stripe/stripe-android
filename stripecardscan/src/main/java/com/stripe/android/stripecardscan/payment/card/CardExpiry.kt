package com.stripe.android.stripecardscan.payment.card

import java.util.Calendar

/**
 * Determine if the given expiry is currently valid.
 *
 * Validity is determined by the following logic:
 * 1. Determine if the expiry year is after the current year. If after, return true.
 * 2. If the year is the same as the current year, determine if the month is after the current
 *    month. If after, return true.
 * 3. If the month is the same as the current month, determine if the day is after the current day.
 *    If after or the current day, return true.
 */
internal fun isValidExpiry(day: String?, month: String, year: String): Boolean {
    val calendar = Calendar.getInstance()

    val cardYear = getFourDigitYear(year, calendar)
    val currentYear = getCurrentYear(calendar)

    val currentMonth = calendar.get(Calendar.MONTH) + 1 // months are 0-based in calendar.
    val cardMonth = formatExpiryMonth(month).toIntOrNull() ?: 0
    if (!isValidMonth(cardMonth)) {
        return false
    }

    val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
    val cardDay = day?.toIntOrNull() ?: 31
    if (day != null && !isValidDay(cardDay, cardMonth, cardYear)) {
        return false
    }

    // according to https://stackoverflow.com/questions/2500588/maximum-year-in-expiry-date-of-credit-card,
    // it's possible to have expires up to 50 years from now.
    if (cardYear > currentYear && cardYear < currentYear + 100) {
        return true
    } else if (cardYear < currentYear) {
        return false
    }

    if (cardMonth > currentMonth) {
        return true
    } else if (cardMonth < currentMonth) {
        return false
    }

    return cardDay >= currentDay
}

/**
 * Format the card expiry as a human readable format.
 */
internal fun formatExpiry(day: String?, month: String, year: String): String {
    val formattedDay = if (day != null) "${formatExpiryDay(day)}/" else ""
    return "$formattedDay${formatExpiryMonth(month)}/${formatExpiryYear(year)}"
}

/**
 * Determine if a month string is valid.
 */
internal fun isValidMonth(month: String) = month.toIntOrNull()?.let { isValidMonth(it) } ?: false

/**
 * Determine if a month integer is valid.
 */
internal fun isValidMonth(month: Int) = month in 1..12

/**
 * Determine if the day
 */
private fun isValidDay(day: Int, month: Int, year: Int): Boolean {
    val calendar = Calendar.getInstance()
    calendar.set(year, month - 1, 1)
    return day >= 1 && day <= calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
}

/**
 * Convert the year string to four digits.
 */
private fun getFourDigitYear(year: String, calendar: Calendar): Int = year.digitsOnly().let {
    when {
        it.length == 4 -> it.toIntOrNull()
        it.length > 4 -> it.takeLast(4).toIntOrNull()
        else -> (getCurrentCentury(calendar) + it.padStart(2, '0').takeLast(2)).toIntOrNull()
    } ?: 0
}

/**
 * Get the current century
 */
private fun getCurrentCentury(calendar: Calendar): String =
    getCurrentYear(calendar).toString().take(2)

/**
 * Get the current year.
 */
private fun getCurrentYear(calendar: Calendar): Int = calendar.get(Calendar.YEAR)

/**
 * Format the expiry day. If the input is null, this returns null.
 */
private fun formatExpiryDay(day: String?) = day?.padStart(2, '0')?.take(2)

/**
 * Format the expiry month as a two-digit number.
 */
private fun formatExpiryMonth(month: String) = month.padStart(2, '0').take(2)

/**
 * Format the expiry year as a two-digit number.
 */
private fun formatExpiryYear(year: String) = year.padStart(2, '0').takeLast(2)

/**
 * Remove all non-digit characters from a string.
 */
private fun String?.digitsOnly(): String = (this?.filter { it.isDigit() } ?: "")
