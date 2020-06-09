package com.stripe.android.view

import android.app.Activity
import android.content.Context
import android.text.TextPaint
import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.CardNumberFixtures.AMEX_NO_SPACES
import com.stripe.android.CardNumberFixtures.AMEX_WITH_SPACES
import com.stripe.android.CardNumberFixtures.DINERS_CLUB_14_NO_SPACES
import com.stripe.android.CardNumberFixtures.DINERS_CLUB_14_WITH_SPACES
import com.stripe.android.CardNumberFixtures.DINERS_CLUB_16_NO_SPACES
import com.stripe.android.CardNumberFixtures.VISA_NO_SPACES
import com.stripe.android.CardNumberFixtures.VISA_WITH_SPACES
import com.stripe.android.PaymentConfiguration
import com.stripe.android.R
import com.stripe.android.model.Address
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.testharness.TestFocusChangeListener
import com.stripe.android.testharness.ViewTestUtils
import com.stripe.android.view.CardInputWidget.Companion.LOGGING_TOKEN
import com.stripe.android.view.CardInputWidget.Companion.shouldIconShowBrand
import java.util.Calendar
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class CardInputWidgetTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val activityScenarioFactory = ActivityScenarioFactory(context)

    private lateinit var cardInputWidget: CardInputWidget
    private val cardNumberEditText: CardNumberEditText by lazy {
        cardInputWidget.cardNumberEditText
    }
    private val expiryEditText: StripeEditText by lazy {
        cardInputWidget.expiryDateEditText
    }
    private val cvcEditText: CvcEditText by lazy {
        cardInputWidget.cvcNumberEditText
    }
    private val postalCodeEditText: PostalCodeEditText by lazy {
        cardInputWidget.postalCodeEditText
    }

    private val onGlobalFocusChangeListener: TestFocusChangeListener = TestFocusChangeListener()
    private val cardInputListener: CardInputListener = mock()

    @BeforeTest
    fun setup() {
        // The input date here will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050)
        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)

        activityScenarioFactory.create<AddPaymentMethodActivity>(
            AddPaymentMethodActivityStarter.Args.Builder()
                .setPaymentMethodType(PaymentMethod.Type.Card)
                .setPaymentConfiguration(PaymentConfiguration.getInstance(context))
                .setBillingAddressFields(BillingAddressFields.PostalCode)
                .build()
        ).use { activityScenario ->
            activityScenario.onActivity { activity ->
                activity.findViewById<ViewGroup>(R.id.add_payment_method_card).let { root ->
                    root.removeAllViews()
                    cardInputWidget = createCardInputWidget(activity)
                    root.addView(cardInputWidget)
                }
            }
        }
    }

    private fun createCardInputWidget(activity: Activity): CardInputWidget {
        return CardInputWidget(activity).also {
            it.layoutWidthCalculator = object : CardInputWidget.LayoutWidthCalculator {
                override fun calculate(text: String, paint: TextPaint): Int {
                    return text.length * 10
                }
            }

            it.frameWidthSupplier = {
                500 // That's a pretty small screen, but one that we theoretically support.
            }

            // Set the width of the icon and its margin so that test calculations have
            // an expected value that is repeatable on all systems.
            it.cardBrandView.layoutParams =
                (it.cardBrandView.layoutParams as ViewGroup.MarginLayoutParams)
                    .also { params ->
                        params.width = 48
                        params.rightMargin = 12
                    }

            it.viewTreeObserver
                .addOnGlobalFocusChangeListener(onGlobalFocusChangeListener)
        }
    }

    @Test
    fun getCard_whenInputIsValidVisa_withPostalCodeDisabled_returnsCardObjectWithLoggingToken() {
        cardInputWidget.postalCodeEnabled = false

        cardNumberEditText.setText(VISA_WITH_SPACES)
        expiryEditText.append("12")
        expiryEditText.append("50")
        cvcEditText.append(CVC_VALUE_COMMON)

        val card = cardInputWidget.card
        assertNotNull(card)
        assertEquals(VISA_NO_SPACES, card.number)
        assertNotNull(card.expMonth)
        assertNotNull(card.expYear)
        assertEquals(12, card.expMonth)
        assertEquals(2050, card.expYear)
        assertEquals(CVC_VALUE_COMMON, card.cvc)
        assertTrue(card.validateCard())
        assertEquals(ATTRIBUTION, card.loggingTokens)

        val actualPaymentMethodParams =
            requireNotNull(cardInputWidget.paymentMethodCreateParams)
        val expectedPaymentMethodParams =
            PaymentMethodCreateParams.create(
                card = PaymentMethodCreateParams.Card(
                    number = VISA_NO_SPACES,
                    cvc = CVC_VALUE_COMMON,
                    expiryMonth = 12,
                    expiryYear = 2050,
                    attribution = ATTRIBUTION
                )
            )
        assertEquals(expectedPaymentMethodParams, actualPaymentMethodParams)
    }

    @Test
    fun getCard_whenInputIsValidVisa_withPostalCodeEnabled_returnsCardObjectWithLoggingToken() {
        cardInputWidget.postalCodeEnabled = true

        cardNumberEditText.setText(VISA_WITH_SPACES)
        expiryEditText.append("12")
        expiryEditText.append("50")
        cvcEditText.append(CVC_VALUE_COMMON)
        postalCodeEditText.setText(POSTAL_CODE_VALUE)

        val card = cardInputWidget.card
        assertNotNull(card)
        assertEquals(VISA_NO_SPACES, card.number)
        assertEquals(12, card.expMonth)
        assertEquals(2050, card.expYear)
        assertEquals(CVC_VALUE_COMMON, card.cvc)
        assertEquals(POSTAL_CODE_VALUE, card.addressZip)
        assertTrue(card.validateCard())
        assertEquals(ATTRIBUTION, card.loggingTokens)

        val actualPaymentMethodParams =
            requireNotNull(cardInputWidget.paymentMethodCreateParams)
        val expectedPaymentMethodParams =
            PaymentMethodCreateParams.create(
                card = PaymentMethodCreateParams.Card(
                    number = VISA_NO_SPACES,
                    cvc = CVC_VALUE_COMMON,
                    expiryMonth = 12,
                    expiryYear = 2050,
                    attribution = ATTRIBUTION
                ),
                billingDetails = PaymentMethod.BillingDetails(
                    address = Address(
                        postalCode = POSTAL_CODE_VALUE
                    )
                )
            )
        assertEquals(expectedPaymentMethodParams, actualPaymentMethodParams)
    }

    @Test
    fun getCard_whenInputIsValidAmEx_withPostalCodeDisabled_createsExpectedObjects() {
        cardInputWidget.postalCodeEnabled = false

        cardNumberEditText.setText(AMEX_WITH_SPACES)
        expiryEditText.append("12")
        expiryEditText.append("50")
        cvcEditText.append(CVC_VALUE_AMEX)

        val card = requireNotNull(cardInputWidget.card)
        assertEquals(AMEX_NO_SPACES, card.number)
        assertNotNull(card.expMonth)
        assertNotNull(card.expYear)
        assertEquals(12, card.expMonth)
        assertEquals(2050, card.expYear)
        assertEquals(CVC_VALUE_AMEX, card.cvc)
        assertTrue(card.validateCard())
        assertEquals(ATTRIBUTION, card.loggingTokens)

        val actualPaymentMethodParams =
            requireNotNull(cardInputWidget.paymentMethodCreateParams)
        val expectedPaymentMethodParams = PaymentMethodCreateParams.create(
            PaymentMethodCreateParams.Card(
                number = AMEX_NO_SPACES,
                cvc = CVC_VALUE_AMEX,
                expiryMonth = 12,
                expiryYear = 2050,
                attribution = ATTRIBUTION
            )
        )
        assertEquals(expectedPaymentMethodParams, actualPaymentMethodParams)
    }

    @Test
    fun getCard_whenInputIsValidAmEx_withPostalCodeEnabled_createsExpectedObjects() {
        cardInputWidget.postalCodeEnabled = true

        cardNumberEditText.setText(AMEX_WITH_SPACES)
        expiryEditText.append("12")
        expiryEditText.append("50")
        cvcEditText.append(CVC_VALUE_AMEX)
        postalCodeEditText.setText(POSTAL_CODE_VALUE)

        val card = requireNotNull(cardInputWidget.card)
        assertEquals(AMEX_NO_SPACES, card.number)
        assertNotNull(card.expMonth)
        assertNotNull(card.expYear)
        assertEquals(12, card.expMonth)
        assertEquals(2050, card.expYear)
        assertEquals(CVC_VALUE_AMEX, card.cvc)
        assertEquals(POSTAL_CODE_VALUE, card.addressZip)
        assertTrue(card.validateCard())
        assertEquals(ATTRIBUTION, card.loggingTokens)

        val actualPaymentMethodParams = cardInputWidget.paymentMethodCreateParams
        assertNotNull(actualPaymentMethodParams)
        val expectedPaymentMethodParams =
            PaymentMethodCreateParams.create(
                card = PaymentMethodCreateParams.Card(
                    number = AMEX_NO_SPACES,
                    cvc = CVC_VALUE_AMEX,
                    expiryYear = 2050,
                    expiryMonth = 12,
                    attribution = ATTRIBUTION
                ),
                billingDetails = PaymentMethod.BillingDetails.Builder()
                    .setAddress(Address(
                        postalCode = POSTAL_CODE_VALUE
                    ))
                    .build()
            )
        assertEquals(expectedPaymentMethodParams, actualPaymentMethodParams)
    }

    @Test
    fun getCard_whenInputIsValidDinersClub_withPostalCodeDisabled_returnsCardObjectWithLoggingToken() {
        cardInputWidget.postalCodeEnabled = false

        cardNumberEditText.setText(DINERS_CLUB_14_WITH_SPACES)
        expiryEditText.append("12")
        expiryEditText.append("50")
        cvcEditText.append(CVC_VALUE_COMMON)

        val card = requireNotNull(cardInputWidget.card)
        assertEquals(DINERS_CLUB_14_NO_SPACES, card.number)
        assertNotNull(card.expMonth)
        assertNotNull(card.expYear)
        assertEquals(12, card.expMonth)
        assertEquals(2050, card.expYear)
        assertEquals(CVC_VALUE_COMMON, card.cvc)
        assertTrue(card.validateCard())
        assertEquals(ATTRIBUTION, card.loggingTokens)

        val paymentMethodCard = cardInputWidget.paymentMethodCard
        assertNotNull(paymentMethodCard)
        val expectedPaymentMethodCard =
            PaymentMethodCreateParams.Card(
                number = DINERS_CLUB_14_NO_SPACES,
                cvc = CVC_VALUE_COMMON,
                expiryMonth = 12,
                expiryYear = 2050,
                attribution = ATTRIBUTION
            )
        assertEquals(expectedPaymentMethodCard, paymentMethodCard)
    }

    @Test
    fun getCard_whenInputIsValidDinersClub_withPostalCodeEnabled_returnsCardObjectWithLoggingToken() {
        cardInputWidget.postalCodeEnabled = true

        cardNumberEditText.setText(DINERS_CLUB_14_WITH_SPACES)
        expiryEditText.append("12")
        expiryEditText.append("50")
        cvcEditText.append(CVC_VALUE_COMMON)
        postalCodeEditText.setText(POSTAL_CODE_VALUE)

        val card = requireNotNull(cardInputWidget.card)
        assertEquals(DINERS_CLUB_14_NO_SPACES, card.number)
        assertNotNull(card.expMonth)
        assertNotNull(card.expYear)
        assertEquals(12, card.expMonth)
        assertEquals(2050, card.expYear)
        assertEquals(CVC_VALUE_COMMON, card.cvc)
        assertTrue(card.validateCard())
        assertEquals(ATTRIBUTION, card.loggingTokens)

        val paymentMethodCard = cardInputWidget.paymentMethodCard
        assertNotNull(paymentMethodCard)
        val expectedPaymentMethodCard = PaymentMethodCreateParams.Card(
            number = DINERS_CLUB_14_NO_SPACES,
            cvc = CVC_VALUE_COMMON,
            expiryYear = 2050,
            expiryMonth = 12,
            attribution = ATTRIBUTION
        )
        assertEquals(expectedPaymentMethodCard, paymentMethodCard)
    }

    @Test
    fun getCard_whenPostalCodeIsEnabledAndRequired_andValueIsBlank_returnsNull() {
        cardInputWidget.postalCodeEnabled = true
        cardInputWidget.postalCodeRequired = true

        cardNumberEditText.setText(VISA_WITH_SPACES)
        expiryEditText.append("12")
        expiryEditText.append("50")
        cvcEditText.append(CVC_VALUE_COMMON)
        postalCodeEditText.append("")

        val card = cardInputWidget.card
        assertNull(card)

        val paymentMethodCard = cardInputWidget.paymentMethodCard
        assertNull(paymentMethodCard)
    }

    @Test
    fun getCard_whenInputHasIncompleteCardNumber_returnsNull() {
        // This will be 242 4242 4242 4242
        cardNumberEditText.setText(VISA_WITH_SPACES.substring(1))
        expiryEditText.append("12")
        expiryEditText.append("50")
        cvcEditText.append(CVC_VALUE_COMMON)

        val card = cardInputWidget.card
        assertNull(card)

        val paymentMethodCard = cardInputWidget.paymentMethodCard
        assertNull(paymentMethodCard)
    }

    @Test
    fun getCard_whenInputHasExpiredDate_returnsNull() {
        // The test will be testing the wrong variable after 2080. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2080)

        cardNumberEditText.setText(VISA_WITH_SPACES)
        // Date interpreted as 12/2012 until 2080, when it will be 12/2112
        expiryEditText.append("12")
        expiryEditText.append("12")
        cvcEditText.append(CVC_VALUE_COMMON)

        val card = cardInputWidget.card
        assertNull(card)

        val paymentMethodCard = cardInputWidget.paymentMethodCard
        assertNull(paymentMethodCard)
    }

    @Test
    fun getCard_whenIncompleteCvCForVisa_returnsNull() {
        cardNumberEditText.setText(VISA_WITH_SPACES)
        expiryEditText.append("12")
        expiryEditText.append("50")
        cvcEditText.append("12")

        val card = cardInputWidget.card
        assertNull(card)

        val paymentMethodCard = cardInputWidget.paymentMethodCard
        assertNull(paymentMethodCard)
    }

    @Test
    fun getCard_doesNotValidatePostalCode() {
        cardInputWidget.postalCodeEnabled = true

        cardNumberEditText.setText(VISA_WITH_SPACES)
        expiryEditText.append("12")
        expiryEditText.append("50")
        cvcEditText.append(CVC_VALUE_COMMON)
        postalCodeEditText.setText("")

        val card = cardInputWidget.card
        assertNotNull(card)

        val paymentMethodCard = cardInputWidget.paymentMethodCard
        assertNotNull(paymentMethodCard)
    }

    @Test
    fun getCard_when3DigitCvCForAmEx_withPostalCodeDisabled_returnsCard() {
        cardInputWidget.postalCodeEnabled = false

        cardNumberEditText.setText(AMEX_WITH_SPACES)
        expiryEditText.append("12")
        expiryEditText.append("50")
        cvcEditText.append(CVC_VALUE_COMMON)

        val card = cardInputWidget.card
        assertNotNull(card)

        val paymentMethodCard = cardInputWidget.paymentMethodCard
        assertNotNull(paymentMethodCard)
    }

    @Test
    fun getCard_when3DigitCvCForAmEx_withPostalCodeEnabled_returnsCard() {
        cardInputWidget.postalCodeEnabled = true

        cardNumberEditText.setText(AMEX_WITH_SPACES)
        expiryEditText.append("12")
        expiryEditText.append("50")
        cvcEditText.append(CVC_VALUE_COMMON)
        postalCodeEditText.setText(POSTAL_CODE_VALUE)

        val card = cardInputWidget.card
        assertNotNull(card)

        val paymentMethodCard = cardInputWidget.paymentMethodCard
        assertNotNull(paymentMethodCard)
    }

    @Test
    fun getCard_whenIncompleteCvCForAmEx_returnsNull() {
        cardNumberEditText.setText(AMEX_WITH_SPACES)
        expiryEditText.append("12")
        expiryEditText.append("50")
        cvcEditText.append("12")

        val card = cardInputWidget.card
        assertNull(card)

        val paymentMethodCard = cardInputWidget.paymentMethodCard
        assertNull(paymentMethodCard)
    }

    @Test
    fun getPaymentMethodCreateParams_shouldReturnExpectedObject() {
        cardInputWidget.postalCodeEnabled = true

        cardInputWidget.setCardNumber(VISA_NO_SPACES)
        cardInputWidget.setExpiryDate(12, 2030)
        cardInputWidget.setCvcCode(CVC_VALUE_COMMON)
        cardInputWidget.setPostalCode(POSTAL_CODE_VALUE)

        val params = cardInputWidget.paymentMethodCreateParams
        assertNotNull(params)

        val expectedParams = PaymentMethodCreateParams.create(
            card = PaymentMethodCreateParams.Card(
                number = VISA_NO_SPACES,
                cvc = CVC_VALUE_COMMON,
                expiryMonth = 12,
                expiryYear = 2030,
                attribution = ATTRIBUTION
            ),
            billingDetails = PaymentMethod.BillingDetails(
                address = Address(
                    postalCode = POSTAL_CODE_VALUE
                )
            )
        )
        assertEquals(expectedParams, params)
    }

    @Test
    fun getCard_whenIncompleteCvCForDiners_returnsNull() {
        cardNumberEditText.setText(DINERS_CLUB_14_WITH_SPACES)
        expiryEditText.append("12")
        expiryEditText.append("50")
        cvcEditText.append("12")

        val card = cardInputWidget.card
        assertNull(card)

        val paymentMethodCard = cardInputWidget.paymentMethodCard
        assertNull(paymentMethodCard)
    }

    @Test
    fun onCompleteCardNumber_whenValid_shiftsFocusToExpiryDate() {
        cardInputWidget.setCardInputListener(cardInputListener)

        cardNumberEditText.setText(VISA_WITH_SPACES)

        verify(cardInputListener).onCardComplete()
        verify(cardInputListener).onFocusChange(CardInputListener.FocusField.ExpiryDate)
        assertEquals(cardNumberEditText.id, onGlobalFocusChangeListener.oldFocusId)
        assertEquals(expiryEditText.id, onGlobalFocusChangeListener.newFocusId)
    }

    @Test
    fun onDeleteFromExpiryDate_whenEmpty_shiftsFocusToCardNumberAndDeletesDigit() {
        cardInputWidget.setCardInputListener(cardInputListener)
        cardNumberEditText.setText(VISA_WITH_SPACES)
        assertTrue(expiryEditText.hasFocus())

        // The above functionality is tested elsewhere, so we reset this listener.
        reset(cardInputListener)

        ViewTestUtils.sendDeleteKeyEvent(expiryEditText)
        verify(cardInputListener).onFocusChange(CardInputListener.FocusField.CardNumber)
        assertEquals(expiryEditText.id, onGlobalFocusChangeListener.oldFocusId)
        assertEquals(cardNumberEditText.id, onGlobalFocusChangeListener.newFocusId)

        val subString = VISA_WITH_SPACES.substring(0, VISA_WITH_SPACES.length - 1)
        assertEquals(subString, cardNumberEditText.text.toString())
        assertEquals(subString.length, cardNumberEditText.selectionStart)
    }

    @Test
    fun onDeleteFromExpiryDate_whenNotEmpty_doesNotShiftFocusOrDeleteDigit() {
        cardNumberEditText.setText(AMEX_WITH_SPACES)
        assertTrue(expiryEditText.hasFocus())

        expiryEditText.append("1")
        ViewTestUtils.sendDeleteKeyEvent(expiryEditText)

        assertTrue(expiryEditText.hasFocus())
        assertEquals(AMEX_WITH_SPACES, cardNumberEditText.text.toString())
    }

    @Test
    fun onDeleteFromCvcDate_whenEmpty_shiftsFocusToExpiryAndDeletesDigit() {
        // This test will be invalid if run between 2080 and 2112. Please update the code.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2080)

        cardInputWidget.setCardInputListener(cardInputListener)
        cardNumberEditText.setText(VISA_WITH_SPACES)

        verify(cardInputListener).onCardComplete()
        verify(cardInputListener).onFocusChange(CardInputListener.FocusField.ExpiryDate)

        expiryEditText.append("12")
        expiryEditText.append("79")

        verify(cardInputListener).onExpirationComplete()
        verify(cardInputListener).onFocusChange(CardInputListener.FocusField.Cvc)
        assertTrue(cvcEditText.hasFocus())

        // Clearing already-verified data.
        reset(cardInputListener)

        ViewTestUtils.sendDeleteKeyEvent(cvcEditText)
        verify(cardInputListener).onFocusChange(CardInputListener.FocusField.ExpiryDate)
        assertEquals(cvcEditText.id, onGlobalFocusChangeListener.oldFocusId)
        assertEquals(expiryEditText.id, onGlobalFocusChangeListener.newFocusId)

        val expectedResult = "12/7"
        assertEquals(expectedResult, expiryEditText.text.toString())
        assertEquals(expectedResult.length, expiryEditText.selectionStart)
    }

    @Test
    fun onDeleteFromCvcDate_withPostalCodeDisabled_whenNotEmpty_doesNotShiftFocusOrDeleteEntry() {
        cardInputWidget.postalCodeEnabled = false

        // This test will be invalid if run between 2080 and 2112. Please update the code.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2080)

        cardNumberEditText.setText(AMEX_WITH_SPACES)

        expiryEditText.append("12")
        expiryEditText.append("79")
        assertTrue(cvcEditText.hasFocus())

        cvcEditText.append(CVC_VALUE_COMMON)
        ViewTestUtils.sendDeleteKeyEvent(cvcEditText)

        assertTrue(cvcEditText.hasFocus())
        assertEquals("12/79", expiryEditText.text.toString())
    }

    @Test
    fun onDeleteFromCvcDate_withPostalCodeEnabled_whenNotEmpty_doesNotShiftFocusOrDeleteEntry() {
        cardInputWidget.postalCodeEnabled = true

        // This test will be invalid if run between 2080 and 2112. Please update the code.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2080)

        cardNumberEditText.setText(AMEX_WITH_SPACES)

        expiryEditText.append("12")
        expiryEditText.append("79")
        assertTrue(cvcEditText.hasFocus())

        cvcEditText.append("12")
        ViewTestUtils.sendDeleteKeyEvent(cvcEditText)

        assertTrue(cvcEditText.hasFocus())
        assertEquals("12/79", expiryEditText.text.toString())
    }

    @Test
    fun onDeleteFromCvcDate_whenEmptyAndExpiryDateIsEmpty_shiftsFocusOnly() {
        cardNumberEditText.setText(DINERS_CLUB_14_WITH_SPACES)

        // Simulates user tapping into this text field without filling out the date first.
        cvcEditText.requestFocus()

        ViewTestUtils.sendDeleteKeyEvent(cvcEditText)
        assertEquals(cvcEditText.id, onGlobalFocusChangeListener.oldFocusId)
        assertEquals(expiryEditText.id, onGlobalFocusChangeListener.newFocusId)
    }

    @Test
    fun onUpdateIcon_forCommonLengthBrand_setsLengthOnCvc() {
        // This should set the brand to Visa. Note that more extensive brand checking occurs
        // in CardNumberEditTextTest.
        cardNumberEditText.append("4")
        assertTrue(ViewTestUtils.hasMaxLength(cvcEditText, 3))
    }

    @Test
    fun onUpdateText_forAmExPrefix_setsLengthOnCvc() {
        cardNumberEditText.append("34")
        assertTrue(ViewTestUtils.hasMaxLength(cvcEditText, 4))
    }

    @Test
    fun updateToInitialSizes_returnsExpectedValues() {
        // Initial spacing should look like
        // |img==60||---total == 500--------|
        // |(card==190)--(space==260)--(date==50)|
        // |img==60||  cardTouchArea | 380 | dateTouchArea | dateStart==510 |

        val initialParameters = cardInputWidget.placementParameters
        assertEquals(190, initialParameters.cardWidth)
        assertEquals(50, initialParameters.dateWidth)
        assertEquals(260, initialParameters.cardDateSeparation)
        assertEquals(380, initialParameters.cardTouchBufferLimit)
        assertEquals(510, initialParameters.dateStartPosition)
    }

    @Test
    fun updateToPeekSize_withPostalCodeDisabled_withNoData_returnsExpectedValuesForCommonCardLength() {
        cardInputWidget.postalCodeEnabled = false

        // Moving left uses Visa-style ("common") defaults
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        cardInputWidget.updateSpaceSizes(false)
        val shiftedParameters = cardInputWidget.placementParameters
        assertEquals(40, shiftedParameters.peekCardWidth)
        assertEquals(185, shiftedParameters.cardDateSeparation)
        assertEquals(50, shiftedParameters.dateWidth)
        assertEquals(195, shiftedParameters.dateCvcSeparation)
        assertEquals(30, shiftedParameters.cvcWidth)
        assertEquals(192, shiftedParameters.cardTouchBufferLimit)
        assertEquals(285, shiftedParameters.dateStartPosition)
        assertEquals(432, shiftedParameters.dateRightTouchBufferLimit)
        assertEquals(530, shiftedParameters.cvcStartPosition)
    }

    @Test
    fun updateToPeekSize_withPostalCodeEnabled_withNoData_returnsExpectedValuesForCommonCardLength() {
        cardInputWidget.postalCodeEnabled = true

        // Moving left uses Visa-style ("common") defaults
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        cardInputWidget.updateSpaceSizes(false)
        val shiftedParameters = cardInputWidget.placementParameters
        assertEquals(40, shiftedParameters.peekCardWidth)
        assertEquals(98, shiftedParameters.cardDateSeparation)
        assertEquals(50, shiftedParameters.dateWidth)
        assertEquals(82, shiftedParameters.dateCvcSeparation)
        assertEquals(30, shiftedParameters.cvcWidth)
        assertEquals(66, shiftedParameters.cardTouchBufferLimit)
        assertEquals(198, shiftedParameters.dateStartPosition)
        assertEquals(110, shiftedParameters.dateRightTouchBufferLimit)
        assertEquals(330, shiftedParameters.cvcStartPosition)
    }

    @Test
    fun getFocusRequestOnTouch_whenTouchOnImage_returnsNull() {
        // |img==60||---total == 500--------|
        // |(card==190)--(space==260)--(date==50)|
        // |img==60||  cardTouchArea | 380 | dateTouchArea | dateStart==510 |
        // So any touch lower than 60 will be the icon
        assertNull(cardInputWidget.getFocusRequestOnTouch(30))
    }

    @Test
    fun getFocusRequestOnTouch_whenTouchActualCardWidget_returnsNull() {
        // |img==60||---total == 500--------|
        // |(card==190)--(space==260)--(date==50)|
        // |img==60||  cardTouchArea | 380 | dateTouchArea | dateStart==510 |
        // So any touch between 60 and 250 will be the actual card widget
        assertNull(cardInputWidget.getFocusRequestOnTouch(200))
    }

    @Test
    fun getFocusRequestOnTouch_whenTouchInCardEditorSlop_returnsCardEditor() {
        // |img==60||---total == 500--------|
        // |(card==190)--(space==260)--(date==50)|
        // |img==60||  cardTouchArea | 380 | dateTouchArea | dateStart==510 |
        // So any touch between 250 and 380 needs to send focus to the card editor
        val focusRequester = cardInputWidget.getFocusRequestOnTouch(300)
        assertNotNull(focusRequester)
        assertEquals(cardNumberEditText, focusRequester)
    }

    @Test
    fun getFocusRequestOnTouch_whenTouchInDateSlop_returnsDateEditor() {
        // |img==60||---total == 500--------|
        // |(card==190)--(space==260)--(date==50)|
        // |img==60||  cardTouchArea | 380 | dateTouchArea | dateStart==510 |
        // So any touch between 380 and 510 needs to send focus to the date editor
        val focusRequester = cardInputWidget.getFocusRequestOnTouch(390)
        assertNotNull(focusRequester)
        assertEquals(expiryEditText, focusRequester)
    }

    @Test
    fun getFocusRequestOnTouch_whenTouchInDateEditor_returnsNull() {
        // |img==60||---total == 500--------|
        // |(card==190)--(space==260)--(date==50)|
        // |img==60||  cardTouchArea | 380 | dateTouchArea | dateStart==510 |
        // So any touch over 510 doesn't need to do anything
        assertNull(cardInputWidget.getFocusRequestOnTouch(530))
    }

    @Test
    fun getFocusRequestOnTouch_whenInPeekAfterShift_returnsNull() {
        // |img==60||---total == 500--------|
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        // So any touch between 60 and 100 does nothing
        cardInputWidget.isShowingFullCard = false
        cardInputWidget.updateSpaceSizes(false)
        assertNull(cardInputWidget.getFocusRequestOnTouch(75))
    }

    @Test
    fun getFocusRequestOnTouch_whenInPeekSlopAfterShift_withPostalCodeDisabled_returnsCardEditor() {
        cardInputWidget.postalCodeEnabled = false

        // |img==60||---total == 500--------|
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        // So any touch between 100 and 192 returns the card editor
        cardInputWidget.isShowingFullCard = false
        cardInputWidget.updateSpaceSizes(false)
        val focusRequester = cardInputWidget.getFocusRequestOnTouch(150)
        assertNotNull(focusRequester)
        assertEquals(cardNumberEditText, focusRequester)
    }

    @Test
    fun getFocusRequestOnTouch_whenInDateLeftSlopAfterShift_withPostalCodeDisabled_returnsDateEditor() {
        cardInputWidget.postalCodeEnabled = false

        // |img==60||---total == 500--------|
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        // So any touch between 192 and 285 returns the date editor
        cardInputWidget.isShowingFullCard = false
        cardInputWidget.updateSpaceSizes(false)
        val focusRequester = cardInputWidget.getFocusRequestOnTouch(200)
        assertNotNull(focusRequester)
        assertEquals(expiryEditText, focusRequester)
    }

    @Test
    fun getFocusRequestOnTouch_whenInDateLeftSlopAfterShift_withPostalCodeEnabled_returnsDateEditor() {
        cardInputWidget.postalCodeEnabled = true

        // |img==60||---total == 500--------|
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        // So any touch between 192 and 285 returns the date editor
        cardInputWidget.isShowingFullCard = false
        cardInputWidget.updateSpaceSizes(false)
        val focusRequester = cardInputWidget.getFocusRequestOnTouch(170)
        assertNotNull(focusRequester)
        assertEquals(expiryEditText, focusRequester)
    }

    @Test
    fun getFocusRequestOnTouch_whenInDateAfterShift_withPostalCodeDisabled_returnsNull() {
        cardInputWidget.postalCodeEnabled = false

        // |img==60||---total == 500--------|
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        // So any touch between 285 and 335 does nothing
        cardInputWidget.isShowingFullCard = false
        cardInputWidget.updateSpaceSizes(false)
        assertNull(cardInputWidget.getFocusRequestOnTouch(300))
    }

    @Test
    fun getFocusRequestOnTouch_withPostalCodeEnabled_whenInDateAfterShift_returnsNull() {
        cardInputWidget.postalCodeEnabled = true

        // |img==60||---total == 500--------|
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        // So any touch between 285 and 335 does nothing
        cardInputWidget.isShowingFullCard = false
        cardInputWidget.updateSpaceSizes(false)
        assertNull(cardInputWidget.getFocusRequestOnTouch(200))
    }

    @Test
    fun getFocusRequestOnTouch_withPostalCodeDisabled_whenInDateRightSlopAfterShift_returnsDateEditor() {
        cardInputWidget.postalCodeEnabled = false

        // |img==60||---total == 500--------|
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        // So any touch between 335 and 432 returns the date editor
        cardInputWidget.isShowingFullCard = false
        cardInputWidget.updateSpaceSizes(false)
        val focusRequester = cardInputWidget.getFocusRequestOnTouch(400)
        assertNotNull(focusRequester)
        assertEquals(expiryEditText, focusRequester)
    }

    @Test
    fun getFocusRequestOnTouch_withPostalCodeEnabled_whenInDateRightSlopAfterShift_returnsDateEditor() {
        cardInputWidget.postalCodeEnabled = true

        // |img==60||---total == 500--------|
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        // So any touch between 335 and 432 returns the date editor
        cardInputWidget.isShowingFullCard = false
        cardInputWidget.updateSpaceSizes(false)
        val focusRequester = cardInputWidget.getFocusRequestOnTouch(185)
        assertNotNull(focusRequester)
        assertEquals(expiryEditText, focusRequester)
    }

    @Test
    fun getFocusRequestOnTouch_withPostalCodeDisabled_whenInCvcSlopAfterShift_returnsCvcEditor() {
        cardInputWidget.postalCodeEnabled = false

        // |img==60||---total == 500--------|
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        // So any touch between 432 and 530 returns the date editor
        cardInputWidget.isShowingFullCard = false
        cardInputWidget.updateSpaceSizes(false)
        val focusRequester = cardInputWidget.getFocusRequestOnTouch(485)
        assertNotNull(focusRequester)
        assertEquals(cvcEditText, focusRequester)
    }

    @Test
    fun getFocusRequestOnTouch_withPostalCodeEnabled_whenInCvcSlopAfterShift_returnsCvcEditor() {
        cardInputWidget.postalCodeEnabled = true

        // |img==60||---total == 500--------|
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        // So any touch between 432 and 530 returns the date editor
        cardInputWidget.isShowingFullCard = false
        cardInputWidget.updateSpaceSizes(false)
        val focusRequester = cardInputWidget.getFocusRequestOnTouch(300)
        assertNotNull(focusRequester)
        assertEquals(cvcEditText, focusRequester)
    }

    @Test
    fun getFocusRequestOnTouch_whenInCvcAfterShift_returnsNull() {
        // |img==60||---total == 500--------|
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        // So any touch over 530 does nothing
        cardInputWidget.isShowingFullCard = false
        cardInputWidget.updateSpaceSizes(false)
        assertNull(cardInputWidget.getFocusRequestOnTouch(545))
    }

    @Test
    fun addValidVisaCard_withPostalCodeDisabled_scrollsOver_andSetsExpectedDisplayValues() {
        cardInputWidget.postalCodeEnabled = false

        // Moving left with an actual Visa number does the same as moving when empty.
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        cardNumberEditText.setText(VISA_WITH_SPACES)
        val shiftedParameters = cardInputWidget.placementParameters
        assertEquals(40, shiftedParameters.peekCardWidth)
        assertEquals(185, shiftedParameters.cardDateSeparation)
        assertEquals(50, shiftedParameters.dateWidth)
        assertEquals(195, shiftedParameters.dateCvcSeparation)
        assertEquals(30, shiftedParameters.cvcWidth)
        assertEquals(0, shiftedParameters.cvcPostalCodeSeparation)
        assertEquals(100, shiftedParameters.postalCodeWidth)
    }

    @Test
    fun addValidVisaCard_withPostalCodeEnabled_scrollsOver_andSetsExpectedDisplayValues() {
        cardInputWidget.postalCodeEnabled = true

        // Moving left with an actual Visa number does the same as moving when empty.
        // |(peek==40)--(space==98)--(date==50)--(space==82)--(cvc==30)--(space==0)--(postal==100)|
        cardNumberEditText.setText(VISA_WITH_SPACES)
        val shiftedParameters = cardInputWidget.placementParameters
        assertEquals(40, shiftedParameters.peekCardWidth)
        assertEquals(98, shiftedParameters.cardDateSeparation)
        assertEquals(50, shiftedParameters.dateWidth)
        assertEquals(82, shiftedParameters.dateCvcSeparation)
        assertEquals(30, shiftedParameters.cvcWidth)
        assertEquals(0, shiftedParameters.cvcPostalCodeSeparation)
        assertEquals(100, shiftedParameters.postalCodeWidth)
    }

    @Test
    fun addValidAmExCard_withPostalCodeDisabled_scrollsOver_andSetsExpectedDisplayValues() {
        cardInputWidget.postalCodeEnabled = false

        // Moving left with an AmEx number has a larger peek and cvc size.
        // |(peek==50)--(space==175)--(date==50)--(space==185)--(cvc==40)|
        cardNumberEditText.setText(AMEX_WITH_SPACES)
        val shiftedParameters = cardInputWidget.placementParameters
        assertEquals(50, shiftedParameters.peekCardWidth)
        assertEquals(175, shiftedParameters.cardDateSeparation)
        assertEquals(50, shiftedParameters.dateWidth)
        assertEquals(185, shiftedParameters.dateCvcSeparation)
        assertEquals(40, shiftedParameters.cvcWidth)
        assertEquals(0, shiftedParameters.cvcPostalCodeSeparation)
        assertEquals(100, shiftedParameters.postalCodeWidth)
    }

    @Test
    fun addValidAmExCard_withPostalCodeEnabled_scrollsOver_andSetsExpectedDisplayValues() {
        cardInputWidget.postalCodeEnabled = true

        // Moving left with an AmEx number has a larger peek and cvc size.
        // |(peek==50)--(space==88)--(date==50)--(space==72)--(cvc==40)--(space==0)--(postal==100)|
        cardNumberEditText.setText(AMEX_WITH_SPACES)
        val shiftedParameters = cardInputWidget.placementParameters
        assertEquals(50, shiftedParameters.peekCardWidth)
        assertEquals(88, shiftedParameters.cardDateSeparation)
        assertEquals(50, shiftedParameters.dateWidth)
        assertEquals(72, shiftedParameters.dateCvcSeparation)
        assertEquals(40, shiftedParameters.cvcWidth)
        assertEquals(0, shiftedParameters.cvcPostalCodeSeparation)
        assertEquals(100, shiftedParameters.postalCodeWidth)
    }

    @Test
    fun addDinersClubCard_withPostalCodeDisabled_scrollsOver_andSetsExpectedDisplayValues() {
        cardInputWidget.postalCodeEnabled = false

        // When we move for a Diner's club card, the peek text is shorter, so we expect:
        // |(peek==20)--(space==205)--(date==50)--(space==195)--(cvc==30)|
        cardNumberEditText.setText(DINERS_CLUB_14_WITH_SPACES)
        val shiftedParameters = cardInputWidget.placementParameters
        assertEquals(20, shiftedParameters.peekCardWidth)
        assertEquals(205, shiftedParameters.cardDateSeparation)
        assertEquals(50, shiftedParameters.dateWidth)
        assertEquals(195, shiftedParameters.dateCvcSeparation)
        assertEquals(30, shiftedParameters.cvcWidth)
        assertEquals(0, shiftedParameters.cvcPostalCodeSeparation)
        assertEquals(100, shiftedParameters.postalCodeWidth)
    }

    @Test
    fun addDinersClubCard_withPostalCodeEnabled_scrollsOver_andSetsExpectedDisplayValues() {
        cardInputWidget.postalCodeEnabled = true

        // When we move for a Diner's club card, the peek text is shorter, so we expect:
        // |(peek==20)--(space==205)--(date==50)--(space==195)--(cvc==30)--(space==0)--(postal==100)|
        cardNumberEditText.setText(DINERS_CLUB_14_WITH_SPACES)
        val shiftedParameters = cardInputWidget.placementParameters
        assertEquals(20, shiftedParameters.peekCardWidth)
        assertEquals(118, shiftedParameters.cardDateSeparation)
        assertEquals(50, shiftedParameters.dateWidth)
        assertEquals(82, shiftedParameters.dateCvcSeparation)
        assertEquals(30, shiftedParameters.cvcWidth)
        assertEquals(0, shiftedParameters.cvcPostalCodeSeparation)
        assertEquals(100, shiftedParameters.postalCodeWidth)
    }

    @Test
    fun setCardNumber_withIncompleteNumber_doesNotValidateCard() {
        cardInputWidget.setCardNumber("123456")
        assertFalse(cardNumberEditText.isCardNumberValid)
        assertTrue(cardNumberEditText.hasFocus())
    }

    @Test
    fun setExpirationDate_withValidData_setsCorrectValues() {
        cardInputWidget.setExpiryDate(12, 79)
        assertEquals("12/79", expiryEditText.text.toString())
    }

    @Test
    fun setCvcCode_withValidData_setsValue() {
        cardInputWidget.setCvcCode(CVC_VALUE_COMMON)
        assertEquals(CVC_VALUE_COMMON, cvcEditText.text.toString())
    }

    @Test
    fun setCvcCode_withLongString_truncatesValue() {
        cvcEditText.updateBrand(CardBrand.Visa)
        cardInputWidget.setCvcCode(CVC_VALUE_AMEX)
        assertEquals(CVC_VALUE_COMMON, cvcEditText.text.toString())
    }

    @Test
    fun setCvcCode_whenCardBrandIsAmericanExpress_allowsFourDigits() {
        cardInputWidget.setCardNumber(AMEX_NO_SPACES)
        cardInputWidget.setCvcCode(CVC_VALUE_AMEX)
        assertEquals("1234", cvcEditText.text.toString())
    }

    @Test
    fun setEnabledTrue_withPostalCodeDisabled_isTrue() {
        cardInputWidget.postalCodeEnabled = false
        cardInputWidget.isEnabled = true
        assertTrue(cardNumberEditText.isEnabled)
        assertTrue(expiryEditText.isEnabled)
        assertTrue(cvcEditText.isEnabled)
        assertFalse(postalCodeEditText.isEnabled)
    }

    @Test
    fun setEnabledTrue_withPostalCodeEnabled_isTrue() {
        cardInputWidget.postalCodeEnabled = true
        cardInputWidget.isEnabled = true
        assertTrue(cardNumberEditText.isEnabled)
        assertTrue(expiryEditText.isEnabled)
        assertTrue(cvcEditText.isEnabled)
        assertTrue(postalCodeEditText.isEnabled)
    }

    @Test
    fun setEnabledFalse_withPostalCodeDisabled_isFalse() {
        cardInputWidget.postalCodeEnabled = false
        cardInputWidget.isEnabled = false
        assertFalse(cardNumberEditText.isEnabled)
        assertFalse(expiryEditText.isEnabled)
        assertFalse(cvcEditText.isEnabled)
        assertFalse(postalCodeEditText.isEnabled)
    }

    @Test
    fun setEnabledFalse_withPostalCodeEnabled_isFalse() {
        cardInputWidget.postalCodeEnabled = true
        cardInputWidget.isEnabled = false
        assertFalse(cardNumberEditText.isEnabled)
        assertFalse(expiryEditText.isEnabled)
        assertFalse(cvcEditText.isEnabled)
        assertFalse(postalCodeEditText.isEnabled)
    }

    @Test
    fun setAllCardFields_whenValidValues_withPostalCodeDisabled_allowsGetCardWithExpectedValues() {
        cardInputWidget.postalCodeEnabled = false
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) <= 2079)

        cardInputWidget.setCardNumber(AMEX_WITH_SPACES)
        cardInputWidget.setExpiryDate(12, 2079)
        cardInputWidget.setCvcCode(CVC_VALUE_AMEX)
        val card = cardInputWidget.card
        assertNotNull(card)
        assertEquals(AMEX_NO_SPACES, card.number)
        assertNotNull(card.expMonth)
        assertNotNull(card.expYear)
        assertEquals(12, card.expMonth)
        assertEquals(2079, card.expYear)
        assertEquals("1234", card.cvc)
        assertEquals(CardBrand.AmericanExpress, card.brand)

        val paymentMethodCard = cardInputWidget.paymentMethodCard
        assertNotNull(paymentMethodCard)
        val expectedPaymentMethodCard = PaymentMethodCreateParams.Card(
            number = AMEX_NO_SPACES,
            cvc = CVC_VALUE_AMEX,
            expiryMonth = 12,
            expiryYear = 2079,
            attribution = ATTRIBUTION
        )
        assertEquals(expectedPaymentMethodCard, paymentMethodCard)
    }

    @Test
    fun setAllCardFields_whenValidValues_withPostalCodeEnabled_allowsGetCardWithExpectedValues() {
        cardInputWidget.postalCodeEnabled = true
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) <= 2079)

        cardInputWidget.setCardNumber(AMEX_WITH_SPACES)
        cardInputWidget.setExpiryDate(12, 2079)
        cardInputWidget.setCvcCode(CVC_VALUE_AMEX)
        cardInputWidget.setPostalCode(POSTAL_CODE_VALUE)
        val card = cardInputWidget.card
        assertNotNull(card)
        assertEquals(AMEX_NO_SPACES, card.number)
        assertNotNull(card.expMonth)
        assertNotNull(card.expYear)
        assertEquals(12, card.expMonth)
        assertEquals(2079, card.expYear)
        assertEquals("1234", card.cvc)
        assertEquals(CardBrand.AmericanExpress, card.brand)

        val paymentMethodCard = cardInputWidget.paymentMethodCard
        assertNotNull(paymentMethodCard)
        val expectedPaymentMethodCard = PaymentMethodCreateParams.Card(
            number = AMEX_NO_SPACES,
            cvc = CVC_VALUE_AMEX,
            expiryYear = 2079,
            expiryMonth = 12,
            attribution = ATTRIBUTION
        )
        assertEquals(expectedPaymentMethodCard, paymentMethodCard)
    }

    @Test
    fun addValues_thenClear_withPostalCodeDisabled_leavesAllTextFieldsEmpty() {
        cardInputWidget.postalCodeEnabled = false
        cardInputWidget.setCardNumber(VISA_NO_SPACES)
        cardInputWidget.setExpiryDate(12, 2079)
        cardInputWidget.setCvcCode(CVC_VALUE_AMEX)
        cardInputWidget.clear()
        assertEquals("", cardNumberEditText.text.toString())
        assertEquals("", expiryEditText.text.toString())
        assertEquals("", cvcEditText.text.toString())
        assertEquals(cvcEditText.id, onGlobalFocusChangeListener.oldFocusId)
        assertEquals(cardNumberEditText.id, onGlobalFocusChangeListener.newFocusId)
    }

    @Test
    fun addValues_thenClear_withPostalCodeEnabled_leavesAllTextFieldsEmpty() {
        cardInputWidget.postalCodeEnabled = true
        cardInputWidget.setCardNumber(VISA_NO_SPACES)
        cardInputWidget.setExpiryDate(12, 2079)
        cardInputWidget.setCvcCode(CVC_VALUE_AMEX)
        cardInputWidget.setPostalCode(POSTAL_CODE_VALUE)
        cardInputWidget.clear()
        assertEquals("", cardNumberEditText.text.toString())
        assertEquals("", expiryEditText.text.toString())
        assertEquals("", cvcEditText.text.toString())
        assertEquals("", postalCodeEditText.text.toString())
        assertEquals(postalCodeEditText.id, onGlobalFocusChangeListener.oldFocusId)
        assertEquals(cardNumberEditText.id, onGlobalFocusChangeListener.newFocusId)
    }

    @Test
    fun shouldIconShowBrand_whenCvcNotFocused_isAlwaysTrue() {
        assertTrue(shouldIconShowBrand(CardBrand.AmericanExpress, false, CVC_VALUE_AMEX))
        assertTrue(shouldIconShowBrand(CardBrand.AmericanExpress, false, ""))
        assertTrue(shouldIconShowBrand(CardBrand.Visa, false, "333"))
        assertTrue(shouldIconShowBrand(CardBrand.DinersClub, false, "12"))
        assertTrue(shouldIconShowBrand(CardBrand.Discover, false, null))
        assertTrue(shouldIconShowBrand(CardBrand.JCB, false, "7"))
    }

    @Test
    fun shouldIconShowBrand_whenAmexAndCvCStringLengthNotFour_isFalse() {
        assertFalse(shouldIconShowBrand(CardBrand.AmericanExpress, true, ""))
        assertFalse(shouldIconShowBrand(CardBrand.AmericanExpress, true, "1"))
        assertFalse(shouldIconShowBrand(CardBrand.AmericanExpress, true, "22"))
        assertFalse(shouldIconShowBrand(CardBrand.AmericanExpress, true, "333"))
    }

    @Test
    fun shouldIconShowBrand_whenAmexAndCvcStringLengthIsFour_isTrue() {
        assertTrue(shouldIconShowBrand(CardBrand.AmericanExpress, true, CVC_VALUE_AMEX))
    }

    @Test
    fun shouldIconShowBrand_whenNotAmexAndCvcStringLengthIsNotThree_isFalse() {
        assertFalse(shouldIconShowBrand(CardBrand.Visa, true, ""))
        assertFalse(shouldIconShowBrand(CardBrand.Discover, true, "12"))
        assertFalse(shouldIconShowBrand(CardBrand.JCB, true, "55"))
        assertFalse(shouldIconShowBrand(CardBrand.MasterCard, true, "9"))
        assertFalse(shouldIconShowBrand(CardBrand.DinersClub, true, null))
        assertFalse(shouldIconShowBrand(CardBrand.Unknown, true, "12"))
    }

    @Test
    fun shouldIconShowBrand_whenNotAmexAndCvcStringLengthIsThree_isTrue() {
        assertTrue(shouldIconShowBrand(CardBrand.Visa, true, "999"))
        assertTrue(shouldIconShowBrand(CardBrand.Discover, true, "123"))
        assertTrue(shouldIconShowBrand(CardBrand.JCB, true, "555"))
        assertTrue(shouldIconShowBrand(CardBrand.MasterCard, true, "919"))
        assertTrue(shouldIconShowBrand(CardBrand.DinersClub, true, "415"))
    }

    @Test
    fun shouldIconShowBrand_whenUnknownBrandAndCvcStringLengthIsFour_isTrue() {
        assertTrue(shouldIconShowBrand(CardBrand.Unknown, true, "2124"))
    }

    @Test
    fun allFields_equals_standardFields_withPostalCodeDisabled() {
        cardInputWidget.postalCodeEnabled = false
        assertEquals(cardInputWidget.requiredFields, cardInputWidget.currentFields)
    }

    @Test
    fun allFields_notEquals_standardFields_withPostalCodeEnabled() {
        cardInputWidget.postalCodeEnabled = true
        assertNotEquals(cardInputWidget.requiredFields, cardInputWidget.currentFields)
    }

    @Test
    fun testCardValidCallback() {
        var currentIsValid = false
        var currentInvalidFields = emptySet<CardValidCallback.Fields>()
        cardInputWidget.setCardValidCallback(object : CardValidCallback {
            override fun onInputChanged(
                isValid: Boolean,
                invalidFields: Set<CardValidCallback.Fields>
            ) {
                currentIsValid = isValid
                currentInvalidFields = invalidFields
            }
        })

        assertFalse(currentIsValid)
        assertEquals(
            setOf(
                CardValidCallback.Fields.Number,
                CardValidCallback.Fields.Expiry,
                CardValidCallback.Fields.Cvc
            ),
            currentInvalidFields
        )

        cardInputWidget.setCardNumber(VISA_NO_SPACES)
        assertFalse(currentIsValid)
        assertEquals(
            setOf(CardValidCallback.Fields.Expiry, CardValidCallback.Fields.Cvc),
            currentInvalidFields
        )

        expiryEditText.append("12")
        assertFalse(currentIsValid)
        assertEquals(
            setOf(CardValidCallback.Fields.Expiry, CardValidCallback.Fields.Cvc),
            currentInvalidFields
        )

        expiryEditText.append("50")
        assertFalse(currentIsValid)
        assertEquals(
            setOf(CardValidCallback.Fields.Cvc),
            currentInvalidFields
        )

        cvcEditText.append("12")
        assertFalse(currentIsValid)
        assertEquals(
            setOf(CardValidCallback.Fields.Cvc),
            currentInvalidFields
        )

        cvcEditText.append("3")
        assertTrue(currentIsValid)
        assertTrue(currentInvalidFields.isEmpty())

        cvcEditText.setText("0")
        assertFalse(currentIsValid)
        assertEquals(
            setOf(CardValidCallback.Fields.Cvc),
            currentInvalidFields
        )
    }

    @Test
    fun shouldShowErrorIcon_shouldBeUpdatedCorrectly() {
        cardInputWidget.setExpiryDate(12, 2030)
        cardInputWidget.setCvcCode(CVC_VALUE_COMMON)

        // show error icon when validating fields with invalid card number
        cardInputWidget.setCardNumber(VISA_NO_SPACES.take(6))
        assertNull(cardInputWidget.paymentMethodCreateParams)
        assertTrue(cardInputWidget.shouldShowErrorIcon)

        // don't show error icon after changing input
        cardInputWidget.setCardNumber(VISA_NO_SPACES.take(7))
        assertFalse(cardInputWidget.shouldShowErrorIcon)

        // don't show error icon when validating fields with invalid card number
        assertNull(cardInputWidget.paymentMethodCreateParams)
        cardInputWidget.setCardNumber(VISA_NO_SPACES)
        assertNotNull(cardInputWidget.paymentMethodCreateParams)
        assertFalse(cardInputWidget.shouldShowErrorIcon)
    }

    @Test
    fun createHiddenCardText_shouldReturnExpectedValue() {
        assertThat(cardInputWidget.createHiddenCardText(CardBrand.Visa, VISA_NO_SPACES))
            .isEqualTo("0000 0000 0000 ")
        assertThat(cardInputWidget.createHiddenCardText(CardBrand.DinersClub, DINERS_CLUB_14_NO_SPACES))
            .isEqualTo("0000 000000 ")
        assertThat(cardInputWidget.createHiddenCardText(CardBrand.DinersClub, DINERS_CLUB_16_NO_SPACES))
            .isEqualTo("0000 0000 0000 ")
    }

    @Test
    fun usZipCodeRequired_whenFalse_shouldSetPostalCodeHint() {
        cardInputWidget.usZipCodeRequired = false
        assertThat(cardInputWidget.postalCodeEditText.hint)
            .isEqualTo("Postal code")

        cardInputWidget.setCardNumber(VISA_WITH_SPACES)
        cardInputWidget.expiryDateEditText.append("12")
        cardInputWidget.expiryDateEditText.append("50")
        cardInputWidget.cvcNumberEditText.append("123")

        assertThat(cardInputWidget.card)
            .isNotNull()
    }

    @Test
    fun usZipCodeRequired_whenTrue_withInvalidZipCode_shouldReturnNullCard() {
        cardInputWidget.usZipCodeRequired = true
        assertThat(cardInputWidget.postalCodeEditText.hint)
            .isEqualTo("ZIP code")

        cardInputWidget.setCardNumber(VISA_WITH_SPACES)
        cardInputWidget.expiryDateEditText.append("12")
        cardInputWidget.expiryDateEditText.append("50")
        cardInputWidget.cvcNumberEditText.append("123")

        // invalid zipcode
        cardInputWidget.postalCodeEditText.setText("1234")
        assertThat(cardInputWidget.card)
            .isNull()
    }

    @Test
    fun usZipCodeRequired_whenTrue_withValidZipCode_shouldReturnNotNullCard() {
        cardInputWidget.usZipCodeRequired = true
        assertThat(cardInputWidget.postalCodeEditText.hint)
            .isEqualTo("ZIP code")

        cardInputWidget.setCardNumber(VISA_WITH_SPACES)
        cardInputWidget.expiryDateEditText.append("12")
        cardInputWidget.expiryDateEditText.append("50")
        cardInputWidget.cvcNumberEditText.append("123")

        // valid zipcode
        cardInputWidget.postalCodeEditText.setText("12345")
        assertThat(cardInputWidget.card)
            .isNotNull()
    }

    private companion object {
        // Every Card made by the CardInputView should have the card widget token.
        private val ATTRIBUTION = setOf(LOGGING_TOKEN)

        private const val CVC_VALUE_COMMON = "123"
        private const val CVC_VALUE_AMEX = "1234"
        private const val POSTAL_CODE_VALUE = "94103"
    }
}
