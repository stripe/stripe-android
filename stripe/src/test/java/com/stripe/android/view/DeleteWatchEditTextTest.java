package com.stripe.android.view;

import com.stripe.android.testharness.CardInputTestActivity;
import com.stripe.android.testharness.ViewTestUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ActivityController;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Test class for {@link DeleteWatchEditText}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 22)
public class DeleteWatchEditTextTest {

    @Mock DeleteWatchEditText.DeleteEmptyListener mDeleteEmptyListener;
    private DeleteWatchEditText mEditText;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        ActivityController activityController =
                Robolectric.buildActivity(CardInputTestActivity.class).create().start();

        // Note that the CVC EditText is a DeleteWatchEditText
        mEditText = ((CardInputTestActivity) activityController.get()).getCvcEditText();
        mEditText.setText("");
        mEditText.setDeleteEmptyListener(mDeleteEmptyListener);
    }

    @Test
    public void deleteText_whenZeroLength_callsListener() {
        ViewTestUtils.sendDeleteKeyEvent(mEditText);
        verify(mDeleteEmptyListener, times(1)).onDeleteEmpty();
    }

    @Test
    public void addText_doesNotCallListener() {
        mEditText.append("1");
        verifyZeroInteractions(mDeleteEmptyListener);
    }

    @Test
    public void deleteText_whenNonZeroLength_doesNotCallListener() {
        mEditText.append("1");
        ViewTestUtils.sendDeleteKeyEvent(mEditText);
        verifyZeroInteractions(mDeleteEmptyListener);
    }

    @Test
    public void deleteText_whenSelectionAtBeginningButLengthNonZero_doesNotCallListener() {
        mEditText.append("12");
        mEditText.setSelection(0);
        ViewTestUtils.sendDeleteKeyEvent(mEditText);
        verifyZeroInteractions(mDeleteEmptyListener);
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
}
