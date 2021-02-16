package com.stripe.android.view

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.LinearLayout
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.textfield.TextInputLayout
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.CardNumberFixtures.AMEX_NO_SPACES
import com.stripe.android.CardNumberFixtures.AMEX_WITH_SPACES
import com.stripe.android.CardNumberFixtures.DINERS_CLUB_14_WITH_SPACES
import com.stripe.android.CardNumberFixtures.VISA_NO_SPACES
import com.stripe.android.CardNumberFixtures.VISA_WITH_SPACES
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentConfiguration
import com.stripe.android.R
import com.stripe.android.cards.AccountRangeFixtures
import com.stripe.android.cards.DefaultCardAccountRangeStore
import com.stripe.android.model.Address
import com.stripe.android.model.BinFixtures
import com.stripe.android.model.Card
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.testharness.ViewTestUtils
import com.stripe.android.utils.TestUtils.idleLooper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar
import kotlin.coroutines.CoroutineContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Test class for [CardMultilineWidget].
 */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class CardMultilineWidgetTest {
    private val testDispatcher = TestCoroutineDispatcher()

    private val cardMultilineWidget: CardMultilineWidget by lazy {
        activityScenarioFactory.createView {
            createWidget(it, shouldShowPostalCode = true)
        }
    }
    private val noZipCardMultilineWidget: CardMultilineWidget by lazy {
        activityScenarioFactory.createView {
            createWidget(it, shouldShowPostalCode = false)
        }
    }
    private val fullGroup: WidgetControlGroup by lazy {
        WidgetControlGroup(cardMultilineWidget, testDispatcher)
    }
    private val noZipGroup: WidgetControlGroup by lazy {
        WidgetControlGroup(noZipCardMultilineWidget, testDispatcher)
    }

    private val fullCardListener: CardInputListener = mock()
    private val noZipCardListener: CardInputListener = mock()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val activityScenarioFactory = ActivityScenarioFactory(context)

    private val accountRangeStore = DefaultCardAccountRangeStore(context)

    @BeforeTest
    fun setup() {
        // The input date here will be invalid after 2050. Please update the test.
        assertThat(Calendar.getInstance().get(Calendar.YEAR) < 2050)
            .isTrue()

        CustomerSession.instance = mock()

        PaymentConfiguration.init(context, ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)

        // populate store with data to circumvent network requests that cause flakiness
        accountRangeStore.save(
            BinFixtures.VISA,
            listOf(AccountRangeFixtures.VISA)
        )
        accountRangeStore.save(
            BinFixtures.AMEX,
            listOf(AccountRangeFixtures.AMERICANEXPRESS)
        )
        accountRangeStore.save(
            BinFixtures.DINERSCLUB14,
            listOf(AccountRangeFixtures.DINERSCLUB14)
        )
    }

    @AfterTest
    fun cleanup() {
        testDispatcher.cleanupTestCoroutines()
    }

    private fun createWidget(
        activity: Activity,
        shouldShowPostalCode: Boolean
    ): CardMultilineWidget {
        return CardMultilineWidget(
            activity,
            shouldShowPostalCode = shouldShowPostalCode
        ).also {
            it.cardNumberEditText.workContext = testDispatcher
        }
    }

    @Test
    fun testExistence() {
        assertThat(cardMultilineWidget)
            .isNotNull()
        assertThat(fullGroup.cardNumberEditText)
            .isNotNull()
        assertThat(fullGroup.expiryDateEditText)
            .isNotNull()
        assertThat(fullGroup.cvcEditText)
            .isNotNull()
        assertThat(fullGroup.postalCodeEditText)
            .isNotNull()
        assertThat(fullGroup.secondRowLayout)
            .isNotNull()

        assertThat(noZipCardMultilineWidget)
            .isNotNull()
        assertThat(noZipGroup.cardNumberEditText)
            .isNotNull()
        assertThat(noZipGroup.expiryDateEditText)
            .isNotNull()
        assertThat(noZipGroup.cvcEditText)
            .isNotNull()
        assertThat(noZipGroup.postalCodeEditText)
            .isNotNull()
        assertThat(noZipGroup.secondRowLayout)
            .isNotNull()
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

        assertThat(shortExpiryContainer.hint)
            .isEqualTo(shortExpiryHint)
        assertThat(longExpiryContainer.hint)
            .isEqualTo(longExpiryHint)
    }

    @Test
    fun getCard_whenInputIsValidVisaWithZip_returnsCardObjectWithLoggingToken() {
        fullGroup.cardNumberEditText.setText(VISA_WITH_SPACES)
        fullGroup.expiryDateEditText.append("12")
        fullGroup.expiryDateEditText.append("50")
        fullGroup.cvcEditText.append(CVC_VALUE_COMMON)
        fullGroup.postalCodeEditText.append(POSTAL_CODE_VALUE)

        assertThat(cardMultilineWidget.card)
            .isEqualTo(
                Card.Builder(VISA_NO_SPACES, 12, 2050, CVC_VALUE_COMMON)
                    .loggingTokens(ATTRIBUTION)
                    .addressZip(POSTAL_CODE_VALUE)
                    .build()
            )
        assertThat(cardMultilineWidget.cardParams)
            .isEqualTo(
                CardParams(
                    brand = CardBrand.Visa,
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

    @Test
    fun getCard_whenInputIsValidVisaButInputHasNoZip_returnsValidCard() {
        fullGroup.cardNumberEditText.setText(VISA_WITH_SPACES)
        fullGroup.expiryDateEditText.append("12")
        fullGroup.expiryDateEditText.append("50")
        fullGroup.cvcEditText.append(CVC_VALUE_COMMON)

        assertThat(cardMultilineWidget.card)
            .isEqualTo(
                Card.Builder(VISA_NO_SPACES, 12, 2050, CVC_VALUE_COMMON)
                    .loggingTokens(ATTRIBUTION)
                    .build()
            )
        assertThat(cardMultilineWidget.cardParams)
            .isEqualTo(
                CardParams(
                    brand = CardBrand.Visa,
                    loggingTokens = ATTRIBUTION,
                    number = VISA_NO_SPACES,
                    expMonth = 12,
                    expYear = 2050,
                    cvc = CVC_VALUE_COMMON,
                    address = Address.Builder()
                        .build()
                )
            )
    }

    @Test
    fun getCard_whenInputIsValidVisaAndNoZipRequired_returnsFullCardAndExpectedLogging() {
        noZipGroup.cardNumberEditText.setText(VISA_WITH_SPACES)
        noZipGroup.expiryDateEditText.append("12")
        noZipGroup.expiryDateEditText.append("50")
        noZipGroup.cvcEditText.append(CVC_VALUE_COMMON)

        assertThat(noZipCardMultilineWidget.card)
            .isEqualTo(
                Card.Builder(VISA_NO_SPACES, 12, 2050, CVC_VALUE_COMMON)
                    .loggingTokens(ATTRIBUTION)
                    .build()
            )
        assertThat(noZipCardMultilineWidget.cardParams)
            .isEqualTo(
                CardParams(
                    brand = CardBrand.Visa,
                    loggingTokens = ATTRIBUTION,
                    number = VISA_NO_SPACES,
                    expMonth = 12,
                    expYear = 2050,
                    cvc = CVC_VALUE_COMMON,
                    address = Address.Builder()
                        .build()
                )
            )
    }

    @Test
    fun getCard_whenInputIsValidAmexAndNoZipRequiredAnd4DigitCvc_returnsFullCardAndExpectedLogging() {
        noZipGroup.cardNumberEditText.setText(AMEX_WITH_SPACES)
        noZipGroup.expiryDateEditText.append("12")
        noZipGroup.expiryDateEditText.append("50")
        noZipGroup.cvcEditText.append("1234")

        assertThat(noZipCardMultilineWidget.card)
            .isEqualTo(
                Card.Builder(AMEX_NO_SPACES, 12, 2050, CVC_VALUE_AMEX)
                    .loggingTokens(ATTRIBUTION)
                    .build()
            )
        assertThat(noZipCardMultilineWidget.cardParams)
            .isEqualTo(
                CardParams(
                    brand = CardBrand.AmericanExpress,
                    loggingTokens = ATTRIBUTION,
                    number = AMEX_NO_SPACES,
                    expMonth = 12,
                    expYear = 2050,
                    cvc = CVC_VALUE_AMEX,
                    address = Address.Builder()
                        .build()
                )
            )
    }

    @Test
    fun getCard_whenInputIsValidAmexAndNoZipRequiredAnd3DigitCvc_returnsFullCardAndExpectedLogging() {
        noZipGroup.cardNumberEditText.setText(AMEX_WITH_SPACES)
        noZipGroup.expiryDateEditText.append("12")
        noZipGroup.expiryDateEditText.append("50")
        noZipGroup.cvcEditText.append(CVC_VALUE_COMMON)

        assertThat(noZipCardMultilineWidget.card)
            .isEqualTo(
                Card.Builder(AMEX_NO_SPACES, 12, 2050, CVC_VALUE_COMMON)
                    .loggingTokens(ATTRIBUTION)
                    .build()
            )
        assertThat(noZipCardMultilineWidget.cardParams)
            .isEqualTo(
                CardParams(
                    brand = CardBrand.AmericanExpress,
                    loggingTokens = ATTRIBUTION,
                    number = AMEX_NO_SPACES,
                    expMonth = 12,
                    expYear = 2050,
                    cvc = CVC_VALUE_COMMON,
                    address = Address.Builder()
                        .build()
                )
            )
    }

    @Test
    fun getPaymentMethodCreateParams_shouldReturnExpectedObject() {
        fullGroup.cardNumberEditText.setText(VISA_WITH_SPACES)
        fullGroup.expiryDateEditText.append("12")
        fullGroup.expiryDateEditText.append("50")
        fullGroup.cvcEditText.append(CVC_VALUE_COMMON)
        fullGroup.postalCodeEditText.append(POSTAL_CODE_VALUE)

        assertThat(cardMultilineWidget.paymentMethodCreateParams)
            .isEqualTo(
                PaymentMethodCreateParams.create(
                    PaymentMethodCreateParams.Card(
                        number = VISA_NO_SPACES,
                        cvc = CVC_VALUE_COMMON,
                        expiryMonth = 12,
                        expiryYear = 2050,
                        attribution = ATTRIBUTION
                    ),
                    PaymentMethod.BillingDetails.Builder()
                        .setAddress(
                            Address.Builder()
                                .setPostalCode(POSTAL_CODE_VALUE)
                                .build()
                        )
                        .build()
                )
            )
    }

    @Test
    fun paymentMethodCard_whenInputIsValidVisaWithZip_returnsCardAndBillingDetails() {
        fullGroup.cardNumberEditText.setText(VISA_WITH_SPACES)
        fullGroup.expiryDateEditText.append("12")
        fullGroup.expiryDateEditText.append("50")
        fullGroup.cvcEditText.append(CVC_VALUE_COMMON)
        fullGroup.postalCodeEditText.append(POSTAL_CODE_VALUE)

        assertThat(cardMultilineWidget.paymentMethodCard)
            .isEqualTo(
                PaymentMethodCreateParams.Card(
                    number = VISA_NO_SPACES,
                    cvc = CVC_VALUE_COMMON,
                    expiryMonth = 12,
                    expiryYear = 2050,
                    attribution = ATTRIBUTION
                )
            )

        assertThat(cardMultilineWidget.paymentMethodBillingDetails?.address?.postalCode)
            .isEqualTo(POSTAL_CODE_VALUE)
    }

    @Test
    fun paymentMethodCreateParams_whenPostalCodeIsRequiredAndValueIsBlank_returnsNull() {
        cardMultilineWidget.setShouldShowPostalCode(true)
        cardMultilineWidget.postalCodeRequired = true

        fullGroup.cardNumberEditText.setText(VISA_WITH_SPACES)
        fullGroup.expiryDateEditText.append("12")
        fullGroup.expiryDateEditText.append("50")
        fullGroup.cvcEditText.append(CVC_VALUE_COMMON)

        assertThat(cardMultilineWidget.paymentMethodCreateParams)
            .isNull()
    }

    @Test
    fun paymentMethodCreateParams_whenPostalCodeIsRequiredAndValueIsNotBlank_returnsNotNull() {
        cardMultilineWidget.setShouldShowPostalCode(true)
        cardMultilineWidget.postalCodeRequired = false

        fullGroup.cardNumberEditText.setText(VISA_WITH_SPACES)
        fullGroup.expiryDateEditText.append("12")
        fullGroup.expiryDateEditText.append("50")
        fullGroup.cvcEditText.append(CVC_VALUE_COMMON)

        assertThat(cardMultilineWidget.paymentMethodCreateParams)
            .isNotNull()
    }

    @Test
    fun paymentMethodCreateParams_whenPostalCodeIsNotRequiredAndValueIsBlank_returnsNotNull() {
        cardMultilineWidget.setShouldShowPostalCode(true)
        cardMultilineWidget.postalCodeRequired = false

        fullGroup.cardNumberEditText.setText(VISA_WITH_SPACES)
        fullGroup.expiryDateEditText.append("12")
        fullGroup.expiryDateEditText.append("50")
        fullGroup.cvcEditText.append(CVC_VALUE_COMMON)

        assertThat(cardMultilineWidget.paymentMethodCreateParams)
            .isNotNull()
    }

    @Test
    fun paymentMethodCard_whenInputIsValidVisaAndNoZipRequired_returnsFullCard() {
        noZipGroup.cardNumberEditText.setText(VISA_WITH_SPACES)
        noZipGroup.expiryDateEditText.append("12")
        noZipGroup.expiryDateEditText.append("50")
        noZipGroup.cvcEditText.append(CVC_VALUE_COMMON)

        assertThat(noZipCardMultilineWidget.paymentMethodCard)
            .isEqualTo(
                PaymentMethodCreateParams.Card(
                    number = VISA_NO_SPACES,
                    cvc = CVC_VALUE_COMMON,
                    expiryMonth = 12,
                    expiryYear = 2050,
                    attribution = ATTRIBUTION
                )
            )

        assertThat(noZipCardMultilineWidget.paymentMethodBillingDetails)
            .isNull()
    }

    @Test
    fun paymentMethodCard_whenInputIsValidAmexAndNoZipRequiredAnd4DigitCvc_returnsFullCard() {
        noZipGroup.cardNumberEditText.setText(AMEX_WITH_SPACES)
        noZipGroup.expiryDateEditText.append("12")
        noZipGroup.expiryDateEditText.append("50")
        noZipGroup.cvcEditText.append("1234")

        assertThat(noZipCardMultilineWidget.paymentMethodCard)
            .isEqualTo(
                PaymentMethodCreateParams.Card(
                    number = AMEX_NO_SPACES,
                    cvc = "1234",
                    expiryMonth = 12,
                    expiryYear = 2050,
                    attribution = ATTRIBUTION
                )
            )

        assertThat(noZipCardMultilineWidget.paymentMethodBillingDetails)
            .isNull()
    }

    @Test
    fun paymentMethodCard_whenInputIsValidAmexAndNoZipRequiredAnd3DigitCvc_returnsFullCard() {
        noZipGroup.cardNumberEditText.setText(AMEX_WITH_SPACES)
        noZipGroup.expiryDateEditText.append("12")
        noZipGroup.expiryDateEditText.append("50")
        noZipGroup.cvcEditText.append(CVC_VALUE_COMMON)

        assertThat(noZipCardMultilineWidget.paymentMethodCard)
            .isEqualTo(
                PaymentMethodCreateParams.Card(
                    number = AMEX_NO_SPACES,
                    cvc = CVC_VALUE_COMMON,
                    expiryMonth = 12,
                    expiryYear = 2050,
                    attribution = ATTRIBUTION
                )
            )

        assertThat(noZipCardMultilineWidget.paymentMethodBillingDetails)
            .isNull()
    }

    @Test
    fun initView_whenZipRequired_secondRowContainsThreeVisibleElements() {
        assertThat(fullGroup.expiryDateEditText.visibility)
            .isEqualTo(View.VISIBLE)
        assertThat(fullGroup.cvcEditText.visibility)
            .isEqualTo(View.VISIBLE)
        assertThat(fullGroup.postalCodeEditText.visibility)
            .isEqualTo(View.VISIBLE)
        assertThat(fullGroup.postalCodeInputLayout.visibility)
            .isEqualTo(View.VISIBLE)
    }

    @Test
    fun clear_whenZipRequiredAndAllFieldsEntered_clearsAllfields() {
        fullGroup.cardNumberEditText.setText(VISA_WITH_SPACES)
        fullGroup.expiryDateEditText.append("12")
        fullGroup.expiryDateEditText.append("50")
        fullGroup.cvcEditText.append(CVC_VALUE_COMMON)
        fullGroup.postalCodeEditText.append(POSTAL_CODE_VALUE)

        cardMultilineWidget.clear()

        assertThat(fullGroup.cardNumberEditText.text?.toString())
            .isEmpty()
        assertThat(fullGroup.expiryDateEditText.text?.toString())
            .isEmpty()
        assertThat(fullGroup.cvcEditText.text?.toString())
            .isEmpty()
        assertThat(fullGroup.postalCodeEditText.text?.toString())
            .isEmpty()
    }

    @Test
    fun clear_whenFieldsInErrorState_clearsFieldsAndHidesErrors() {
        // Makes this 4242 4242 4242 4243
        val badVisa = VISA_WITH_SPACES.take(VISA_WITH_SPACES.length - 1) + "3"
        fullGroup.cardNumberEditText.setText(badVisa)

        fullGroup.expiryDateEditText.append("01")
        fullGroup.expiryDateEditText.append("11")
        fullGroup.cvcEditText.append("12")
        fullGroup.postalCodeEditText.append("1234")

        cardMultilineWidget.validateAllFields()

        assertThat(fullGroup.cardNumberEditText.shouldShowError)
            .isTrue()
        assertThat(fullGroup.expiryDateEditText.shouldShowError)
            .isTrue()
        assertThat(fullGroup.cvcEditText.shouldShowError)
            .isTrue()
        assertThat(fullGroup.postalCodeEditText.shouldShowError)
            .isFalse()

        cardMultilineWidget.clear()

        assertThat(fullGroup.cardNumberEditText.shouldShowError)
            .isFalse()
        assertThat(fullGroup.expiryDateEditText.shouldShowError)
            .isFalse()
        assertThat(fullGroup.cvcEditText.shouldShowError)
            .isFalse()
        assertThat(fullGroup.postalCodeEditText.shouldShowError)
            .isFalse()
    }

    @Test
    fun setCvcLabel_shouldShowCustomLabelIfPresent() {
        cardMultilineWidget.setCvcLabel("my cool cvc")
        assertThat(fullGroup.cvcInputLayout.hint)
            .isEqualTo("my cool cvc")

        cardMultilineWidget.setCvcLabel(null)
        assertThat(fullGroup.cvcInputLayout.hint)
            .isEqualTo("CVC")
    }

    @Test
    fun initView_whenZipRequiredThenSetToHidden_secondRowLosesPostalCodeAndAdjustsMargin() {
        assertThat(fullGroup.postalCodeInputLayout.visibility)
            .isEqualTo(View.VISIBLE)
        cardMultilineWidget.setShouldShowPostalCode(false)
        assertThat(fullGroup.postalCodeInputLayout.visibility)
            .isEqualTo(View.GONE)

        val params = fullGroup.cvcInputLayout.layoutParams as LinearLayout.LayoutParams
        assertThat(params.marginEnd)
            .isEqualTo(0)
        assertThat(params.marginEnd)
            .isEqualTo(0)
    }

    @Test
    fun initView_whenNoZipRequired_secondRowContainsTwoVisibleElements() {
        assertThat(noZipGroup.expiryDateEditText.visibility)
            .isEqualTo(View.VISIBLE)
        assertThat(noZipGroup.cvcEditText.visibility)
            .isEqualTo(View.VISIBLE)
        assertThat(noZipGroup.postalCodeInputLayout.visibility)
            .isEqualTo(View.GONE)
    }

    @Test
    fun initView_whenZipHiddenThenSetToRequired_secondRowAddsPostalCodeAndAdjustsMargin() {
        assertThat(noZipGroup.postalCodeInputLayout.visibility)
            .isEqualTo(View.GONE)
        noZipCardMultilineWidget.setShouldShowPostalCode(true)
        assertThat(noZipGroup.postalCodeInputLayout.visibility)
            .isEqualTo(View.VISIBLE)

        val expectedMargin = noZipCardMultilineWidget.resources
            .getDimensionPixelSize(R.dimen.stripe_add_card_expiry_middle_margin)

        val params = noZipGroup.cvcInputLayout.layoutParams as LinearLayout.LayoutParams
        assertThat(params.marginEnd)
            .isEqualTo(expectedMargin)
        assertThat(params.marginEnd)
            .isEqualTo(expectedMargin)
    }

    @Test
    fun onCompleteCardNumber_whenValid_shiftsFocusToExpiryDate() {
        cardMultilineWidget.setCardInputListener(fullCardListener)
        noZipCardMultilineWidget.setCardInputListener(noZipCardListener)

        fullGroup.cardNumberEditText.setText(VISA_WITH_SPACES)
        idleLooper()

        verify(fullCardListener).onCardComplete()
        verify(fullCardListener).onFocusChange(CardInputListener.FocusField.ExpiryDate)
        assertThat(fullGroup.expiryDateEditText.hasFocus())
            .isTrue()

        noZipGroup.cardNumberEditText.setText(AMEX_WITH_SPACES)
        idleLooper()

        verify(noZipCardListener).onCardComplete()
        verify(noZipCardListener).onFocusChange(CardInputListener.FocusField.ExpiryDate)
        assertThat(noZipGroup.expiryDateEditText.hasFocus())
            .isTrue()
    }

    @Test
    fun onCompleteExpiry_whenValid_shiftsFocusToCvc() {
        cardMultilineWidget.setCardInputListener(fullCardListener)
        noZipCardMultilineWidget.setCardInputListener(noZipCardListener)

        fullGroup.expiryDateEditText.append("12")
        fullGroup.expiryDateEditText.append("50")
        verify(fullCardListener).onExpirationComplete()
        verify(fullCardListener).onFocusChange(CardInputListener.FocusField.Cvc)
        assertThat(fullGroup.cvcEditText.hasFocus())
            .isTrue()

        noZipGroup.expiryDateEditText.append("12")
        noZipGroup.expiryDateEditText.append("50")
        verify(noZipCardListener).onExpirationComplete()
        verify(noZipCardListener).onFocusChange(CardInputListener.FocusField.Cvc)
        assertThat(noZipGroup.cvcEditText.hasFocus())
            .isTrue()
    }

    @Test
    fun onCompleteCvc_whenValid_shiftsFocusOnlyIfPostalCodeShown() {
        cardMultilineWidget.setCardInputListener(fullCardListener)
        noZipCardMultilineWidget.setCardInputListener(noZipCardListener)

        fullGroup.cardNumberEditText.setText(VISA_WITH_SPACES)
        fullGroup.expiryDateEditText.append("12")
        fullGroup.expiryDateEditText.append("50")
        fullGroup.cvcEditText.append(CVC_VALUE_COMMON)
        verify(fullCardListener).onCvcComplete()
        verify(fullCardListener).onFocusChange(CardInputListener.FocusField.PostalCode)
        assertThat(fullGroup.postalCodeEditText.hasFocus())
            .isTrue()

        noZipGroup.cardNumberEditText.setText(VISA_WITH_SPACES)
        noZipGroup.expiryDateEditText.append("12")
        noZipGroup.expiryDateEditText.append("50")
        noZipGroup.cvcEditText.append(CVC_VALUE_COMMON)
        verify(noZipCardListener).onCvcComplete()
        verify(noZipCardListener, never()).onFocusChange(CardInputListener.FocusField.PostalCode)
        assertThat(noZipGroup.cvcEditText.hasFocus())
            .isTrue()
    }

    @Test
    fun deleteWhenEmpty_fromExpiry_withPostalCode_shiftsToCardNumber() {
        cardMultilineWidget.setCardInputListener(fullCardListener)
        fullGroup.cardNumberEditText.setText(VISA_WITH_SPACES)

        assertThat(fullGroup.expiryDateEditText.hasFocus())
            .isTrue()
        ViewTestUtils.sendDeleteKeyEvent(fullGroup.expiryDateEditText)

        verify(fullCardListener).onFocusChange(CardInputListener.FocusField.CardNumber)
        assertThat(fullGroup.cardNumberEditText.hasFocus())
            .isTrue()
        assertThat(fullGroup.cardNumberEditText.text?.toString())
            .isEqualTo(VISA_WITH_SPACES.take(VISA_WITH_SPACES.length - 1))
    }

    @Test
    fun deleteWhenEmpty_fromExpiry_withoutPostalCode_shiftsToCardNumber() {
        noZipCardMultilineWidget.setCardInputListener(noZipCardListener)
        noZipGroup.cardNumberEditText.setText(VISA_WITH_SPACES)

        assertThat(noZipGroup.expiryDateEditText.hasFocus())
            .isTrue()
        ViewTestUtils.sendDeleteKeyEvent(noZipGroup.expiryDateEditText)

        verify(noZipCardListener).onFocusChange(CardInputListener.FocusField.CardNumber)
        assertThat(noZipGroup.cardNumberEditText.hasFocus())
            .isTrue()
        assertThat(noZipGroup.cardNumberEditText.text?.toString())
            .isEqualTo(VISA_WITH_SPACES.take(VISA_WITH_SPACES.length - 1))
    }

    @Test
    fun deleteWhenEmpty_fromCvc_shiftsToExpiry() {
        cardMultilineWidget.setCardInputListener(fullCardListener)
        noZipCardMultilineWidget.setCardInputListener(noZipCardListener)

        fullGroup.expiryDateEditText.append("12")
        fullGroup.expiryDateEditText.append("50")

        assertThat(fullGroup.cvcEditText.hasFocus())
            .isTrue()
        ViewTestUtils.sendDeleteKeyEvent(fullGroup.cvcEditText)

        verify(fullCardListener).onFocusChange(CardInputListener.FocusField.ExpiryDate)
        assertThat(fullGroup.expiryDateEditText.hasFocus())
            .isTrue()
        assertThat(fullGroup.expiryDateEditText.text?.toString())
            .isEqualTo("12/5")

        noZipGroup.expiryDateEditText.append("12")
        noZipGroup.expiryDateEditText.append("50")

        assertThat(noZipGroup.cvcEditText.hasFocus())
            .isTrue()
        ViewTestUtils.sendDeleteKeyEvent(noZipGroup.cvcEditText)

        verify(noZipCardListener).onFocusChange(CardInputListener.FocusField.ExpiryDate)
        assertThat(noZipGroup.expiryDateEditText.hasFocus())
            .isTrue()
        assertThat(noZipGroup.expiryDateEditText.fieldText)
            .isEqualTo("12/5")
    }

    @Test
    fun deleteWhenEmpty_fromPostalCode_shiftsToCvc() {
        cardMultilineWidget.setCardInputListener(fullCardListener)

        fullGroup.cardNumberEditText.setText(DINERS_CLUB_14_WITH_SPACES)
        fullGroup.expiryDateEditText.append("12")
        fullGroup.expiryDateEditText.append("50")
        fullGroup.cvcEditText.append(CVC_VALUE_COMMON)

        reset(fullCardListener)
        ViewTestUtils.sendDeleteKeyEvent(fullGroup.postalCodeEditText)

        verify(fullCardListener).onFocusChange(CardInputListener.FocusField.Cvc)
        assertThat(fullGroup.cvcEditText.text?.toString())
            .isEqualTo("12")
    }

    @Test
    fun setCardNumber_whenHasSpaces_canCreateValidCard() {
        cardMultilineWidget.setCardNumber(VISA_NO_SPACES)
        fullGroup.expiryDateEditText.append("12")
        fullGroup.expiryDateEditText.append("50")
        fullGroup.cvcEditText.append(CVC_VALUE_COMMON)
        fullGroup.postalCodeEditText.append(POSTAL_CODE_VALUE)

        assertThat(cardMultilineWidget.card)
            .isEqualTo(
                Card.Builder(VISA_NO_SPACES, 12, 2050, CVC_VALUE_COMMON)
                    .loggingTokens(ATTRIBUTION)
                    .addressZip(POSTAL_CODE_VALUE)
                    .build()
            )
        assertThat(cardMultilineWidget.cardParams)
            .isEqualTo(
                CardParams(
                    brand = CardBrand.Visa,
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

    @Test
    fun setCardNumber_whenHasNoSpaces_canCreateValidCard() {
        cardMultilineWidget.setCardNumber(VISA_WITH_SPACES)
        fullGroup.expiryDateEditText.append("12")
        fullGroup.expiryDateEditText.append("50")
        fullGroup.cvcEditText.append(CVC_VALUE_COMMON)
        fullGroup.postalCodeEditText.append(POSTAL_CODE_VALUE)

        assertThat(cardMultilineWidget.card)
            .isEqualTo(
                Card.Builder(VISA_NO_SPACES, 12, 2050, CVC_VALUE_COMMON)
                    .loggingTokens(ATTRIBUTION)
                    .addressZip(POSTAL_CODE_VALUE)
                    .build()
            )
        assertThat(cardMultilineWidget.cardParams)
            .isEqualTo(
                CardParams(
                    brand = CardBrand.Visa,
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

    @Test
    fun validateCardNumber_whenValid_doesNotShowError() {
        cardMultilineWidget.setCardNumber(VISA_WITH_SPACES)

        val isValid = cardMultilineWidget.validateCardNumber()
        val shouldShowError = fullGroup.cardNumberEditText.shouldShowError

        assertThat(isValid)
            .isTrue()
        assertThat(shouldShowError)
            .isFalse()
    }

    @Test
    fun validateCardNumber_whenInvalid_setsShowError() {
        val invalidNumber = "1234 1234 1234 1234"
        cardMultilineWidget.setCardNumber(invalidNumber)

        val isValid = cardMultilineWidget.validateCardNumber()
        val shouldShowError = fullGroup.cardNumberEditText.shouldShowError

        assertThat(isValid)
            .isFalse()
        assertThat(shouldShowError)
            .isTrue()
    }

    @Test
    fun onFinishInflate_shouldSetPostalCodeInputLayoutHint() {
        var inflatedCardMultilineWidget: CardMultilineWidget? = null
        activityScenarioFactory
            .createAddPaymentMethodActivity()
            .use { activityScenario ->
                activityScenario.onActivity { activity ->
                    inflatedCardMultilineWidget = activity.findViewById(R.id.card_multiline_widget)
                }
            }
        assertThat(requireNotNull(inflatedCardMultilineWidget).postalInputLayout.hint)
            .isEqualTo("Postal code")
    }

    @Test
    fun usZipCodeRequired_whenFalse_shouldSetPostalCodeHint() {
        cardMultilineWidget.usZipCodeRequired = false
        assertThat(cardMultilineWidget.postalInputLayout.hint)
            .isEqualTo("Postal code")

        cardMultilineWidget.setCardNumber(VISA_WITH_SPACES)
        fullGroup.expiryDateEditText.append("12")
        fullGroup.expiryDateEditText.append("50")
        fullGroup.cvcEditText.append(CVC_VALUE_COMMON)

        assertThat(cardMultilineWidget.card)
            .isEqualTo(
                Card.Builder(VISA_NO_SPACES, 12, 2050, CVC_VALUE_COMMON)
                    .loggingTokens(ATTRIBUTION)
                    .build()
            )
        assertThat(cardMultilineWidget.cardParams)
            .isEqualTo(
                CardParams(
                    brand = CardBrand.Visa,
                    loggingTokens = ATTRIBUTION,
                    number = VISA_NO_SPACES,
                    expMonth = 12,
                    expYear = 2050,
                    cvc = CVC_VALUE_COMMON,
                    address = Address.Builder()
                        .build()
                )
            )
    }

    @Test
    fun usZipCodeRequired_whenTrue_withInvalidZipCode_shouldReturnNullCard() {
        cardMultilineWidget.usZipCodeRequired = true
        assertThat(cardMultilineWidget.postalInputLayout.hint)
            .isEqualTo("ZIP code")

        cardMultilineWidget.setCardNumber(VISA_WITH_SPACES)
        fullGroup.expiryDateEditText.append("12")
        fullGroup.expiryDateEditText.append("50")
        fullGroup.cvcEditText.append(CVC_VALUE_COMMON)

        // invalid zipcode
        fullGroup.postalCodeEditText.setText("1234")
        assertThat(cardMultilineWidget.card)
            .isNull()
        assertThat(cardMultilineWidget.cardParams)
            .isNull()
    }

    @Test
    fun usZipCodeRequired_whenTrue_withValidZipCode_shouldReturnNotNullCard() {
        cardMultilineWidget.usZipCodeRequired = true
        assertThat(cardMultilineWidget.postalInputLayout.hint)
            .isEqualTo("ZIP code")

        cardMultilineWidget.setCardNumber(VISA_WITH_SPACES)
        fullGroup.expiryDateEditText.append("12")
        fullGroup.expiryDateEditText.append("50")
        fullGroup.cvcEditText.append(CVC_VALUE_COMMON)

        // valid zipcode
        fullGroup.postalCodeEditText.setText(POSTAL_CODE_VALUE)
        assertThat(cardMultilineWidget.card)
            .isEqualTo(
                Card.Builder(VISA_NO_SPACES, 12, 2050, CVC_VALUE_COMMON)
                    .loggingTokens(ATTRIBUTION)
                    .addressZip(POSTAL_CODE_VALUE)
                    .build()
            )
        assertThat(cardMultilineWidget.cardParams)
            .isEqualTo(
                CardParams(
                    brand = CardBrand.Visa,
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

    @Test
    fun setEnabled_setsEnabledPropertyOnAllChildWidgets() {
        assertThat(cardMultilineWidget.isEnabled)
            .isTrue()
        assertThat(fullGroup.cardInputLayout.isEnabled)
            .isTrue()
        assertThat(fullGroup.expiryInputLayout.isEnabled)
            .isTrue()
        assertThat(fullGroup.postalCodeInputLayout.isEnabled)
            .isTrue()
        assertThat(fullGroup.cvcInputLayout.isEnabled)
            .isTrue()
        assertThat(fullGroup.expiryDateEditText.isEnabled)
            .isTrue()
        assertThat(fullGroup.cardNumberEditText.isEnabled)
            .isTrue()
        assertThat(fullGroup.cvcEditText.isEnabled)
            .isTrue()
        assertThat(fullGroup.postalCodeEditText.isEnabled)
            .isTrue()

        cardMultilineWidget.isEnabled = false

        assertThat(cardMultilineWidget.isEnabled)
            .isFalse()
        assertThat(fullGroup.cardInputLayout.isEnabled)
            .isFalse()
        assertThat(fullGroup.expiryInputLayout.isEnabled)
            .isFalse()
        assertThat(fullGroup.postalCodeInputLayout.isEnabled)
            .isFalse()
        assertThat(fullGroup.cvcInputLayout.isEnabled)
            .isFalse()
        assertThat(fullGroup.expiryDateEditText.isEnabled)
            .isFalse()
        assertThat(fullGroup.cardNumberEditText.isEnabled)
            .isFalse()
        assertThat(fullGroup.cvcEditText.isEnabled)
            .isFalse()
        assertThat(fullGroup.postalCodeEditText.isEnabled)
            .isFalse()

        cardMultilineWidget.isEnabled = true

        assertThat(cardMultilineWidget.isEnabled)
            .isTrue()
        assertThat(fullGroup.cardInputLayout.isEnabled)
            .isTrue()
        assertThat(fullGroup.expiryInputLayout.isEnabled)
            .isTrue()
        assertThat(fullGroup.postalCodeInputLayout.isEnabled)
            .isTrue()
        assertThat(fullGroup.cvcInputLayout.isEnabled)
            .isTrue()
        assertThat(fullGroup.expiryDateEditText.isEnabled)
            .isTrue()
        assertThat(fullGroup.cardNumberEditText.isEnabled)
            .isTrue()
        assertThat(fullGroup.cvcEditText.isEnabled)
            .isTrue()
        assertThat(fullGroup.postalCodeEditText.isEnabled)
            .isTrue()
    }

    @Test
    fun testCardValidCallback() {
        var currentIsValid = false
        var currentInvalidFields = emptySet<CardValidCallback.Fields>()
        cardMultilineWidget.setCardValidCallback { isValid, invalidFields ->
            currentIsValid = isValid
            currentInvalidFields = invalidFields
        }

        assertThat(currentIsValid)
            .isFalse()
        assertThat(currentInvalidFields)
            .containsExactly(
                CardValidCallback.Fields.Number,
                CardValidCallback.Fields.Expiry,
                CardValidCallback.Fields.Cvc
            )

        cardMultilineWidget.setCardNumber(VISA_NO_SPACES)
        assertThat(currentIsValid)
            .isFalse()
        assertThat(currentInvalidFields)
            .containsExactly(
                CardValidCallback.Fields.Expiry,
                CardValidCallback.Fields.Cvc
            )

        fullGroup.expiryDateEditText.append("12")
        assertThat(currentIsValid)
            .isFalse()
        assertThat(currentInvalidFields)
            .containsExactly(
                CardValidCallback.Fields.Expiry,
                CardValidCallback.Fields.Cvc
            )

        fullGroup.expiryDateEditText.append("50")
        assertThat(currentIsValid)
            .isFalse()
        assertThat(currentInvalidFields)
            .containsExactly(
                CardValidCallback.Fields.Cvc
            )

        fullGroup.cvcEditText.append("12")
        assertThat(currentIsValid)
            .isFalse()
        assertThat(currentInvalidFields)
            .containsExactly(CardValidCallback.Fields.Cvc)

        fullGroup.cvcEditText.append("3")
        assertThat(currentIsValid)
            .isTrue()
        assertThat(currentInvalidFields)
            .isEmpty()

        fullGroup.cvcEditText.setText("0")
        assertThat(currentIsValid)
            .isFalse()
        assertThat(currentInvalidFields)
            .containsExactly(CardValidCallback.Fields.Cvc)
    }

    @Test
    fun shouldShowErrorIcon_shouldBeUpdatedCorrectly() {
        cardMultilineWidget.setExpiryDate(12, 2030)
        cardMultilineWidget.setCvcCode(CVC_VALUE_COMMON)

        // show error icon when validating fields with invalid card number
        cardMultilineWidget.setCardNumber(VISA_NO_SPACES.take(6))
        assertThat(cardMultilineWidget.paymentMethodCreateParams)
            .isNull()
        assertThat(cardMultilineWidget.shouldShowErrorIcon)
            .isTrue()

        // don't show error icon after changing input
        cardMultilineWidget.setCardNumber(VISA_NO_SPACES.take(7))
        assertThat(cardMultilineWidget.shouldShowErrorIcon)
            .isFalse()

        // don't show error icon when validating fields with invalid card number
        assertThat(cardMultilineWidget.paymentMethodCreateParams)
            .isNull()
        cardMultilineWidget.setCardNumber(VISA_NO_SPACES)
        assertThat(cardMultilineWidget.paymentMethodCreateParams)
            .isNotNull()
        assertThat(cardMultilineWidget.shouldShowErrorIcon)
            .isFalse()
    }

    @Test
    fun `cardNumberEditText's drawable should be on the end`() {
        assertThat(
            cardMultilineWidget.cardNumberEditText.compoundDrawables[0]
        ).isNull()
        assertThat(
            cardMultilineWidget.cardNumberEditText.compoundDrawables[2]
        ).isNotNull()
    }

    internal class WidgetControlGroup(
        widget: CardMultilineWidget,
        workContext: CoroutineContext
    ) {
        val cardNumberEditText: CardNumberEditText = widget.findViewById<CardNumberEditText>(R.id.et_card_number).also {
            it.workContext = workContext
        }
        val cardInputLayout: TextInputLayout = widget.findViewById(R.id.tl_card_number)
        val expiryDateEditText: ExpiryDateEditText = widget.findViewById(R.id.et_expiry)
        val expiryInputLayout: TextInputLayout = widget.findViewById(R.id.tl_expiry)
        val cvcEditText: StripeEditText = widget.findViewById(R.id.et_cvc)
        val cvcInputLayout: TextInputLayout = widget.findViewById(R.id.tl_cvc)
        val postalCodeEditText: StripeEditText = widget.findViewById(R.id.et_postal_code)
        val postalCodeInputLayout: TextInputLayout = widget.findViewById(R.id.tl_postal_code)
        val secondRowLayout: LinearLayout = widget.findViewById(R.id.second_row_layout)
    }

    private companion object {
        // Every Card made by the CardInputView should have the card widget token.
        private val ATTRIBUTION = setOf("CardMultilineView")

        private const val CVC_VALUE_COMMON = "123"
        private const val CVC_VALUE_AMEX = "1234"
        private const val POSTAL_CODE_VALUE = "94103"
    }
}
