package com.stripe.android.view;

import android.os.Build;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.stripe.android.R;
import com.stripe.android.model.Card;
import com.stripe.android.model.PaymentMethodCreateParams;
import com.stripe.android.testharness.TestFocusChangeListener;
import com.stripe.android.testharness.ViewTestUtils;

import java.util.Calendar;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.stripe.android.CardNumberFixtures.VALID_AMEX_NO_SPACES;
import static com.stripe.android.CardNumberFixtures.VALID_AMEX_WITH_SPACES;
import static com.stripe.android.CardNumberFixtures.VALID_DINERS_CLUB_NO_SPACES;
import static com.stripe.android.CardNumberFixtures.VALID_DINERS_CLUB_WITH_SPACES;
import static com.stripe.android.CardNumberFixtures.VALID_VISA_NO_SPACES;
import static com.stripe.android.CardNumberFixtures.VALID_VISA_WITH_SPACES;
import static com.stripe.android.view.CardInputListener.FocusField.FOCUS_CARD;
import static com.stripe.android.view.CardInputListener.FocusField.FOCUS_CVC;
import static com.stripe.android.view.CardInputListener.FocusField.FOCUS_EXPIRY;
import static com.stripe.android.view.CardInputWidget.LOGGING_TOKEN;
import static com.stripe.android.view.CardInputWidget.shouldIconShowBrand;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

