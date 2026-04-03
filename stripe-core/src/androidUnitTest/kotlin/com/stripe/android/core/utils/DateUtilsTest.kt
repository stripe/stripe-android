package com.stripe.android.core.utils

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

/**
 * Test class for [DateUtils].
 */
class DateUtilsTest {

    @Test
    fun convertTwoDigitYearToFour_whenCurrentYearIsLessThanEighty_addsNormalBase() {
        assertThat(
            DateUtils.convertTwoDigitYearToFour(
                inputYear = 19,
                currentYear = 2017
            )
        ).isEqualTo(2019)
    }

    @Test
    fun convertTwoDigitYearToFour_whenDateIsNearCenturyButYearIsSmall_addsIncreasedBase() {
        assertThat(
            DateUtils.convertTwoDigitYearToFour(
                inputYear = 8,
                currentYear = 2081
            )
        ).isEqualTo(2108)
    }

    @Test
    fun convertTwoDigitYearToFour_whenDateIsNearCenturyAndYearIsLarge_addsNormalBase() {
        assertThat(
            DateUtils.convertTwoDigitYearToFour(
                inputYear = 95,
                currentYear = 2088
            )
        ).isEqualTo(2095)
    }

    @Test
    fun convertTwoDigitYearToFour_whenDateIsEarlyCenturyAndYearIsLarge_addsLowerBase() {
        // In the year 2502, when you say "95", you probably mean 2495.
        assertThat(
            DateUtils.convertTwoDigitYearToFour(
                inputYear = 95,
                currentYear = 2502
            )
        ).isEqualTo(2495)

        // A more practical test
        assertThat(
            DateUtils.convertTwoDigitYearToFour(
                inputYear = 99,
                currentYear = 2017
            )
        ).isEqualTo(1999)
    }

    @Test
    fun convertTwoDigitYearToFour_whenDateIsMidCenturyAndYearIsLarge_addsNormalBase() {
        assertThat(
            DateUtils.convertTwoDigitYearToFour(
                inputYear = 99,
                currentYear = 3535
            )
        ).isEqualTo(3599)
    }

    @Test
    fun isExpiryDataValid_whenDateIsAfterCalendarYear_returnsTrue() {
        assertThat(
            DateUtils.isExpiryDataValid(
                expiryMonth = 1,
                expiryYear = 2019,
                currentYear = 2018,
                currentMonth = 1
            )
        ).isTrue()
    }

    @Test
    fun isExpiryDataValid_whenDateIsSameCalendarYearButLaterMonth_returnsTrue() {
        assertThat(
            DateUtils.isExpiryDataValid(
                expiryMonth = 2,
                expiryYear = 2018,
                currentYear = 2018,
                currentMonth = 1
            )
        ).isTrue()
    }

    @Test
    fun isExpiryDataValid_whenDateIsSameCalendarYearAndMonth_returnsTrue() {
        assertThat(
            DateUtils.isExpiryDataValid(
                expiryMonth = 1,
                expiryYear = 2018,
                currentYear = 2018,
                currentMonth = 1
            )
        ).isTrue()
    }

    @Test
    fun isExpiryDataValid_whenDateIsSameCalendarYearButEarlierMonth_returnsFalse() {
        assertThat(DateUtils.isExpiryDataValid(expiryMonth = 1, expiryYear = 2018, currentYear = 2018, currentMonth = 3))
            .isFalse()
    }

    @Test
    fun isExpiryDataValid_whenMonthIsInvalid_returnsFalse() {
        assertThat(DateUtils.isExpiryDataValid(15, 2019))
            .isFalse()
        assertThat(DateUtils.isExpiryDataValid(-1, 2019))
            .isFalse()
    }

    @Test
    fun isExpiryDataValid_whenYearIsInvalid_returnsFalse() {
        assertThat(DateUtils.isExpiryDataValid(5, -1))
            .isFalse()

        // Should not validate years beyond 9980
        assertThat(
            DateUtils.isExpiryDataValid(5, 9985)
        ).isFalse()
    }
}
