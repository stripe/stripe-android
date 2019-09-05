package com.stripe.android.view;

import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.google.android.material.textfield.TextInputLayout;
import com.stripe.android.R;
import com.stripe.android.model.Address;
import com.stripe.android.model.Card;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.model.PaymentMethodCreateParams;
import com.stripe.android.testharness.ViewTestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.Calendar;
import java.util.Objects;

import static com.stripe.android.view.CardInputListener.FocusField.FOCUS_CARD;
import static com.stripe.android.view.CardInputListener.FocusField.FOCUS_CVC;
import static com.stripe.android.view.CardInputListener.FocusField.FOCUS_EXPIRY;
import static com.stripe.android.view.CardInputListener.FocusField.FOCUS_POSTAL;
import static com.stripe.android.view.CardInputTestActivity.VALID_AMEX_NO_SPACES;
import static com.stripe.android.view.CardInputTestActivity.VALID_AMEX_WITH_SPACES;
import static com.stripe.android.view.CardInputTestActivity.VALID_DINERS_CLUB_WITH_SPACES;
import static com.stripe.android.view.CardInputTestActivity.VALID_VISA_NO_SPACES;
import static com.stripe.android.view.CardInputTestActivity.VALID_VISA_WITH_SPACES;
import static com.stripe.android.view.CardMultilineWidget.CARD_MULTILINE_TOKEN;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test class for {@link CardMultilineWidget}.
 */
@RunWith(RobolectricTestRunner.class)
public class CardMultilineWidgetTest extends BaseViewTest<CardInputTestActivity> {

    // Every Card made by the CardInputView should have the card widget token.
    private static final String[] EXPECTED_LOGGING_ARRAY = {CARD_MULTILINE_TOKEN};

    private CardMultilineWidget mCardMultilineWidget;
    private CardMultilineWidget mNoZipCardMultilineWidget;
    private WidgetControlGroup mFullGroup;
    private WidgetControlGroup mNoZipGroup;

    @Mock private CardInputListener mFullCardListener;
    @Mock private CardInputListener mNoZipCardListener;

    public CardMultilineWidgetTest() {
        super(CardInputTestActivity.class);
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        final CardInputTestActivity activity = createStartedActivity();
        mCardMultilineWidget = activity.getCardMultilineWidget();
        mFullGroup = new WidgetControlGroup(mCardMultilineWidget);

        mNoZipCardMultilineWidget = activity.getNoZipCardMulitlineWidget();
        mNoZipGroup = new WidgetControlGroup(mNoZipCardMultilineWidget);

        mFullGroup.cardNumberEditText.setText("");
    }

    @After
    @Override
    public void tearDown() {
        super.tearDown();
    }

    @Test
    public void testExistence() {
        assertNotNull(mCardMultilineWidget);
        assertNotNull(mFullGroup.cardNumberEditText);
        assertNotNull(mFullGroup.expiryDateEditText);
        assertNotNull(mFullGroup.cvcEditText);
        assertNotNull(mFullGroup.postalCodeEditText);
        assertNotNull(mFullGroup.secondRowLayout);

        assertNotNull(mNoZipCardMultilineWidget);
        assertNotNull(mNoZipGroup.cardNumberEditText);
        assertNotNull(mNoZipGroup.expiryDateEditText);
        assertNotNull(mNoZipGroup.cvcEditText);
        assertNotNull(mNoZipGroup.postalCodeEditText);
        assertNotNull(mNoZipGroup.secondRowLayout);
    }

    @Test
    public void onCreate_setsCorrectHintForExpiry() {
        TextInputLayout shortExpiryContainer = mCardMultilineWidget
                .findViewById(R.id.tl_add_source_expiry_ml);

        TextInputLayout longExpiryContainer = mNoZipCardMultilineWidget
                .findViewById(R.id.tl_add_source_expiry_ml);

        String shortExpiryHint = mCardMultilineWidget
                .getResources().getString(R.string.expiry_label_short);
        String longExpiryHint = mCardMultilineWidget
                .getResources().getString(R.string.acc_label_expiry_date);

        assertNotNull(shortExpiryContainer.getHint());
        assertEquals(shortExpiryHint, shortExpiryContainer.getHint().toString());
        assertNotNull(longExpiryContainer.getHint());
        assertEquals(longExpiryHint, longExpiryContainer.getHint().toString());
    }

