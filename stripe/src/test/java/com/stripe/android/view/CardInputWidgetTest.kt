package com.stripe.android.view

import android.os.Build
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import com.stripe.android.CardNumberFixtures.VALID_AMEX_NO_SPACES
import com.stripe.android.CardNumberFixtures.VALID_AMEX_WITH_SPACES
import com.stripe.android.CardNumberFixtures.VALID_DINERS_CLUB_NO_SPACES
import com.stripe.android.CardNumberFixtures.VALID_DINERS_CLUB_WITH_SPACES
import com.stripe.android.CardNumberFixtures.VALID_VISA_NO_SPACES
import com.stripe.android.CardNumberFixtures.VALID_VISA_WITH_SPACES
import com.stripe.android.R
import com.stripe.android.model.Card
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.testharness.TestFocusChangeListener
import com.stripe.android.testharness.ViewTestUtils
import com.stripe.android.view.CardInputListener.FocusField.Companion.FOCUS_CARD
import com.stripe.android.view.CardInputListener.FocusField.Companion.FOCUS_CVC
import com.stripe.android.view.CardInputListener.FocusField.Companion.FOCUS_EXPIRY
import com.stripe.android.view.CardInputWidget.LOGGING_TOKEN
import com.stripe.android.view.CardInputWidget.shouldIconShowBrand
import java.util.Calendar
import kotlin.test.AfterTest
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
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Test class for [CardInputWidget].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
internal class CardInputWidgetTest : BaseViewTest<CardInputTestActivity>(
    CardInputTestActivity::class.java
) {
    private lateinit var cardInputWidget: CardInputWidget
    private lateinit var cardNumberEditText: CardNumberEditText
    private lateinit var expiryEditText: StripeEditText
    private lateinit var cvcEditText: StripeEditText
    private lateinit var onGlobalFocusChangeListener: TestFocusChangeListener
    private lateinit var activity: CardInputTestActivity

    @Mock
    private lateinit var cardInputListener: CardInputListener

    @BeforeTest
    fun setup() {
        MockitoAnnotations.initMocks(this)

        activity = createStartedActivity()

        val dimensionOverrides = object : CardInputWidget.DimensionOverrideSettings {
            override fun getPixelWidth(text: String, editText: EditText): Int {
                // This makes it simple to know what to expect.
                return text.length * 10
            }

            override fun getFrameWidth(): Int {
                // That's a pretty small screen, but one that we theoretically support.
                return 500
            }
        }

        cardInputWidget = activity.cardInputWidget
        cardInputWidget.setDimensionOverrideSettings(dimensionOverrides)
        onGlobalFocusChangeListener = TestFocusChangeListener()
        cardInputWidget.viewTreeObserver
            .addOnGlobalFocusChangeListener(onGlobalFocusChangeListener)

        cardNumberEditText = activity.cardNumberEditText
        cardNumberEditText.setText("")

        expiryEditText = cardInputWidget.findViewById(R.id.et_expiry_date)
        cvcEditText = cardInputWidget.findViewById(R.id.et_cvc_number)
        val iconView = cardInputWidget.findViewById<ImageView>(R.id.iv_card_icon)

        // Set the width of the icon and its margin so that test calculations have
        // an expected value that is repeatable on all systems.
        val params = iconView.layoutParams as ViewGroup.MarginLayoutParams
        params.width = 48
        params.rightMargin = 12
        iconView.layoutParams = params

        resumeStartedActivity(activity)
    }

    @AfterTest
    override fun tearDown() {
        super.tearDown()
        activity.finish()
    }

    @Test
    fun getCard_whenInputIsValidVisa_returnsCardObjectWithLoggingToken() {
        // The input date here will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050)

        cardNumberEditText.setText(VALID_VISA_WITH_SPACES)
        expiryEditText.append("12")
        expiryEditText.append("50")
        cvcEditText.append("123")

        val card = cardInputWidget.card
        assertNotNull(card)
        assertEquals(VALID_VISA_NO_SPACES, card.number)
        assertNotNull(card.expMonth)
        assertNotNull(card.expYear)
        assertEquals(12, card.expMonth)
        assertEquals(2050, card.expYear)
        assertEquals("123", card.cvc)
        assertTrue(card.validateCard())
        assertTrue(EXPECTED_LOGGING_ARRAY.contentEquals(card.loggingTokens.toTypedArray()))

        val paymentMethodCard = cardInputWidget.paymentMethodCard
        assertNotNull(paymentMethodCard)
        val expectedPaymentMethodCard = PaymentMethodCreateParams.Card.Builder().setNumber(VALID_VISA_NO_SPACES)
            .setCvc("123").setExpiryYear(2050).setExpiryMonth(12).build()
        assertEquals(expectedPaymentMethodCard, paymentMethodCard)
    }

    @Test
    fun getCard_whenInputIsValidAmEx_returnsCardObjectWithLoggingToken() {
        // The input date here will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050)

        cardNumberEditText.setText(VALID_AMEX_WITH_SPACES)
        expiryEditText.append("12")
        expiryEditText.append("50")
        cvcEditText.append("1234")

        val card = cardInputWidget.card
        assertNotNull(card)
        assertEquals(VALID_AMEX_NO_SPACES, card.number)
        assertNotNull(card.expMonth)
        assertNotNull(card.expYear)
        assertEquals(12, card.expMonth)
        assertEquals(2050, card.expYear)
        assertEquals("1234", card.cvc)
        assertTrue(card.validateCard())
        assertTrue(EXPECTED_LOGGING_ARRAY.contentEquals(card.loggingTokens.toTypedArray()))

        val paymentMethodCard = cardInputWidget.paymentMethodCard
        assertNotNull(paymentMethodCard)
        val expectedPaymentMethodCard = PaymentMethodCreateParams.Card.Builder()
            .setNumber(VALID_AMEX_NO_SPACES)
            .setCvc("1234")
            .setExpiryYear(2050)
            .setExpiryMonth(12)
            .build()
        assertEquals(expectedPaymentMethodCard, paymentMethodCard)
    }

    @Test
    fun getCard_whenInputIsValidDinersClub_returnsCardObjectWithLoggingToken() {
        // The input date here will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050)

        cardNumberEditText.setText(VALID_DINERS_CLUB_WITH_SPACES)
        expiryEditText.append("12")
        expiryEditText.append("50")
        cvcEditText.append("123")

        val card = cardInputWidget.card
        assertNotNull(card)
        assertEquals(VALID_DINERS_CLUB_NO_SPACES, card.number)
        assertNotNull(card.expMonth)
        assertNotNull(card.expYear)
        assertEquals(12, card.expMonth)
        assertEquals(2050, card.expYear)
        assertEquals("123", card.cvc)
        assertTrue(card.validateCard())
        assertTrue(EXPECTED_LOGGING_ARRAY.contentEquals(card.loggingTokens.toTypedArray()))

        val paymentMethodCard = cardInputWidget.paymentMethodCard
        assertNotNull(paymentMethodCard)
        val expectedPaymentMethodCard =
            PaymentMethodCreateParams.Card.Builder()
                .setNumber(VALID_DINERS_CLUB_NO_SPACES)
                .setCvc("123")
                .setExpiryYear(2050)
                .setExpiryMonth(12)
                .build()
        assertEquals(expectedPaymentMethodCard, paymentMethodCard)
    }

    @Test
    fun getCard_whenInputHasIncompleteCardNumber_returnsNull() {
        // The test will be testing the wrong variable after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050)

        // This will be 242 4242 4242 4242
        cardNumberEditText.setText(VALID_VISA_WITH_SPACES.substring(1))
        expiryEditText.append("12")
        expiryEditText.append("50")
        cvcEditText.append("123")

        val card = cardInputWidget.card
        assertNull(card)

        val paymentMethodCard = cardInputWidget.paymentMethodCard
        assertNull(paymentMethodCard)
    }

    @Test
    fun getCard_whenInputHasExpiredDate_returnsNull() {
        // The test will be testing the wrong variable after 2080. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2080)

        cardNumberEditText.setText(VALID_VISA_WITH_SPACES)
        // Date interpreted as 12/2012 until 2080, when it will be 12/2112
        expiryEditText.append("12")
        expiryEditText.append("12")
        cvcEditText.append("123")

        val card = cardInputWidget.card
        assertNull(card)

        val paymentMethodCard = cardInputWidget.paymentMethodCard
        assertNull(paymentMethodCard)
    }

    @Test
    fun getCard_whenIncompleteCvCForVisa_returnsNull() {
        // The test will be testing the wrong variable after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050)

        cardNumberEditText.setText(VALID_VISA_WITH_SPACES)
        expiryEditText.append("12")
        expiryEditText.append("50")
        cvcEditText.append("12")

        val card = cardInputWidget.card
        assertNull(card)

        val paymentMethodCard = cardInputWidget.paymentMethodCard
        assertNull(paymentMethodCard)
    }

    @Test
    fun getCard_when3DigitCvCForAmEx_returnsCard() {
        // The test will be testing the wrong variable after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050)

        cardNumberEditText.setText(VALID_AMEX_WITH_SPACES)
        expiryEditText.append("12")
        expiryEditText.append("50")
        cvcEditText.append("123")

        val card = cardInputWidget.card
        assertNotNull(card)

        val paymentMethodCard = cardInputWidget.paymentMethodCard
        assertNotNull(paymentMethodCard)
    }

    @Test
    fun getCard_whenIncompleteCvCForAmEx_returnsNull() {
        // The test will be testing the wrong variable after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050)

        cardNumberEditText.setText(VALID_AMEX_WITH_SPACES)
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
        // The input date here will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050)

        cardInputWidget.setCardNumber(VALID_VISA_NO_SPACES)
        cardInputWidget.setExpiryDate(12, 2030)
        cardInputWidget.setCvcCode("123")

        val params = cardInputWidget.paymentMethodCreateParams
        assertNotNull(params)

        val expectedParams = PaymentMethodCreateParams.create(
            PaymentMethodCreateParams.Card.Builder()
                .setNumber(VALID_VISA_NO_SPACES)
                .setCvc("123")
                .setExpiryYear(2030)
                .setExpiryMonth(12)
                .build()
        )
        assertEquals(expectedParams, params)
    }

    @Test
    fun getCard_whenIncompleteCvCForDiners_returnsNull() {
        // The test will be testing the wrong variable after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050)

        cardNumberEditText.setText(VALID_DINERS_CLUB_WITH_SPACES)
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

        cardNumberEditText.setText(VALID_VISA_WITH_SPACES)

        verify<CardInputListener>(cardInputListener).onCardComplete()
        verify<CardInputListener>(cardInputListener).onFocusChange(FOCUS_EXPIRY)
        assertEquals(R.id.et_card_number, onGlobalFocusChangeListener.oldFocusId)
        assertEquals(R.id.et_expiry_date, onGlobalFocusChangeListener.newFocusId)
    }

    @Test
    fun onDeleteFromExpiryDate_whenEmpty_shiftsFocusToCardNumberAndDeletesDigit() {
        cardInputWidget.setCardInputListener(cardInputListener)
        cardNumberEditText.setText(VALID_VISA_WITH_SPACES)
        assertTrue(expiryEditText.hasFocus())

        // The above functionality is tested elsewhere, so we reset this listener.
        reset<CardInputListener>(cardInputListener)

        ViewTestUtils.sendDeleteKeyEvent(expiryEditText)
        verify<CardInputListener>(cardInputListener).onFocusChange(FOCUS_CARD)
        assertEquals(R.id.et_expiry_date, onGlobalFocusChangeListener.oldFocusId)
        assertEquals(R.id.et_card_number, onGlobalFocusChangeListener.newFocusId)

        val subString = VALID_VISA_WITH_SPACES.substring(0, VALID_VISA_WITH_SPACES.length - 1)
        assertEquals(subString, cardNumberEditText.text.toString())
        assertEquals(subString.length, cardNumberEditText.selectionStart)
    }

    @Test
    fun onDeleteFromExpiryDate_whenNotEmpty_doesNotShiftFocusOrDeleteDigit() {
        cardNumberEditText.setText(VALID_AMEX_WITH_SPACES)
        assertTrue(expiryEditText.hasFocus())

        expiryEditText.append("1")
        ViewTestUtils.sendDeleteKeyEvent(expiryEditText)

        assertTrue(expiryEditText.hasFocus())
        assertEquals(VALID_AMEX_WITH_SPACES, cardNumberEditText.text.toString())
    }

    @Test
    fun onDeleteFromCvcDate_whenEmpty_shiftsFocusToExpiryAndDeletesDigit() {
        // This test will be invalid if run between 2080 and 2112. Please update the code.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2080)

        cardInputWidget.setCardInputListener(cardInputListener)
        cardNumberEditText.setText(VALID_VISA_WITH_SPACES)

        verify<CardInputListener>(cardInputListener).onCardComplete()
        verify<CardInputListener>(cardInputListener).onFocusChange(FOCUS_EXPIRY)

        expiryEditText.append("12")
        expiryEditText.append("79")

        verify<CardInputListener>(cardInputListener).onExpirationComplete()
        verify<CardInputListener>(cardInputListener).onFocusChange(FOCUS_CVC)
        assertTrue(cvcEditText.hasFocus())

        // Clearing already-verified data.
        reset<CardInputListener>(cardInputListener)

        ViewTestUtils.sendDeleteKeyEvent(cvcEditText)
        verify<CardInputListener>(cardInputListener).onFocusChange(FOCUS_EXPIRY)
        assertEquals(R.id.et_cvc_number, onGlobalFocusChangeListener.oldFocusId)
        assertEquals(R.id.et_expiry_date, onGlobalFocusChangeListener.newFocusId)

        val expectedResult = "12/7"
        assertEquals(expectedResult, expiryEditText.text.toString())
        assertEquals(expectedResult.length, expiryEditText.selectionStart)
    }

    @Test
    fun onDeleteFromCvcDate_whenNotEmpty_doesNotShiftFocusOrDeleteEntry() {
        // This test will be invalid if run between 2080 and 2112. Please update the code.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2080)

        cardNumberEditText.setText(VALID_AMEX_WITH_SPACES)

        expiryEditText.append("12")
        expiryEditText.append("79")
        assertTrue(cvcEditText.hasFocus())

        cvcEditText.append("123")
        ViewTestUtils.sendDeleteKeyEvent(cvcEditText)

        assertTrue(cvcEditText.hasFocus())
        assertEquals("12/79", expiryEditText.text.toString())
    }

    @Test
    fun onDeleteFromCvcDate_whenEmptyAndExpiryDateIsEmpty_shiftsFocusOnly() {
        cardNumberEditText.setText(VALID_DINERS_CLUB_WITH_SPACES)

        // Simulates user tapping into this text field without filling out the date first.
        cvcEditText.requestFocus()

        ViewTestUtils.sendDeleteKeyEvent(cvcEditText)
        assertEquals(R.id.et_cvc_number, onGlobalFocusChangeListener.oldFocusId)
        assertEquals(R.id.et_expiry_date, onGlobalFocusChangeListener.newFocusId)
    }

    @Test
    fun onUpdateIcon_forCommonLengthBrand_setsLengthOnCvc() {
        // This should set the brand to Visa. Note that more extensive brand checking occurs
        // in CardNumberEditTextTest.
        cardNumberEditText.append(Card.PREFIXES_VISA[0])
        assertTrue(ViewTestUtils.hasMaxLength(cvcEditText, 3))
    }

    @Test
    fun onUpdateText_forAmExPrefix_setsLengthOnCvc() {
        cardNumberEditText.append(Card.PREFIXES_AMERICAN_EXPRESS[0])
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
    fun updateToPeekSize_withNoData_returnsExpectedValuesForCommonCardLength() {
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
        cardInputWidget.setCardNumberIsViewed(false)
        cardInputWidget.updateSpaceSizes(false)
        assertNull(cardInputWidget.getFocusRequestOnTouch(75))
    }

    @Test
    fun getFocusRequestOnTouch_whenInPeekSlopAfterShift_returnsCardEditor() {
        // |img==60||---total == 500--------|
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        // So any touch between 100 and 192 returns the card editor
        cardInputWidget.setCardNumberIsViewed(false)
        cardInputWidget.updateSpaceSizes(false)
        val focusRequester = cardInputWidget.getFocusRequestOnTouch(150)
        assertNotNull(focusRequester)
        assertEquals(cardNumberEditText, focusRequester)
    }

    @Test
    fun getFocusRequestOnTouch_whenInDateLeftSlopAfterShift_returnsDateEditor() {
        // |img==60||---total == 500--------|
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        // So any touch between 192 and 285 returns the date editor
        cardInputWidget.setCardNumberIsViewed(false)
        cardInputWidget.updateSpaceSizes(false)
        val focusRequester = cardInputWidget.getFocusRequestOnTouch(200)
        assertNotNull(focusRequester)
        assertEquals(expiryEditText, focusRequester)
    }

    @Test
    fun getFocusRequestOnTouch_whenInDateAfterShift_returnsNull() {
        // |img==60||---total == 500--------|
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        // So any touch between 285 and 335 does nothing
        cardInputWidget.setCardNumberIsViewed(false)
        cardInputWidget.updateSpaceSizes(false)
        assertNull(cardInputWidget.getFocusRequestOnTouch(300))
    }

    @Test
    fun getFocusRequestOnTouch_whenInDateRightSlopAfterShift_returnsDateEditor() {
        // |img==60||---total == 500--------|
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        // So any touch between 335 and 432 returns the date editor
        cardInputWidget.setCardNumberIsViewed(false)
        cardInputWidget.updateSpaceSizes(false)
        val focusRequester = cardInputWidget.getFocusRequestOnTouch(400)
        assertNotNull(focusRequester)
        assertEquals(expiryEditText, focusRequester)
    }

    @Test
    fun getFocusRequestOnTouch_whenInCvcSlopAfterShift_returnsCvcEditor() {
        // |img==60||---total == 500--------|
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        // So any touch between 432 and 530 returns the date editor
        cardInputWidget.setCardNumberIsViewed(false)
        cardInputWidget.updateSpaceSizes(false)
        val focusRequester = cardInputWidget.getFocusRequestOnTouch(485)
        assertNotNull(focusRequester)
        assertEquals(cvcEditText, focusRequester)
    }

    @Test
    fun getFocusRequestOnTouch_whenInCvcAfterShift_returnsNull() {
        // |img==60||---total == 500--------|
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        // So any touch over 530 does nothing
        cardInputWidget.setCardNumberIsViewed(false)
        cardInputWidget.updateSpaceSizes(false)
        assertNull(cardInputWidget.getFocusRequestOnTouch(545))
    }

    @Test
    fun addValidVisaCard_scrollsOver_andSetsExpectedDisplayValues() {
        // Moving left with an actual Visa number does the same as moving when empty.
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        cardNumberEditText.setText(VALID_VISA_WITH_SPACES)
        val shiftedParameters = cardInputWidget.placementParameters
        assertEquals(40, shiftedParameters.peekCardWidth)
        assertEquals(185, shiftedParameters.cardDateSeparation)
        assertEquals(50, shiftedParameters.dateWidth)
        assertEquals(195, shiftedParameters.dateCvcSeparation)
        assertEquals(30, shiftedParameters.cvcWidth)
    }

    @Test
    fun addValidAmExCard_scrollsOver_andSetsExpectedDisplayValues() {
        // Moving left with an AmEx number has a larger peek and cvc size.
        // |(peek==50)--(space==175)--(date==50)--(space==185)--(cvc==40)|
        cardNumberEditText.setText(VALID_AMEX_WITH_SPACES)
        val shiftedParameters = cardInputWidget.placementParameters
        assertEquals(50, shiftedParameters.peekCardWidth)
        assertEquals(175, shiftedParameters.cardDateSeparation)
        assertEquals(50, shiftedParameters.dateWidth)
        assertEquals(185, shiftedParameters.dateCvcSeparation)
        assertEquals(40, shiftedParameters.cvcWidth)
    }

    @Test
    fun addDinersClubCard_scrollsOver_andSetsExpectedDisplayValues() {
        // When we move for a Diner's club card, the peek text is shorter, so we expect:
        // |(peek==20)--(space==205)--(date==50)--(space==195)--(cvc==30)|
        cardNumberEditText.setText(VALID_DINERS_CLUB_WITH_SPACES)
        val shiftedParameters = cardInputWidget.placementParameters
        assertEquals(20, shiftedParameters.peekCardWidth)
        assertEquals(205, shiftedParameters.cardDateSeparation)
        assertEquals(50, shiftedParameters.dateWidth)
        assertEquals(195, shiftedParameters.dateCvcSeparation)
        assertEquals(30, shiftedParameters.cvcWidth)
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
        cardInputWidget.setCvcCode("123")
        assertEquals("123", cvcEditText.text.toString())
    }

    @Test
    fun setCvcCode_withLongString_truncatesValue() {
        cardInputWidget.setCvcCode("1234")
        assertEquals("123", cvcEditText.text.toString())
    }

    @Test
    fun setCvcCode_whenCardBrandIsAmericanExpress_allowsFourDigits() {
        cardInputWidget.setCardNumber(VALID_AMEX_NO_SPACES)
        cardInputWidget.setCvcCode("1234")
        assertEquals("1234", cvcEditText.text.toString())
    }

    @Test
    fun setEnabled_isTrue() {
        cardInputWidget.isEnabled = true
        assertTrue(cardNumberEditText.isEnabled)
        assertTrue(expiryEditText.isEnabled)
        assertTrue(cvcEditText.isEnabled)
    }

    @Test
    fun setEnabled_isFalse() {
        cardInputWidget.isEnabled = false
        assertFalse(cardNumberEditText.isEnabled)
        assertFalse(expiryEditText.isEnabled)
        assertFalse(cvcEditText.isEnabled)
    }

    @Test
    fun setAllCardFields_whenValidValues_allowsGetCardWithExpectedValues() {
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) <= 2079)

        cardInputWidget.setCardNumber(VALID_AMEX_WITH_SPACES)
        cardInputWidget.setExpiryDate(12, 2079)
        cardInputWidget.setCvcCode("1234")
        val card = cardInputWidget.card
        assertNotNull(card)
        assertEquals(VALID_AMEX_NO_SPACES, card.number)
        assertNotNull(card.expMonth)
        assertNotNull(card.expYear)
        assertEquals(12, card.expMonth)
        assertEquals(2079, card.expYear)
        assertEquals("1234", card.cvc)
        assertEquals(Card.CardBrand.AMERICAN_EXPRESS, card.brand)

        val paymentMethodCard = cardInputWidget.paymentMethodCard
        assertNotNull(paymentMethodCard)
        val expectedPaymentMethodCard = PaymentMethodCreateParams.Card.Builder()
            .setNumber(VALID_AMEX_NO_SPACES)
            .setCvc("1234")
            .setExpiryYear(2079)
            .setExpiryMonth(12)
            .build()
        assertEquals(expectedPaymentMethodCard, paymentMethodCard)
    }

    @Test
    fun addValues_thenClear_leavesAllTextFieldsEmpty() {
        cardInputWidget.setCardNumber(VALID_VISA_NO_SPACES)
        cardInputWidget.setExpiryDate(12, 2079)
        cardInputWidget.setCvcCode("1234")
        cardInputWidget.clear()
        assertEquals("", cardNumberEditText.text.toString())
        assertEquals("", expiryEditText.text.toString())
        assertEquals("", cvcEditText.text.toString())
        assertEquals(R.id.et_cvc_number, onGlobalFocusChangeListener.oldFocusId)
        assertEquals(R.id.et_card_number, onGlobalFocusChangeListener.newFocusId)
    }

    @Test
    fun shouldIconShowBrand_whenCvcNotFocused_isAlwaysTrue() {
        assertTrue(shouldIconShowBrand(Card.CardBrand.AMERICAN_EXPRESS, false, "1234"))
        assertTrue(shouldIconShowBrand(Card.CardBrand.AMERICAN_EXPRESS, false, ""))
        assertTrue(shouldIconShowBrand(Card.CardBrand.VISA, false, "333"))
        assertTrue(shouldIconShowBrand(Card.CardBrand.DINERS_CLUB, false, "12"))
        assertTrue(shouldIconShowBrand(Card.CardBrand.DISCOVER, false, null))
        assertTrue(shouldIconShowBrand(Card.CardBrand.JCB, false, "7"))
    }

    @Test
    fun shouldIconShowBrand_whenAmexAndCvCStringLengthNotFour_isFalse() {
        assertFalse(shouldIconShowBrand(Card.CardBrand.AMERICAN_EXPRESS, true, ""))
        assertFalse(shouldIconShowBrand(Card.CardBrand.AMERICAN_EXPRESS, true, "1"))
        assertFalse(shouldIconShowBrand(Card.CardBrand.AMERICAN_EXPRESS, true, "22"))
        assertFalse(shouldIconShowBrand(Card.CardBrand.AMERICAN_EXPRESS, true, "333"))
    }

    @Test
    fun shouldIconShowBrand_whenAmexAndCvcStringLengthIsFour_isTrue() {
        assertTrue(shouldIconShowBrand(Card.CardBrand.AMERICAN_EXPRESS, true, "1234"))
    }

    @Test
    fun shouldIconShowBrand_whenNotAmexAndCvcStringLengthIsNotThree_isFalse() {
        assertFalse(shouldIconShowBrand(Card.CardBrand.VISA, true, ""))
        assertFalse(shouldIconShowBrand(Card.CardBrand.DISCOVER, true, "12"))
        assertFalse(shouldIconShowBrand(Card.CardBrand.JCB, true, "55"))
        assertFalse(shouldIconShowBrand(Card.CardBrand.MASTERCARD, true, "9"))
        assertFalse(shouldIconShowBrand(Card.CardBrand.DINERS_CLUB, true, null))
        assertFalse(shouldIconShowBrand(Card.CardBrand.UNKNOWN, true, "12"))
    }

    @Test
    fun shouldIconShowBrand_whenNotAmexAndCvcStringLengthIsThree_isTrue() {
        assertTrue(shouldIconShowBrand(Card.CardBrand.VISA, true, "999"))
        assertTrue(shouldIconShowBrand(Card.CardBrand.DISCOVER, true, "123"))
        assertTrue(shouldIconShowBrand(Card.CardBrand.JCB, true, "555"))
        assertTrue(shouldIconShowBrand(Card.CardBrand.MASTERCARD, true, "919"))
        assertTrue(shouldIconShowBrand(Card.CardBrand.DINERS_CLUB, true, "415"))
        assertTrue(shouldIconShowBrand(Card.CardBrand.UNKNOWN, true, "212"))
    }

    companion object {

        // Every Card made by the CardInputView should have the card widget token.
        private val EXPECTED_LOGGING_ARRAY = arrayOf(LOGGING_TOKEN)
    }
}
