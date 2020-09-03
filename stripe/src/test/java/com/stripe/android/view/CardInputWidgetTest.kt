package com.stripe.android.view

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.CardNumberFixtures
import com.stripe.android.CardNumberFixtures.AMEX_NO_SPACES
import com.stripe.android.CardNumberFixtures.AMEX_WITH_SPACES
import com.stripe.android.CardNumberFixtures.DINERS_CLUB_14_NO_SPACES
import com.stripe.android.CardNumberFixtures.DINERS_CLUB_14_WITH_SPACES
import com.stripe.android.CardNumberFixtures.VISA_NO_SPACES
import com.stripe.android.CardNumberFixtures.VISA_WITH_SPACES
import com.stripe.android.PaymentConfiguration
import com.stripe.android.R
import com.stripe.android.model.Address
import com.stripe.android.model.Card
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.testharness.TestFocusChangeListener
import com.stripe.android.testharness.ViewTestUtils
import com.stripe.android.utils.TestUtils.idleLooper
import com.stripe.android.view.CardInputWidget.Companion.LOGGING_TOKEN
import com.stripe.android.view.CardInputWidget.Companion.shouldIconShowBrand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class CardInputWidgetTest {
    private val testDispatcher = TestCoroutineDispatcher()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val activityScenarioFactory = ActivityScenarioFactory(context)

    private lateinit var cardInputWidget: CardInputWidget
    private val cardNumberEditText: CardNumberEditText by lazy {
        cardInputWidget.cardNumberEditText.also {
            it.workDispatcher = testDispatcher
        }
    }
    private val expiryEditText: StripeEditText by lazy {
        cardInputWidget.expiryDateEditText
    }
    private val cvcEditText: CvcEditText by lazy {
        cardInputWidget.cvcEditText
    }
    private val postalCodeEditText: PostalCodeEditText by lazy {
        cardInputWidget.postalCodeEditText
    }

    private val onGlobalFocusChangeListener: TestFocusChangeListener = TestFocusChangeListener()
    private val cardInputListener: CardInputListener = mock()

    @BeforeTest
    fun setup() {
        // The input date here will be invalid after 2050. Please update the test.
        assertThat(Calendar.getInstance().get(Calendar.YEAR) < 2050)
            .isTrue()

        Dispatchers.setMain(testDispatcher)

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

    @AfterTest
    fun cleanup() {
        Dispatchers.resetMain()
    }

    private fun createCardInputWidget(activity: Activity): CardInputWidget {
        return CardInputWidget(activity).also {
            it.layoutWidthCalculator = CardInputWidget.LayoutWidthCalculator { text, _ ->
                text.length * 10
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

        updateCardNumberAndIdle(VISA_WITH_SPACES)
        expiryEditText.append("12")
        expiryEditText.append("50")
        cvcEditText.append(CVC_VALUE_COMMON)

        assertThat(cardInputWidget.card)
            .isEqualTo(
                Card.Builder(VISA_NO_SPACES, 12, 2050, CVC_VALUE_COMMON)
                    .loggingTokens(ATTRIBUTION)
                    .build()
            )

        assertThat(cardInputWidget.cardParams)
            .isEqualTo(
                CardParams(
                    loggingTokens = ATTRIBUTION,
                    number = VISA_NO_SPACES,
                    expMonth = 12,
                    expYear = 2050,
                    cvc = CVC_VALUE_COMMON,
                    address = Address.Builder()
                        .build()
                )
            )

        assertThat(cardInputWidget.paymentMethodCreateParams)
            .isEqualTo(
                PaymentMethodCreateParams.create(
                    card = PaymentMethodCreateParams.Card(
                        number = VISA_NO_SPACES,
                        cvc = CVC_VALUE_COMMON,
                        expiryMonth = 12,
                        expiryYear = 2050,
                        attribution = ATTRIBUTION
                    )
                )
            )
    }

    @Test
    fun getCard_whenInputIsValidVisa_withPostalCodeEnabled_returnsCardObjectWithLoggingToken() {
        cardInputWidget.postalCodeEnabled = true

        updateCardNumberAndIdle(VISA_WITH_SPACES)
        expiryEditText.append("12")
        expiryEditText.append("50")
        cvcEditText.append(CVC_VALUE_COMMON)
        postalCodeEditText.setText(POSTAL_CODE_VALUE)

        assertThat(cardInputWidget.card)
            .isEqualTo(
                Card.Builder(VISA_NO_SPACES, 12, 2050, CVC_VALUE_COMMON)
                    .loggingTokens(ATTRIBUTION)
                    .addressZip(POSTAL_CODE_VALUE)
                    .build()
            )

        assertThat(cardInputWidget.cardParams)
            .isEqualTo(
                CardParams(
                    loggingTokens = ATTRIBUTION,
                    number = VISA_NO_SPACES,
                    expMonth = 12,
                    expYear = 2050,
                    cvc = CVC_VALUE_COMMON,
                    address = Address.Builder()
                        .setPostalCode(POSTAL_CODE_VALUE)
                        .build()
                )
            )

        assertThat(cardInputWidget.paymentMethodCreateParams)
            .isEqualTo(
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
            )
    }

    @Test
    fun getCard_whenInputIsValidAmEx_withPostalCodeDisabled_createsExpectedObjects() {
        cardInputWidget.postalCodeEnabled = false

        updateCardNumberAndIdle(AMEX_WITH_SPACES)
        expiryEditText.append("12")
        expiryEditText.append("50")
        cvcEditText.append(CVC_VALUE_AMEX)

        assertThat(cardInputWidget.card)
            .isEqualTo(
                Card.Builder(AMEX_NO_SPACES, 12, 2050, CVC_VALUE_AMEX)
                    .loggingTokens(ATTRIBUTION)
                    .build()
            )

        assertThat(cardInputWidget.cardParams)
            .isEqualTo(
                CardParams(
                    loggingTokens = ATTRIBUTION,
                    number = AMEX_NO_SPACES,
                    expMonth = 12,
                    expYear = 2050,
                    cvc = CVC_VALUE_AMEX,
                    address = Address.Builder()
                        .build()
                )
            )

        assertThat(cardInputWidget.paymentMethodCreateParams)
            .isEqualTo(
                PaymentMethodCreateParams.create(
                    PaymentMethodCreateParams.Card(
                        number = AMEX_NO_SPACES,
                        cvc = CVC_VALUE_AMEX,
                        expiryMonth = 12,
                        expiryYear = 2050,
                        attribution = ATTRIBUTION
                    )
                )
            )
    }

    @Test
    fun getCard_whenInputIsValidAmEx_withPostalCodeEnabled_createsExpectedObjects() {
        cardInputWidget.postalCodeEnabled = true

        updateCardNumberAndIdle(AMEX_WITH_SPACES)
        expiryEditText.append("12")
        expiryEditText.append("50")
        cvcEditText.append(CVC_VALUE_AMEX)
        postalCodeEditText.setText(POSTAL_CODE_VALUE)

        assertThat(cardInputWidget.card)
            .isEqualTo(
                Card.Builder(AMEX_NO_SPACES, 12, 2050, CVC_VALUE_AMEX)
                    .loggingTokens(ATTRIBUTION)
                    .addressZip(POSTAL_CODE_VALUE)
                    .build()
            )

        assertThat(cardInputWidget.cardParams)
            .isEqualTo(
                CardParams(
                    loggingTokens = ATTRIBUTION,
                    number = AMEX_NO_SPACES,
                    expMonth = 12,
                    expYear = 2050,
                    cvc = CVC_VALUE_AMEX,
                    address = Address.Builder()
                        .setPostalCode(POSTAL_CODE_VALUE)
                        .build()
                )
            )

        assertThat(cardInputWidget.paymentMethodCreateParams)
            .isEqualTo(
                PaymentMethodCreateParams.create(
                    card = PaymentMethodCreateParams.Card(
                        number = AMEX_NO_SPACES,
                        cvc = CVC_VALUE_AMEX,
                        expiryYear = 2050,
                        expiryMonth = 12,
                        attribution = ATTRIBUTION
                    ),
                    billingDetails = PaymentMethod.BillingDetails.Builder()
                        .setAddress(
                            Address(
                                postalCode = POSTAL_CODE_VALUE
                            )
                        )
                        .build()
                )
            )
    }

    @Test
    fun getCard_whenInputIsValidDinersClub_withPostalCodeDisabled_returnsCardObjectWithLoggingToken() {
        cardInputWidget.postalCodeEnabled = false

        updateCardNumberAndIdle(DINERS_CLUB_14_WITH_SPACES)
        expiryEditText.append("12")
        expiryEditText.append("50")
        cvcEditText.append(CVC_VALUE_COMMON)

        assertThat(cardInputWidget.card)
            .isEqualTo(
                Card.Builder(DINERS_CLUB_14_NO_SPACES, 12, 2050, CVC_VALUE_COMMON)
                    .loggingTokens(ATTRIBUTION)
                    .build()
            )

        assertThat(cardInputWidget.cardParams)
            .isEqualTo(
                CardParams(
                    loggingTokens = ATTRIBUTION,
                    number = DINERS_CLUB_14_NO_SPACES,
                    expMonth = 12,
                    expYear = 2050,
                    cvc = CVC_VALUE_COMMON,
                    address = Address.Builder()
                        .build()
                )
            )

        assertThat(cardInputWidget.paymentMethodCard)
            .isEqualTo(
                PaymentMethodCreateParams.Card(
                    number = DINERS_CLUB_14_NO_SPACES,
                    cvc = CVC_VALUE_COMMON,
                    expiryMonth = 12,
                    expiryYear = 2050,
                    attribution = ATTRIBUTION
                )
            )
    }

    @Test
    fun getCard_whenInputIsValidDinersClub_withPostalCodeEnabled_returnsCardObjectWithLoggingToken() {
        cardInputWidget.postalCodeEnabled = true

        updateCardNumberAndIdle(DINERS_CLUB_14_WITH_SPACES)
        expiryEditText.append("12")
        expiryEditText.append("50")
        cvcEditText.append(CVC_VALUE_COMMON)
        postalCodeEditText.setText(POSTAL_CODE_VALUE)

        assertThat(cardInputWidget.card)
            .isEqualTo(
                Card.Builder(DINERS_CLUB_14_NO_SPACES, 12, 2050, CVC_VALUE_COMMON)
                    .loggingTokens(ATTRIBUTION)
                    .addressZip(POSTAL_CODE_VALUE)
                    .build()
            )

        assertThat(cardInputWidget.cardParams)
            .isEqualTo(
                CardParams(
                    loggingTokens = ATTRIBUTION,
                    number = DINERS_CLUB_14_NO_SPACES,
                    expMonth = 12,
                    expYear = 2050,
                    cvc = CVC_VALUE_COMMON,
                    address = Address.Builder()
                        .setPostalCode(POSTAL_CODE_VALUE)
                        .build()
                )
            )

        assertThat(cardInputWidget.paymentMethodCard)
            .isEqualTo(
                PaymentMethodCreateParams.Card(
                    number = DINERS_CLUB_14_NO_SPACES,
                    cvc = CVC_VALUE_COMMON,
                    expiryYear = 2050,
                    expiryMonth = 12,
                    attribution = ATTRIBUTION
                )
            )
    }

    @Test
    fun getCard_whenPostalCodeIsEnabledAndRequired_andValueIsBlank_returnsNull() {
        cardInputWidget.postalCodeEnabled = true
        cardInputWidget.postalCodeRequired = true

        updateCardNumberAndIdle(VISA_WITH_SPACES)
        expiryEditText.append("12")
        expiryEditText.append("50")
        cvcEditText.append(CVC_VALUE_COMMON)
        postalCodeEditText.append("")

        assertThat(cardInputWidget.card)
            .isNull()
        assertThat(cardInputWidget.cardParams)
            .isNull()
        assertThat(cardInputWidget.paymentMethodCard)
            .isNull()
    }

    @Test
    fun getCard_whenInputHasIncompleteCardNumber_returnsNull() {
        // This will be 242 4242 4242 4242
        updateCardNumberAndIdle(VISA_WITH_SPACES.drop(1))
        expiryEditText.append("12")
        expiryEditText.append("50")
        cvcEditText.append(CVC_VALUE_COMMON)

        assertThat(cardInputWidget.card)
            .isNull()
        assertThat(cardInputWidget.cardParams)
            .isNull()
        assertThat(cardInputWidget.paymentMethodCard)
            .isNull()
    }

    @Test
    fun getCard_whenInputHasExpiredDate_returnsNull() {
        updateCardNumberAndIdle(VISA_WITH_SPACES)
        // Date interpreted as 12/2012 until 2080, when it will be 12/2112
        expiryEditText.append("12")
        expiryEditText.append("12")
        cvcEditText.append(CVC_VALUE_COMMON)

        assertThat(cardInputWidget.card)
            .isNull()
        assertThat(cardInputWidget.cardParams)
            .isNull()
        assertThat(cardInputWidget.paymentMethodCard)
            .isNull()
    }

    @Test
    fun getCard_whenIncompleteCvCForVisa_returnsNull() {
        updateCardNumberAndIdle(VISA_WITH_SPACES)
        expiryEditText.append("12")
        expiryEditText.append("50")
        cvcEditText.append("12")

        assertThat(cardInputWidget.card)
            .isNull()
        assertThat(cardInputWidget.cardParams)
            .isNull()
        assertThat(cardInputWidget.paymentMethodCard)
            .isNull()
    }

    @Test
    fun getCard_doesNotValidatePostalCode() {
        cardInputWidget.postalCodeEnabled = true

        updateCardNumberAndIdle(VISA_WITH_SPACES)
        expiryEditText.append("12")
        expiryEditText.append("50")
        cvcEditText.append(CVC_VALUE_COMMON)
        postalCodeEditText.setText("")

        assertThat(cardInputWidget.card)
            .isEqualTo(
                Card.Builder(VISA_NO_SPACES, 12, 2050, CVC_VALUE_COMMON)
                    .loggingTokens(ATTRIBUTION)
                    .build()
            )
        assertThat(cardInputWidget.cardParams)
            .isEqualTo(
                CardParams(
                    loggingTokens = ATTRIBUTION,
                    number = VISA_NO_SPACES,
                    expMonth = 12,
                    expYear = 2050,
                    cvc = CVC_VALUE_COMMON,
                    address = Address()
                )
            )
        assertThat(cardInputWidget.paymentMethodCard)
            .isNotNull()
    }

    @Test
    fun getCard_when3DigitCvCForAmEx_withPostalCodeDisabled_returnsCard() {
        cardInputWidget.postalCodeEnabled = false

        updateCardNumberAndIdle(AMEX_WITH_SPACES)
        expiryEditText.append("12")
        expiryEditText.append("50")
        cvcEditText.append(CVC_VALUE_COMMON)

        assertThat(cardInputWidget.card)
            .isEqualTo(
                Card.Builder(AMEX_NO_SPACES, 12, 2050, CVC_VALUE_COMMON)
                    .loggingTokens(ATTRIBUTION)
                    .build()
            )
        assertThat(cardInputWidget.cardParams)
            .isEqualTo(
                CardParams(
                    loggingTokens = ATTRIBUTION,
                    number = AMEX_NO_SPACES,
                    expMonth = 12,
                    expYear = 2050,
                    cvc = CVC_VALUE_COMMON,
                    address = Address()
                )
            )
        assertThat(cardInputWidget.paymentMethodCard)
            .isNotNull()
    }

    @Test
    fun getCard_when3DigitCvCForAmEx_withPostalCodeEnabled_returnsCard() {
        cardInputWidget.postalCodeEnabled = true

        updateCardNumberAndIdle(AMEX_WITH_SPACES)
        expiryEditText.append("12")
        expiryEditText.append("50")
        cvcEditText.append(CVC_VALUE_COMMON)
        postalCodeEditText.setText(POSTAL_CODE_VALUE)

        assertThat(cardInputWidget.card)
            .isEqualTo(
                Card.Builder(AMEX_NO_SPACES, 12, 2050, CVC_VALUE_COMMON)
                    .loggingTokens(ATTRIBUTION)
                    .addressZip(POSTAL_CODE_VALUE)
                    .build()
            )
        assertThat(cardInputWidget.cardParams)
            .isEqualTo(
                CardParams(
                    loggingTokens = ATTRIBUTION,
                    number = AMEX_NO_SPACES,
                    expMonth = 12,
                    expYear = 2050,
                    cvc = CVC_VALUE_COMMON,
                    address = Address.Builder()
                        .setPostalCode(POSTAL_CODE_VALUE)
                        .build()
                )
            )
        assertThat(cardInputWidget.paymentMethodCard)
            .isNotNull()
    }

    @Test
    fun getCard_whenIncompleteCvCForAmEx_returnsNull() {
        updateCardNumberAndIdle(AMEX_WITH_SPACES)
        expiryEditText.append("12")
        expiryEditText.append("50")
        cvcEditText.append("12")

        assertThat(cardInputWidget.card)
            .isNull()
        assertThat(cardInputWidget.cardParams)
            .isNull()
        assertThat(cardInputWidget.paymentMethodCard)
            .isNull()
    }

    @Test
    fun getPaymentMethodCreateParams_shouldReturnExpectedObject() {
        cardInputWidget.postalCodeEnabled = true

        cardInputWidget.setCardNumber(VISA_NO_SPACES)
        cardInputWidget.setExpiryDate(12, 2030)
        cardInputWidget.setCvcCode(CVC_VALUE_COMMON)
        cardInputWidget.setPostalCode(POSTAL_CODE_VALUE)

        assertThat(cardInputWidget.paymentMethodCreateParams)
            .isEqualTo(
                PaymentMethodCreateParams.create(
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
            )
    }

    @Test
    fun getCard_whenIncompleteCvCForDiners_returnsNull() {
        updateCardNumberAndIdle(DINERS_CLUB_14_WITH_SPACES)
        expiryEditText.append("12")
        expiryEditText.append("50")
        cvcEditText.append("12")

        assertThat(cardInputWidget.card)
            .isNull()
        assertThat(cardInputWidget.cardParams)
            .isNull()
        assertThat(cardInputWidget.paymentMethodCard)
            .isNull()
    }

    @Test
    fun onCompleteCardNumber_whenValid_shiftsFocusToExpiryDate() {
        cardInputWidget.setCardInputListener(cardInputListener)

        updateCardNumberAndIdle(VISA_WITH_SPACES)

        verify(cardInputListener).onCardComplete()
        verify(cardInputListener).onFocusChange(CardInputListener.FocusField.ExpiryDate)

        assertThat(onGlobalFocusChangeListener.oldFocusId)
            .isEqualTo(cardNumberEditText.id)
        assertThat(onGlobalFocusChangeListener.newFocusId)
            .isEqualTo(expiryEditText.id)
    }

    @Test
    fun onDeleteFromExpiryDate_whenEmpty_shiftsFocusToCardNumberAndDeletesDigit() {
        cardInputWidget.setCardInputListener(cardInputListener)

        updateCardNumberAndIdle(VISA_WITH_SPACES)

        assertThat(expiryEditText.hasFocus())
            .isTrue()

        // The above functionality is tested elsewhere, so we reset this listener.
        reset(cardInputListener)

        ViewTestUtils.sendDeleteKeyEvent(expiryEditText)
        verify(cardInputListener).onFocusChange(CardInputListener.FocusField.CardNumber)
        assertThat(onGlobalFocusChangeListener.oldFocusId)
            .isEqualTo(expiryEditText.id)
        assertThat(onGlobalFocusChangeListener.newFocusId)
            .isEqualTo(cardNumberEditText.id)

        val subString = VISA_WITH_SPACES.take(VISA_WITH_SPACES.length - 1)
        assertThat(cardNumberEditText.text.toString())
            .isEqualTo(subString)
        assertThat(cardNumberEditText.selectionStart)
            .isEqualTo(subString.length)
    }

    @Test
    fun onDeleteFromExpiryDate_whenNotEmpty_doesNotShiftFocusOrDeleteDigit() {
        updateCardNumberAndIdle(AMEX_WITH_SPACES)

        assertThat(expiryEditText.hasFocus())
            .isTrue()

        expiryEditText.append("1")
        ViewTestUtils.sendDeleteKeyEvent(expiryEditText)

        assertThat(expiryEditText.hasFocus())
            .isTrue()
        assertThat(cardNumberEditText.text.toString())
            .isEqualTo(AMEX_WITH_SPACES)
    }

    @Test
    fun onDeleteFromCvcDate_whenEmpty_shiftsFocusToExpiryAndDeletesDigit() {
        cardInputWidget.setCardInputListener(cardInputListener)
        updateCardNumberAndIdle(VISA_WITH_SPACES)

        verify(cardInputListener).onCardComplete()
        verify(cardInputListener).onFocusChange(CardInputListener.FocusField.ExpiryDate)

        expiryEditText.append("12")
        expiryEditText.append("79")

        verify(cardInputListener).onExpirationComplete()
        verify(cardInputListener).onFocusChange(CardInputListener.FocusField.Cvc)
        assertThat(cvcEditText.hasFocus())
            .isTrue()

        // Clearing already-verified data.
        reset(cardInputListener)

        ViewTestUtils.sendDeleteKeyEvent(cvcEditText)
        verify(cardInputListener).onFocusChange(CardInputListener.FocusField.ExpiryDate)
        assertThat(onGlobalFocusChangeListener.oldFocusId)
            .isEqualTo(cvcEditText.id)
        assertThat(onGlobalFocusChangeListener.newFocusId)
            .isEqualTo(expiryEditText.id)

        val expectedResult = "12/7"
        assertThat(expiryEditText.text.toString())
            .isEqualTo(expectedResult)
        assertThat(expiryEditText.selectionStart)
            .isEqualTo(expectedResult.length)
    }

    @Test
    fun onDeleteFromCvcDate_withPostalCodeDisabled_whenNotEmpty_doesNotShiftFocusOrDeleteEntry() {
        cardInputWidget.postalCodeEnabled = false

        updateCardNumberAndIdle(AMEX_WITH_SPACES)

        expiryEditText.append("12")
        expiryEditText.append("79")
        assertThat(cvcEditText.hasFocus())
            .isTrue()

        cvcEditText.append(CVC_VALUE_COMMON)
        ViewTestUtils.sendDeleteKeyEvent(cvcEditText)

        assertThat(cvcEditText.hasFocus())
            .isTrue()
        assertThat(expiryEditText.text.toString())
            .isEqualTo("12/79")
    }

    @Test
    fun onDeleteFromCvcDate_withPostalCodeEnabled_whenNotEmpty_doesNotShiftFocusOrDeleteEntry() {
        cardInputWidget.postalCodeEnabled = true

        updateCardNumberAndIdle(AMEX_WITH_SPACES)

        expiryEditText.append("12")
        expiryEditText.append("79")
        assertThat(cvcEditText.hasFocus())
            .isTrue()

        cvcEditText.append("12")
        ViewTestUtils.sendDeleteKeyEvent(cvcEditText)

        assertThat(cvcEditText.hasFocus())
            .isTrue()
        assertThat(expiryEditText.text.toString())
            .isEqualTo("12/79")
    }

    @Test
    fun onDeleteFromCvcDate_whenEmptyAndExpiryDateIsEmpty_shiftsFocusOnly() {
        updateCardNumberAndIdle(DINERS_CLUB_14_WITH_SPACES)

        // Simulates user tapping into this text field without filling out the date first.
        cvcEditText.requestFocus()

        ViewTestUtils.sendDeleteKeyEvent(cvcEditText)
        assertThat(onGlobalFocusChangeListener.oldFocusId)
            .isEqualTo(cvcEditText.id)
        assertThat(onGlobalFocusChangeListener.newFocusId)
            .isEqualTo(expiryEditText.id)
    }

    @Test
    fun onUpdateIcon_forCommonLengthBrand_setsLengthOnCvc() {
        // This should set the brand to Visa. Note that more extensive brand checking occurs
        // in CardNumberEditTextTest.
        updateCardNumberAndIdle(CardNumberFixtures.VISA_BIN)

        assertThat(ViewTestUtils.hasMaxLength(cvcEditText, 3))
            .isTrue()
    }

    @Test
    fun onUpdateText_forAmexBin_setsLengthOnCvc() {
        updateCardNumberAndIdle(CardNumberFixtures.AMEX_BIN)

        assertThat(ViewTestUtils.hasMaxLength(cvcEditText, 4))
            .isTrue()
    }

    @Test
    fun updateToInitialSizes_returnsExpectedValues() {
        // Initial spacing should look like
        // |img==60||---total == 500--------|
        // |(card==230)--(space==220)--(date==50)|
        // |img==60||  cardTouchArea | 420 | dateTouchArea | dateStart==510 |

        assertThat(cardInputWidget.placementParameters)
            .isEqualTo(
                CardInputWidget.PlacementParameters(
                    totalLengthInPixels = 500,
                    cardWidth = 230,
                    hiddenCardWidth = 150,
                    peekCardWidth = 40,
                    cardDateSeparation = 220,
                    dateWidth = 50,
                    dateCvcSeparation = 0,
                    cvcWidth = 30,
                    cvcPostalCodeSeparation = 0,
                    postalCodeWidth = 100,
                    cvcStartPosition = 0,
                    dateRightTouchBufferLimit = 0,
                    cardTouchBufferLimit = 400,
                    dateStartPosition = 510
                )
            )
    }

    @Test
    fun updateToPeekSize_withPostalCodeDisabled_withNoData_returnsExpectedValuesForCommonCardLength() {
        cardInputWidget.postalCodeEnabled = false

        // Moving left uses Visa-style ("common") defaults
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        cardInputWidget.updateSpaceSizes(false)
        idleLooper()

        assertThat(cardInputWidget.placementParameters)
            .isEqualTo(
                CardInputWidget.PlacementParameters(
                    totalLengthInPixels = 500,
                    cardWidth = 230,
                    hiddenCardWidth = 150,
                    peekCardWidth = 40,
                    cardDateSeparation = 185,
                    dateWidth = 50,
                    dateCvcSeparation = 195,
                    cvcWidth = 30,
                    cvcPostalCodeSeparation = 0,
                    postalCodeWidth = 100,
                    cvcStartPosition = 530,
                    dateRightTouchBufferLimit = 432,
                    cardTouchBufferLimit = 192,
                    dateStartPosition = 285
                )
            )
    }

    @Test
    fun updateToPeekSize_withPostalCodeEnabled_withNoData_returnsExpectedValuesForCommonCardLength() {
        cardInputWidget.postalCodeEnabled = true

        // Moving left uses Visa-style ("common") defaults
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        cardInputWidget.updateSpaceSizes(false)
        idleLooper()

        assertThat(cardInputWidget.placementParameters)
            .isEqualTo(
                CardInputWidget.PlacementParameters(
                    totalLengthInPixels = 500,
                    cardWidth = 230,
                    hiddenCardWidth = 150,
                    peekCardWidth = 40,
                    cardDateSeparation = 98,
                    dateWidth = 50,
                    dateCvcSeparation = 82,
                    cvcWidth = 30,
                    cvcPostalCodeSeparation = 0,
                    postalCodeWidth = 100,
                    cvcStartPosition = 330,
                    dateRightTouchBufferLimit = 110,
                    cardTouchBufferLimit = 66,
                    dateStartPosition = 198,
                    cvcRightTouchBufferLimit = 120,
                    postalCodeStartPosition = 360
                )
            )
    }

    @Test
    fun getFocusRequestOnTouch_whenTouchOnImage_returnsNull() {
        // |img==60||---total == 500--------|
        // |(card==230)--(space==220)--(date==50)|
        // |img==60||  cardTouchArea | 420 | dateTouchArea | dateStart==510 |
        // So any touch lower than 60 will be the icon
        assertThat(cardInputWidget.getFocusRequestOnTouch(30))
            .isNull()
    }

    @Test
    fun getFocusRequestOnTouch_whenTouchActualCardWidget_returnsNull() {
        // |img==60||---total == 500--------|
        // |(card==230)--(space==220)--(date==50)|
        // |img==60||  cardTouchArea | 420 | dateTouchArea | dateStart==510 |
        // So any touch between 60 and 250 will be the actual card widget
        assertThat(cardInputWidget.getFocusRequestOnTouch(200))
            .isNull()
    }

    @Test
    fun getFocusRequestOnTouch_whenTouchInCardEditorSlop_returnsCardEditor() {
        // |img==60||---total == 500--------|
        // |(card==230)--(space==220)--(date==50)|
        // |img==60||  cardTouchArea | 420 | dateTouchArea | dateStart==510 |
        // So any touch between 250 and 420 needs to send focus to the card editor
        assertThat(cardInputWidget.getFocusRequestOnTouch(300))
            .isEqualTo(cardNumberEditText)
    }

    @Test
    fun getFocusRequestOnTouch_whenTouchInDateSlop_returnsDateEditor() {
        // |img==60||---total == 500--------|
        // |(card==230)--(space==220)--(date==50)|
        // |img==60||  cardTouchArea | 420 | dateTouchArea | dateStart==510 |
        // So any touch between 420 and 510 needs to send focus to the date editor
        assertThat(cardInputWidget.getFocusRequestOnTouch(430))
            .isEqualTo(expiryEditText)
    }

    @Test
    fun getFocusRequestOnTouch_whenTouchInDateEditor_returnsNull() {
        // |img==60||---total == 500--------|
        // |(card==230)--(space==220)--(date==50)|
        // |img==60||  cardTouchArea | 420 | dateTouchArea | dateStart==510 |
        // So any touch over 510 doesn't need to do anything
        assertThat(cardInputWidget.getFocusRequestOnTouch(530))
            .isNull()
    }

    @Test
    fun getFocusRequestOnTouch_whenInPeekAfterShift_returnsNull() {
        // |img==60||---total == 500--------|
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        // So any touch between 60 and 100 does nothing
        cardInputWidget.isShowingFullCard = false
        cardInputWidget.updateSpaceSizes(false)
        assertThat(cardInputWidget.getFocusRequestOnTouch(75))
            .isNull()
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

        assertThat(cardInputWidget.getFocusRequestOnTouch(150))
            .isEqualTo(cardNumberEditText)
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

        assertThat(cardInputWidget.getFocusRequestOnTouch(200))
            .isEqualTo(expiryEditText)
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

        assertThat(cardInputWidget.getFocusRequestOnTouch(170))
            .isEqualTo(expiryEditText)
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
        idleLooper()

        assertThat(cardInputWidget.getFocusRequestOnTouch(300))
            .isNull()
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
        assertThat(cardInputWidget.getFocusRequestOnTouch(200))
            .isNull()
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

        assertThat(cardInputWidget.getFocusRequestOnTouch(400))
            .isEqualTo(expiryEditText)
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

        assertThat(cardInputWidget.getFocusRequestOnTouch(185))
            .isEqualTo(expiryEditText)
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

        assertThat(cardInputWidget.getFocusRequestOnTouch(485))
            .isEqualTo(cvcEditText)
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

        assertThat(cardInputWidget.getFocusRequestOnTouch(300))
            .isEqualTo(cvcEditText)
    }

    @Test
    fun getFocusRequestOnTouch_whenInCvcAfterShift_returnsNull() {
        // |img==60||---total == 500--------|
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        // So any touch over 530 does nothing
        cardInputWidget.isShowingFullCard = false
        cardInputWidget.updateSpaceSizes(false)

        assertThat(cardInputWidget.getFocusRequestOnTouch(545))
            .isNull()
    }

    @Test
    fun addValidVisaCard_withPostalCodeDisabled_scrollsOver_andSetsExpectedDisplayValues() {
        cardInputWidget.postalCodeEnabled = false

        // Moving left with an actual Visa number does the same as moving when empty.
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        updateCardNumberAndIdle(VISA_WITH_SPACES)

        assertThat(cardInputWidget.placementParameters)
            .isEqualTo(
                CardInputWidget.PlacementParameters(
                    totalLengthInPixels = 500,
                    cardWidth = 230,
                    hiddenCardWidth = 150,
                    peekCardWidth = 40,
                    cardDateSeparation = 185,
                    dateWidth = 50,
                    dateCvcSeparation = 195,
                    cvcWidth = 30,
                    cvcPostalCodeSeparation = 0,
                    postalCodeWidth = 100,
                    cvcStartPosition = 530,
                    dateRightTouchBufferLimit = 432,
                    cardTouchBufferLimit = 192,
                    dateStartPosition = 285
                )
            )
    }

    @Test
    fun addValidVisaCard_withPostalCodeEnabled_scrollsOver_andSetsExpectedDisplayValues() {
        cardInputWidget.postalCodeEnabled = true

        // Moving left with an actual Visa number does the same as moving when empty.
        // |(peek==40)--(space==98)--(date==50)--(space==82)--(cvc==30)--(space==0)--(postal==100)|
        updateCardNumberAndIdle(VISA_WITH_SPACES)

        assertThat(cardInputWidget.placementParameters)
            .isEqualTo(
                CardInputWidget.PlacementParameters(
                    totalLengthInPixels = 500,
                    cardWidth = 230,
                    hiddenCardWidth = 150,
                    peekCardWidth = 40,
                    cardDateSeparation = 98,
                    dateWidth = 50,
                    dateCvcSeparation = 82,
                    cvcWidth = 30,
                    cvcPostalCodeSeparation = 0,
                    postalCodeWidth = 100,
                    cvcStartPosition = 330,
                    cvcRightTouchBufferLimit = 120,
                    dateRightTouchBufferLimit = 110,
                    cardTouchBufferLimit = 66,
                    dateStartPosition = 198,
                    postalCodeStartPosition = 360
                )
            )
    }

    @Test
    fun addValidAmExCard_withPostalCodeDisabled_scrollsOver_andSetsExpectedDisplayValues() {
        cardInputWidget.postalCodeEnabled = false

        // Moving left with an AmEx number has a larger peek and cvc size.
        // |(peek==50)--(space==175)--(date==50)--(space==185)--(cvc==40)|
        updateCardNumberAndIdle(AMEX_WITH_SPACES)

        assertThat(cardInputWidget.placementParameters)
            .isEqualTo(
                CardInputWidget.PlacementParameters(
                    totalLengthInPixels = 500,
                    cardWidth = 230,
                    hiddenCardWidth = 120,
                    peekCardWidth = 50,
                    cardDateSeparation = 175,
                    dateWidth = 50,
                    dateCvcSeparation = 185,
                    cvcWidth = 40,
                    cvcPostalCodeSeparation = 0,
                    postalCodeWidth = 100,
                    cvcStartPosition = 520,
                    dateRightTouchBufferLimit = 427,
                    cardTouchBufferLimit = 197,
                    dateStartPosition = 285
                )
            )
    }

    @Test
    fun addValidAmExCard_withPostalCodeEnabled_scrollsOver_andSetsExpectedDisplayValues() {
        cardInputWidget.postalCodeEnabled = true

        // Moving left with an AmEx number has a larger peek and cvc size.
        // |(peek==50)--(space==88)--(date==50)--(space==72)--(cvc==40)--(space==0)--(postal==100)|
        updateCardNumberAndIdle(AMEX_WITH_SPACES)

        assertThat(cardInputWidget.placementParameters)
            .isEqualTo(
                CardInputWidget.PlacementParameters(
                    totalLengthInPixels = 500,
                    cardWidth = 230,
                    hiddenCardWidth = 120,
                    peekCardWidth = 50,
                    cardDateSeparation = 88,
                    dateWidth = 50,
                    dateCvcSeparation = 72,
                    cvcWidth = 40,
                    cvcPostalCodeSeparation = 0,
                    postalCodeWidth = 100,
                    cvcStartPosition = 320,
                    cvcRightTouchBufferLimit = 120,
                    dateRightTouchBufferLimit = 106,
                    cardTouchBufferLimit = 66,
                    dateStartPosition = 198,
                    postalCodeStartPosition = 360
                )
            )
    }

    @Test
    fun addDinersClubCard_withPostalCodeDisabled_scrollsOver_andSetsExpectedDisplayValues() {
        cardInputWidget.postalCodeEnabled = false

        // When we move for a Diner's club card, the peek text is shorter, so we expect:
        // |(peek==20)--(space==205)--(date==50)--(space==195)--(cvc==30)|
        updateCardNumberAndIdle(DINERS_CLUB_14_WITH_SPACES)

        assertThat(cardInputWidget.placementParameters)
            .isEqualTo(
                CardInputWidget.PlacementParameters(
                    totalLengthInPixels = 500,
                    cardWidth = 230,
                    hiddenCardWidth = 120,
                    peekCardWidth = 20,
                    cardDateSeparation = 205,
                    dateWidth = 50,
                    dateCvcSeparation = 195,
                    cvcWidth = 30,
                    cvcPostalCodeSeparation = 0,
                    postalCodeWidth = 100,
                    cvcStartPosition = 530,
                    dateRightTouchBufferLimit = 432,
                    cardTouchBufferLimit = 182,
                    dateStartPosition = 285
                )
            )
    }

    @Test
    fun addDinersClubCard_withPostalCodeEnabled_scrollsOver_andSetsExpectedDisplayValues() {
        cardInputWidget.postalCodeEnabled = true

        // When we move for a Diner's club card, the peek text is shorter, so we expect:
        // |(peek==20)--(space==205)--(date==50)--(space==195)--(cvc==30)--(space==0)--(postal==100)|
        updateCardNumberAndIdle(DINERS_CLUB_14_WITH_SPACES)

        assertThat(cardInputWidget.placementParameters)
            .isEqualTo(
                CardInputWidget.PlacementParameters(
                    totalLengthInPixels = 500,
                    cardWidth = 230,
                    hiddenCardWidth = 120,
                    peekCardWidth = 20,
                    cardDateSeparation = 118,
                    dateWidth = 50,
                    dateCvcSeparation = 82,
                    cvcWidth = 30,
                    cvcPostalCodeSeparation = 0,
                    postalCodeWidth = 100,
                    cvcStartPosition = 330,
                    dateRightTouchBufferLimit = 110,
                    cardTouchBufferLimit = 66,
                    dateStartPosition = 198,
                    cvcRightTouchBufferLimit = 120,
                    postalCodeStartPosition = 360
                )
            )
    }

    @Test
    fun setCardNumber_withIncompleteNumber_doesNotValidateCard() {
        cardInputWidget.setCardNumber("123456")
        assertThat(cardNumberEditText.isCardNumberValid)
            .isFalse()
        assertThat(cardNumberEditText.hasFocus())
            .isTrue()
    }

    @Test
    fun setExpirationDate_withValidData_setsCorrectValues() {
        cardInputWidget.setExpiryDate(12, 79)
        assertThat(expiryEditText.text.toString())
            .isEqualTo("12/79")
    }

    @Test
    fun setCvcCode_withValidData_setsValue() {
        cardInputWidget.setCvcCode(CVC_VALUE_COMMON)
        assertThat(cvcEditText.text.toString())
            .isEqualTo(CVC_VALUE_COMMON)
    }

    @Test
    fun setCvcCode_withLongString_truncatesValue() {
        cvcEditText.updateBrand(CardBrand.Visa)
        cardInputWidget.setCvcCode(CVC_VALUE_AMEX)

        assertThat(cvcEditText.text.toString())
            .isEqualTo(CVC_VALUE_COMMON)
    }

    @Test
    fun setCvcCode_whenCardBrandIsAmericanExpress_allowsFourDigits() {
        cardInputWidget.setCardNumber(AMEX_NO_SPACES)
        cardInputWidget.setCvcCode(CVC_VALUE_AMEX)

        assertThat(cvcEditText.text.toString())
            .isEqualTo(CVC_VALUE_AMEX)
    }

    @Test
    fun setEnabledTrue_withPostalCodeDisabled_isTrue() {
        cardInputWidget.postalCodeEnabled = false
        cardInputWidget.isEnabled = true
        assertThat(cardNumberEditText.isEnabled)
            .isTrue()
        assertThat(expiryEditText.isEnabled)
            .isTrue()
        assertThat(cvcEditText.isEnabled)
            .isTrue()
        assertThat(postalCodeEditText.isEnabled)
            .isFalse()
    }

    @Test
    fun setEnabledTrue_withPostalCodeEnabled_isTrue() {
        cardInputWidget.postalCodeEnabled = true
        cardInputWidget.isEnabled = true
        assertThat(cardNumberEditText.isEnabled)
            .isTrue()
        assertThat(expiryEditText.isEnabled)
            .isTrue()
        assertThat(cvcEditText.isEnabled)
            .isTrue()
        assertThat(postalCodeEditText.isEnabled)
            .isTrue()
    }

    @Test
    fun setEnabledFalse_withPostalCodeDisabled_isFalse() {
        cardInputWidget.postalCodeEnabled = false
        cardInputWidget.isEnabled = false
        assertThat(cardNumberEditText.isEnabled)
            .isFalse()
        assertThat(expiryEditText.isEnabled)
            .isFalse()
        assertThat(cvcEditText.isEnabled)
            .isFalse()
        assertThat(postalCodeEditText.isEnabled)
            .isFalse()
    }

    @Test
    fun setEnabledFalse_withPostalCodeEnabled_isFalse() {
        cardInputWidget.postalCodeEnabled = true
        cardInputWidget.isEnabled = false
        assertThat(cardNumberEditText.isEnabled)
            .isFalse()
        assertThat(expiryEditText.isEnabled)
            .isFalse()
        assertThat(cvcEditText.isEnabled)
            .isFalse()
        assertThat(postalCodeEditText.isEnabled)
            .isFalse()
    }

    @Test
    fun setAllCardFields_whenValidValues_withPostalCodeDisabled_allowsGetCardWithExpectedValues() {
        cardInputWidget.postalCodeEnabled = false

        cardInputWidget.setCardNumber(AMEX_WITH_SPACES)
        cardInputWidget.setExpiryDate(12, 2079)
        cardInputWidget.setCvcCode(CVC_VALUE_AMEX)

        assertThat(cardInputWidget.card)
            .isEqualTo(
                Card(
                    id = null,
                    brand = CardBrand.AmericanExpress,
                    number = AMEX_NO_SPACES,
                    expMonth = 12,
                    expYear = 2079,
                    cvc = CVC_VALUE_AMEX,
                    loggingTokens = ATTRIBUTION,
                    last4 = "0005"
                )
            )

        assertThat(cardInputWidget.cardParams)
            .isEqualTo(
                CardParams(
                    loggingTokens = ATTRIBUTION,
                    number = AMEX_NO_SPACES,
                    expMonth = 12,
                    expYear = 2079,
                    cvc = CVC_VALUE_AMEX,
                    address = Address.Builder()
                        .build()
                )
            )

        assertThat(cardInputWidget.paymentMethodCard)
            .isEqualTo(
                PaymentMethodCreateParams.Card(
                    number = AMEX_NO_SPACES,
                    cvc = CVC_VALUE_AMEX,
                    expiryMonth = 12,
                    expiryYear = 2079,
                    attribution = ATTRIBUTION
                )
            )
    }

    @Test
    fun setAllCardFields_whenValidValues_withPostalCodeEnabled_allowsGetCardWithExpectedValues() {
        cardInputWidget.postalCodeEnabled = true

        cardInputWidget.setCardNumber(AMEX_WITH_SPACES)
        cardInputWidget.setExpiryDate(12, 2079)
        cardInputWidget.setCvcCode(CVC_VALUE_AMEX)
        cardInputWidget.setPostalCode(POSTAL_CODE_VALUE)

        assertThat(cardInputWidget.card)
            .isEqualTo(
                Card.Builder(AMEX_NO_SPACES, 12, 2079, CVC_VALUE_AMEX)
                    .loggingTokens(ATTRIBUTION)
                    .addressZip(POSTAL_CODE_VALUE)
                    .build()
            )

        assertThat(cardInputWidget.cardParams)
            .isEqualTo(
                CardParams(
                    loggingTokens = ATTRIBUTION,
                    number = AMEX_NO_SPACES,
                    expMonth = 12,
                    expYear = 2079,
                    cvc = CVC_VALUE_AMEX,
                    address = Address.Builder()
                        .setPostalCode(POSTAL_CODE_VALUE)
                        .build()
                )
            )

        assertThat(cardInputWidget.paymentMethodCard)
            .isEqualTo(
                PaymentMethodCreateParams.Card(
                    number = AMEX_NO_SPACES,
                    cvc = CVC_VALUE_AMEX,
                    expiryYear = 2079,
                    expiryMonth = 12,
                    attribution = ATTRIBUTION
                )
            )
    }

    @Test
    fun addValues_thenClear_withPostalCodeDisabled_leavesAllTextFieldsEmpty() {
        cardInputWidget.postalCodeEnabled = false
        updateCardNumberAndIdle(VISA_NO_SPACES)

        cardInputWidget.setExpiryDate(12, 2079)
        cardInputWidget.setCvcCode(CVC_VALUE_AMEX)
        cardInputWidget.clear()

        assertThat(cardNumberEditText.text.toString())
            .isEmpty()
        assertThat(expiryEditText.text.toString())
            .isEmpty()
        assertThat(cvcEditText.text.toString())
            .isEmpty()

        assertThat(onGlobalFocusChangeListener.oldFocusId)
            .isEqualTo(cvcEditText.id)
        assertThat(onGlobalFocusChangeListener.newFocusId)
            .isEqualTo(cardNumberEditText.id)
    }

    @Test
    fun addValues_thenClear_withPostalCodeEnabled_leavesAllTextFieldsEmpty() {
        cardInputWidget.postalCodeEnabled = true
        cardInputWidget.setCardNumber(VISA_NO_SPACES)
        cardInputWidget.setExpiryDate(12, 2079)
        cardInputWidget.setCvcCode(CVC_VALUE_AMEX)
        cardInputWidget.setPostalCode(POSTAL_CODE_VALUE)
        cardInputWidget.clear()

        assertThat(cardNumberEditText.text.toString())
            .isEmpty()
        assertThat(expiryEditText.text.toString())
            .isEmpty()
        assertThat(cvcEditText.text.toString())
            .isEmpty()
        assertThat(postalCodeEditText.text.toString())
            .isEmpty()

        assertThat(onGlobalFocusChangeListener.oldFocusId)
            .isEqualTo(postalCodeEditText.id)
        assertThat(onGlobalFocusChangeListener.newFocusId)
            .isEqualTo(cardNumberEditText.id)
    }

    @Test
    fun shouldIconShowBrand_whenCvcNotFocused_isAlwaysTrue() {
        assertThat(shouldIconShowBrand(CardBrand.AmericanExpress, false, CVC_VALUE_AMEX))
            .isTrue()
        assertThat(shouldIconShowBrand(CardBrand.AmericanExpress, false, ""))
            .isTrue()
        assertThat(shouldIconShowBrand(CardBrand.Visa, false, "333"))
            .isTrue()
        assertThat(shouldIconShowBrand(CardBrand.DinersClub, false, "12"))
            .isTrue()
        assertThat(shouldIconShowBrand(CardBrand.Discover, false, null))
            .isTrue()
        assertThat(shouldIconShowBrand(CardBrand.JCB, false, "7"))
            .isTrue()
    }

    @Test
    fun shouldIconShowBrand_whenAmexAndCvCStringLengthNotFour_isFalse() {
        assertThat(shouldIconShowBrand(CardBrand.AmericanExpress, true, ""))
            .isFalse()
        assertThat(shouldIconShowBrand(CardBrand.AmericanExpress, true, "1"))
            .isFalse()
        assertThat(shouldIconShowBrand(CardBrand.AmericanExpress, true, "22"))
            .isFalse()
        assertThat(shouldIconShowBrand(CardBrand.AmericanExpress, true, "333"))
            .isFalse()
    }

    @Test
    fun shouldIconShowBrand_whenAmexAndCvcStringLengthIsFour_isTrue() {
        assertThat(shouldIconShowBrand(CardBrand.AmericanExpress, true, CVC_VALUE_AMEX))
            .isTrue()
    }

    @Test
    fun shouldIconShowBrand_whenNotAmexAndCvcStringLengthIsNotThree_isFalse() {
        assertThat(shouldIconShowBrand(CardBrand.Visa, true, ""))
            .isFalse()
        assertThat(shouldIconShowBrand(CardBrand.Discover, true, "12"))
            .isFalse()
        assertThat(shouldIconShowBrand(CardBrand.JCB, true, "55"))
            .isFalse()
        assertThat(shouldIconShowBrand(CardBrand.MasterCard, true, "9"))
            .isFalse()
        assertThat(shouldIconShowBrand(CardBrand.DinersClub, true, null))
            .isFalse()
        assertThat(shouldIconShowBrand(CardBrand.Unknown, true, "12"))
            .isFalse()
    }

    @Test
    fun shouldIconShowBrand_whenNotAmexAndCvcStringLengthIsThree_isTrue() {
        assertThat(shouldIconShowBrand(CardBrand.Visa, true, "999"))
            .isTrue()
        assertThat(shouldIconShowBrand(CardBrand.Discover, true, "123"))
            .isTrue()
        assertThat(shouldIconShowBrand(CardBrand.JCB, true, "555"))
            .isTrue()
        assertThat(shouldIconShowBrand(CardBrand.MasterCard, true, "919"))
            .isTrue()
        assertThat(shouldIconShowBrand(CardBrand.DinersClub, true, "415"))
            .isTrue()
    }

    @Test
    fun shouldIconShowBrand_whenUnknownBrandAndCvcStringLengthIsFour_isTrue() {
        assertThat(shouldIconShowBrand(CardBrand.Unknown, true, "2124"))
            .isTrue()
    }

    @Test
    fun currentFields_equals_requiredFields_withPostalCodeDisabled() {
        cardInputWidget.postalCodeEnabled = false
        idleLooper()

        assertThat(cardInputWidget.requiredFields)
            .isEqualTo(cardInputWidget.currentFields)
    }

    @Test
    fun currentFields_notEquals_requiredFields_withPostalCodeEnabled() {
        cardInputWidget.postalCodeEnabled = true
        assertThat(cardInputWidget.requiredFields)
            .isNotEqualTo(cardInputWidget.currentFields)
    }

    @Test
    fun testCardValidCallback() {
        var currentIsValid = false
        var currentInvalidFields = emptySet<CardValidCallback.Fields>()
        cardInputWidget.setCardValidCallback(
            object : CardValidCallback {
                override fun onInputChanged(
                    isValid: Boolean,
                    invalidFields: Set<CardValidCallback.Fields>
                ) {
                    currentIsValid = isValid
                    currentInvalidFields = invalidFields
                }
            }
        )

        assertThat(currentIsValid)
            .isFalse()
        assertThat(currentInvalidFields)
            .containsExactly(
                CardValidCallback.Fields.Number,
                CardValidCallback.Fields.Expiry,
                CardValidCallback.Fields.Cvc
            )

        cardInputWidget.setCardNumber(VISA_NO_SPACES)
        assertThat(currentIsValid)
            .isFalse()
        assertThat(currentInvalidFields)
            .containsExactly(CardValidCallback.Fields.Expiry, CardValidCallback.Fields.Cvc)

        expiryEditText.append("12")
        assertThat(currentIsValid)
            .isFalse()
        assertThat(currentInvalidFields)
            .containsExactly(CardValidCallback.Fields.Expiry, CardValidCallback.Fields.Cvc)

        expiryEditText.append("50")
        assertThat(currentIsValid)
            .isFalse()
        assertThat(currentInvalidFields)
            .containsExactly(CardValidCallback.Fields.Cvc)

        cvcEditText.append("12")
        assertThat(currentIsValid)
            .isFalse()
        assertThat(currentInvalidFields)
            .containsExactly(CardValidCallback.Fields.Cvc)

        cvcEditText.append("3")
        assertThat(currentIsValid)
            .isTrue()
        assertThat(currentInvalidFields)
            .isEmpty()

        cvcEditText.setText("0")
        assertThat(currentIsValid)
            .isFalse()
        assertThat(currentInvalidFields)
            .containsExactly(CardValidCallback.Fields.Cvc)
    }

    @Test
    fun shouldShowErrorIcon_shouldBeUpdatedCorrectly() {
        cardInputWidget.setExpiryDate(12, 2030)
        cardInputWidget.setCvcCode(CVC_VALUE_COMMON)

        // show error icon when validating fields with invalid card number
        cardInputWidget.setCardNumber(VISA_NO_SPACES.take(6))
        assertThat(cardInputWidget.paymentMethodCreateParams)
            .isNull()
        assertThat(cardInputWidget.shouldShowErrorIcon)
            .isTrue()

        // don't show error icon after changing input
        cardInputWidget.setCardNumber(VISA_NO_SPACES.take(7))
        assertThat(cardInputWidget.shouldShowErrorIcon)
            .isFalse()

        // don't show error icon when validating fields with invalid card number
        assertThat(cardInputWidget.paymentMethodCreateParams)
            .isNull()
        cardInputWidget.setCardNumber(VISA_NO_SPACES)
        assertThat(cardInputWidget.paymentMethodCreateParams)
            .isNotNull()
        assertThat(cardInputWidget.shouldShowErrorIcon)
            .isFalse()
    }

    @Test
    fun `createHiddenCardText with 19 digit PAN`() {
        assertThat(
            cardInputWidget.createHiddenCardText(19)
        ).isEqualTo("0000 0000 0000 0000 ")
    }

    @Test
    fun `createHiddenCardText with 16 digit PAN`() {
        assertThat(
            cardInputWidget.createHiddenCardText(16)
        ).isEqualTo("0000 0000 0000 ")
    }

    @Test
    fun `createHiddenCardText with 15 digit PAN`() {
        assertThat(
            cardInputWidget.createHiddenCardText(15)
        ).isEqualTo("0000 000000 ")
    }

    @Test
    fun `createHiddenCardText with 14 digit PAN`() {
        assertThat(
            cardInputWidget.createHiddenCardText(14)
        ).isEqualTo("0000 000000 ")
    }

    @Test
    fun usZipCodeRequired_whenFalse_shouldSetPostalCodeHint() {
        cardInputWidget.usZipCodeRequired = false
        assertThat(cardInputWidget.postalCodeEditText.hint)
            .isEqualTo("Postal code")
    }

    @Test
    fun usZipCodeRequired_whenTrue_withInvalidZipCode_shouldReturnNullCard() {
        cardInputWidget.usZipCodeRequired = true
        assertThat(cardInputWidget.postalCodeEditText.hint)
            .isEqualTo("ZIP code")

        cardInputWidget.setCardNumber(VISA_WITH_SPACES)
        cardInputWidget.expiryDateEditText.append("12")
        cardInputWidget.expiryDateEditText.append("50")
        cardInputWidget.cvcEditText.append(CVC_VALUE_COMMON)

        // invalid zipcode
        cardInputWidget.postalCodeEditText.setText(CVC_VALUE_AMEX)
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
        cardInputWidget.cvcEditText.append(CVC_VALUE_COMMON)

        // valid zipcode
        cardInputWidget.postalCodeEditText.setText(POSTAL_CODE_VALUE)

        assertThat(cardInputWidget.card)
            .isEqualTo(
                Card.Builder(VISA_NO_SPACES, 12, 2050, CVC_VALUE_COMMON)
                    .loggingTokens(ATTRIBUTION)
                    .addressZip(POSTAL_CODE_VALUE)
                    .build()
            )

        assertThat(cardInputWidget.cardParams)
            .isEqualTo(
                CardParams(
                    loggingTokens = ATTRIBUTION,
                    number = VISA_NO_SPACES,
                    expMonth = 12,
                    expYear = 2050,
                    cvc = CVC_VALUE_COMMON,
                    address = Address.Builder()
                        .setPostalCode(POSTAL_CODE_VALUE)
                        .build()
                )
            )
    }

    private fun updateCardNumberAndIdle(cardNumber: String) {
        cardNumberEditText.setText(cardNumber)
        idleLooper()
    }

    private companion object {
        // Every Card made by the CardInputView should have the card widget token.
        private val ATTRIBUTION = setOf(LOGGING_TOKEN)

        private const val CVC_VALUE_COMMON = "123"
        private const val CVC_VALUE_AMEX = "1234"
        private const val POSTAL_CODE_VALUE = "94103"
    }
}