    @Test
    public void getCard_whenInputIsValidVisaWithZip_returnsCardObjectWithLoggingToken() {
        // The input date here will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050);

        mFullGroup.cardNumberEditText.setText(VALID_VISA_WITH_SPACES);
        mFullGroup.expiryDateEditText.append("12");
        mFullGroup.expiryDateEditText.append("50");
        mFullGroup.cvcEditText.append("123");
        mFullGroup.postalCodeEditText.append("12345");

        Card card = mCardMultilineWidget.getCard();
        assertNotNull(card);
        assertEquals(VALID_VISA_NO_SPACES, card.getNumber());
        assertNotNull(card.getExpMonth());
        assertNotNull(card.getExpYear());
        assertEquals(12, card.getExpMonth().intValue());
        assertEquals(2050, card.getExpYear().intValue());
        assertEquals("123", card.getCVC());
        assertEquals("12345", card.getAddressZip());
        assertTrue(card.validateCard());
        assertArrayEquals(EXPECTED_LOGGING_ARRAY, card.getLoggingTokens().toArray());
    }

    @Test
    public void isPostalCodeMaximalLength_whenZipEnteredAndIsMaximalLength_returnsTrue() {
        assertTrue(CardMultilineWidget.isPostalCodeMaximalLength(true, "12345"));
    }

    @Test
    public void isPostalCodeMaximalLength_whenZipEnteredAndIsNotMaximalLength_returnsFalse() {
        assertFalse(CardMultilineWidget.isPostalCodeMaximalLength(true, "123"));
    }

    @Test
    public void isPostalCodeMaximalLength_whenZipEnteredAndIsEmpty_returnsFalse() {
        assertFalse(CardMultilineWidget.isPostalCodeMaximalLength(true, ""));
    }

    @Test
    public void isPostalCodeMaximalLength_whenZipEnteredAndIsNull_returnsFalse() {
        assertFalse(CardMultilineWidget.isPostalCodeMaximalLength(true, null));
    }

    /**
     * This test should change when we allow and validate postal codes outside of the US
     * in this control.
     */
    @Test
    public void isPostalCodeMaximalLength_whenNotZip_returnsFalse() {
        assertFalse(CardMultilineWidget.isPostalCodeMaximalLength(false, "12345"));
    }

    @Test
    public void getCard_whenInputIsValidVisaButInputHasNoZip_returnsNull() {
        // The input date here will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050);

        mFullGroup.cardNumberEditText.setText(VALID_VISA_WITH_SPACES);
        mFullGroup.expiryDateEditText.append("12");
        mFullGroup.expiryDateEditText.append("50");
        mFullGroup.cvcEditText.append("123");

        Card card = mCardMultilineWidget.getCard();
        assertNull(card);
    }

    @Test
    public void getCard_whenInputIsValidVisaAndNoZipRequired_returnsFullCardAndExpectedLogging() {
        // The input date here will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050);

        mNoZipGroup.cardNumberEditText.setText(VALID_VISA_WITH_SPACES);
        mNoZipGroup.expiryDateEditText.append("12");
        mNoZipGroup.expiryDateEditText.append("50");
        mNoZipGroup.cvcEditText.append("123");
        Card card = mNoZipCardMultilineWidget.getCard();
        assertNotNull(card);
        assertEquals(VALID_VISA_NO_SPACES, card.getNumber());
        assertNotNull(card.getExpMonth());
        assertNotNull(card.getExpYear());
        assertEquals(12, card.getExpMonth().intValue());
        assertEquals(2050, card.getExpYear().intValue());
        assertEquals("123", card.getCVC());
        assertNull(card.getAddressZip());
        assertTrue(card.validateCard());
        assertArrayEquals(EXPECTED_LOGGING_ARRAY, card.getLoggingTokens().toArray());
    }

