package com.stripe.android.view

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.utils.DateUtils
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar
import kotlin.test.Test

/**
 * Test class for [DateUtils].
 */
@RunWith(RobolectricTestRunner::class)
class DateUtilsTest {

    @Test
    fun convertTwoDigitYearToFour_whenCurrentYearIsLessThanEighty_addsNormalBase() {
        assertThat(
            DateUtils.convertTwoDigitYearToFour(
                19,
                createCalendar(year = 2017)
            )
        ).isEqualTo(2019)
    }

    @Test
    fun convertTwoDigitYearToFour_whenDateIsNearCenturyButYearIsSmall_addsIncreasedBase() {
        assertThat(
            DateUtils.convertTwoDigitYearToFour(
                8,
                createCalendar(year = 2081)
            )
        ).isEqualTo(2108)
    }

    @Test
    fun convertTwoDigitYearToFour_whenDateIsNearCenturyAndYearIsLarge_addsNormalBase() {
        assertThat(
            DateUtils.convertTwoDigitYearToFour(
                95,
                createCalendar(year = 2088)
            )
        ).isEqualTo(2095)
    }

    @Test
    fun convertTwoDigitYearToFour_whenDateIsEarlyCenturyAndYearIsLarge_addsLowerBase() {
        // In the year 2502, when you say "95", you probably mean 2495.
        assertThat(
            DateUtils.convertTwoDigitYearToFour(
                95,
                createCalendar(year = 2502)
            )
        ).isEqualTo(2495)

        // A more practical test
        assertThat(
            DateUtils.convertTwoDigitYearToFour(
                99,
                createCalendar(year = 2017)
            )
        ).isEqualTo(1999)
    }

    @Test
    fun convertTwoDigitYearToFour_whenDateIsMidCenturyAndYearIsLarge_addsNormalBase() {
        assertThat(
            DateUtils.convertTwoDigitYearToFour(
                99,
                createCalendar(year = 3535)
            )
        ).isEqualTo(3599)
    }

    @Test
    fun isExpiryDataValid_whenDateIsAfterCalendarYear_returnsTrue() {
        assertThat(
            DateUtils.isExpiryDataValid(
                1,
                2019,
                createCalendar(year = 2018, month = Calendar.JANUARY)
            )
        ).isTrue()
    }

    @Test
    fun isExpiryDataValid_whenDateIsSameCalendarYearButLaterMonth_returnsTrue() {
        assertThat(
            DateUtils.isExpiryDataValid(
                2,
                2018,
                createCalendar(year = 2018, month = Calendar.JANUARY)
            )
        ).isTrue()
    }

    @Test
    fun isExpiryDataValid_whenDateIsSameCalendarYearAndMonth_returnsTrue() {
        assertThat(
            DateUtils.isExpiryDataValid(
                1,
                2018,
                createCalendar(year = 2018, month = Calendar.JANUARY)
            )
        ).isTrue()
    }

    @Test
    fun isExpiryDataValid_whenDateIsSameCalendarYearButEarlierMonth_returnsFalse() {
        val testCalendar = Calendar.getInstance().also {
            it.set(Calendar.YEAR, 2018)
            it.set(Calendar.MONTH, Calendar.MARCH)
        }

        assertThat(DateUtils.isExpiryDataValid(1, 2018, testCalendar))
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

    private companion object {
        fun createCalendar(
            year: Int? = null,
            month: Int? = null
        ): Calendar {
            return Calendar.getInstance().also { calendar ->
                if (year != null) {
                    calendar.set(Calendar.YEAR, year)
                }
                if (month != null) {
                    calendar.set(Calendar.MONTH, month)
                }
            }
        }
    }
}
