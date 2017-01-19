package com.stripe.android.util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link DateUtils}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class DateUtilsTest {

    @Test
    public void convertTwoDigitYearToFour_whenCurrentYearIsLessThanEighty_addsNormalBase() {
        Calendar earlyCenturyCalendar = Calendar.getInstance();
        earlyCenturyCalendar.set(Calendar.YEAR, 2017);
        assertEquals(DateUtils.convertTwoDigitYearToFour(19, earlyCenturyCalendar), 2019);
    }

    @Test
    public void convertTwoDigitYearToFour_whenDateIsNearCenturyButYearIsSmall_addsIncreasedBase() {
        Calendar lateCenturyCalendar = Calendar.getInstance();
        lateCenturyCalendar.set(Calendar.YEAR, 2081);
        assertEquals(DateUtils.convertTwoDigitYearToFour(8, lateCenturyCalendar), 2108);
    }

    @Test
    public void convertTwoDigitYearToFour_whenDateIsNearCenturyAndYearIsLarge_addsNormalBase() {
        Calendar lateCenturyCalendar = Calendar.getInstance();
        lateCenturyCalendar.set(Calendar.YEAR, 2088);
        assertEquals(DateUtils.convertTwoDigitYearToFour(95, lateCenturyCalendar), 2095);
    }

    @Test
    public void convertTwoDigitYearToFour_whenDateIsEarlyCenturyAndYearIsLarge_addsNormalBase() {
        Calendar earlyCenturyCalendar = Calendar.getInstance();
        earlyCenturyCalendar.set(Calendar.YEAR, 2502);
        assertEquals(DateUtils.convertTwoDigitYearToFour(95, earlyCenturyCalendar), 2595);
    }

    @Test
    public void isExpiryDataValid_whenDateIsAfterCalendarYear_returnsTrue() {
        Calendar testCalendar = Calendar.getInstance();
        testCalendar.set(Calendar.YEAR, 2018);
        testCalendar.set(Calendar.MONTH, Calendar.JANUARY);

        assertTrue(DateUtils.isExpiryDataValid(1, 2019, testCalendar));
    }

    @Test
    public void isExpiryDataValid_whenDateIsSameCalendarYearButLaterMonth_returnsTrue() {
        Calendar testCalendar = Calendar.getInstance();
        testCalendar.set(Calendar.YEAR, 2018);
        testCalendar.set(Calendar.MONTH, Calendar.JANUARY);

        assertTrue(DateUtils.isExpiryDataValid(2, 2018, testCalendar));
    }

    @Test
    public void isExpiryDataValid_whenDateIsSameCalendarYearAndMonth_returnsTrue() {
        Calendar testCalendar = Calendar.getInstance();
        testCalendar.set(Calendar.YEAR, 2018);
        testCalendar.set(Calendar.MONTH, Calendar.JANUARY);

        assertTrue(DateUtils.isExpiryDataValid(1, 2018, testCalendar));
    }

    @Test
    public void isExpiryDataValid_whenDateIsSameCalendarYearButEarlierMonth_returnsFalse() {
        Calendar testCalendar = Calendar.getInstance();
        testCalendar.set(Calendar.YEAR, 2018);
        testCalendar.set(Calendar.MONTH, Calendar.MARCH);

        assertFalse(DateUtils.isExpiryDataValid(1, 2018, testCalendar));
    }

    @Test
    public void isExpiryDataValid_whenMonthIsInvalid_returnsFalse() {
        assertFalse(DateUtils.isExpiryDataValid(15, 2019));
        assertFalse(DateUtils.isExpiryDataValid(-1, 2019));
    }

    @Test
    public void isExpiryDataValid_whenYearIsInvalid_returnsFalse() {
        assertFalse(DateUtils.isExpiryDataValid(5, -1));
        assertFalse("Should not validate years beyond 9980", DateUtils.isExpiryDataValid(5, 9985));
    }
}