/**
 * Test class for {@link CardInputWidget}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.O_MR1)
public class CardInputWidgetTest extends BaseViewTest<CardInputTestActivity> {

    // Every Card made by the CardInputView should have the card widget token.
    private static final String[] EXPECTED_LOGGING_ARRAY = {LOGGING_TOKEN};
    private CardInputWidget mCardInputWidget;
    private CardNumberEditText mCardNumberEditText;
    private StripeEditText mExpiryEditText;
    private StripeEditText mCvcEditText;
    private TestFocusChangeListener mOnGlobalFocusChangeListener;

    private CardInputTestActivity mActivity;

    @Mock private CardInputListener mCardInputListener;

    public CardInputWidgetTest() {
        super(CardInputTestActivity.class);
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        mActivity = createStartedActivity();

        CardInputWidget.DimensionOverrideSettings dimensionOverrides =
                new CardInputWidget.DimensionOverrideSettings() {
                    @Override
                    public int getPixelWidth(@NonNull String text, @NonNull EditText editText) {
                        // This makes it simple to know what to expect.
                        return text.length() * 10;
                    }

                    @Override
                    public int getFrameWidth() {
                        // That's a pretty small screen, but one that we theoretically support.
                        return 500;
                    }
                };

        mCardInputWidget = mActivity.getCardInputWidget();
        mCardInputWidget.setDimensionOverrideSettings(dimensionOverrides);
        mOnGlobalFocusChangeListener = new TestFocusChangeListener();
        mCardInputWidget.getViewTreeObserver()
                .addOnGlobalFocusChangeListener(mOnGlobalFocusChangeListener);

        mCardNumberEditText = mActivity.getCardNumberEditText();
        mCardNumberEditText.setText("");

        mExpiryEditText = mCardInputWidget.findViewById(R.id.et_expiry_date);
        mCvcEditText = mCardInputWidget.findViewById(R.id.et_cvc_number);
        ImageView iconView = mCardInputWidget.findViewById(R.id.iv_card_icon);

        // Set the width of the icon and its margin so that test calculations have
        // an expected value that is repeatable on all systems.
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) iconView.getLayoutParams();
        params.width = 48;
        params.rightMargin = 12;
        iconView.setLayoutParams(params);

        resumeStartedActivity(mActivity);
    }

    @After
    @Override
    public void tearDown() {
        super.tearDown();
        mActivity.finish();
    }

    @Test
    public void getCard_whenInputIsValidVisa_returnsCardObjectWithLoggingToken() {
        // The input date here will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050);

        mCardNumberEditText.setText(VALID_VISA_WITH_SPACES);
        mExpiryEditText.append("12");
        mExpiryEditText.append("50");
        mCvcEditText.append("123");

        Card card = mCardInputWidget.getCard();
        assertNotNull(card);
        assertEquals(VALID_VISA_NO_SPACES, card.getNumber());
        assertNotNull(card.getExpMonth());
        assertNotNull(card.getExpYear());
        assertEquals(12, card.getExpMonth().intValue());
        assertEquals(2050, card.getExpYear().intValue());
        assertEquals("123", card.getCVC());
        assertTrue(card.validateCard());
        assertArrayEquals(EXPECTED_LOGGING_ARRAY, card.getLoggingTokens().toArray());

        final PaymentMethodCreateParams.Card paymentMethodCard =
                mCardInputWidget.getPaymentMethodCard();
        assertNotNull(paymentMethodCard);
        final PaymentMethodCreateParams.Card expectedPaymentMethodCard =
                new PaymentMethodCreateParams.Card.Builder().setNumber(VALID_VISA_NO_SPACES)
                        .setCvc("123").setExpiryYear(2050).setExpiryMonth(12).build();
        assertEquals(expectedPaymentMethodCard, paymentMethodCard);
    }

    @Test
    public void getCard_whenInputIsValidAmEx_returnsCardObjectWithLoggingToken() {
        // The input date here will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050);

        mCardNumberEditText.setText(VALID_AMEX_WITH_SPACES);
        mExpiryEditText.append("12");
        mExpiryEditText.append("50");
        mCvcEditText.append("1234");

        Card card = mCardInputWidget.getCard();
        assertNotNull(card);
        assertEquals(VALID_AMEX_NO_SPACES, card.getNumber());
        assertNotNull(card.getExpMonth());
        assertNotNull(card.getExpYear());
        assertEquals(12, card.getExpMonth().intValue());
        assertEquals(2050, card.getExpYear().intValue());
        assertEquals("1234", card.getCVC());
        assertTrue(card.validateCard());
        assertArrayEquals(EXPECTED_LOGGING_ARRAY, card.getLoggingTokens().toArray());

        final PaymentMethodCreateParams.Card paymentMethodCard =
                mCardInputWidget.getPaymentMethodCard();
        assertNotNull(paymentMethodCard);
        final PaymentMethodCreateParams.Card expectedPaymentMethodCard =
                new PaymentMethodCreateParams.Card.Builder()
                        .setNumber(VALID_AMEX_NO_SPACES)
                        .setCvc("1234")
                        .setExpiryYear(2050)
                        .setExpiryMonth(12)
                        .build();
        assertEquals(expectedPaymentMethodCard, paymentMethodCard);
    }

    @Test
    public void getCard_whenInputIsValidDinersClub_returnsCardObjectWithLoggingToken() {
        // The input date here will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050);

        mCardNumberEditText.setText(VALID_DINERS_CLUB_WITH_SPACES);
        mExpiryEditText.append("12");
        mExpiryEditText.append("50");
        mCvcEditText.append("123");

        Card card = mCardInputWidget.getCard();
        assertNotNull(card);
        assertEquals(VALID_DINERS_CLUB_NO_SPACES, card.getNumber());
        assertNotNull(card.getExpMonth());
        assertNotNull(card.getExpYear());
        assertEquals(12, card.getExpMonth().intValue());
        assertEquals(2050, card.getExpYear().intValue());
        assertEquals("123", card.getCVC());
        assertTrue(card.validateCard());
        assertArrayEquals(EXPECTED_LOGGING_ARRAY, card.getLoggingTokens().toArray());

        final PaymentMethodCreateParams.Card paymentMethodCard =
                mCardInputWidget.getPaymentMethodCard();
        assertNotNull(paymentMethodCard);
        final PaymentMethodCreateParams.Card expectedPaymentMethodCard =
                new PaymentMethodCreateParams.Card.Builder()
                        .setNumber(VALID_DINERS_CLUB_NO_SPACES)
                        .setCvc("123")
                        .setExpiryYear(2050)
                        .setExpiryMonth(12)
                        .build();
        assertEquals(expectedPaymentMethodCard, paymentMethodCard);
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

        Card card = mCardInputWidget.getCard();
        assertNull(card);

        final PaymentMethodCreateParams.Card paymentMethodCard =
                mCardInputWidget.getPaymentMethodCard();
        assertNull(paymentMethodCard);
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

        Card card = mCardInputWidget.getCard();
        assertNull(card);

        final PaymentMethodCreateParams.Card paymentMethodCard =
                mCardInputWidget.getPaymentMethodCard();
        assertNull(paymentMethodCard);
    }

    @Test
    public void getCard_whenIncompleteCvCForVisa_returnsNull() {
        // The test will be testing the wrong variable after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050);

        mCardNumberEditText.setText(VALID_VISA_WITH_SPACES);
        mExpiryEditText.append("12");
        mExpiryEditText.append("50");
        mCvcEditText.append("12");

        Card card = mCardInputWidget.getCard();
        assertNull(card);

        final PaymentMethodCreateParams.Card paymentMethodCard =
                mCardInputWidget.getPaymentMethodCard();
        assertNull(paymentMethodCard);
    }

    @Test
    public void getCard_when3DigitCvCForAmEx_returnsCard() {
        // The test will be testing the wrong variable after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050);

        mCardNumberEditText.setText(VALID_AMEX_WITH_SPACES);
        mExpiryEditText.append("12");
        mExpiryEditText.append("50");
        mCvcEditText.append("123");

        Card card = mCardInputWidget.getCard();
        assertNotNull(card);

        final PaymentMethodCreateParams.Card paymentMethodCard =
                mCardInputWidget.getPaymentMethodCard();
        assertNotNull(paymentMethodCard);
    }

    @Test
    public void getCard_whenIncompleteCvCForAmEx_returnsNull() {
        // The test will be testing the wrong variable after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050);

        mCardNumberEditText.setText(VALID_AMEX_WITH_SPACES);
        mExpiryEditText.append("12");
        mExpiryEditText.append("50");
        mCvcEditText.append("12");

        Card card = mCardInputWidget.getCard();
        assertNull(card);

        final PaymentMethodCreateParams.Card paymentMethodCard =
                mCardInputWidget.getPaymentMethodCard();
        assertNull(paymentMethodCard);
    }

    @Test
    public void getPaymentMethodCreateParams_shouldReturnExpectedObject() {
        // The input date here will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050);

        mCardInputWidget.setCardNumber(VALID_VISA_NO_SPACES);
        mCardInputWidget.setExpiryDate(12, 2030);
        mCardInputWidget.setCvcCode("123");

        final PaymentMethodCreateParams params =
                mCardInputWidget.getPaymentMethodCreateParams();
        assertNotNull(params);

        final PaymentMethodCreateParams expectedParams = PaymentMethodCreateParams.create(
                new PaymentMethodCreateParams.Card.Builder()
                        .setNumber(VALID_VISA_NO_SPACES)
                        .setCvc("123")
                        .setExpiryYear(2030)
                        .setExpiryMonth(12)
                        .build()
        );
        assertEquals(expectedParams, params);
    }

    @Test
    public void getCard_whenIncompleteCvCForDiners_returnsNull() {
        // The test will be testing the wrong variable after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050);

        mCardNumberEditText.setText(VALID_DINERS_CLUB_WITH_SPACES);
        mExpiryEditText.append("12");
        mExpiryEditText.append("50");
        mCvcEditText.append("12");

        Card card = mCardInputWidget.getCard();
        assertNull(card);

        final PaymentMethodCreateParams.Card paymentMethodCard =
                mCardInputWidget.getPaymentMethodCard();
        assertNull(paymentMethodCard);
    }

    @Test
    public void onCompleteCardNumber_whenValid_shiftsFocusToExpiryDate() {
        mCardInputWidget.setCardInputListener(mCardInputListener);

        mCardNumberEditText.setText(VALID_VISA_WITH_SPACES);

        verify(mCardInputListener).onCardComplete();
        verify(mCardInputListener).onFocusChange(FOCUS_EXPIRY);
        assertEquals(R.id.et_card_number, mOnGlobalFocusChangeListener.getOldFocusId());
        assertEquals(R.id.et_expiry_date, mOnGlobalFocusChangeListener.getNewFocusId());
    }

    @Test
    public void onDeleteFromExpiryDate_whenEmpty_shiftsFocusToCardNumberAndDeletesDigit() {
        mCardInputWidget.setCardInputListener(mCardInputListener);
        mCardNumberEditText.setText(VALID_VISA_WITH_SPACES);
        assertTrue(mExpiryEditText.hasFocus());

        // The above functionality is tested elsewhere, so we reset this listener.
        reset(mCardInputListener);

        ViewTestUtils.sendDeleteKeyEvent(mExpiryEditText);
        verify(mCardInputListener).onFocusChange(FOCUS_CARD);
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

        mCardInputWidget.setCardInputListener(mCardInputListener);
        mCardNumberEditText.setText(VALID_VISA_WITH_SPACES);

        verify(mCardInputListener).onCardComplete();
        verify(mCardInputListener).onFocusChange(FOCUS_EXPIRY);

        mExpiryEditText.append("12");
        mExpiryEditText.append("79");

        verify(mCardInputListener).onExpirationComplete();
        verify(mCardInputListener).onFocusChange(FOCUS_CVC);
        assertTrue(mCvcEditText.hasFocus());

        // Clearing already-verified data.
        reset(mCardInputListener);

        ViewTestUtils.sendDeleteKeyEvent(mCvcEditText);
        verify(mCardInputListener).onFocusChange(FOCUS_EXPIRY);
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
    public void onUpdateIcon_forCommonLengthBrand_setsLengthOnCvc() {
        // This should set the brand to Visa. Note that more extensive brand checking occurs
        // in CardNumberEditTextTest.
        mCardNumberEditText.append(Card.PREFIXES_VISA[0]);
        assertTrue(ViewTestUtils.hasMaxLength(mCvcEditText, 3));
    }

    @Test
    public void onUpdateText_forAmExPrefix_setsLengthOnCvc() {
        mCardNumberEditText.append(Card.PREFIXES_AMERICAN_EXPRESS[0]);
        assertTrue(ViewTestUtils.hasMaxLength(mCvcEditText, 4));
    }

    @Test
    public void updateToInitialSizes_returnsExpectedValues() {
        // Initial spacing should look like
        // |img==60||---total == 500--------|
        // |(card==190)--(space==260)--(date==50)|
        // |img==60||  cardTouchArea | 380 | dateTouchArea | dateStart==510 |

        CardInputWidget.PlacementParameters initialParameters =
                mCardInputWidget.getPlacementParameters();
        assertEquals(190, initialParameters.cardWidth);
        assertEquals(50, initialParameters.dateWidth);
        assertEquals(260, initialParameters.cardDateSeparation);
        assertEquals(380, initialParameters.cardTouchBufferLimit);
        assertEquals(510, initialParameters.dateStartPosition);
    }

    @Test
    public void updateToPeekSize_withNoData_returnsExpectedValuesForCommonCardLength() {
        // Moving left uses Visa-style ("common") defaults
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        mCardInputWidget.updateSpaceSizes(false);
        CardInputWidget.PlacementParameters shiftedParameters =
                mCardInputWidget.getPlacementParameters();
        assertEquals(40, shiftedParameters.peekCardWidth);
        assertEquals(185, shiftedParameters.cardDateSeparation);
        assertEquals(50, shiftedParameters.dateWidth);
        assertEquals(195, shiftedParameters.dateCvcSeparation);
        assertEquals(30, shiftedParameters.cvcWidth);
        assertEquals(192, shiftedParameters.cardTouchBufferLimit);
        assertEquals(285, shiftedParameters.dateStartPosition);
        assertEquals(432, shiftedParameters.dateRightTouchBufferLimit);
        assertEquals(530, shiftedParameters.cvcStartPosition);
    }

    @Test
    public void getFocusRequestOnTouch_whenTouchOnImage_returnsNull() {
        // |img==60||---total == 500--------|
        // |(card==190)--(space==260)--(date==50)|
        // |img==60||  cardTouchArea | 380 | dateTouchArea | dateStart==510 |
        // So any touch lower than 60 will be the icon
        assertNull(mCardInputWidget.getFocusRequestOnTouch(30));
    }

    @Test
    public void getFocusRequestOnTouch_whenTouchActualCardWidget_returnsNull() {
        // |img==60||---total == 500--------|
        // |(card==190)--(space==260)--(date==50)|
        // |img==60||  cardTouchArea | 380 | dateTouchArea | dateStart==510 |
        // So any touch between 60 and 250 will be the actual card widget
        assertNull(mCardInputWidget.getFocusRequestOnTouch(200));
    }

    @Test
    public void getFocusRequestOnTouch_whenTouchInCardEditorSlop_returnsCardEditor() {
        // |img==60||---total == 500--------|
        // |(card==190)--(space==260)--(date==50)|
        // |img==60||  cardTouchArea | 380 | dateTouchArea | dateStart==510 |
        // So any touch between 250 and 380 needs to send focus to the card editor
        StripeEditText focusRequester = mCardInputWidget.getFocusRequestOnTouch(300);
        assertNotNull(focusRequester);
        assertEquals(mCardNumberEditText, focusRequester);
    }

    @Test
    public void getFocusRequestOnTouch_whenTouchInDateSlop_returnsDateEditor() {
        // |img==60||---total == 500--------|
        // |(card==190)--(space==260)--(date==50)|
        // |img==60||  cardTouchArea | 380 | dateTouchArea | dateStart==510 |
        // So any touch between 380 and 510 needs to send focus to the date editor
        StripeEditText focusRequester = mCardInputWidget.getFocusRequestOnTouch(390);
        assertNotNull(focusRequester);
        assertEquals(mExpiryEditText, focusRequester);
    }

    @Test
    public void getFocusRequestOnTouch_whenTouchInDateEditor_returnsNull() {
        // |img==60||---total == 500--------|
        // |(card==190)--(space==260)--(date==50)|
        // |img==60||  cardTouchArea | 380 | dateTouchArea | dateStart==510 |
        // So any touch over 510 doesn't need to do anything
        assertNull(mCardInputWidget.getFocusRequestOnTouch(530));
    }

    @Test
    public void getFocusRequestOnTouch_whenInPeekAfterShift_returnsNull() {
        // |img==60||---total == 500--------|
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        // So any touch between 60 and 100 does nothing
        mCardInputWidget.setCardNumberIsViewed(false);
        mCardInputWidget.updateSpaceSizes(false);
        assertNull(mCardInputWidget.getFocusRequestOnTouch(75));
    }

    @Test
    public void getFocusRequestOnTouch_whenInPeekSlopAfterShift_returnsCardEditor() {
        // |img==60||---total == 500--------|
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        // So any touch between 100 and 192 returns the card editor
        mCardInputWidget.setCardNumberIsViewed(false);
        mCardInputWidget.updateSpaceSizes(false);
        StripeEditText focusRequester = mCardInputWidget.getFocusRequestOnTouch(150);
        assertNotNull(focusRequester);
        assertEquals(mCardNumberEditText, focusRequester);
    }

    @Test
    public void getFocusRequestOnTouch_whenInDateLeftSlopAfterShift_returnsDateEditor() {
        // |img==60||---total == 500--------|
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        // So any touch between 192 and 285 returns the date editor
        mCardInputWidget.setCardNumberIsViewed(false);
        mCardInputWidget.updateSpaceSizes(false);
        StripeEditText focusRequester = mCardInputWidget.getFocusRequestOnTouch(200);
        assertNotNull(focusRequester);
        assertEquals(mExpiryEditText, focusRequester);
    }

    @Test
    public void getFocusRequestOnTouch_whenInDateAfterShift_returnsNull() {
        // |img==60||---total == 500--------|
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        // So any touch between 285 and 335 does nothing
        mCardInputWidget.setCardNumberIsViewed(false);
        mCardInputWidget.updateSpaceSizes(false);
        assertNull(mCardInputWidget.getFocusRequestOnTouch(300));
    }

    @Test
    public void getFocusRequestOnTouch_whenInDateRightSlopAfterShift_returnsDateEditor() {
        // |img==60||---total == 500--------|
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        // So any touch between 335 and 432 returns the date editor
        mCardInputWidget.setCardNumberIsViewed(false);
        mCardInputWidget.updateSpaceSizes(false);
        StripeEditText focusRequester = mCardInputWidget.getFocusRequestOnTouch(400);
        assertNotNull(focusRequester);
        assertEquals(mExpiryEditText, focusRequester);
    }

    @Test
    public void getFocusRequestOnTouch_whenInCvcSlopAfterShift_returnsCvcEditor() {
        // |img==60||---total == 500--------|
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        // So any touch between 432 and 530 returns the date editor
        mCardInputWidget.setCardNumberIsViewed(false);
        mCardInputWidget.updateSpaceSizes(false);
        StripeEditText focusRequester = mCardInputWidget.getFocusRequestOnTouch(485);
        assertNotNull(focusRequester);
        assertEquals(mCvcEditText, focusRequester);
    }

    @Test
    public void getFocusRequestOnTouch_whenInCvcAfterShift_returnsNull() {
        // |img==60||---total == 500--------|
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        // So any touch over 530 does nothing
        mCardInputWidget.setCardNumberIsViewed(false);
        mCardInputWidget.updateSpaceSizes(false);
        assertNull(mCardInputWidget.getFocusRequestOnTouch(545));
    }

    @Test
    public void addValidVisaCard_scrollsOver_andSetsExpectedDisplayValues() {
        // Moving left with an actual Visa number does the same as moving when empty.
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        mCardNumberEditText.setText(VALID_VISA_WITH_SPACES);
        CardInputWidget.PlacementParameters shiftedParameters =
                mCardInputWidget.getPlacementParameters();
        assertEquals(40, shiftedParameters.peekCardWidth);
        assertEquals(185, shiftedParameters.cardDateSeparation);
        assertEquals(50, shiftedParameters.dateWidth);
        assertEquals(195, shiftedParameters.dateCvcSeparation);
        assertEquals(30, shiftedParameters.cvcWidth);
    }

    @Test
    public void addValidAmExCard_scrollsOver_andSetsExpectedDisplayValues() {
        // Moving left with an AmEx number has a larger peek and cvc size.
        // |(peek==50)--(space==175)--(date==50)--(space==185)--(cvc==40)|
        mCardNumberEditText.setText(VALID_AMEX_WITH_SPACES);
        CardInputWidget.PlacementParameters shiftedParameters =
                mCardInputWidget.getPlacementParameters();
        assertEquals(50, shiftedParameters.peekCardWidth);
        assertEquals(175, shiftedParameters.cardDateSeparation);
        assertEquals(50, shiftedParameters.dateWidth);
        assertEquals(185, shiftedParameters.dateCvcSeparation);
        assertEquals(40, shiftedParameters.cvcWidth);
    }

    @Test
    public void addDinersClubCard_scrollsOver_andSetsExpectedDisplayValues() {
        // When we move for a Diner's club card, the peek text is shorter, so we expect:
        // |(peek==20)--(space==205)--(date==50)--(space==195)--(cvc==30)|
        mCardNumberEditText.setText(VALID_DINERS_CLUB_WITH_SPACES);
        CardInputWidget.PlacementParameters shiftedParameters =
                mCardInputWidget.getPlacementParameters();
        assertEquals(20, shiftedParameters.peekCardWidth);
        assertEquals(205, shiftedParameters.cardDateSeparation);
        assertEquals(50, shiftedParameters.dateWidth);
        assertEquals(195, shiftedParameters.dateCvcSeparation);
        assertEquals(30, shiftedParameters.cvcWidth);
    }

    @Test
    public void setCardNumber_withIncompleteNumber_doesNotValidateCard() {
        mCardInputWidget.setCardNumber("123456");
        assertFalse(mCardNumberEditText.isCardNumberValid());
        assertTrue(mCardNumberEditText.hasFocus());
    }

    @Test
    public void setExpirationDate_withValidData_setsCorrectValues() {
        mCardInputWidget.setExpiryDate(12, 79);
        assertEquals("12/79", mExpiryEditText.getText().toString());
    }

    @Test
    public void setCvcCode_withValidData_setsValue() {
        mCardInputWidget.setCvcCode("123");
        assertEquals("123", mCvcEditText.getText().toString());
    }

    @Test
    public void setCvcCode_withLongString_truncatesValue() {
        mCardInputWidget.setCvcCode("1234");
        assertEquals("123", mCvcEditText.getText().toString());
    }

    @Test
    public void setCvcCode_whenCardBrandIsAmericanExpress_allowsFourDigits() {
        mCardInputWidget.setCardNumber(VALID_AMEX_NO_SPACES);
        mCardInputWidget.setCvcCode("1234");
        assertEquals("1234", mCvcEditText.getText().toString());
    }

    @Test
    public void setEnabled_isTrue() {
        mCardInputWidget.setEnabled(true);
        assertTrue(mCardNumberEditText.isEnabled());
        assertTrue(mExpiryEditText.isEnabled());
        assertTrue(mCvcEditText.isEnabled());
    }

    @Test
    public void setEnabled_isFalse() {
        mCardInputWidget.setEnabled(false);
        assertFalse(mCardNumberEditText.isEnabled());
        assertFalse(mExpiryEditText.isEnabled());
        assertFalse(mCvcEditText.isEnabled());
    }

    @Test
    public void setAllCardFields_whenValidValues_allowsGetCardWithExpectedValues() {
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) <= 2079);

        mCardInputWidget.setCardNumber(VALID_AMEX_WITH_SPACES);
        mCardInputWidget.setExpiryDate(12, 2079);
        mCardInputWidget.setCvcCode("1234");
        Card card = mCardInputWidget.getCard();
        assertNotNull(card);
        assertEquals(VALID_AMEX_NO_SPACES, card.getNumber());
        assertNotNull(card.getExpMonth());
        assertNotNull(card.getExpYear());
        assertEquals(12, (int) card.getExpMonth());
        assertEquals(2079, (int) card.getExpYear());
        assertEquals("1234", card.getCVC());
        assertEquals(Card.CardBrand.AMERICAN_EXPRESS, card.getBrand());

        final PaymentMethodCreateParams.Card paymentMethodCard =
                mCardInputWidget.getPaymentMethodCard();
        assertNotNull(paymentMethodCard);
        final PaymentMethodCreateParams.Card expectedPaymentMethodCard =
                new PaymentMethodCreateParams.Card.Builder()
                        .setNumber(VALID_AMEX_NO_SPACES)
                        .setCvc("1234")
                        .setExpiryYear(2079)
                        .setExpiryMonth(12)
                        .build();
        assertEquals(expectedPaymentMethodCard, paymentMethodCard);
    }

    @Test
    public void addValues_thenClear_leavesAllTextFieldsEmpty() {
        mCardInputWidget.setCardNumber(VALID_VISA_NO_SPACES);
        mCardInputWidget.setExpiryDate(12, 2079);
        mCardInputWidget.setCvcCode("1234");
        mCardInputWidget.clear();
        assertEquals("", mCardNumberEditText.getText().toString());
        assertEquals("", mExpiryEditText.getText().toString());
        assertEquals("", mCvcEditText.getText().toString());
        assertEquals(R.id.et_cvc_number, mOnGlobalFocusChangeListener.getOldFocusId());
        assertEquals(R.id.et_card_number, mOnGlobalFocusChangeListener.getNewFocusId());
    }

    @Test
    public void shouldIconShowBrand_whenCvcNotFocused_isAlwaysTrue() {
        assertTrue(shouldIconShowBrand(Card.CardBrand.AMERICAN_EXPRESS, false, "1234"));
        assertTrue(shouldIconShowBrand(Card.CardBrand.AMERICAN_EXPRESS, false, ""));
        assertTrue(shouldIconShowBrand(Card.CardBrand.VISA, false, "333"));
        assertTrue(shouldIconShowBrand(Card.CardBrand.DINERS_CLUB, false, "12"));
        assertTrue(shouldIconShowBrand(Card.CardBrand.DISCOVER, false, null));
        assertTrue(shouldIconShowBrand(Card.CardBrand.JCB, false, "7"));
    }

    @Test
    public void shouldIconShowBrand_whenAmexAndCvCStringLengthNotFour_isFalse() {
        assertFalse(shouldIconShowBrand(Card.CardBrand.AMERICAN_EXPRESS, true, ""));
        assertFalse(shouldIconShowBrand(Card.CardBrand.AMERICAN_EXPRESS, true, "1"));
        assertFalse(shouldIconShowBrand(Card.CardBrand.AMERICAN_EXPRESS, true, "22"));
        assertFalse(shouldIconShowBrand(Card.CardBrand.AMERICAN_EXPRESS, true, "333"));
    }

    @Test
    public void shouldIconShowBrand_whenAmexAndCvcStringLengthIsFour_isTrue() {
        assertTrue(shouldIconShowBrand(Card.CardBrand.AMERICAN_EXPRESS, true, "1234"));
    }

    @Test
    public void shouldIconShowBrand_whenNotAmexAndCvcStringLengthIsNotThree_isFalse() {
        assertFalse(shouldIconShowBrand(Card.CardBrand.VISA, true, ""));
        assertFalse(shouldIconShowBrand(Card.CardBrand.DISCOVER, true, "12"));
        assertFalse(shouldIconShowBrand(Card.CardBrand.JCB, true, "55"));
        assertFalse(shouldIconShowBrand(Card.CardBrand.MASTERCARD, true, "9"));
        assertFalse(shouldIconShowBrand(Card.CardBrand.DINERS_CLUB, true, null));
        assertFalse(shouldIconShowBrand(Card.CardBrand.UNKNOWN, true, "12"));
    }

    @Test
    public void shouldIconShowBrand_whenNotAmexAndCvcStringLengthIsThree_isTrue() {
        assertTrue(shouldIconShowBrand(Card.CardBrand.VISA, true, "999"));
        assertTrue(shouldIconShowBrand(Card.CardBrand.DISCOVER, true, "123"));
        assertTrue(shouldIconShowBrand(Card.CardBrand.JCB, true, "555"));
        assertTrue(shouldIconShowBrand(Card.CardBrand.MASTERCARD, true, "919"));
        assertTrue(shouldIconShowBrand(Card.CardBrand.DINERS_CLUB, true, "415"));
        assertTrue(shouldIconShowBrand(Card.CardBrand.UNKNOWN, true, "212"));
    }
}