    @Test
    public void getCard_whenInputIsValidAmexAndNoZipRequiredAnd4DigitCvc_returnsFullCardAndExpectedLogging() {
        // The input date here will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050);

        mNoZipGroup.cardNumberEditText.setText(VALID_AMEX_WITH_SPACES);
        mNoZipGroup.expiryDateEditText.append("12");
        mNoZipGroup.expiryDateEditText.append("50");
        mNoZipGroup.cvcEditText.append("1234");
        Card card = mNoZipCardMultilineWidget.getCard();
        assertNotNull(card);
        assertEquals(VALID_AMEX_NO_SPACES, card.getNumber());
        assertNotNull(card.getExpMonth());
        assertNotNull(card.getExpYear());
        assertEquals(12, card.getExpMonth().intValue());
        assertEquals(2050, card.getExpYear().intValue());
        assertEquals("1234", card.getCVC());
        assertNull(card.getAddressZip());
        assertTrue(card.validateCard());
        assertArrayEquals(EXPECTED_LOGGING_ARRAY, card.getLoggingTokens().toArray());
    }

    @Test
    public void getCard_whenInputIsValidAmexAndNoZipRequiredAnd3DigitCvc_returnsFullCardAndExpectedLogging() {
        // The input date here will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050);

        mNoZipGroup.cardNumberEditText.setText(VALID_AMEX_WITH_SPACES);
        mNoZipGroup.expiryDateEditText.append("12");
        mNoZipGroup.expiryDateEditText.append("50");
        mNoZipGroup.cvcEditText.append("123");
        Card card = mNoZipCardMultilineWidget.getCard();
        assertNotNull(card);
        assertEquals(VALID_AMEX_NO_SPACES, card.getNumber());
        assertNotNull(card.getExpMonth());
        assertNotNull(card.getExpYear());
        assertEquals(12, card.getExpMonth().intValue());
        assertEquals(2050, card.getExpYear().intValue());
        assertEquals("123", card.getCVC());
        assertNull(card.getAddressZip());
        assertTrue(card.validateCard());
        assertArrayEquals(EXPECTED_LOGGING_ARRAY, card.getLoggingTokens().toArray());
    }

    @Test
    public void getPaymentMethodCreateParams_shouldReturnExpectedObject() {
        // The input date here will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050);

        mFullGroup.cardNumberEditText.setText(VALID_VISA_WITH_SPACES);
        mFullGroup.expiryDateEditText.append("12");
        mFullGroup.expiryDateEditText.append("50");
        mFullGroup.cvcEditText.append("123");
        mFullGroup.postalCodeEditText.append("12345");

        final PaymentMethodCreateParams params =
                mCardMultilineWidget.getPaymentMethodCreateParams();
        assertNotNull(params);

        final PaymentMethodCreateParams expectedParams = PaymentMethodCreateParams.create(
                new PaymentMethodCreateParams.Card.Builder()
                        .setNumber(VALID_VISA_NO_SPACES)
                        .setCvc("123")
                        .setExpiryYear(2050)
                        .setExpiryMonth(12)
                        .build(),
                new PaymentMethod.BillingDetails.Builder()
                        .setAddress(new Address.Builder()
                                .setPostalCode("12345")
                                .build()
                        )
                .build()
        );
        assertEquals(expectedParams, params);
    }

    @Test
    public void getPaymentMethodCard_whenInputIsValidVisaWithZip_returnsCardAndBillingDetails() {
        // The input date here will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050);

        mFullGroup.cardNumberEditText.setText(VALID_VISA_WITH_SPACES);
        mFullGroup.expiryDateEditText.append("12");
        mFullGroup.expiryDateEditText.append("50");
        mFullGroup.cvcEditText.append("123");
        mFullGroup.postalCodeEditText.append("12345");

        final PaymentMethodCreateParams.Card card =
                mCardMultilineWidget.getPaymentMethodCard();
        assertNotNull(card);

        final PaymentMethodCreateParams.Card inputCard =
                new PaymentMethodCreateParams.Card.Builder().setNumber(VALID_VISA_NO_SPACES)
                        .setCvc("123").setExpiryYear(2050).setExpiryMonth(12).build();
        assertEquals(inputCard, card);

        final PaymentMethod.BillingDetails billingDetails =
                Objects.requireNonNull(mCardMultilineWidget.getPaymentMethodBillingDetails());

        assertEquals("12345",
                Objects.requireNonNull(billingDetails.address).getPostalCode());
    }

