package com.stripe.android.view

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.textfield.TextInputLayout
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.CardNumberFixtures.VALID_AMEX_NO_SPACES
import com.stripe.android.CardNumberFixtures.VALID_AMEX_WITH_SPACES
import com.stripe.android.CardNumberFixtures.VALID_DINERS_CLUB_WITH_SPACES
import com.stripe.android.CardNumberFixtures.VALID_VISA_NO_SPACES
import com.stripe.android.CardNumberFixtures.VALID_VISA_WITH_SPACES
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentConfiguration
import com.stripe.android.R
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.testharness.ViewTestUtils
import com.stripe.android.view.CardInputListener.FocusField.Companion.FOCUS_CARD
import com.stripe.android.view.CardInputListener.FocusField.Companion.FOCUS_CVC
import com.stripe.android.view.CardInputListener.FocusField.Companion.FOCUS_EXPIRY
import com.stripe.android.view.CardInputListener.FocusField.Companion.FOCUS_POSTAL
import com.stripe.android.view.CardMultilineWidget.Companion.CARD_MULTILINE_TOKEN
import java.util.Calendar
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.reset
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

/**
 * Test class for [CardMultilineWidget].
 */
@RunWith(RobolectricTestRunner::class)
internal class CardMultilineWidgetTest {

    private lateinit var cardMultilineWidget: CardMultilineWidget
    private lateinit var noZipCardMultilineWidget: CardMultilineWidget
    private lateinit var fullGroup: WidgetControlGroup
    private lateinit var noZipGroup: WidgetControlGroup

    @Mock
    private lateinit var fullCardListener: CardInputListener
    @Mock
    private lateinit var noZipCardListener: CardInputListener

    private val activityScenarioFactory: ActivityScenarioFactory by lazy {
        ActivityScenarioFactory(ApplicationProvider.getApplicationContext())
    }

    @BeforeTest
    fun setup() {
        MockitoAnnotations.initMocks(this)

        CustomerSession.instance = mock()

        val context: Context = ApplicationProvider.getApplicationContext()
        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)

        activityScenarioFactory.create<AddPaymentMethodActivity>(
            AddPaymentMethodActivityStarter.Args.Builder()
                .setPaymentMethodType(PaymentMethod.Type.Card)
                .setPaymentConfiguration(PaymentConfiguration.getInstance(context))
                .setBillingAddressFields(BillingAddressFields.PostalCode)
                .build()
        ).use { activityScenario ->
            activityScenario.onActivity { activity ->
                cardMultilineWidget = activity.findViewById(R.id.card_multiline_widget)
                fullGroup = WidgetControlGroup(cardMultilineWidget)

                fullGroup.cardNumberEditText.setText("")

                cardMultilineWidget.setCardNumberTextWatcher(EMPTY_WATCHER)
                cardMultilineWidget.setExpiryDateTextWatcher(EMPTY_WATCHER)
                cardMultilineWidget.setCvcNumberTextWatcher(EMPTY_WATCHER)
                cardMultilineWidget.setPostalCodeTextWatcher(EMPTY_WATCHER)
                cardMultilineWidget.paymentMethodBillingDetailsBuilder
            }
        }

