package com.stripe.android.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.stripe.android.time.FrozenClock;
import com.stripe.android.util.CardExpiry;

public class CardExpiryTest {
    @Before
    public void setup() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 1997);
        cal.set(Calendar.MONTH, Calendar.AUGUST);
        cal.set(Calendar.DAY_OF_MONTH, 29);
        FrozenClock.freeze(cal);
    }

    @After
    public void teardown() {
        FrozenClock.unfreeze();
    }

    @Test
    public void emptyConstructorShouldBeInvalid() {
        CardExpiry cardExpiry = new CardExpiry();
        assertTrue(cardExpiry.isPartiallyValid());
        assertFalse(cardExpiry.isValid());
    }

    @Test
    public void emptyStringShouldBeInvalid() {
        CardExpiry cardExpiry = new CardExpiry("");
        assertTrue(cardExpiry.isPartiallyValid());
        assertFalse(cardExpiry.isValid());
    }

    @Test
    public void futureDateShouldBeValid() {
        CardExpiry cardExpiry = new CardExpiry("12/99");
        assertTrue(cardExpiry.isPartiallyValid());
        assertTrue(cardExpiry.isValid());
        assertEquals(12, cardExpiry.getMonth());
        assertEquals(1999, cardExpiry.getYear());
        assertEquals("12/99", cardExpiry.toString());
    }

    @Test
    public void pastDateShouldBeInValid() {
        CardExpiry cardExpiry = new CardExpiry("01/01");
        assertFalse(cardExpiry.isPartiallyValid());
        assertFalse(cardExpiry.isValid());
        assertEquals(1, cardExpiry.getMonth());
        assertEquals(1901, cardExpiry.getYear());
    }

    @Test
    public void monthOf0ShouldBeInvalid() {
        CardExpiry cardExpiry = new CardExpiry("0");
        assertTrue(cardExpiry.isPartiallyValid());
        assertFalse(cardExpiry.isValid());
    }

    @Test
    public void monthOf13ShouldBeInvalid() {
        CardExpiry cardExpiry = new CardExpiry("13");
        assertFalse(cardExpiry.isPartiallyValid());
        assertFalse(cardExpiry.isValid());
    }

    @Test
    public void monthsFrom1To12ShouldBePartiallyValid() {
        for (int i = 1; i <= 12; ++i) {
            String str = String.valueOf(i);
            CardExpiry cardExpiry = new CardExpiry(str);
            assertTrue(cardExpiry.isPartiallyValid());
            assertFalse(cardExpiry.isValid());
            assertEquals(i, cardExpiry.getMonth());
            assertEquals(0, cardExpiry.getYear());
        }
    }

    @Test
    public void monthsFrom01To09ShouldBePartiallyValid() {
        for (int i = 1; i <= 9; ++i) {
            String str = "0" + String.valueOf(i);
            CardExpiry cardExpiry = new CardExpiry(str);
            assertTrue(cardExpiry.isPartiallyValid());
            assertFalse(cardExpiry.isValid());
            assertEquals(i, cardExpiry.getMonth());
            assertEquals(0, cardExpiry.getYear());
        }
    }

    @Test
    public void zeroShouldBePartiallyValid() {;
        CardExpiry cardExpiry = new CardExpiry("0");
        assertTrue(cardExpiry.isPartiallyValid());
        assertFalse(cardExpiry.isValid());
        assertEquals("0", cardExpiry.toString());
        assertEquals("0", cardExpiry.toStringWithTrail());
    }

    @Test
    public void oneShouldBePartiallyValid() {;
        CardExpiry cardExpiry = new CardExpiry("1");
        assertTrue(cardExpiry.isPartiallyValid());
        assertFalse(cardExpiry.isValid());
        assertEquals("1", cardExpiry.toString());
        assertEquals("1", cardExpiry.toStringWithTrail());
    }

    @Test
    public void singleDigitMonthShouldBeZeroPadded() {
        for (int i = 2; i <= 9; ++i) {
            String str = String.valueOf(i);
            CardExpiry cardExpiry = new CardExpiry(str);
            assertTrue(cardExpiry.isPartiallyValid());
            assertFalse(cardExpiry.isValid());
            assertEquals("0" + str, cardExpiry.toString());
            assertEquals("0" + str + "/", cardExpiry.toStringWithTrail());
        }
    }

    @Test
    public void doubleDigitMonthShouldNotBePadded() {
        for (int i = 10; i <= 12; ++i) {
            String str = String.valueOf(i);
            CardExpiry cardExpiry = new CardExpiry(str);
            assertTrue(cardExpiry.isPartiallyValid());
            assertFalse(cardExpiry.isValid());
            assertEquals(str, cardExpiry.toString());
            assertEquals(str + "/", cardExpiry.toStringWithTrail());
        }
    }

    @Test
    public void trailingSlashInRangeShouldBePartiallyValid() {
        CardExpiry cardExpiry = new CardExpiry("01/");
        assertTrue(cardExpiry.isPartiallyValid());
        assertFalse(cardExpiry.isValid());
        assertEquals("01", cardExpiry.toString());
        assertEquals("01/", cardExpiry.toStringWithTrail());
    }

    @Test
    public void trailingSlashOutOfRangeShouldBePartiallyInvalid() {
        CardExpiry cardExpiry = new CardExpiry("15/");
        assertFalse(cardExpiry.isPartiallyValid());
        assertFalse(cardExpiry.isValid());
        assertEquals("15", cardExpiry.toString());
        assertEquals("15/", cardExpiry.toStringWithTrail());
    }

    @Test
    public void trailingDoubleSlashShouldBeInvalid() {
        CardExpiry cardExpiry = new CardExpiry("12//");
        assertFalse(cardExpiry.isPartiallyValid());
        assertFalse(cardExpiry.isValid());
    }

    @Test
    public void lastMonthShouldBeInvalid() {
        CardExpiry cardExpiry = new CardExpiry("7/97");
        assertFalse(cardExpiry.isPartiallyValid());
        assertFalse(cardExpiry.isValid());
        assertEquals("07/97", cardExpiry.toString());
    }

    @Test
    public void currentMonthShouldBeValid() {
        CardExpiry cardExpiry = new CardExpiry("8/1997");
        assertTrue(cardExpiry.isPartiallyValid());
        assertTrue(cardExpiry.isValid());
        assertEquals("08/1997", cardExpiry.toString());
    }

    @Test
    public void nextMonthShouldBeInvalid() {
        CardExpiry cardExpiry = new CardExpiry("9/97");
        assertTrue(cardExpiry.isPartiallyValid());
        assertTrue(cardExpiry.isValid());
        assertEquals("09/97", cardExpiry.toString());
    }
}
