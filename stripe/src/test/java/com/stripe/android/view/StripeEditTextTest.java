package com.stripe.android.view;

import android.graphics.Color;
import android.support.annotation.ColorInt;

import com.stripe.android.BuildConfig;
import com.stripe.android.R;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Test class for {@link StripeEditText}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 25)
public class StripeEditTextTest {

    @Mock StripeEditText.AfterTextChangedListener mAfterTextChangedListener;
    @Mock StripeEditText.DeleteEmptyListener mDeleteEmptyListener;
    private ActivityController<CardInputTestActivity> mActivityController;
    private StripeEditText mEditText;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mActivityController = Robolectric.buildActivity(CardInputTestActivity.class).create().start();

        // Note that the CVC EditText is a StripeEditText
        mEditText =  mActivityController.get().getCvcEditText();
        mEditText.setText("");
        mEditText.setDeleteEmptyListener(mDeleteEmptyListener);
        mEditText.setAfterTextChangedListener(mAfterTextChangedListener);
    }

    @Test
    public void deleteText_whenZeroLength_callsDeleteListener() {
        ViewTestUtils.sendDeleteKeyEvent(mEditText);
        verify(mDeleteEmptyListener, times(1)).onDeleteEmpty();
        verifyZeroInteractions(mAfterTextChangedListener);
    }

    @Test
    public void addText_callsAppropriateListeners() {
        mEditText.append("1");
        verifyZeroInteractions(mDeleteEmptyListener);
        verify(mAfterTextChangedListener, times(1)).onTextChanged("1");
    }

    @Test
    public void deleteText_whenNonZeroLength_callsAppropriateListeners() {
        mEditText.append("1");
        reset(mAfterTextChangedListener);

        ViewTestUtils.sendDeleteKeyEvent(mEditText);
        verifyZeroInteractions(mDeleteEmptyListener);
        verify(mAfterTextChangedListener, times(1)).onTextChanged("");
    }

    @Test
    public void deleteText_whenSelectionAtBeginningButLengthNonZero_doesNotCallListener() {
        mEditText.append("12");
        verify(mAfterTextChangedListener).onTextChanged("12");
        mEditText.setSelection(0);
        ViewTestUtils.sendDeleteKeyEvent(mEditText);
        verifyZeroInteractions(mDeleteEmptyListener);
        verifyNoMoreInteractions(mAfterTextChangedListener);
    }

    @Test
    public void deleteText_whenDeletingMultipleItems_onlyCallsListenerOneTime() {
        mEditText.append("123");
        // Doing this four times because we need to delete all three items, then jump back.
        for (int i = 0; i < 4; i++) {
            ViewTestUtils.sendDeleteKeyEvent(mEditText);
        }

        verify(mDeleteEmptyListener, times(1)).onDeleteEmpty();
    }

    @Test
    @SuppressWarnings("deprecation")
    public void getDefaultErrorColorInt_onDarkTheme_returnsDarkError() {
        mEditText.setTextColor(
                mActivityController.get().getResources()
                        .getColor(android.R.color.primary_text_dark));
        @ColorInt int colorInt = mEditText.getDefaultErrorColorInt();
        @ColorInt int expectedErrorInt =
                mActivityController.get().getResources().getColor(R.color.error_text_dark_theme);
        assertEquals(expectedErrorInt, colorInt);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void getDefaultErrorColorInt_onLightTheme_returnsLightError() {
        mEditText.setTextColor(
                mActivityController.get().getResources()
                        .getColor(android.R.color.primary_text_light));
        @ColorInt int colorInt = mEditText.getDefaultErrorColorInt();
        @ColorInt int expectedErrorInt =
                mActivityController.get().getResources().getColor(R.color.error_text_light_theme);
        assertEquals(expectedErrorInt, colorInt);
    }

    @Test
    public void setErrorColor_whenInError_overridesDefault() {
        // By default, the text color in this test activity is a light theme
        @ColorInt int blueError = 0x0000ff;
        mEditText.setErrorColor(blueError);
        mEditText.setShouldShowError(true);
        int currentColorInt = mEditText.getTextColors().getDefaultColor();
        assertEquals(blueError, currentColorInt);
    }
}
