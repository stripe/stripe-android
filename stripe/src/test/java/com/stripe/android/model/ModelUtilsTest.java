package com.stripe.android.model;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Calendar;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * Test class for {@link ModelUtils}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class ModelUtilsTest {

    @Test
    public void wholePositiveNumberShouldFailNull() {
        assertFalse(ModelUtils.isWholePositiveNumber(null));
    }

    @Test
    public void wholePositiveNumberShouldPassIfEmpty() {
        assertTrue(ModelUtils.isWholePositiveNumber(""));
    }

    @Test
    public void wholePositiveNumberShouldPass() {
        assertTrue(ModelUtils.isWholePositiveNumber("123"));
    }

    @Test
    public void wholePositiveNumberShouldPassWithLeadingZero() {
        assertTrue(ModelUtils.isWholePositiveNumber("000"));
    }

    @Test
    public void wholePositiveNumberShouldFailIfNegative() {
        assertFalse(ModelUtils.isWholePositiveNumber("-1"));
    }

    @Test
    public void wholePositiveNumberShouldFailIfLetters() {
        assertFalse(ModelUtils.isWholePositiveNumber("1a"));
    }

    @Test
    public void normalizeSameCenturyShouldPass() {
        Calendar now = Calendar.getInstance();
        int year = 1997;
        now.set(Calendar.YEAR, year);
        assertEquals(ModelUtils.normalizeYear(97, now), year);
    }

    @Test
    public void normalizeDifferentCenturyShouldFail() {
        Calendar now = Calendar.getInstance();
        int year = 1997;
        now.set(Calendar.YEAR, year);
        assertNotEquals(ModelUtils.normalizeYear(97, now), 2097);
    }
}
