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

import java.util.Calendar;

import static com.stripe.android.testharness.CardInputTestActivity.VALID_AMEX_NO_SPACES;
import static com.stripe.android.testharness.CardInputTestActivity.VALID_AMEX_WITH_SPACES;
import static com.stripe.android.testharness.CardInputTestActivity.VALID_DINERS_CLUB_NO_SPACES;
import static com.stripe.android.testharness.CardInputTestActivity.VALID_DINERS_CLUB_WITH_SPACES;
import static com.stripe.android.testharness.CardInputTestActivity.VALID_VISA_NO_SPACES;
import static com.stripe.android.testharness.CardInputTestActivity.VALID_VISA_WITH_SPACES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
    public void getCard_whenInputIsValidVisa_returnsCardObject() {
        // The input date here will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050);

        mCardNumberEditText.setText(VALID_VISA_WITH_SPACES);
        mExpiryEditText.append("12");
        mExpiryEditText.append("50");
        mCvcEditText.append("123");

        Card card = mCardInputView.getCard();
        assertNotNull(card);
        assertEquals(VALID_VISA_NO_SPACES, card.getNumber());
        assertNotNull(card.getExpMonth());
        assertNotNull(card.getExpYear());
        assertEquals(12, card.getExpMonth().intValue());
        assertEquals(2050, card.getExpYear().intValue());
        assertEquals("123", card.getCVC());
        assertTrue(card.validateCard());
    }

    @Test
    public void getCard_whenInputIsValidAmEx_returnsCardObject() {
        // The input date here will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050);

        mCardNumberEditText.setText(VALID_AMEX_WITH_SPACES);
        mExpiryEditText.append("12");
        mExpiryEditText.append("50");
        mCvcEditText.append("1234");

        Card card = mCardInputView.getCard();
        assertNotNull(card);
        assertEquals(VALID_AMEX_NO_SPACES, card.getNumber());
        assertNotNull(card.getExpMonth());
        assertNotNull(card.getExpYear());
        assertEquals(12, card.getExpMonth().intValue());
        assertEquals(2050, card.getExpYear().intValue());
        assertEquals("1234", card.getCVC());
        assertTrue(card.validateCard());
    }

    @Test
    public void getCard_whenInputIsValidDinersClub_returnsCardObject() {
        // The input date here will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050);

        mCardNumberEditText.setText(VALID_DINERS_CLUB_WITH_SPACES);
        mExpiryEditText.append("12");
        mExpiryEditText.append("50");
        mCvcEditText.append("123");

        Card card = mCardInputView.getCard();
        assertNotNull(card);
        assertEquals(VALID_DINERS_CLUB_NO_SPACES, card.getNumber());
        assertNotNull(card.getExpMonth());
        assertNotNull(card.getExpYear());
        assertEquals(12, card.getExpMonth().intValue());
        assertEquals(2050, card.getExpYear().intValue());
        assertEquals("123", card.getCVC());
        assertTrue(card.validateCard());
    }

    @Test
    public void getCard_whenInputHasIncompleteCardNumber_returnsNull() {
        // The test will be testing the wrong variable after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050);

        // This will be 242 4242 4242 4242
        mCardNumberEditText.setText(VALID_VISA_WITH_SPACES.substring(1));
        mExpiryEditText.append("12");
        mExpiryEditText.append("50");
        mCvcEditText.append("123");

        Card card = mCardInputView.getCard();
        assertNull(card);
    }

    @Test
    public void getCard_whenInputHasExpiredDate_returnsNull() {
        // The test will be testing the wrong variable after 2080. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2080);

        mCardNumberEditText.setText(VALID_VISA_WITH_SPACES);
        // Date interpreted as 12/2012 until 2080, when it will be 12/2112
        mExpiryEditText.append("12");
        mExpiryEditText.append("12");
        mCvcEditText.append("123");

        Card card = mCardInputView.getCard();
        assertNull(card);
    }

    @Test
    public void getCard_whenIncompleteCvCForVisa_returnsNull() {
        // The test will be testing the wrong variable after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050);

        mCardNumberEditText.setText(VALID_VISA_WITH_SPACES);
        mExpiryEditText.append("12");
        mExpiryEditText.append("50");
        mCvcEditText.append("12");

        Card card = mCardInputView.getCard();
        assertNull(card);
    }

    @Test
    public void getCard_whenIncompleteCvCForAmEx_returnsNull() {
        // The test will be testing the wrong variable after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050);

        mCardNumberEditText.setText(VALID_AMEX_WITH_SPACES);
        mExpiryEditText.append("12");
        mExpiryEditText.append("50");
        mCvcEditText.append("123");

        Card card = mCardInputView.getCard();
        assertNull(card);
    }

    @Test
    public void getCard_whenIncompleteCvCForDiners_returnsNull() {
        // The test will be testing the wrong variable after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050);

        mCardNumberEditText.setText(VALID_DINERS_CLUB_WITH_SPACES);
        mExpiryEditText.append("12");
        mExpiryEditText.append("50");
        mCvcEditText.append("12");

        Card card = mCardInputView.getCard();
        assertNull(card);
    }

    @Test
    public void onCompleteCardNumber_whenValid_shiftsFocusToExpiryDate() {
        mCardNumberEditText.setText(VALID_VISA_WITH_SPACES);
        assertEquals(R.id.et_card_number, mOnGlobalFocusChangeListener.getOldFocusId());
        assertEquals(R.id.et_expiry_date, mOnGlobalFocusChangeListener.getNewFocusId());
    }

    @Test
    public void onDeleteFromExpiryDate_whenEmpty_shiftsFocusToCardNumberAndDeletesDigit() {
        mCardNumberEditText.setText(VALID_VISA_WITH_SPACES);
        assertTrue(mExpiryEditText.hasFocus());
        ViewTestUtils.sendDeleteKeyEvent(mExpiryEditText);
        assertEquals(R.id.et_expiry_date, mOnGlobalFocusChangeListener.getOldFocusId());
        assertEquals(R.id.et_card_number, mOnGlobalFocusChangeListener.getNewFocusId());

        String subString = VALID_VISA_WITH_SPACES.substring(0, VALID_VISA_WITH_SPACES.length() - 1);
        assertEquals(subString, mCardNumberEditText.getText().toString());
        assertEquals(subString.length(), mCardNumberEditText.getSelectionStart());
    }

    @Test
    public void onDeleteFromExpiryDate_whenNotEmpty_doesNotShiftFocusOrDeleteDigit() {
        mCardNumberEditText.setText(VALID_AMEX_WITH_SPACES);
        assertTrue(mExpiryEditText.hasFocus());

        mExpiryEditText.append("1");
        ViewTestUtils.sendDeleteKeyEvent(mExpiryEditText);

        assertTrue(mExpiryEditText.hasFocus());
        assertEquals(VALID_AMEX_WITH_SPACES, mCardNumberEditText.getText().toString());
    }

    @Test
    public void onDeleteFromCvcDate_whenEmpty_shiftsFocusToExpiryAndDeletesDigit() {
        // This test will be invalid if run between 2080 and 2112. Please update the code.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2080);

        mCardNumberEditText.setText(VALID_VISA_WITH_SPACES);
        mExpiryEditText.append("12");
        mExpiryEditText.append("79");
        assertTrue(mCvcEditText.hasFocus());
        ViewTestUtils.sendDeleteKeyEvent(mCvcEditText);
        assertEquals(R.id.et_cvc_number, mOnGlobalFocusChangeListener.getOldFocusId());
        assertEquals(R.id.et_expiry_date, mOnGlobalFocusChangeListener.getNewFocusId());

        String expectedResult = "12/7";
        assertEquals(expectedResult, mExpiryEditText.getText().toString());
        assertEquals(expectedResult.length(), mExpiryEditText.getSelectionStart());
    }

    @Test
    public void onDeleteFromCvcDate_whenNotEmpty_doesNotShiftFocusOrDeleteEntry() {
        // This test will be invalid if run between 2080 and 2112. Please update the code.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2080);

        mCardNumberEditText.setText(VALID_AMEX_WITH_SPACES);
        mExpiryEditText.append("12");
        mExpiryEditText.append("79");
        assertTrue(mCvcEditText.hasFocus());

        mCvcEditText.append("123");
        ViewTestUtils.sendDeleteKeyEvent(mCvcEditText);

        assertTrue(mCvcEditText.hasFocus());
        assertEquals("12/79", mExpiryEditText.getText().toString());
    }

    @Test
    public void onDeleteFromCvcDate_whenEmptyAndExpiryDateIsEmpty_shiftsFocusOnly() {
        mCardNumberEditText.setText(VALID_DINERS_CLUB_WITH_SPACES);

        // Simulates user tapping into this text field without filling out the date first.
        mCvcEditText.requestFocus();

        ViewTestUtils.sendDeleteKeyEvent(mCvcEditText);
        assertEquals(R.id.et_cvc_number, mOnGlobalFocusChangeListener.getOldFocusId());
        assertEquals(R.id.et_expiry_date, mOnGlobalFocusChangeListener.getNewFocusId());
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