    @Test
    public void getPaymentMethodCard_whenInputIsValidVisaButInputHasNoZip_returnsNull() {
        // The input date here will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050);

        mFullGroup.cardNumberEditText.setText(VALID_VISA_WITH_SPACES);
        mFullGroup.expiryDateEditText.append("12");
        mFullGroup.expiryDateEditText.append("50");
        mFullGroup.cvcEditText.append("123");

        final PaymentMethodCreateParams.Card card =
                mNoZipCardMultilineWidget.getPaymentMethodCard();
        assertNull(card);
    }

    @Test
    public void getPaymentMethodCard_whenInputIsValidVisaAndNoZipRequired_returnsFullCard() {
        // The input date here will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050);

        mNoZipGroup.cardNumberEditText.setText(VALID_VISA_WITH_SPACES);
        mNoZipGroup.expiryDateEditText.append("12");
        mNoZipGroup.expiryDateEditText.append("50");
        mNoZipGroup.cvcEditText.append("123");
        final PaymentMethodCreateParams.Card card =
                mNoZipCardMultilineWidget.getPaymentMethodCard();
        assertNotNull(card);

        final PaymentMethodCreateParams.Card inputCard =
                new PaymentMethodCreateParams.Card.Builder().setNumber(VALID_VISA_NO_SPACES)
                        .setCvc("123").setExpiryYear(2050).setExpiryMonth(12).build();
        assertEquals(inputCard, card);

        assertNull(mNoZipCardMultilineWidget.getPaymentMethodBillingDetails());
    }

    @Test
    public void getPaymentMethodCard_whenInputIsValidAmexAndNoZipRequiredAnd4DigitCvc_returnsFullCard() {
        // The input date here will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050);

        mNoZipGroup.cardNumberEditText.setText(VALID_AMEX_WITH_SPACES);
        mNoZipGroup.expiryDateEditText.append("12");
        mNoZipGroup.expiryDateEditText.append("50");
        mNoZipGroup.cvcEditText.append("1234");

        final PaymentMethodCreateParams.Card card =
                mNoZipCardMultilineWidget.getPaymentMethodCard();

        assertNotNull(card);

        final PaymentMethodCreateParams.Card inputCard =
                new PaymentMethodCreateParams.Card.Builder().setNumber(VALID_AMEX_NO_SPACES)
                        .setCvc("1234").setExpiryYear(2050).setExpiryMonth(12).build();
        assertEquals(inputCard, card);

        assertNull(mNoZipCardMultilineWidget.getPaymentMethodBillingDetails());
    }

    @Test
    public void getPaymentMethodCard_whenInputIsValidAmexAndNoZipRequiredAnd3DigitCvc_returnsFullCard() {
        // The input date here will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050);

        mNoZipGroup.cardNumberEditText.setText(VALID_AMEX_WITH_SPACES);
        mNoZipGroup.expiryDateEditText.append("12");
        mNoZipGroup.expiryDateEditText.append("50");
        mNoZipGroup.cvcEditText.append("123");
        final PaymentMethodCreateParams.Card card =
                mNoZipCardMultilineWidget.getPaymentMethodCard();

        assertNotNull(card);
        final PaymentMethodCreateParams.Card inputCard =
                new PaymentMethodCreateParams.Card.Builder().setNumber(VALID_AMEX_NO_SPACES)
                        .setCvc("123").setExpiryYear(2050).setExpiryMonth(12).build();
        assertEquals(inputCard, card);

        assertNull(mNoZipCardMultilineWidget.getPaymentMethodBillingDetails());
    }

    @Test
    public void initView_whenZipRequired_secondRowContainsThreeVisibleElements() {
        assertEquals(View.VISIBLE, mFullGroup.expiryDateEditText.getVisibility());
        assertEquals(View.VISIBLE, mFullGroup.cvcEditText.getVisibility());
        assertEquals(View.VISIBLE, mFullGroup.postalCodeEditText.getVisibility());
        assertEquals(View.VISIBLE, mFullGroup.postalCodeInputLayout.getVisibility());
    }

