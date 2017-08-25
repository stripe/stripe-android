package com.stripe.android.view;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Calendar;

import static org.junit.Assert.assertArrayEquals;
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
    public void convertTwoDigitYearToFour_whenDateIsEarlyCenturyAndYearIsLarge_addsLowerBase() {
        Calendar earlyCenturyCalendar = Calendar.getInstance();
        earlyCenturyCalendar.set(Calendar.YEAR, 2502);
        // In the year 2502, when you say "95", you probably mean 2495.
        assertEquals(DateUtils.convertTwoDigitYearToFour(95, earlyCenturyCalendar), 2495);

        // A more practical test
        earlyCenturyCalendar.set(Calendar.YEAR, 2017);
        assertEquals(DateUtils.convertTwoDigitYearToFour(99, earlyCenturyCalendar), 1999);
    }

    @Test
    public void convertTwoDigitYearToFour_whenDateIsMidCenturyAndYearIsLarge_addsNormalBase() {
        Calendar midCenturyCalendar = Calendar.getInstance();
        midCenturyCalendar.set(Calendar.YEAR, 3535);
        assertEquals(DateUtils.convertTwoDigitYearToFour(99, midCenturyCalendar), 3599);
    }

    @Test
    public void createDateStringFromIntegerInput_whenDateHasOneDigitMonthAndYear_addsZero() {
        assertEquals("0102", DateUtils.createDateStringFromIntegerInput(1, 2));
    }

    @Test
    public void createDateStringFromIntegerInput_whenDateHasTwoDigitValues_returnsExpectedValue() {
        assertEquals("1132", DateUtils.createDateStringFromIntegerInput(11, 32));
    }

    @Test
    public void createDateStringFromIntegerInput_whenDateHasFullYear_truncatesYear() {
        assertEquals("0132", DateUtils.createDateStringFromIntegerInput(1, 2032));
    }

    @Test
    public void createDateStringFromIntegerInput_whenDateHasThreeDigitYear_returnsEmpty() {
        assertEquals("", DateUtils.createDateStringFromIntegerInput(12, 101));
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

    @Test
    public void separateDateStringParts_withValidDate_properlySeparatesString() {
        String[] parts = DateUtils.separateDateStringParts("1234");
        String[] expected = {"12", "34"};

        assertArrayEquals(expected, parts);
    }

    @Test
    public void separateDateStringParts_withPartialDate_properlySeparatesString() {
        String[] parts = DateUtils.separateDateStringParts("123");
        String[] expected = {"12", "3"};

        assertArrayEquals(expected, parts);
    }

    @Test
    public void separateDateStringParts_withLessThanHalfOfDate_properlySeparatesString() {
        String[] parts = DateUtils.separateDateStringParts("1");
        String[] expected = {"1", ""};

        assertArrayEquals(expected, parts);
    }

    @Test
    public void separateDateStringParts_withEmptyInput_returnsNonNullEmptyOutput() {
        String[] parts = DateUtils.separateDateStringParts("");
        String[] expected = {"", ""};

        assertArrayEquals(expected, parts);
    }

    @Test
    public void isValidMonth_forProperMonths_returnsTrue() {
        String[] validMonths = {"01", "02", "03", "04", "05",
                "06", "07", "08", "09", "10", "11", "12"};
        for (int i = 0; i < validMonths.length; i++) {
            assertTrue(DateUtils.isValidMonth(validMonths[i]));
        }
    }

    @Test
    public void isValidMonth_forInvalidNumericInput_returnsFalse() {
        assertFalse(DateUtils.isValidMonth("15"));
        assertFalse(DateUtils.isValidMonth("0"));
        assertFalse(DateUtils.isValidMonth("-08"));
    }

    @Test
    public void isValidMonth_forNullInput_returnsFalse() {
        assertFalse(DateUtils.isValidMonth(null));
    }

    @Test
    public void isValidMonth_forNonNumericInput_returnsFalse() {
        assertFalse(DateUtils.isValidMonth("     "));
        assertFalse(DateUtils.isValidMonth("abc"));
        // This is looking for a valid numeric month, not month names.
        assertFalse(DateUtils.isValidMonth("January"));
        assertFalse(DateUtils.isValidMonth("\n"));
    }
}