        activityScenarioFactory.create<AddPaymentMethodActivity>(
            AddPaymentMethodActivityStarter.Args.Builder()
                .setPaymentMethodType(PaymentMethod.Type.Card)
                .setPaymentConfiguration(PaymentConfiguration.getInstance(context))
                .setBillingAddressFields(BillingAddressFields.None)
                .build()
        ).use { activityScenario ->
            activityScenario.onActivity {
                noZipCardMultilineWidget = it.findViewById(R.id.card_multiline_widget)
                noZipGroup = WidgetControlGroup(noZipCardMultilineWidget)
            }
        }
    }

    @Test
    fun testExistence() {
        assertNotNull(cardMultilineWidget)
        assertNotNull(fullGroup.cardNumberEditText)
        assertNotNull(fullGroup.expiryDateEditText)
        assertNotNull(fullGroup.cvcEditText)
        assertNotNull(fullGroup.postalCodeEditText)
        assertNotNull(fullGroup.secondRowLayout)

        assertNotNull(noZipCardMultilineWidget)
        assertNotNull(noZipGroup.cardNumberEditText)
        assertNotNull(noZipGroup.expiryDateEditText)
        assertNotNull(noZipGroup.cvcEditText)
        assertNotNull(noZipGroup.postalCodeEditText)
        assertNotNull(noZipGroup.secondRowLayout)
    }

    @Test
    fun onCreate_setsCorrectHintForExpiry() {
        val shortExpiryContainer = cardMultilineWidget
            .findViewById<TextInputLayout>(R.id.tl_expiry)

        val longExpiryContainer = noZipCardMultilineWidget
            .findViewById<TextInputLayout>(R.id.tl_expiry)

        val shortExpiryHint = cardMultilineWidget
            .resources.getString(R.string.expiry_label_short)
        val longExpiryHint = cardMultilineWidget
            .resources.getString(R.string.acc_label_expiry_date)

        assertNotNull(shortExpiryContainer.hint)
        assertEquals(shortExpiryHint, shortExpiryContainer.hint?.toString())
        assertNotNull(longExpiryContainer.hint)
        assertEquals(longExpiryHint, longExpiryContainer.hint?.toString())
    }

    @Test
    fun getCard_whenInputIsValidVisaWithZip_returnsCardObjectWithLoggingToken() {
        // The input date here will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050)

        fullGroup.cardNumberEditText.setText(VALID_VISA_WITH_SPACES)
        fullGroup.expiryDateEditText.append("12")
        fullGroup.expiryDateEditText.append("50")
        fullGroup.cvcEditText.append("123")
        fullGroup.postalCodeEditText.append("12345")

        val card = requireNotNull(cardMultilineWidget.card)
        assertEquals(VALID_VISA_NO_SPACES, card.number)
        assertNotNull(card.expMonth)
        assertNotNull(card.expYear)
        assertEquals(12, card.expMonth)
        assertEquals(2050, card.expYear)
        assertEquals("123", card.cvc)
        assertEquals("12345", card.addressZip)
        assertTrue(card.validateCard())
        assertTrue(EXPECTED_LOGGING_ARRAY.contentEquals(card.loggingTokens.toTypedArray()))
    }

    @Test
    fun isPostalCodeMaximalLength_whenZipEnteredAndIsMaximalLength_returnsTrue() {
        assertTrue(CardMultilineWidget.isPostalCodeMaximalLength("12345"))
    }

    @Test
    fun isPostalCodeMaximalLength_whenZipEnteredAndIsNotMaximalLength_returnsFalse() {
        assertFalse(CardMultilineWidget.isPostalCodeMaximalLength("123"))
    }

    @Test
    fun isPostalCodeMaximalLength_whenZipEnteredAndIsEmpty_returnsFalse() {
        assertFalse(CardMultilineWidget.isPostalCodeMaximalLength(""))
    }

    @Test
    fun isPostalCodeMaximalLength_whenZipEnteredAndIsNull_returnsFalse() {
        assertFalse(CardMultilineWidget.isPostalCodeMaximalLength(null))
    }

    /**
     * This test should change when we allow and validate postal codes outside of the US
     * in this control.
     */
    @Test
    fun isPostalCodeMaximalLength_whenNotZip_returnsFalse() {
        assertFalse(CardMultilineWidget.isPostalCodeMaximalLength("12345", 10))
    }

    @Test
    fun getCard_whenInputIsValidVisaButInputHasNoZip_returnsNull() {
        // The input date here will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050)

        fullGroup.cardNumberEditText.setText(VALID_VISA_WITH_SPACES)
        fullGroup.expiryDateEditText.append("12")
        fullGroup.expiryDateEditText.append("50")
        fullGroup.cvcEditText.append("123")

        val card = cardMultilineWidget.card
        assertNull(card)
    }

    @Test
    fun getCard_whenInputIsValidVisaAndNoZipRequired_returnsFullCardAndExpectedLogging() {
        // The input date here will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050)

        noZipGroup.cardNumberEditText.setText(VALID_VISA_WITH_SPACES)
        noZipGroup.expiryDateEditText.append("12")
        noZipGroup.expiryDateEditText.append("50")
        noZipGroup.cvcEditText.append("123")
        val card = noZipCardMultilineWidget.card
        assertNotNull(card)
        assertEquals(VALID_VISA_NO_SPACES, card.number)
        assertNotNull(card.expMonth)
        assertNotNull(card.expYear)
        assertEquals(12, card.expMonth)
        assertEquals(2050, card.expYear)
        assertEquals("123", card.cvc)
        assertNull(card.addressZip)
        assertTrue(card.validateCard())
        assertTrue(EXPECTED_LOGGING_ARRAY.contentEquals(card.loggingTokens.toTypedArray()))
    }

    @Test
    fun getCard_whenInputIsValidAmexAndNoZipRequiredAnd4DigitCvc_returnsFullCardAndExpectedLogging() {
        // The input date here will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050)

        noZipGroup.cardNumberEditText.setText(VALID_AMEX_WITH_SPACES)
        noZipGroup.expiryDateEditText.append("12")
        noZipGroup.expiryDateEditText.append("50")
        noZipGroup.cvcEditText.append("1234")
        val card = noZipCardMultilineWidget.card
        assertNotNull(card)
        assertEquals(VALID_AMEX_NO_SPACES, card.number)
        assertNotNull(card.expMonth)
        assertNotNull(card.expYear)
        assertEquals(12, card.expMonth)
        assertEquals(2050, card.expYear)
        assertEquals("1234", card.cvc)
        assertNull(card.addressZip)
        assertTrue(card.validateCard())
        assertTrue(EXPECTED_LOGGING_ARRAY.contentEquals(card.loggingTokens.toTypedArray()))
    }

    @Test
    fun getCard_whenInputIsValidAmexAndNoZipRequiredAnd3DigitCvc_returnsFullCardAndExpectedLogging() {
        // The input date here will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050)

        noZipGroup.cardNumberEditText.setText(VALID_AMEX_WITH_SPACES)
        noZipGroup.expiryDateEditText.append("12")
        noZipGroup.expiryDateEditText.append("50")
        noZipGroup.cvcEditText.append("123")
        val card = noZipCardMultilineWidget.card
        assertNotNull(card)
        assertEquals(VALID_AMEX_NO_SPACES, card.number)
        assertNotNull(card.expMonth)
        assertNotNull(card.expYear)
        assertEquals(12, card.expMonth)
        assertEquals(2050, card.expYear)
        assertEquals("123", card.cvc)
        assertNull(card.addressZip)
        assertTrue(card.validateCard())
        assertTrue(EXPECTED_LOGGING_ARRAY.contentEquals(card.loggingTokens.toTypedArray()))
    }

    @Test
    fun getPaymentMethodCreateParams_shouldReturnExpectedObject() {
        // The input date here will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050)

        fullGroup.cardNumberEditText.setText(VALID_VISA_WITH_SPACES)
        fullGroup.expiryDateEditText.append("12")
        fullGroup.expiryDateEditText.append("50")
        fullGroup.cvcEditText.append("123")
        fullGroup.postalCodeEditText.append("12345")

        val params = cardMultilineWidget.paymentMethodCreateParams
        assertNotNull(params)

        val expectedParams = PaymentMethodCreateParams.create(
            PaymentMethodCreateParams.Card(
                number = VALID_VISA_NO_SPACES,
                cvc = "123",
                expiryMonth = 12,
                expiryYear = 2050
            ),
            PaymentMethod.BillingDetails.Builder()
                .setAddress(Address.Builder()
                    .setPostalCode("12345")
                    .build()
                )
                .build()
        )
        assertEquals(expectedParams, params)
    }

    @Test
    fun getPaymentMethodCard_whenInputIsValidVisaWithZip_returnsCardAndBillingDetails() {
        // The input date here will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050)

        fullGroup.cardNumberEditText.setText(VALID_VISA_WITH_SPACES)
        fullGroup.expiryDateEditText.append("12")
        fullGroup.expiryDateEditText.append("50")
        fullGroup.cvcEditText.append("123")
        fullGroup.postalCodeEditText.append("12345")

        val card = cardMultilineWidget.paymentMethodCard
        assertNotNull(card)

        val inputCard = PaymentMethodCreateParams.Card(
            number = VALID_VISA_NO_SPACES,
            cvc = "123",
            expiryMonth = 12,
            expiryYear = 2050
        )
        assertEquals(inputCard, card)

        assertEquals(
            "12345",
            cardMultilineWidget.paymentMethodBillingDetails?.address?.postalCode
        )
    }

    @Test
    fun getPaymentMethodCard_whenInputIsValidVisaButInputHasNoZip_returnsNull() {
        // The input date here will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050)

        fullGroup.cardNumberEditText.setText(VALID_VISA_WITH_SPACES)
        fullGroup.expiryDateEditText.append("12")
        fullGroup.expiryDateEditText.append("50")
        fullGroup.cvcEditText.append("123")

        val card = noZipCardMultilineWidget.paymentMethodCard
        assertNull(card)
    }

    @Test
    fun getPaymentMethodCard_whenInputIsValidVisaAndNoZipRequired_returnsFullCard() {
        // The input date here will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050)

        noZipGroup.cardNumberEditText.setText(VALID_VISA_WITH_SPACES)
        noZipGroup.expiryDateEditText.append("12")
        noZipGroup.expiryDateEditText.append("50")
        noZipGroup.cvcEditText.append("123")
        val card = noZipCardMultilineWidget.paymentMethodCard
        assertNotNull(card)

        val inputCard = PaymentMethodCreateParams.Card(
            number = VALID_VISA_NO_SPACES,
            cvc = "123",
            expiryMonth = 12,
            expiryYear = 2050
        )
        assertEquals(inputCard, card)

        assertNull(noZipCardMultilineWidget.paymentMethodBillingDetails)
    }

    @Test
    fun getPaymentMethodCard_whenInputIsValidAmexAndNoZipRequiredAnd4DigitCvc_returnsFullCard() {
        // The input date here will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050)

        noZipGroup.cardNumberEditText.setText(VALID_AMEX_WITH_SPACES)
        noZipGroup.expiryDateEditText.append("12")
        noZipGroup.expiryDateEditText.append("50")
        noZipGroup.cvcEditText.append("1234")

        val card = noZipCardMultilineWidget.paymentMethodCard

        assertNotNull(card)

        val inputCard = PaymentMethodCreateParams.Card(
            number = VALID_AMEX_NO_SPACES,
            cvc = "1234",
            expiryMonth = 12,
            expiryYear = 2050
        )
        assertEquals(inputCard, card)

        assertNull(noZipCardMultilineWidget.paymentMethodBillingDetails)
    }

    @Test
    fun getPaymentMethodCard_whenInputIsValidAmexAndNoZipRequiredAnd3DigitCvc_returnsFullCard() {
        // The input date here will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050)

        noZipGroup.cardNumberEditText.setText(VALID_AMEX_WITH_SPACES)
        noZipGroup.expiryDateEditText.append("12")
        noZipGroup.expiryDateEditText.append("50")
        noZipGroup.cvcEditText.append("123")
        val card = noZipCardMultilineWidget.paymentMethodCard

        assertNotNull(card)
        val inputCard = PaymentMethodCreateParams.Card(
            number = VALID_AMEX_NO_SPACES,
            cvc = "123",
            expiryMonth = 12,
            expiryYear = 2050
        )
        assertEquals(inputCard, card)

        assertNull(noZipCardMultilineWidget.paymentMethodBillingDetails)
    }

    @Test
    fun initView_whenZipRequired_secondRowContainsThreeVisibleElements() {
        assertEquals(View.VISIBLE, fullGroup.expiryDateEditText.visibility)
        assertEquals(View.VISIBLE, fullGroup.cvcEditText.visibility)
        assertEquals(View.VISIBLE, fullGroup.postalCodeEditText.visibility)
        assertEquals(View.VISIBLE, fullGroup.postalCodeInputLayout.visibility)
    }

    @Test
    fun clear_whenZipRequiredAndAllFieldsEntered_clearsAllfields() {
        // The input date here will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050)

        fullGroup.cardNumberEditText.setText(VALID_VISA_WITH_SPACES)
        fullGroup.expiryDateEditText.append("12")
        fullGroup.expiryDateEditText.append("50")
        fullGroup.cvcEditText.append("123")
        fullGroup.postalCodeEditText.append("12345")

        cardMultilineWidget.clear()

        assertEquals("", fullGroup.cardNumberEditText.text?.toString())
        assertEquals("", fullGroup.expiryDateEditText.text?.toString())
        assertEquals("", fullGroup.cvcEditText.text?.toString())
        assertEquals("", fullGroup.postalCodeEditText.text?.toString())
    }

    @Test
    fun clear_whenFieldsInErrorState_clearsFieldsAndHidesErrors() {
        // The input date here will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050)

        var badVisa = VALID_VISA_WITH_SPACES.substring(VALID_VISA_WITH_SPACES.length - 1)
        badVisa += 3 // Makes this 4242 4242 4242 4243
        fullGroup.cardNumberEditText.setText(badVisa)

        fullGroup.expiryDateEditText.append("01")
        fullGroup.expiryDateEditText.append("11")
        fullGroup.cvcEditText.append("12")
        fullGroup.postalCodeEditText.append("1234")

        cardMultilineWidget.validateAllFields()

        assertTrue(fullGroup.cardNumberEditText.shouldShowError)
        assertTrue(fullGroup.expiryDateEditText.shouldShowError)
        assertTrue(fullGroup.cvcEditText.shouldShowError)
        assertTrue(fullGroup.postalCodeEditText.shouldShowError)

        cardMultilineWidget.clear()

        assertFalse(fullGroup.cardNumberEditText.shouldShowError)
        assertFalse(fullGroup.expiryDateEditText.shouldShowError)
        assertFalse(fullGroup.cvcEditText.shouldShowError)
        assertFalse(fullGroup.postalCodeEditText.shouldShowError)
    }

    @Test
    fun setCvcLabel_shouldShowCustomLabelIfPresent() {
        cardMultilineWidget.setCvcLabel("my cool cvc")
        assertEquals("my cool cvc", fullGroup.cvcInputLayout.hint)

        cardMultilineWidget.setCvcLabel(null)
        assertEquals("CVC", fullGroup.cvcInputLayout.hint)
    }

    @Test
    fun initView_whenZipRequiredThenSetToHidden_secondRowLosesPostalCodeAndAdjustsMargin() {
        assertEquals(View.VISIBLE, fullGroup.postalCodeInputLayout.visibility)
        cardMultilineWidget.setShouldShowPostalCode(false)
        assertEquals(View.GONE, fullGroup.postalCodeInputLayout.visibility)
        val params = fullGroup.cvcInputLayout.layoutParams as LinearLayout.LayoutParams
        assertEquals(0, params.rightMargin)
        assertEquals(0, params.marginEnd)
    }

    @Test
    fun initView_whenNoZipRequired_secondRowContainsTwoVisibleElements() {
        assertEquals(View.VISIBLE, noZipGroup.expiryDateEditText.visibility)
        assertEquals(View.VISIBLE, noZipGroup.cvcEditText.visibility)
        assertEquals(View.GONE, noZipGroup.postalCodeInputLayout.visibility)
    }

    @Test
    fun initView_whenZipHiddenThenSetToRequired_secondRowAddsPostalCodeAndAdjustsMargin() {
        assertEquals(View.GONE, noZipGroup.postalCodeInputLayout.visibility)
        noZipCardMultilineWidget.setShouldShowPostalCode(true)
        assertEquals(View.VISIBLE, noZipGroup.postalCodeInputLayout.visibility)

        val expectedMargin = noZipCardMultilineWidget.resources
            .getDimensionPixelSize(R.dimen.stripe_add_card_expiry_middle_margin)

        val params = noZipGroup.cvcInputLayout.layoutParams as LinearLayout.LayoutParams
        assertEquals(expectedMargin, params.rightMargin)
        assertEquals(expectedMargin, params.marginEnd)
    }

    @Test
    fun onCompleteCardNumber_whenValid_shiftsFocusToExpiryDate() {
        cardMultilineWidget.setCardInputListener(fullCardListener)
        noZipCardMultilineWidget.setCardInputListener(noZipCardListener)

        fullGroup.cardNumberEditText.setText(VALID_VISA_WITH_SPACES)
        verify(fullCardListener).onCardComplete()
        verify(fullCardListener).onFocusChange(FOCUS_EXPIRY)
        assertTrue(fullGroup.expiryDateEditText.hasFocus())

        noZipGroup.cardNumberEditText.setText(VALID_AMEX_WITH_SPACES)
        verify(noZipCardListener).onCardComplete()
        verify(noZipCardListener).onFocusChange(FOCUS_EXPIRY)
        assertTrue(noZipGroup.expiryDateEditText.hasFocus())
    }

    @Test
    fun onCompleteExpiry_whenValid_shiftsFocusToCvc() {
        cardMultilineWidget.setCardInputListener(fullCardListener)
        noZipCardMultilineWidget.setCardInputListener(noZipCardListener)

        fullGroup.expiryDateEditText.append("12")
        fullGroup.expiryDateEditText.append("50")
        verify(fullCardListener).onExpirationComplete()
        verify(fullCardListener).onFocusChange(FOCUS_CVC)
        assertTrue(fullGroup.cvcEditText.hasFocus())

        noZipGroup.expiryDateEditText.append("12")
        noZipGroup.expiryDateEditText.append("50")
        verify(noZipCardListener).onExpirationComplete()
        verify(noZipCardListener).onFocusChange(FOCUS_CVC)
        assertTrue(noZipGroup.cvcEditText.hasFocus())
    }

    @Test
    fun onCompleteCvc_whenValid_shiftsFocusOnlyIfPostalCodeShown() {
        cardMultilineWidget.setCardInputListener(fullCardListener)
        noZipCardMultilineWidget.setCardInputListener(noZipCardListener)

        fullGroup.cardNumberEditText.setText(VALID_VISA_WITH_SPACES)
        fullGroup.expiryDateEditText.append("12")
        fullGroup.expiryDateEditText.append("50")
        fullGroup.cvcEditText.append("123")
        verify(fullCardListener).onCvcComplete()
        verify(fullCardListener).onFocusChange(FOCUS_POSTAL)
        assertTrue(fullGroup.postalCodeEditText.hasFocus())

        noZipGroup.cardNumberEditText.setText(VALID_VISA_WITH_SPACES)
        noZipGroup.expiryDateEditText.append("12")
        noZipGroup.expiryDateEditText.append("50")
        noZipGroup.cvcEditText.append("123")
        verify(noZipCardListener).onCvcComplete()
        verify(noZipCardListener, never()).onFocusChange(FOCUS_POSTAL)
        assertTrue(noZipGroup.cvcEditText.hasFocus())
    }

    @Test
    fun deleteWhenEmpty_fromExpiry_shiftsToCardNumber() {
        cardMultilineWidget.setCardInputListener(fullCardListener)
        noZipCardMultilineWidget.setCardInputListener(noZipCardListener)

        val deleteOneCharacterString = VALID_VISA_WITH_SPACES
            .substring(0, VALID_VISA_WITH_SPACES.length - 1)
        fullGroup.cardNumberEditText.setText(VALID_VISA_WITH_SPACES)

        reset<CardInputListener>(fullCardListener)
        assertTrue(fullGroup.expiryDateEditText.hasFocus())
        ViewTestUtils.sendDeleteKeyEvent(fullGroup.expiryDateEditText)

        verify(fullCardListener).onFocusChange(FOCUS_CARD)
        assertTrue(fullGroup.cardNumberEditText.hasFocus())
        assertEquals(deleteOneCharacterString, fullGroup.cardNumberEditText.text?.toString())

        noZipGroup.cardNumberEditText.setText(VALID_VISA_WITH_SPACES)

        reset<CardInputListener>(noZipCardListener)
        assertTrue(noZipGroup.expiryDateEditText.hasFocus())
        ViewTestUtils.sendDeleteKeyEvent(noZipGroup.expiryDateEditText)

        verify(noZipCardListener).onFocusChange(FOCUS_CARD)
        assertTrue(noZipGroup.cardNumberEditText.hasFocus())
        assertEquals(deleteOneCharacterString, noZipGroup.cardNumberEditText.text?.toString())
    }

    @Test
    fun deleteWhenEmpty_fromCvc_shiftsToExpiry() {
        cardMultilineWidget.setCardInputListener(fullCardListener)
        noZipCardMultilineWidget.setCardInputListener(noZipCardListener)

        fullGroup.expiryDateEditText.append("12")
        fullGroup.expiryDateEditText.append("50")

        reset<CardInputListener>(fullCardListener)
        assertTrue(fullGroup.cvcEditText.hasFocus())
        ViewTestUtils.sendDeleteKeyEvent(fullGroup.cvcEditText)

        verify(fullCardListener).onFocusChange(FOCUS_EXPIRY)
        assertTrue(fullGroup.expiryDateEditText.hasFocus())
        assertEquals("12/5", fullGroup.expiryDateEditText.text?.toString())

        noZipGroup.expiryDateEditText.append("12")
        noZipGroup.expiryDateEditText.append("50")

        reset<CardInputListener>(noZipCardListener)
        assertTrue(noZipGroup.cvcEditText.hasFocus())
        ViewTestUtils.sendDeleteKeyEvent(noZipGroup.cvcEditText)

        verify(noZipCardListener).onFocusChange(FOCUS_EXPIRY)
        assertTrue(noZipGroup.expiryDateEditText.hasFocus())
        assertEquals("12/5", noZipGroup.expiryDateEditText.text.toString())
    }

    @Test
    fun deleteWhenEmpty_fromPostalCode_shiftsToCvc() {
        cardMultilineWidget.setCardInputListener(fullCardListener)

        fullGroup.cardNumberEditText.setText(VALID_DINERS_CLUB_WITH_SPACES)
        fullGroup.expiryDateEditText.append("12")
        fullGroup.expiryDateEditText.append("50")
        fullGroup.cvcEditText.append("123")

        reset<CardInputListener>(fullCardListener)
        ViewTestUtils.sendDeleteKeyEvent(fullGroup.postalCodeEditText)

        verify(fullCardListener).onFocusChange(FOCUS_CVC)
        assertEquals("12", fullGroup.cvcEditText.text?.toString())
    }

    @Test
    fun setCardNumber_whenHasSpaces_canCreateValidCard() {
        cardMultilineWidget.setCardNumber(VALID_VISA_NO_SPACES)
        fullGroup.expiryDateEditText.append("12")
        fullGroup.expiryDateEditText.append("50")
        fullGroup.cvcEditText.append("123")
        fullGroup.postalCodeEditText.append("12345")

        val card = cardMultilineWidget.card

        assertEquals(VALID_VISA_NO_SPACES, card?.number)
    }

    @Test
    fun setCardNumber_whenHasNoSpaces_canCreateValidCard() {
        cardMultilineWidget.setCardNumber(VALID_VISA_WITH_SPACES)
        fullGroup.expiryDateEditText.append("12")
        fullGroup.expiryDateEditText.append("50")
        fullGroup.cvcEditText.append("123")
        fullGroup.postalCodeEditText.append("12345")

        val card = cardMultilineWidget.card
        assertEquals(VALID_VISA_NO_SPACES, card?.number)
    }

    @Test
    fun validateCardNumber_whenValid_doesNotShowError() {
        cardMultilineWidget.setCardNumber(VALID_VISA_WITH_SPACES)

        val isValid = cardMultilineWidget.validateCardNumber()
        val shouldShowError = fullGroup.cardNumberEditText.shouldShowError

        assertTrue(isValid)
        assertFalse(shouldShowError)
    }

    @Test
    fun validateCardNumber_whenInvalid_setsShowError() {
        val invalidNumber = "1234 1234 1234 1234"
        cardMultilineWidget.setCardNumber(invalidNumber)

        val isValid = cardMultilineWidget.validateCardNumber()
        val shouldShowError = fullGroup.cardNumberEditText.shouldShowError

        assertFalse(isValid)
        assertTrue(shouldShowError)
    }

    @Test
    fun setEnabled_setsEnabledPropertyOnAllChildWidgets() {
        assertTrue(cardMultilineWidget.isEnabled)
        assertTrue(fullGroup.cardInputLayout.isEnabled)
        assertTrue(fullGroup.expiryInputLayout.isEnabled)
        assertTrue(fullGroup.postalCodeInputLayout.isEnabled)
        assertTrue(fullGroup.cvcInputLayout.isEnabled)
        assertTrue(fullGroup.expiryDateEditText.isEnabled)
        assertTrue(fullGroup.cardNumberEditText.isEnabled)
        assertTrue(fullGroup.cvcEditText.isEnabled)
        assertTrue(fullGroup.postalCodeEditText.isEnabled)

        cardMultilineWidget.isEnabled = false

        assertFalse(cardMultilineWidget.isEnabled)
        assertFalse(fullGroup.cardInputLayout.isEnabled)
        assertFalse(fullGroup.expiryInputLayout.isEnabled)
        assertFalse(fullGroup.postalCodeInputLayout.isEnabled)
        assertFalse(fullGroup.cvcInputLayout.isEnabled)
        assertFalse(fullGroup.expiryDateEditText.isEnabled)
        assertFalse(fullGroup.cardNumberEditText.isEnabled)
        assertFalse(fullGroup.cvcEditText.isEnabled)
        assertFalse(fullGroup.postalCodeEditText.isEnabled)

        cardMultilineWidget.isEnabled = true

        assertTrue(cardMultilineWidget.isEnabled)
        assertTrue(fullGroup.cardInputLayout.isEnabled)
        assertTrue(fullGroup.expiryInputLayout.isEnabled)
        assertTrue(fullGroup.postalCodeInputLayout.isEnabled)
        assertTrue(fullGroup.cvcInputLayout.isEnabled)
        assertTrue(fullGroup.expiryDateEditText.isEnabled)
        assertTrue(fullGroup.cardNumberEditText.isEnabled)
        assertTrue(fullGroup.cvcEditText.isEnabled)
        assertTrue(fullGroup.postalCodeEditText.isEnabled)
    }

    internal class WidgetControlGroup(parentWidget: CardMultilineWidget) {
        val cardNumberEditText: CardNumberEditText =
            parentWidget.findViewById(R.id.et_card_number)
        val cardInputLayout: TextInputLayout =
            parentWidget.findViewById(R.id.tl_card_number)
        val expiryDateEditText: ExpiryDateEditText =
            parentWidget.findViewById(R.id.et_expiry)
        val expiryInputLayout: TextInputLayout =
            parentWidget.findViewById(R.id.tl_expiry)
        val cvcEditText: StripeEditText =
            parentWidget.findViewById(R.id.et_cvc)
        val cvcInputLayout: TextInputLayout =
            parentWidget.findViewById(R.id.tl_cvc)
        val postalCodeEditText: StripeEditText =
            parentWidget.findViewById(R.id.et_postal_code)
        val postalCodeInputLayout: TextInputLayout =
            parentWidget.findViewById(R.id.tl_postal_code)
        val secondRowLayout: LinearLayout =
            parentWidget.findViewById(R.id.second_row_layout)
    }

    private companion object {
        // Every Card made by the CardInputView should have the card widget token.
        private val EXPECTED_LOGGING_ARRAY = arrayOf(CARD_MULTILINE_TOKEN)

        private val EMPTY_WATCHER = object : StripeTextWatcher() {}
    }
}