    @Test
    public void clear_whenZipRequiredAndAllFieldsEntered_clearsAllfields() {
        // The input date here will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050);

        mFullGroup.cardNumberEditText.setText(VALID_VISA_WITH_SPACES);
        mFullGroup.expiryDateEditText.append("12");
        mFullGroup.expiryDateEditText.append("50");
        mFullGroup.cvcEditText.append("123");
        mFullGroup.postalCodeEditText.append("12345");

        mCardMultilineWidget.clear();

        assertEquals("", mFullGroup.cardNumberEditText.getText().toString());
        assertEquals("", mFullGroup.expiryDateEditText.getText().toString());
        assertEquals("", mFullGroup.cvcEditText.getText().toString());
        assertEquals("", mFullGroup.postalCodeEditText.getText().toString());
    }

    @Test
    public void clear_whenFieldsInErrorState_clearsFieldsAndHidesErrors() {
        // The input date here will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050);

        String badVisa = VALID_VISA_WITH_SPACES.substring(VALID_VISA_WITH_SPACES.length() - 1);
        badVisa += 3; // Makes this 4242 4242 4242 4243
        mFullGroup.cardNumberEditText.setText(badVisa);

        mFullGroup.expiryDateEditText.append("01");
        mFullGroup.expiryDateEditText.append("11");
        mFullGroup.cvcEditText.append("12");
        mFullGroup.postalCodeEditText.append("1234");

        mCardMultilineWidget.validateAllFields();

        assertTrue(mFullGroup.cardNumberEditText.getShouldShowError());
        assertTrue(mFullGroup.expiryDateEditText.getShouldShowError());
        assertTrue(mFullGroup.cvcEditText.getShouldShowError());
        assertTrue(mFullGroup.postalCodeEditText.getShouldShowError());

        mCardMultilineWidget.clear();

        assertFalse(mFullGroup.cardNumberEditText.getShouldShowError());
        assertFalse(mFullGroup.expiryDateEditText.getShouldShowError());
        assertFalse(mFullGroup.cvcEditText.getShouldShowError());
        assertFalse(mFullGroup.postalCodeEditText.getShouldShowError());
    }

    @Test
    public void setCvcLabel_shouldShowCustomLabelIfPresent() {
        mCardMultilineWidget.setCvcLabel("my cool cvc");
        assertEquals("my cool cvc", mFullGroup.cvcInputLayout.getHint());

        mCardMultilineWidget.setCvcLabel(null);
        assertEquals("CVC", mFullGroup.cvcInputLayout.getHint());
    }

