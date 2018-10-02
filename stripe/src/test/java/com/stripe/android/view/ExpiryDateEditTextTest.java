package com.stripe.android.view;

import com.stripe.android.BuildConfig;
import com.stripe.android.testharness.ViewTestUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Test class for {@link ExpiryDateEditText}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 25)
public class ExpiryDateEditTextTest {

    @Mock ExpiryDateEditText.ExpiryDateEditListener mExpiryDateEditListener;
    private ExpiryDateEditText mExpiryDateEditText;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        ActivityController<CardInputTestActivity> activityController =
                Robolectric.buildActivity(CardInputTestActivity.class).create().start();

        mExpiryDateEditText = activityController.get().getExpiryDateEditText();
        mExpiryDateEditText.setText("");
        mExpiryDateEditText.setExpiryDateEditListener(mExpiryDateEditListener);
    }

    @Test
    public void afterInputTwoDigits_textContainsSlash() {
        mExpiryDateEditText.append("1");
        mExpiryDateEditText.append("2");

        String text = mExpiryDateEditText.getText().toString();
        assertEquals("12/", text);
    }

    @Test
    public void inputSingleDigit_whenDigitIsTooLargeForMonth_prependsZero() {
        mExpiryDateEditText.append("4");
        assertEquals("04/", mExpiryDateEditText.getText().toString());
        assertEquals(3, mExpiryDateEditText.getSelectionStart());
    }

    @Test
    public void inputSingleDigit_whenDigitIsZeroOrOne_doesNotPrependZero() {
        mExpiryDateEditText.append("1");
        assertEquals("1", mExpiryDateEditText.getText().toString());
        assertEquals(1, mExpiryDateEditText.getSelectionStart());
    }

    @Test
    public void inputSingleDigit_whenAtFirstCharacterButTextNotEmpty_doesNotPrependZero() {
        mExpiryDateEditText.append("1");
        // This case can occur when the user moves the cursor behind the already-entered text.
        mExpiryDateEditText.setSelection(0);
        mExpiryDateEditText.getEditableText().replace(0, 0, "3", 0, 1);

        assertEquals("31", mExpiryDateEditText.getText().toString());
        assertEquals(1, mExpiryDateEditText.getSelectionStart());
    }

    @Test
    public void inputMultipleValidDigits_whenEmpty_doesNotPrependZeroOrShowErrorState() {
        mExpiryDateEditText.append("11");

        String text = mExpiryDateEditText.getText().toString();
        assertEquals("11/", text);
        assertFalse(mExpiryDateEditText.getShouldShowError());
        assertEquals(3, mExpiryDateEditText.getSelectionStart());
    }

    @Test
    public void afterInputThreeDigits_whenDeletingOne_textDoesNotContainSlash() {
        mExpiryDateEditText.append("1");
        mExpiryDateEditText.append("2");
        mExpiryDateEditText.append("3");
        ViewTestUtils.sendDeleteKeyEvent(mExpiryDateEditText);
        String text = mExpiryDateEditText.getText().toString();
        assertEquals("12", text);
    }

    @Test
    public void afterAddingFinalDigit_whenGoingFromInvalidToValid_callsListener() {
        if (Calendar.getInstance().get(Calendar.YEAR) > 2059) {
            fail("Update the code with a date that is still valid. Also, hello from the past.");
        }

        mExpiryDateEditText.append("1");
        mExpiryDateEditText.append("2");
        mExpiryDateEditText.append("5");
        verifyZeroInteractions(mExpiryDateEditListener);

        mExpiryDateEditText.append("9");
        assertTrue(mExpiryDateEditText.isDateValid());
        verify(mExpiryDateEditListener, times(1)).onExpiryDateComplete();
    }

    @Test
    public void afterAddingFinalDigit_whenDeletingItem_revertsToInvalidState() {
        if (Calendar.getInstance().get(Calendar.YEAR) > 2059) {
            fail("Update the code with a date that is still valid. Also, hello from the past.");
        }

        mExpiryDateEditText.append("12");
        mExpiryDateEditText.append("59");
        assertTrue(mExpiryDateEditText.isDateValid());

        ViewTestUtils.sendDeleteKeyEvent(mExpiryDateEditText);
        verify(mExpiryDateEditListener, times(1)).onExpiryDateComplete();
        assertFalse(mExpiryDateEditText.isDateValid());
    }

    @Test
    public void updateSelectionIndex_whenMovingAcrossTheGap_movesToEnd() {
        assertEquals(3, mExpiryDateEditText.updateSelectionIndex(3, 1, 1, 5));
    }

    @Test
    public void updateSelectionIndex_atStart_onlyMovesForwardByOne() {
        assertEquals(1, mExpiryDateEditText.updateSelectionIndex(1, 0, 1, 5));
    }

    @Test
    public void updateSelectionIndex_whenDeletingAcrossTheGap_staysAtEnd() {
        assertEquals(2, mExpiryDateEditText.updateSelectionIndex(2, 4, 0, 5));
    }

    @Test
    public void updateSelectionIndex_whenInputVeryLong_respectMaxInputLength() {
        assertEquals(5, mExpiryDateEditText.updateSelectionIndex(6, 4, 2, 5));
    }

    @Test
    public void inputZero_whenEmpty_doesNotShowErrorState() {
        mExpiryDateEditText.append("0");

        assertFalse(mExpiryDateEditText.getShouldShowError());
        assertEquals("0", mExpiryDateEditText.getText().toString());
    }

    @Test
    public void inputOne_whenEmpty_doesNotShowErrorState() {
        mExpiryDateEditText.append("1");

        assertFalse(mExpiryDateEditText.getShouldShowError());
        assertEquals("1", mExpiryDateEditText.getText().toString());
    }

    @Test
    public void inputTwoDigitMonth_whenInvalid_showsErrorAndDoesNotAddSlash() {
        mExpiryDateEditText.append("14");

        assertTrue(mExpiryDateEditText.getShouldShowError());
        assertEquals("14", mExpiryDateEditText.getText().toString());
    }

    @Test
    public void inputThreeDigits_whenInvalid_showsErrorAndDoesAddSlash() {
        mExpiryDateEditText.append("143");

        assertTrue(mExpiryDateEditText.getShouldShowError());
        assertEquals("14/3", mExpiryDateEditText.getText().toString());
    }

    @Test
    public void delete_whenAcrossSeparator_alwaysDeletesNumber() {
        mExpiryDateEditText.append("12");
        assertEquals("12/", mExpiryDateEditText.getText().toString());

        ViewTestUtils.sendDeleteKeyEvent(mExpiryDateEditText);
        assertEquals("1", mExpiryDateEditText.getText().toString());
    }

    @Test
    public void inputCompleteDate_whenInPast_showsInvalid() {
        // This test will be invalid if run between 2080 and 2112. Please update the code.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2080);
        // Date translates to December, 2012 UNTIL the 2080, at which point it translates to
        // December, 2112. Also, this simulates a PASTE action.
        mExpiryDateEditText.append("1212");

        assertTrue(mExpiryDateEditText.getShouldShowError());
        verifyZeroInteractions(mExpiryDateEditListener);
    }

    @Test
    public void inputCompleteDateInPast_thenDelete_showsValid() {
        // This test will be invalid if run between 2080 and 2112. Please update the code.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2080);
        // Date translates to December, 2012 UNTIL the 2080, at which point it translates to
        // December, 2112.
        mExpiryDateEditText.append("12/12");
        assertTrue(mExpiryDateEditText.getShouldShowError());

        ViewTestUtils.sendDeleteKeyEvent(mExpiryDateEditText);
        assertEquals("12/1", mExpiryDateEditText.getText().toString());
        assertFalse(mExpiryDateEditText.getShouldShowError());

        // The date is no longer "in error", but it still shouldn't have triggered the listener.
        verifyZeroInteractions(mExpiryDateEditListener);
    }

    @Test
    public void getValidDateFields_whenDataIsValid_returnsExpectedValues() {
        // This test will be invalid if run after the year 2050. Please update the code.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050);

        mExpiryDateEditText.append("12");
        mExpiryDateEditText.append("50");

        int [] retrievedDate = mExpiryDateEditText.getValidDateFields();
        assertNotNull(retrievedDate);
        assertEquals(12, retrievedDate[0]);
        assertEquals(2050, retrievedDate[1]);
    }

    @Test
    public void getValidDateFields_whenDateIsValidFormatButExpired_returnsNull() {
        // This test will be invalid if run after the year 2050. Please update the code.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2080);

        mExpiryDateEditText.append("12");
        mExpiryDateEditText.append("12");
        // 12/12 is an invalid date until 2080, at which point it will be interpreted as 12/2112

        assertNull(mExpiryDateEditText.getValidDateFields());
    }

    @Test
    public void getValidDateFields_whenDateIsIncomplete_returnsNull() {
        mExpiryDateEditText.append("4");
        assertNull(mExpiryDateEditText.getValidDateFields());
    }

    @Test
    public void getValidDateFields_whenDateIsValidAndThenChangedToInvalid_returnsNull() {
        // This test will be invalid if run after the year 2050. Please update the code.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050);

        mExpiryDateEditText.append("12");
        mExpiryDateEditText.append("50");
        ViewTestUtils.sendDeleteKeyEvent(mExpiryDateEditText);

        assertNull(mExpiryDateEditText.getValidDateFields());
    }
}
