package com.stripe.android.view;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.IdRes;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.stripe.android.R;
import com.stripe.android.model.Card;
import com.stripe.android.testharness.CardInputTestActivity;
import com.stripe.android.testharness.ViewTestUtils;

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
import static org.junit.Assert.assertNotEquals;
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
    private StripeEditText mExpiryEditText;
    private TestFocusChangeListener mOnGlobalFocusChangeListener;
    private StripeEditText mCvcEditText;
    private ImageView mIconView;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        ActivityController activityController =
                Robolectric.buildActivity(CardInputTestActivity.class)
                        .create().start().visible().resume();

        mOnGlobalFocusChangeListener = new TestFocusChangeListener();
        mCardNumberEditText =
                ((CardInputTestActivity) activityController.get()).getCardNumberEditText();
        mCardNumberEditText.setText("");
        mCardInputView = ((CardInputTestActivity) activityController.get()).getCardInputView();
        mCardInputView.getViewTreeObserver()
                .addOnGlobalFocusChangeListener(mOnGlobalFocusChangeListener);
        mExpiryEditText = (StripeEditText) mCardInputView.findViewById(R.id.et_expiry_date);
        mCvcEditText = (StripeEditText) mCardInputView.findViewById(R.id.et_cvc_number);
        mIconView = (ImageView) mCardInputView.findViewById(R.id.iv_card_icon);
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
        ViewTestUtils.sendDeleteKeyEvent(mExpiryEditText);
        assertEquals(R.id.et_expiry_date, mOnGlobalFocusChangeListener.getOldFocusId());
        assertEquals(R.id.et_card_number, mOnGlobalFocusChangeListener.getNewFocusId());
    }

    @Test
    public void onUpdateIcon_forCommonLengthBrand_callsSetImageResourceAndSetsLengthOnCvc() {
        // This should set the brand to Visa. Note that more extensive brand checking occurs
        // in CardNumberEditTextTest.
        Bitmap oldBitmap  = ((BitmapDrawable) mIconView.getDrawable()).getBitmap();
        mCardNumberEditText.append(Card.PREFIXES_VISA[0]);
        assertNotEquals(oldBitmap, ((BitmapDrawable) mIconView.getDrawable()).getBitmap());
        assertTrue(ViewTestUtils.hasMaxLength(mCvcEditText, 3));
    }

    @Test
    public void onUpdateText_forAmExPrefix_callsSetImageResourceAndSetsLengthOnCvc() {
        Bitmap oldBitmap  = ((BitmapDrawable) mIconView.getDrawable()).getBitmap();
        mCardNumberEditText.append(Card.PREFIXES_AMERICAN_EXPRESS[0]);
        assertNotEquals(oldBitmap, ((BitmapDrawable) mIconView.getDrawable()).getBitmap());
        assertTrue(ViewTestUtils.hasMaxLength(mCvcEditText, 4));
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
