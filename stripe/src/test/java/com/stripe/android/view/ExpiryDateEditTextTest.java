package com.stripe.android.view;

import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Test class for {@link ExpiryDateEditText}.Note that we have to test against SDK 22
 * because of a <a href="https://github.com/robolectric/robolectric/issues/1932">known issue</a> in
 * Robolectric.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 22)
public class ExpiryDateEditTextTest {

    private ExpiryDateEditText mExpiryDateEditText;

}
