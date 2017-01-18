package com.stripe.android.view;

import android.support.annotation.IdRes;
import android.support.annotation.IntRange;
import android.support.annotation.LayoutRes;
import android.text.InputFilter;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;

import com.stripe.android.R;
import com.stripe.android.testharness.CardInputTestActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ActivityController;

import static com.stripe.android.testharness.CardInputTestActivity.VALID_VISA_WITH_SPACES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link CardInputView}. Note that we have to test against SDK 22
 * because of a <a href="https://github.com/robolectric/robolectric/issues/1932">known issue</a> in
 * Robolectric.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 22)
public class CardInputViewTest {

    private CardInputView mCardInputView;
    private CardNumberEditText mCardNumberEditText;
    private EditText mExpiryEditText;
    private TestFocusChangeListener mOnGlobalFocusChangeListener;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        ActivityController activityController =
                Robolectric.buildActivity(CardInputTestActivity.class).create().start().visible();

        mOnGlobalFocusChangeListener = new TestFocusChangeListener();
        mCardNumberEditText =
                ((CardInputTestActivity) activityController.get()).getCardNumberEditText();
        mCardNumberEditText.setText("");
        mCardInputView = ((CardInputTestActivity) activityController.get()).getCardInputView();
        mCardInputView.getViewTreeObserver()
                .addOnGlobalFocusChangeListener(mOnGlobalFocusChangeListener);
        mExpiryEditText = (EditText) mCardInputView.findViewById(R.id.et_expiry_date);
    }

    @Test
    public void onCompleteCardNumber_whenValid_shiftsFocusToExpiryDate() {
        mCardNumberEditText.setText(VALID_VISA_WITH_SPACES);
        assertEquals(R.id.et_card_number, mOnGlobalFocusChangeListener.getOldFocusId());
        assertEquals(R.id.et_expiry_date, mOnGlobalFocusChangeListener.getNewFocusId());
    }

    @Test
    public void onDeleteFromExpiryDate_whenEmpty_shiftsFocusToCardNumber() {
        mCardNumberEditText.setText(VALID_VISA_WITH_SPACES);
        mExpiryEditText.dispatchKeyEvent(
                new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL, 0));
        assertEquals(R.id.et_expiry_date, mOnGlobalFocusChangeListener.getOldFocusId());
        assertEquals(R.id.et_card_number, mOnGlobalFocusChangeListener.getNewFocusId());
    }

    class TestFocusChangeListener implements ViewTreeObserver.OnGlobalFocusChangeListener {
        View mOldFocus;
        View mNewFocus;

        @Override
        public void onGlobalFocusChanged(View oldFocus, View newFocus) {
            mOldFocus = oldFocus;
            mNewFocus = newFocus;
        }

        @IdRes
        int getOldFocusId() {
            return mOldFocus.getId();
        }

        @IdRes
        int getNewFocusId() {
            return mNewFocus.getId();
        }

        boolean hasOldFocus() {
            return mOldFocus != null;
        }

        boolean hasNewFocus() {
            return mNewFocus != null;
        }
    }

}
