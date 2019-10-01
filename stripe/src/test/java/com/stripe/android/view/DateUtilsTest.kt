package com.stripe.android.view

import java.util.Calendar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Test class for [DateUtils].
 */
@RunWith(RobolectricTestRunner::class)
class DateUtilsTest {

    @Test
    fun convertTwoDigitYearToFour_whenCurrentYearIsLessThanEighty_addsNormalBase() {
        val earlyCenturyCalendar = Calendar.getInstance()
        earlyCenturyCalendar.set(Calendar.YEAR, 2017)
        assertEquals(DateUtils.convertTwoDigitYearToFour(19, earlyCenturyCalendar), 2019)
    }

    @Test
    fun convertTwoDigitYearToFour_whenDateIsNearCenturyButYearIsSmall_addsIncreasedBase() {
        val lateCenturyCalendar = Calendar.getInstance()
        lateCenturyCalendar.set(Calendar.YEAR, 2081)
        assertEquals(DateUtils.convertTwoDigitYearToFour(8, lateCenturyCalendar), 2108)
    }

    @Test
    fun convertTwoDigitYearToFour_whenDateIsNearCenturyAndYearIsLarge_addsNormalBase() {
        val lateCenturyCalendar = Calendar.getInstance()
        lateCenturyCalendar.set(Calendar.YEAR, 2088)
        assertEquals(DateUtils.convertTwoDigitYearToFour(95, lateCenturyCalendar), 2095)
    }

    @Test
    fun convertTwoDigitYearToFour_whenDateIsEarlyCenturyAndYearIsLarge_addsLowerBase() {
        val earlyCenturyCalendar = Calendar.getInstance()
        earlyCenturyCalendar.set(Calendar.YEAR, 2502)
        // In the year 2502, when you say "95", you probably mean 2495.
        assertEquals(DateUtils.convertTwoDigitYearToFour(95, earlyCenturyCalendar), 2495)

        // A more practical test
        earlyCenturyCalendar.set(Calendar.YEAR, 2017)
        assertEquals(DateUtils.convertTwoDigitYearToFour(99, earlyCenturyCalendar), 1999)
    }

    @Test
    fun convertTwoDigitYearToFour_whenDateIsMidCenturyAndYearIsLarge_addsNormalBase() {
        val midCenturyCalendar = Calendar.getInstance()
        midCenturyCalendar.set(Calendar.YEAR, 3535)
        assertEquals(DateUtils.convertTwoDigitYearToFour(99, midCenturyCalendar), 3599)
    }

    @Test
    fun createDateStringFromIntegerInput_whenDateHasOneDigitMonthAndYear_addsZero() {
        assertEquals("0102", DateUtils.createDateStringFromIntegerInput(1, 2))
    }

    @Test
    fun createDateStringFromIntegerInput_whenDateHasTwoDigitValues_returnsExpectedValue() {
        assertEquals("1132", DateUtils.createDateStringFromIntegerInput(11, 32))
    }

    @Test
    fun createDateStringFromIntegerInput_whenDateHasFullYear_truncatesYear() {
        assertEquals("0132", DateUtils.createDateStringFromIntegerInput(1, 2032))
    }

    @Test
    fun createDateStringFromIntegerInput_whenDateHasThreeDigitYear_returnsEmpty() {
        assertEquals("", DateUtils.createDateStringFromIntegerInput(12, 101))
    }

    @Test
    fun isExpiryDataValid_whenDateIsAfterCalendarYear_returnsTrue() {
        val testCalendar = Calendar.getInstance()
        testCalendar.set(Calendar.YEAR, 2018)
        testCalendar.set(Calendar.MONTH, Calendar.JANUARY)

        assertTrue(DateUtils.isExpiryDataValid(1, 2019, testCalendar))
    }

    @Test
    fun isExpiryDataValid_whenDateIsSameCalendarYearButLaterMonth_returnsTrue() {
        val testCalendar = Calendar.getInstance()
        testCalendar.set(Calendar.YEAR, 2018)
        testCalendar.set(Calendar.MONTH, Calendar.JANUARY)

        assertTrue(DateUtils.isExpiryDataValid(2, 2018, testCalendar))
    }

    @Test
    fun isExpiryDataValid_whenDateIsSameCalendarYearAndMonth_returnsTrue() {
        val testCalendar = Calendar.getInstance()
        testCalendar.set(Calendar.YEAR, 2018)
        testCalendar.set(Calendar.MONTH, Calendar.JANUARY)

        assertTrue(DateUtils.isExpiryDataValid(1, 2018, testCalendar))
    }

    @Test
    fun isExpiryDataValid_whenDateIsSameCalendarYearButEarlierMonth_returnsFalse() {
        val testCalendar = Calendar.getInstance()
        testCalendar.set(Calendar.YEAR, 2018)
        testCalendar.set(Calendar.MONTH, Calendar.MARCH)

        assertFalse(DateUtils.isExpiryDataValid(1, 2018, testCalendar))
    }

    @Test
    fun isExpiryDataValid_whenMonthIsInvalid_returnsFalse() {
        assertFalse(DateUtils.isExpiryDataValid(15, 2019))
        assertFalse(DateUtils.isExpiryDataValid(-1, 2019))
    }

    @Test
    fun isExpiryDataValid_whenYearIsInvalid_returnsFalse() {
        assertFalse(DateUtils.isExpiryDataValid(5, -1))
        assertFalse(
            DateUtils.isExpiryDataValid(5, 9985),
            "Should not validate years beyond 9980"
        )
    }

    @Test
    fun separateDateStringParts_withValidDate_properlySeparatesString() {
        val parts = DateUtils.separateDateStringParts("1234")
        val expected = arrayOf("12", "34")
        assertTrue(expected.contentEquals(parts))
    }

    @Test
    fun separateDateStringParts_withPartialDate_properlySeparatesString() {
        val parts = DateUtils.separateDateStringParts("123")
        val expected = arrayOf("12", "3")
        assertTrue(expected.contentEquals(parts))
    }

    @Test
    fun separateDateStringParts_withLessThanHalfOfDate_properlySeparatesString() {
        val parts = DateUtils.separateDateStringParts("1")
        val expected = arrayOf("1", "")
        assertTrue(expected.contentEquals(parts))
    }

    @Test
    fun separateDateStringParts_withEmptyInput_returnsNonNullEmptyOutput() {
        val parts = DateUtils.separateDateStringParts("")
        val expected = arrayOf("", "")
        assertTrue(expected.contentEquals(parts))
    }

    @Test
    fun isValidMonth_forProperMonths_returnsTrue() {
        val validMonths = arrayOf("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12")
        for (validMonth in validMonths) {
            assertTrue(DateUtils.isValidMonth(validMonth))
        }
    }

    @Test
    fun isValidMonth_forInvalidNumericInput_returnsFalse() {
        assertFalse(DateUtils.isValidMonth("15"))
        assertFalse(DateUtils.isValidMonth("0"))
        assertFalse(DateUtils.isValidMonth("-08"))
    }

    @Test
    fun isValidMonth_forNullInput_returnsFalse() {
        assertFalse(DateUtils.isValidMonth(null))
    }

    @Test
    fun isValidMonth_forNonNumericInput_returnsFalse() {
        assertFalse(DateUtils.isValidMonth("     "))
        assertFalse(DateUtils.isValidMonth("abc"))
        // This is looking for a valid numeric month, not month names.
        assertFalse(DateUtils.isValidMonth("January"))
        assertFalse(DateUtils.isValidMonth("\n"))
    }
}
