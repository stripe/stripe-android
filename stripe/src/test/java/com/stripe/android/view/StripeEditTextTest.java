package com.stripe.android.view;

import android.content.Context;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

import androidx.test.core.app.ApplicationProvider;

import com.stripe.android.R;
import com.stripe.android.testharness.ViewTestUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Test class for {@link StripeEditText}.
 */
@RunWith(RobolectricTestRunner.class)
public class StripeEditTextTest {

    @Mock private StripeEditText.AfterTextChangedListener mAfterTextChangedListener;
    @Mock private StripeEditText.DeleteEmptyListener mDeleteEmptyListener;
    private StripeEditText mEditText;

    @NonNull private final Context mContext;

    public StripeEditTextTest() {
        mContext = ApplicationProvider.getApplicationContext();
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        // Note that the CVC EditText is a StripeEditText
        mEditText = new StripeEditText(mContext);
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
    public void getDefaultErrorColorInt_onDarkTheme_returnsDarkError() {
        mEditText.setTextColor(mContext.getResources()
                .getColor(android.R.color.primary_text_dark));
        @ColorInt int colorInt = mEditText.getDefaultErrorColorInt();
        @ColorInt int expectedErrorInt = ContextCompat.getColor(mContext,
                R.color.error_text_dark_theme);
        assertEquals(expectedErrorInt, colorInt);
    }

    @Test
    public void getDefaultErrorColorInt_onLightTheme_returnsLightError() {
        mEditText.setTextColor(mContext.getResources()
                .getColor(android.R.color.primary_text_light));
        @ColorInt int colorInt = mEditText.getDefaultErrorColorInt();
        @ColorInt int expectedErrorInt = ContextCompat.getColor(mContext,
                R.color.error_text_light_theme);
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

    @Test
    public void getCachedColorStateList_afterInit_returnsNotNull() {
        assertNotNull(mEditText.getCachedColorStateList());
    }

    @Test
    public void setShouldShowError_whenErrorColorNotSet_shouldUseDefaultErrorColor() {
        mEditText.setShouldShowError(true);
        assertEquals(ContextCompat.getColor(mContext, R.color.error_text_light_theme),
                mEditText.getTextColors().getDefaultColor());
    }
}