    @Test
    public void initView_whenZipRequiredThenSetToHidden_secondRowLosesPostalCodeAndAdjustsMargin() {
        assertEquals(View.VISIBLE, mFullGroup.postalCodeInputLayout.getVisibility());
        mCardMultilineWidget.setShouldShowPostalCode(false);
        assertEquals(View.GONE, mFullGroup.postalCodeInputLayout.getVisibility());
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)
                mFullGroup.cvcInputLayout.getLayoutParams();
        assertEquals(0, params.rightMargin);
        assertEquals(0, params.getMarginEnd());
    }

    @Test
    public void initView_whenNoZipRequired_secondRowContainsTwoVisibleElements() {
        assertEquals(View.VISIBLE, mNoZipGroup.expiryDateEditText.getVisibility());
        assertEquals(View.VISIBLE, mNoZipGroup.cvcEditText.getVisibility());
        assertEquals(View.GONE, mNoZipGroup.postalCodeInputLayout.getVisibility());
    }

    @Test
    public void initView_whenZipHiddenThenSetToRequired_secondRowAddsPostalCodeAndAdjustsMargin() {
        assertEquals(View.GONE, mNoZipGroup.postalCodeInputLayout.getVisibility());
        mNoZipCardMultilineWidget.setShouldShowPostalCode(true);
        assertEquals(View.VISIBLE, mNoZipGroup.postalCodeInputLayout.getVisibility());

        int expectedMargin = mNoZipCardMultilineWidget.getResources()
                .getDimensionPixelSize(R.dimen.add_card_expiry_middle_margin);

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)
                mNoZipGroup.cvcInputLayout.getLayoutParams();
        assertEquals(expectedMargin, params.rightMargin);
        assertEquals(expectedMargin, params.getMarginEnd());
    }

    @Test
    public void onCompleteCardNumber_whenValid_shiftsFocusToExpiryDate() {
        mCardMultilineWidget.setCardInputListener(mFullCardListener);
        mNoZipCardMultilineWidget.setCardInputListener(mNoZipCardListener);

        mFullGroup.cardNumberEditText.setText(VALID_VISA_WITH_SPACES);
        verify(mFullCardListener, times(1)).onCardComplete();
        verify(mFullCardListener, times(1)).onFocusChange(FOCUS_EXPIRY);
        assertTrue(mFullGroup.expiryDateEditText.hasFocus());

        mNoZipGroup.cardNumberEditText.setText(VALID_AMEX_WITH_SPACES);
        verify(mNoZipCardListener, times(1)).onCardComplete();
        verify(mNoZipCardListener, times(1)).onFocusChange(FOCUS_EXPIRY);
        assertTrue(mNoZipGroup.expiryDateEditText.hasFocus());
    }

    @Test
    public void onCompleteExpiry_whenValid_shiftsFocusToCvc() {
        mCardMultilineWidget.setCardInputListener(mFullCardListener);
        mNoZipCardMultilineWidget.setCardInputListener(mNoZipCardListener);

        mFullGroup.expiryDateEditText.append("12");
        mFullGroup.expiryDateEditText.append("50");
        verify(mFullCardListener, times(1)).onExpirationComplete();
        verify(mFullCardListener, times(1)).onFocusChange(FOCUS_CVC);
        assertTrue(mFullGroup.cvcEditText.hasFocus());

        mNoZipGroup.expiryDateEditText.append("12");
        mNoZipGroup.expiryDateEditText.append("50");
        verify(mNoZipCardListener, times(1)).onExpirationComplete();
        verify(mNoZipCardListener, times(1)).onFocusChange(FOCUS_CVC);
        assertTrue(mNoZipGroup.cvcEditText.hasFocus());
    }

    @Test
    public void onCompleteCvc_whenValid_shiftsFocusOnlyIfPostalCodeShown() {
        mCardMultilineWidget.setCardInputListener(mFullCardListener);
        mNoZipCardMultilineWidget.setCardInputListener(mNoZipCardListener);

        mFullGroup.cardNumberEditText.setText(VALID_VISA_WITH_SPACES);
        mFullGroup.expiryDateEditText.append("12");
        mFullGroup.expiryDateEditText.append("50");
        mFullGroup.cvcEditText.append("123");
        verify(mFullCardListener, times(1)).onCvcComplete();
        verify(mFullCardListener, times(1)).onFocusChange(FOCUS_POSTAL);
        assertTrue(mFullGroup.postalCodeEditText.hasFocus());

        mNoZipGroup.cardNumberEditText.setText(VALID_VISA_WITH_SPACES);
        mNoZipGroup.expiryDateEditText.append("12");
        mNoZipGroup.expiryDateEditText.append("50");
        mNoZipGroup.cvcEditText.append("123");
        verify(mNoZipCardListener, times(1)).onCvcComplete();
        verify(mNoZipCardListener, times(0)).onFocusChange(FOCUS_POSTAL);
        assertTrue(mNoZipGroup.cvcEditText.hasFocus());
    }

    @Test
    public void deleteWhenEmpty_fromExpiry_shiftsToCardNumber() {
        mCardMultilineWidget.setCardInputListener(mFullCardListener);
        mNoZipCardMultilineWidget.setCardInputListener(mNoZipCardListener);

        String deleteOneCharacterString = VALID_VISA_WITH_SPACES
                .substring(0, VALID_VISA_WITH_SPACES.length() - 1);
        mFullGroup.cardNumberEditText.setText(VALID_VISA_WITH_SPACES);

        reset(mFullCardListener);
        assertTrue(mFullGroup.expiryDateEditText.hasFocus());
        ViewTestUtils.sendDeleteKeyEvent(mFullGroup.expiryDateEditText);

        verify(mFullCardListener, times(1)).onFocusChange(FOCUS_CARD);
        assertTrue(mFullGroup.cardNumberEditText.hasFocus());
        assertEquals(deleteOneCharacterString, mFullGroup.cardNumberEditText.getText().toString());

        mNoZipGroup.cardNumberEditText.setText(VALID_VISA_WITH_SPACES);

        reset(mNoZipCardListener);
        assertTrue(mNoZipGroup.expiryDateEditText.hasFocus());
        ViewTestUtils.sendDeleteKeyEvent(mNoZipGroup.expiryDateEditText);

        verify(mNoZipCardListener, times(1)).onFocusChange(FOCUS_CARD);
        assertTrue(mNoZipGroup.cardNumberEditText.hasFocus());
        assertEquals(deleteOneCharacterString, mNoZipGroup.cardNumberEditText.getText().toString());
    }

    @Test
    public void deleteWhenEmpty_fromCvc_shiftsToExpiry() {
        mCardMultilineWidget.setCardInputListener(mFullCardListener);
        mNoZipCardMultilineWidget.setCardInputListener(mNoZipCardListener);

        mFullGroup.expiryDateEditText.append("12");
        mFullGroup.expiryDateEditText.append("50");

        reset(mFullCardListener);
        assertTrue(mFullGroup.cvcEditText.hasFocus());
        ViewTestUtils.sendDeleteKeyEvent(mFullGroup.cvcEditText);

        verify(mFullCardListener, times(1)).onFocusChange(FOCUS_EXPIRY);
        assertTrue(mFullGroup.expiryDateEditText.hasFocus());
        assertEquals("12/5", mFullGroup.expiryDateEditText.getText().toString());

        mNoZipGroup.expiryDateEditText.append("12");
        mNoZipGroup.expiryDateEditText.append("50");

        reset(mNoZipCardListener);
        assertTrue(mNoZipGroup.cvcEditText.hasFocus());
        ViewTestUtils.sendDeleteKeyEvent(mNoZipGroup.cvcEditText);

        verify(mNoZipCardListener, times(1)).onFocusChange(FOCUS_EXPIRY);
        assertTrue(mNoZipGroup.expiryDateEditText.hasFocus());
        assertEquals("12/5", mNoZipGroup.expiryDateEditText.getText().toString());
    }

    @Test
    public void deleteWhenEmpty_fromPostalCode_shiftsToCvc() {
        mCardMultilineWidget.setCardInputListener(mFullCardListener);

        mFullGroup.cardNumberEditText.setText(VALID_DINERS_CLUB_WITH_SPACES);
        mFullGroup.expiryDateEditText.append("12");
        mFullGroup.expiryDateEditText.append("50");
        mFullGroup.cvcEditText.append("123");

        reset(mFullCardListener);
        ViewTestUtils.sendDeleteKeyEvent(mFullGroup.postalCodeEditText);

        verify(mFullCardListener, times(1)).onFocusChange(FOCUS_CVC);
        assertEquals("12", mFullGroup.cvcEditText.getText().toString());
    }

    @Test
    public void setCardNumber_whenHasSpaces_canCreateValidCard() {
        mCardMultilineWidget.setCardNumber(VALID_VISA_NO_SPACES);
        mFullGroup.expiryDateEditText.append("12");
        mFullGroup.expiryDateEditText.append("50");
        mFullGroup.cvcEditText.append("123");
        mFullGroup.postalCodeEditText.append("12345");

        Card card = mCardMultilineWidget.getCard();

        assertNotNull(card);
        assertEquals(VALID_VISA_NO_SPACES, card.getNumber());
    }

    @Test
    public void setCardNumber_whenHasNoSpaces_canCreateValidCard() {
        mCardMultilineWidget.setCardNumber(VALID_VISA_WITH_SPACES);
        mFullGroup.expiryDateEditText.append("12");
        mFullGroup.expiryDateEditText.append("50");
        mFullGroup.cvcEditText.append("123");
        mFullGroup.postalCodeEditText.append("12345");

        Card card = mCardMultilineWidget.getCard();

        assertNotNull(card);
        assertEquals(VALID_VISA_NO_SPACES, card.getNumber());
    }

    @Test
    public void validateCardNumber_whenValid_doesNotShowError() {
        mCardMultilineWidget.setCardNumber(VALID_VISA_WITH_SPACES);

        boolean isValid = mCardMultilineWidget.validateCardNumber();
        boolean shouldShowError = mFullGroup.cardNumberEditText.getShouldShowError();

        assertTrue(isValid);
        assertFalse(shouldShowError);
    }

    @Test
    public void validateCardNumber_whenInvalid_setsShowError() {
        String invalidNumber = "1234 1234 1234 1234";
        mCardMultilineWidget.setCardNumber(invalidNumber);

        boolean isValid = mCardMultilineWidget.validateCardNumber();
        boolean shouldShowError = mFullGroup.cardNumberEditText.getShouldShowError();

        assertFalse(isValid);
        assertTrue(shouldShowError);
    }

    @Test
    public void setEnabled_setsEnabledPropertyOnAllChildWidgets() {
        assertTrue(mCardMultilineWidget.isEnabled());
        assertTrue(mFullGroup.cardInputLayout.isEnabled());
        assertTrue(mFullGroup.expiryInputLayout.isEnabled());
        assertTrue(mFullGroup.postalCodeInputLayout.isEnabled());
        assertTrue(mFullGroup.cvcInputLayout.isEnabled());
        assertTrue(mFullGroup.expiryDateEditText.isEnabled());
        assertTrue(mFullGroup.cardNumberEditText.isEnabled());
        assertTrue(mFullGroup.cvcEditText.isEnabled());
        assertTrue(mFullGroup.postalCodeEditText.isEnabled());

        mCardMultilineWidget.setEnabled(false);

        assertFalse(mCardMultilineWidget.isEnabled());
        assertFalse(mFullGroup.cardInputLayout.isEnabled());
        assertFalse(mFullGroup.expiryInputLayout.isEnabled());
        assertFalse(mFullGroup.postalCodeInputLayout.isEnabled());
        assertFalse(mFullGroup.cvcInputLayout.isEnabled());
        assertFalse(mFullGroup.expiryDateEditText.isEnabled());
        assertFalse(mFullGroup.cardNumberEditText.isEnabled());
        assertFalse(mFullGroup.cvcEditText.isEnabled());
        assertFalse(mFullGroup.postalCodeEditText.isEnabled());

        mCardMultilineWidget.setEnabled(true);

        assertTrue(mCardMultilineWidget.isEnabled());
        assertTrue(mFullGroup.cardInputLayout.isEnabled());
        assertTrue(mFullGroup.expiryInputLayout.isEnabled());
        assertTrue(mFullGroup.postalCodeInputLayout.isEnabled());
        assertTrue(mFullGroup.cvcInputLayout.isEnabled());
        assertTrue(mFullGroup.expiryDateEditText.isEnabled());
        assertTrue(mFullGroup.cardNumberEditText.isEnabled());
        assertTrue(mFullGroup.cvcEditText.isEnabled());
        assertTrue(mFullGroup.postalCodeEditText.isEnabled());
    }

    static class WidgetControlGroup {
        @NonNull final CardNumberEditText cardNumberEditText;
        @NonNull final TextInputLayout cardInputLayout;
        @NonNull final ExpiryDateEditText expiryDateEditText;
        @NonNull final TextInputLayout expiryInputLayout;
        @NonNull final StripeEditText cvcEditText;
        @NonNull final TextInputLayout cvcInputLayout;
        @NonNull final StripeEditText postalCodeEditText;
        @NonNull final TextInputLayout postalCodeInputLayout;
        @NonNull final LinearLayout secondRowLayout;

        WidgetControlGroup(@NonNull CardMultilineWidget parentWidget) {
            cardNumberEditText = parentWidget.findViewById(R.id.et_add_source_card_number_ml);
            cardInputLayout = parentWidget.findViewById(R.id.tl_add_source_card_number_ml);
            expiryDateEditText = parentWidget.findViewById(R.id.et_add_source_expiry_ml);
            expiryInputLayout = parentWidget.findViewById(R.id.tl_add_source_expiry_ml);
            cvcEditText = parentWidget.findViewById(R.id.et_add_source_cvc_ml);
            cvcInputLayout = parentWidget.findViewById(R.id.tl_add_source_cvc_ml);
            postalCodeEditText = parentWidget.findViewById(R.id.et_add_source_postal_ml);
            postalCodeInputLayout = parentWidget.findViewById(R.id.tl_add_source_postal_ml);
            secondRowLayout = parentWidget.findViewById(R.id.second_row_layout);
        }
    }
}
