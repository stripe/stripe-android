package com.stripe.android.view;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link StringUtils}
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 25)
public class StringUtilsTest {

    @Test
    public void isEmptyOrNull_whenNull_returnFalse() {
        assertTrue(StringUtils.isNullOrEmpty(null));
    }

    @Test
    public void isEmptyOrNull_whenEmpty_returnFalse() {
        assertTrue(StringUtils.isNullOrEmpty(""));
    }

    @Test
    public void isEmptyOrNull_whenNotEmpty_returnTrue() {
        assertFalse(StringUtils.isNullOrEmpty("Test String"));
    }
}
