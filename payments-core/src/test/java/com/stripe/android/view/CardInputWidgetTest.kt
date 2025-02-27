package com.stripe.android.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.view.updateLayoutParams
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.CardNumberFixtures
import com.stripe.android.CardNumberFixtures.AMEX_NO_SPACES
import com.stripe.android.CardNumberFixtures.AMEX_WITH_SPACES
import com.stripe.android.CardNumberFixtures.CO_BRAND_CARTES_MASTERCARD_WITH_SPACES
import com.stripe.android.CardNumberFixtures.DINERS_CLUB_14_NO_SPACES
import com.stripe.android.CardNumberFixtures.DINERS_CLUB_14_WITH_SPACES
import com.stripe.android.CardNumberFixtures.VISA_NO_SPACES
import com.stripe.android.CardNumberFixtures.VISA_WITH_SPACES
import com.stripe.android.PaymentConfiguration
import com.stripe.android.R
import com.stripe.android.cards.AccountRangeFixtures
import com.stripe.android.cards.DefaultCardAccountRangeStore
import com.stripe.android.model.Address
import com.stripe.android.model.BinFixtures
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardParams
import com.stripe.android.model.Networks
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.testharness.ViewTestUtils
import com.stripe.android.utils.CardElementTestHelper
import com.stripe.android.utils.TestUtils.idleLooper
import com.stripe.android.utils.createTestActivityRule
import com.stripe.android.view.CardInputWidget.Companion.LOGGING_TOKEN
import com.stripe.android.view.CardInputWidget.Companion.shouldIconShowBrand
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.parcelize.Parcelize
import org.hamcrest.CoreMatchers.anything
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import java.util.Calendar
import kotlin.coroutines.CoroutineContext
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
internal class CardInputWidgetTest {

    @get:Rule
    val testActivityRule = createTestActivityRule<CardInputWidgetTestActivity>()

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val cardInputListener = FakeCardInputListener()
    private val accountRangeStore = DefaultCardAccountRangeStore(context)

    @BeforeTest
    fun setup() {
        // The input date here will be invalid after 2050. Please update the test.
        assertThat(Calendar.getInstance().get(Calendar.YEAR) < 2050)
            .isTrue()

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

    @Test
    fun getCard_whenInputIsValidVisa_withPostalCodeDisabled_returnsCardObjectWithLoggingToken() =
        runCardInputWidgetTest {
            postalCodeEnabled = false

            updateCardNumberAndIdle(VISA_WITH_SPACES)
            expiryDateEditText.append("12")
            expiryDateEditText.append("50")
            cvcEditText.append(CVC_VALUE_COMMON)

            assertThat(cardParams)
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

            assertThat(paymentMethodCreateParams)
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
    fun getCard_whenInputIsCoBrandedCard_withPreNetworks_returnsCardObjectWithPrefNetworks() =
        runCardInputWidgetTest {
            postalCodeEnabled = false

            updateCardNumberAndIdle(CO_BRAND_CARTES_MASTERCARD_WITH_SPACES)
            expiryDateEditText.append("12")
            expiryDateEditText.append("50")
            cvcEditText.append(CVC_VALUE_COMMON)
            setPreferredNetworks(listOf(CardBrand.CartesBancaires))

            assertThat(cardParams?.networks)
                .isEqualTo(Networks(CardBrand.CartesBancaires.code))

            assertThat(paymentMethodCreateParams?.card?.networks?.preferred)
                .isEqualTo(CardBrand.CartesBancaires.code)
        }

    @Test
    fun getCard_whenInputIsValidVisa_withPostalCodeEnabled_returnsCardObjectWithLoggingToken() =
        runCardInputWidgetTest {
            postalCodeEnabled = true

            updateCardNumberAndIdle(VISA_WITH_SPACES)
            expiryDateEditText.append("12")
            expiryDateEditText.append("50")
            cvcEditText.append(CVC_VALUE_COMMON)
            postalCodeEditText.setText(POSTAL_CODE_VALUE)

            assertThat(cardParams)
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

            assertThat(paymentMethodCreateParams)
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
    fun getCard_whenInputIsValidAmEx_withPostalCodeDisabled_createsExpectedObjects() = runCardInputWidgetTest {
        postalCodeEnabled = false

        updateCardNumberAndIdle(AMEX_WITH_SPACES)
        expiryDateEditText.append("12")
        expiryDateEditText.append("50")
        cvcEditText.append(CVC_VALUE_AMEX)

        assertThat(cardParams)
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

        assertThat(paymentMethodCreateParams)
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
    fun getCard_whenInputIsValidAmEx_withPostalCodeEnabled_createsExpectedObjects() = runCardInputWidgetTest {
        postalCodeEnabled = true

        updateCardNumberAndIdle(AMEX_WITH_SPACES)
        expiryDateEditText.append("12")
        expiryDateEditText.append("50")
        cvcEditText.append(CVC_VALUE_AMEX)
        postalCodeEditText.setText(POSTAL_CODE_VALUE)

        assertThat(cardParams)
            .isEqualTo(
                CardParams(
                    brand = CardBrand.AmericanExpress,
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

        assertThat(paymentMethodCreateParams)
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
    fun getCard_whenInputIsValidDinersClub_withPostalCodeDisabled_returnsCardObjectWithLoggingToken() =
        runCardInputWidgetTest {
            postalCodeEnabled = false

            updateCardNumberAndIdle(DINERS_CLUB_14_WITH_SPACES)
            expiryDateEditText.append("12")
            expiryDateEditText.append("50")
            cvcEditText.append(CVC_VALUE_COMMON)

            assertThat(cardParams)
                .isEqualTo(
                    CardParams(
                        brand = CardBrand.DinersClub,
                        loggingTokens = ATTRIBUTION,
                        number = DINERS_CLUB_14_NO_SPACES,
                        expMonth = 12,
                        expYear = 2050,
                        cvc = CVC_VALUE_COMMON,
                        address = Address.Builder()
                            .build()
                    )
                )

            assertThat(paymentMethodCard)
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
    fun getCard_whenInputIsValidDinersClub_withPostalCodeEnabled_returnsCardObjectWithLoggingToken() =
        runCardInputWidgetTest {
            postalCodeEnabled = true

            updateCardNumberAndIdle(DINERS_CLUB_14_WITH_SPACES)
            expiryDateEditText.append("12")
            expiryDateEditText.append("50")
            cvcEditText.append(CVC_VALUE_COMMON)
            postalCodeEditText.setText(POSTAL_CODE_VALUE)

            assertThat(cardParams)
                .isEqualTo(
                    CardParams(
                        brand = CardBrand.DinersClub,
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

            assertThat(paymentMethodCard)
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
    fun getCard_whenPostalCodeIsEnabledAndRequired_andValueIsBlank_returnsNull() = runCardInputWidgetTest {
        postalCodeEnabled = true
        postalCodeRequired = true

        updateCardNumberAndIdle(VISA_WITH_SPACES)
        expiryDateEditText.append("12")
        expiryDateEditText.append("50")
        cvcEditText.append(CVC_VALUE_COMMON)
        postalCodeEditText.append("")

        assertThat(cardParams)
            .isNull()
        assertThat(paymentMethodCard)
            .isNull()
    }

    @Test
    fun getCard_whenInputHasIncompleteCardNumber_returnsNull() = runCardInputWidgetTest {
        // This will be 242 4242 4242 4242
        updateCardNumberAndIdle(VISA_WITH_SPACES.drop(1))
        expiryDateEditText.append("12")
        expiryDateEditText.append("50")
        cvcEditText.append(CVC_VALUE_COMMON)

        assertThat(cardParams)
            .isNull()
        assertThat(paymentMethodCard)
            .isNull()
    }

    @Test
    fun getCard_whenInputHasExpiredDate_returnsNull() = runCardInputWidgetTest {
        updateCardNumberAndIdle(VISA_WITH_SPACES)
        // Date interpreted as 12/2012 until 2080, when it will be 12/2112
        expiryDateEditText.append("12")
        expiryDateEditText.append("12")
        cvcEditText.append(CVC_VALUE_COMMON)

        assertThat(cardParams)
            .isNull()
        assertThat(paymentMethodCard)
            .isNull()
    }

    @Test
    fun getCard_whenIncompleteCvCForVisa_returnsNull() = runCardInputWidgetTest {
        updateCardNumberAndIdle(VISA_WITH_SPACES)
        expiryDateEditText.append("12")
        expiryDateEditText.append("50")
        cvcEditText.append("12")

        assertThat(cardParams)
            .isNull()
        assertThat(paymentMethodCard)
            .isNull()
    }

    @Test
    fun getCard_doesNotValidatePostalCode() = runCardInputWidgetTest {
        postalCodeEnabled = true

        updateCardNumberAndIdle(VISA_WITH_SPACES)
        expiryDateEditText.append("12")
        expiryDateEditText.append("50")
        cvcEditText.append(CVC_VALUE_COMMON)
        postalCodeEditText.setText("")

        assertThat(cardParams)
            .isEqualTo(
                CardParams(
                    brand = CardBrand.Visa,
                    loggingTokens = ATTRIBUTION,
                    number = VISA_NO_SPACES,
                    expMonth = 12,
                    expYear = 2050,
                    cvc = CVC_VALUE_COMMON,
                    address = Address()
                )
            )
        assertThat(paymentMethodCard)
            .isNotNull()
    }

    @Test
    fun getCard_when3DigitCvCForAmEx_withPostalCodeDisabled_returnsCard() = runCardInputWidgetTest {
        postalCodeEnabled = false

        updateCardNumberAndIdle(AMEX_WITH_SPACES)
        expiryDateEditText.append("12")
        expiryDateEditText.append("50")
        cvcEditText.append(CVC_VALUE_COMMON)

        assertThat(cardParams)
            .isEqualTo(
                CardParams(
                    brand = CardBrand.AmericanExpress,
                    loggingTokens = ATTRIBUTION,
                    number = AMEX_NO_SPACES,
                    expMonth = 12,
                    expYear = 2050,
                    cvc = CVC_VALUE_COMMON,
                    address = Address()
                )
            )
        assertThat(paymentMethodCard)
            .isNotNull()
    }

    @Test
    fun getCard_when3DigitCvCForAmEx_withPostalCodeEnabled_returnsCard() = runCardInputWidgetTest {
        postalCodeEnabled = true

        updateCardNumberAndIdle(AMEX_WITH_SPACES)
        expiryDateEditText.append("12")
        expiryDateEditText.append("50")
        cvcEditText.append(CVC_VALUE_COMMON)
        postalCodeEditText.setText(POSTAL_CODE_VALUE)

        assertThat(cardParams)
            .isEqualTo(
                CardParams(
                    brand = CardBrand.AmericanExpress,
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
        assertThat(paymentMethodCard)
            .isNotNull()
    }

    @Test
    fun getCard_whenIncompleteCvCForAmEx_returnsNull() = runCardInputWidgetTest {
        updateCardNumberAndIdle(AMEX_WITH_SPACES)
        expiryDateEditText.append("12")
        expiryDateEditText.append("50")
        cvcEditText.append("12")

        assertThat(cardParams)
            .isNull()
        assertThat(paymentMethodCard)
            .isNull()
    }

    @Test
    fun getPaymentMethodCreateParams_shouldReturnExpectedObject() = runCardInputWidgetTest {
        postalCodeEnabled = true

        setCardNumber(VISA_NO_SPACES)
        setExpiryDate(12, 2030)
        setCvcCode(CVC_VALUE_COMMON)
        setPostalCode(POSTAL_CODE_VALUE)

        assertThat(paymentMethodCreateParams)
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
    fun getCard_whenIncompleteCvCForDiners_returnsNull() = runCardInputWidgetTest {
        updateCardNumberAndIdle(DINERS_CLUB_14_WITH_SPACES)
        expiryDateEditText.append("12")
        expiryDateEditText.append("50")
        cvcEditText.append("12")

        assertThat(cardParams)
            .isNull()
        assertThat(paymentMethodCard)
            .isNull()
    }

    @Test
    fun onCompleteCardNumber_whenValid_shiftsFocusToExpiryDate() = runCardInputWidgetTest {
        updateCardNumberAndIdle(VISA_WITH_SPACES)

        assertThat(cardInputListener.cardCompleteCalls)
            .isEqualTo(1)
        assertThat(cardInputListener.focusedFields)
            .isEqualTo(
                listOf(CardInputListener.FocusField.ExpiryDate)
            )
    }

    @Test
    fun onDeleteFromExpiryDate_whenEmpty_shiftsFocusToCardNumberAndDeletesDigit() = runCardInputWidgetTest {
        setCardInputListener(cardInputListener)

        updateCardNumberAndIdle(VISA_WITH_SPACES)

        assertThat(expiryDateEditText.hasFocus())
            .isTrue()

        ViewTestUtils.sendDeleteKeyEvent(expiryDateEditText)

        assertThat(cardInputListener.focusedFields)
            .isEqualTo(
                listOf(
                    CardInputListener.FocusField.ExpiryDate,
                    CardInputListener.FocusField.CardNumber
                )
            )

        val subString = VISA_WITH_SPACES.take(VISA_WITH_SPACES.length - 1)
        assertThat(cardNumberEditText.fieldText)
            .isEqualTo(subString)
        assertThat(cardNumberEditText.selectionStart)
            .isEqualTo(subString.length)
    }

    @Test
    fun onDeleteFromExpiryDate_whenNotEmpty_doesNotShiftFocusOrDeleteDigit() = runCardInputWidgetTest {
        updateCardNumberAndIdle(AMEX_WITH_SPACES)

        assertThat(expiryDateEditText.hasFocus())
            .isTrue()

        expiryDateEditText.append("1")
        ViewTestUtils.sendDeleteKeyEvent(expiryDateEditText)

        assertThat(expiryDateEditText.hasFocus())
            .isTrue()
        assertThat(cardNumberEditText.fieldText)
            .isEqualTo(AMEX_WITH_SPACES)
    }

    @Test
    fun onDeleteFromCvcDate_whenEmpty_shiftsFocusToExpiryAndDeletesDigit() = runCardInputWidgetTest {
        updateCardNumberAndIdle(VISA_WITH_SPACES)

        assertThat(cardInputListener.cardCompleteCalls)
            .isEqualTo(1)
        assertThat(cardInputListener.focusedFields)
            .isEqualTo(
                listOf(CardInputListener.FocusField.ExpiryDate)
            )

        expiryDateEditText.append("12")
        expiryDateEditText.append("79")
        idleLooper()

        assertThat(cardInputListener.expirationCompleteCalls)
            .isEqualTo(1)
        assertThat(cardInputListener.focusedFields)
            .isEqualTo(
                listOf(
                    CardInputListener.FocusField.ExpiryDate,
                    CardInputListener.FocusField.Cvc
                )
            )
        assertThat(cvcEditText.hasFocus())
            .isTrue()

        ViewTestUtils.sendDeleteKeyEvent(cvcEditText)
        assertThat(cardInputListener.focusedFields)
            .isEqualTo(
                listOf(
                    CardInputListener.FocusField.ExpiryDate,
                    CardInputListener.FocusField.Cvc,
                    CardInputListener.FocusField.ExpiryDate
                )
            )

        val expectedResult = "12/7"
        assertThat(expiryDateEditText.fieldText)
            .isEqualTo(expectedResult)
        assertThat(expiryDateEditText.selectionStart)
            .isEqualTo(expectedResult.length)
    }

    @Test
    fun onDeleteFromCvcDate_withPostalCodeDisabled_whenNotEmpty_doesNotShiftFocusOrDeleteEntry() =
        runCardInputWidgetTest {
            postalCodeEnabled = false

            updateCardNumberAndIdle(AMEX_WITH_SPACES)

            expiryDateEditText.append("12")
            expiryDateEditText.append("79")

            idleLooper()

            assertThat(cvcEditText.hasFocus())
                .isTrue()

            cvcEditText.append(CVC_VALUE_COMMON)
            ViewTestUtils.sendDeleteKeyEvent(cvcEditText)

            assertThat(cvcEditText.hasFocus())
                .isTrue()
            assertThat(expiryDateEditText.fieldText)
                .isEqualTo("12/79")
        }

    @Test
    fun onDeleteFromCvcDate_withPostalCodeEnabled_whenNotEmpty_doesNotShiftFocusOrDeleteEntry() =
        runCardInputWidgetTest {
            postalCodeEnabled = true

            updateCardNumberAndIdle(AMEX_WITH_SPACES)

            expiryDateEditText.append("12")
            expiryDateEditText.append("79")
            idleLooper()
            assertThat(cvcEditText.hasFocus())
                .isTrue()

            cvcEditText.append("12")
            ViewTestUtils.sendDeleteKeyEvent(cvcEditText)

            assertThat(cvcEditText.hasFocus())
                .isTrue()
            assertThat(expiryDateEditText.fieldText)
                .isEqualTo("12/79")
        }

    @Test
    fun onDeleteFromCvcDate_whenEmptyAndExpiryDateIsEmpty_shiftsFocusOnly() = runCardInputWidgetTest {
        updateCardNumberAndIdle(DINERS_CLUB_14_WITH_SPACES)

        // Simulates user tapping into this text field without filling out the date first.
        cvcEditText.requestFocus()

        ViewTestUtils.sendDeleteKeyEvent(cvcEditText)
        assertThat(cardInputListener.focusedFields)
            .isEqualTo(
                listOf(
                    CardInputListener.FocusField.ExpiryDate,
                    CardInputListener.FocusField.Cvc,
                    CardInputListener.FocusField.ExpiryDate
                )
            )
    }

    @Test
    fun onUpdateIcon_forCommonLengthBrand_setsLengthOnCvc() = runCardInputWidgetTest {
        // This should set the brand to Visa. Note that more extensive brand checking occurs
        // in CardNumberEditTextTest.
        updateCardNumberAndIdle(CardNumberFixtures.VISA_BIN)

        assertThat(ViewTestUtils.hasMaxLength(cvcEditText, 3))
            .isTrue()
    }

    @Test
    fun onUpdateText_forAmexBin_setsLengthOnCvc() = runCardInputWidgetTest {
        updateCardNumberAndIdle(CardNumberFixtures.AMEX_BIN)

        assertThat(ViewTestUtils.hasMaxLength(cvcEditText, 4))
            .isTrue()
    }

    @Test
    fun updateToInitialSizes_returnsExpectedValues() = runCardInputWidgetTest {
        // Initial spacing should look like
        // |img==60||---total == 500--------|
        // |(card==230)--(space==220)--(date==50)|
        // |img==60||  cardTouchArea | 420 | dateTouchArea | dateStart==510 |

        assertThat(placement)
            .isEqualTo(
                CardInputWidgetPlacement(
                    totalLengthInPixels = SCREEN_WIDTH,
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
                    dateEndTouchBufferLimit = 0,
                    cardTouchBufferLimit = 400,
                    dateStartPosition = 510
                )
            )
    }

    @Test
    fun updateToPeekSize_withPostalCodeDisabled_withNoData_returnsExpectedValuesForCommonCardLength() =
        runCardInputWidgetTest {
            postalCodeEnabled = false

            // Moving to the end uses Visa-style ("common") defaults
            // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
            // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
            updateSpaceSizes(
                false,
                frameWidth = SCREEN_WIDTH,
                frameStart = BRAND_ICON_WIDTH
            )
            idleLooper()

            assertThat(placement)
                .isEqualTo(
                    CardInputWidgetPlacement(
                        totalLengthInPixels = SCREEN_WIDTH,
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
                        dateEndTouchBufferLimit = 432,
                        cardTouchBufferLimit = 192,
                        dateStartPosition = 285
                    )
                )
        }

    @Test
    fun updateToPeekSize_withPostalCodeEnabled_withNoData_returnsExpectedValuesForCommonCardLength() =
        runCardInputWidgetTest {
            postalCodeEnabled = true

            // Moving to the end uses Visa-style ("common") defaults
            // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
            // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
            updateSpaceSizes(
                false,
                frameWidth = SCREEN_WIDTH,
                frameStart = BRAND_ICON_WIDTH
            )
            idleLooper()

            assertThat(placement)
                .isEqualTo(
                    CardInputWidgetPlacement(
                        totalLengthInPixels = SCREEN_WIDTH,
                        cardWidth = 230,
                        hiddenCardWidth = 150,
                        peekCardWidth = 40,
                        cardDateSeparation = 98,
                        dateWidth = 50,
                        dateCvcSeparation = 82,
                        cvcWidth = 30,
                        cvcPostalCodeSeparation = 100,
                        postalCodeWidth = 100,
                        cvcStartPosition = 330,
                        dateEndTouchBufferLimit = 110,
                        cardTouchBufferLimit = 66,
                        dateStartPosition = 198,
                        cvcEndTouchBufferLimit = 153,
                        postalCodeStartPosition = 460
                    )
                )
        }

    @Test
    fun addValidVisaCard_withPostalCodeDisabled_scrollsOver_andSetsExpectedDisplayValues() = runCardInputWidgetTest {
        postalCodeEnabled = false

        // Moving to the end with an actual Visa number does the same as moving when empty.
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        updateCardNumberAndIdle(VISA_WITH_SPACES)

        assertThat(placement)
            .isEqualTo(
                CardInputWidgetPlacement(
                    totalLengthInPixels = SCREEN_WIDTH,
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
                    dateEndTouchBufferLimit = 432,
                    cardTouchBufferLimit = 192,
                    dateStartPosition = 285
                )
            )
    }

    @Test
    fun addValidVisaCard_withPostalCodeEnabled_scrollsOver_andSetsExpectedDisplayValues() = runCardInputWidgetTest {
        postalCodeEnabled = true

        // Moving to the end with an actual Visa number does the same as moving when empty.
        // |(peek==40)--(space==98)--(date==50)--(space==82)--(cvc==30)--(space==100)--(postal==100)|
        updateCardNumberAndIdle(VISA_WITH_SPACES)

        assertThat(placement)
            .isEqualTo(
                CardInputWidgetPlacement(
                    totalLengthInPixels = SCREEN_WIDTH,
                    cardWidth = 230,
                    hiddenCardWidth = 150,
                    peekCardWidth = 40,
                    cardDateSeparation = 98,
                    dateWidth = 50,
                    dateCvcSeparation = 82,
                    cvcWidth = 30,
                    cvcPostalCodeSeparation = 100,
                    postalCodeWidth = 100,
                    cvcStartPosition = 330,
                    cvcEndTouchBufferLimit = 153,
                    dateEndTouchBufferLimit = 110,
                    cardTouchBufferLimit = 66,
                    dateStartPosition = 198,
                    postalCodeStartPosition = 460
                )
            )
    }

    @Test
    fun addValidAmExCard_withPostalCodeDisabled_scrollsOver_andSetsExpectedDisplayValues() = runCardInputWidgetTest {
        postalCodeEnabled = false

        // Moving to the end with an AmEx number has a larger peek and cvc size.
        // |(peek==50)--(space==175)--(date==50)--(space==185)--(cvc==40)|
        updateCardNumberAndIdle(AMEX_WITH_SPACES)

        assertThat(placement)
            .isEqualTo(
                CardInputWidgetPlacement(
                    totalLengthInPixels = SCREEN_WIDTH,
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
                    dateEndTouchBufferLimit = 427,
                    cardTouchBufferLimit = 197,
                    dateStartPosition = 285
                )
            )
    }

    @Test
    fun addValidAmExCard_withPostalCodeEnabled_scrollsOver_andSetsExpectedDisplayValues() = runCardInputWidgetTest {
        postalCodeEnabled = true

        // Moving to the end with an AmEx number has a larger peek and cvc size.
        // |(peek==50)--(space==88)--(date==50)--(space==72)--(cvc==40)--(space==100)--(postal==100)|
        updateCardNumberAndIdle(AMEX_WITH_SPACES)

        assertThat(placement)
            .isEqualTo(
                CardInputWidgetPlacement(
                    totalLengthInPixels = SCREEN_WIDTH,
                    cardWidth = 230,
                    hiddenCardWidth = 120,
                    peekCardWidth = 50,
                    cardDateSeparation = 88,
                    dateWidth = 50,
                    dateCvcSeparation = 72,
                    cvcWidth = 40,
                    cvcPostalCodeSeparation = 100,
                    postalCodeWidth = 100,
                    cvcStartPosition = 320,
                    cvcEndTouchBufferLimit = 153,
                    dateEndTouchBufferLimit = 106,
                    cardTouchBufferLimit = 66,
                    dateStartPosition = 198,
                    postalCodeStartPosition = 460
                )
            )
    }

    @Test
    fun addDinersClubCard_withPostalCodeDisabled_scrollsOver_andSetsExpectedDisplayValues() = runCardInputWidgetTest {
        postalCodeEnabled = false

        // When we move for a Diner's club card, the peek text is shorter, so we expect:
        // |(peek==20)--(space==205)--(date==50)--(space==195)--(cvc==30)|
        updateCardNumberAndIdle(DINERS_CLUB_14_WITH_SPACES)

        assertThat(placement)
            .isEqualTo(
                CardInputWidgetPlacement(
                    totalLengthInPixels = SCREEN_WIDTH,
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
                    dateEndTouchBufferLimit = 432,
                    cardTouchBufferLimit = 182,
                    dateStartPosition = 285
                )
            )
    }

    @Test
    fun addDinersClubCard_withPostalCodeEnabled_scrollsOver_andSetsExpectedDisplayValues() = runCardInputWidgetTest {
        postalCodeEnabled = true

        // When we move for a Diner's club card, the peek text is shorter, so we expect:
        // |(peek==20)--(space==205)--(date==50)--(space==195)--(cvc==30)--(space==100)--(postal==100)|
        updateCardNumberAndIdle(DINERS_CLUB_14_WITH_SPACES)

        assertThat(placement)
            .isEqualTo(
                CardInputWidgetPlacement(
                    totalLengthInPixels = SCREEN_WIDTH,
                    cardWidth = 230,
                    hiddenCardWidth = 120,
                    peekCardWidth = 20,
                    cardDateSeparation = 118,
                    dateWidth = 50,
                    dateCvcSeparation = 82,
                    cvcWidth = 30,
                    cvcPostalCodeSeparation = 100,
                    postalCodeWidth = 100,
                    cvcStartPosition = 330,
                    dateEndTouchBufferLimit = 110,
                    cardTouchBufferLimit = 66,
                    dateStartPosition = 198,
                    cvcEndTouchBufferLimit = 153,
                    postalCodeStartPosition = 460
                )
            )
    }

    @Test
    fun setCardNumber_withIncompleteNumber_doesNotValidateCard() = runCardInputWidgetTest {
        setCardNumber("123456")
        assertThat(cardNumberEditText.isCardNumberValid)
            .isFalse()
        assertThat(cardNumberEditText.hasFocus())
            .isTrue()
    }

    @Test
    fun setExpirationDate_withValidData_setsCorrectValues() = runCardInputWidgetTest {
        setExpiryDate(12, 79)
        assertThat(expiryDateEditText.fieldText)
            .isEqualTo("12/79")
    }

    @Test
    fun setCvcCode_withValidData_setsValue() = runCardInputWidgetTest {
        setCvcCode(CVC_VALUE_COMMON)
        assertThat(cvcEditText.fieldText)
            .isEqualTo(CVC_VALUE_COMMON)
    }

    @Test
    fun setCvcCode_withLongString_truncatesValue() = runCardInputWidgetTest {
        cvcEditText.updateBrand(CardBrand.Visa)
        setCvcCode(CVC_VALUE_AMEX)

        assertThat(cvcEditText.fieldText)
            .isEqualTo(CVC_VALUE_COMMON)
    }

    @Test
    fun setCvcCode_whenCardBrandIsAmericanExpress_allowsFourDigits() = runCardInputWidgetTest {
        setCardNumber(AMEX_NO_SPACES)
        setCvcCode(CVC_VALUE_AMEX)

        assertThat(cvcEditText.fieldText)
            .isEqualTo(CVC_VALUE_AMEX)
    }

    @Test
    fun setEnabledTrue_withPostalCodeDisabled_isTrue() = runCardInputWidgetTest {
        postalCodeEnabled = false
        isEnabled = true
        assertThat(cardNumberEditText.isEnabled)
            .isTrue()
        assertThat(expiryDateEditText.isEnabled)
            .isTrue()
        assertThat(cvcEditText.isEnabled)
            .isTrue()
        assertThat(postalCodeEditText.isEnabled)
            .isFalse()
    }

    @Test
    fun setEnabledTrue_withPostalCodeEnabled_isTrue() = runCardInputWidgetTest {
        postalCodeEnabled = true
        isEnabled = true
        assertThat(cardNumberEditText.isEnabled)
            .isTrue()
        assertThat(expiryDateEditText.isEnabled)
            .isTrue()
        assertThat(cvcEditText.isEnabled)
            .isTrue()
        assertThat(postalCodeEditText.isEnabled)
            .isTrue()
    }

    @Test
    fun setEnabledFalse_withPostalCodeDisabled_isFalse() = runCardInputWidgetTest {
        postalCodeEnabled = false
        isEnabled = false
        assertThat(cardNumberEditText.isEnabled)
            .isFalse()
        assertThat(expiryDateEditText.isEnabled)
            .isFalse()
        assertThat(cvcEditText.isEnabled)
            .isFalse()
        assertThat(postalCodeEditText.isEnabled)
            .isFalse()
    }

    @Test
    fun setEnabledFalse_withPostalCodeEnabled_isFalse() = runCardInputWidgetTest {
        postalCodeEnabled = true
        isEnabled = false
        assertThat(cardNumberEditText.isEnabled)
            .isFalse()
        assertThat(expiryDateEditText.isEnabled)
            .isFalse()
        assertThat(cvcEditText.isEnabled)
            .isFalse()
        assertThat(postalCodeEditText.isEnabled)
            .isFalse()
    }

    @Test
    fun setAllCardFields_whenValidValues_withPostalCodeDisabled_allowsGetCardWithExpectedValues() =
        runCardInputWidgetTest {
            postalCodeEnabled = false

            setCardNumber(AMEX_WITH_SPACES)
            setExpiryDate(12, 2079)
            setCvcCode(CVC_VALUE_AMEX)

            assertThat(cardParams)
                .isEqualTo(
                    CardParams(
                        brand = CardBrand.AmericanExpress,
                        loggingTokens = ATTRIBUTION,
                        number = AMEX_NO_SPACES,
                        expMonth = 12,
                        expYear = 2079,
                        cvc = CVC_VALUE_AMEX,
                        address = Address.Builder()
                            .build()
                    )
                )

            assertThat(paymentMethodCard)
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
    fun setAllCardFields_whenValidValues_withPostalCodeEnabled_allowsGetCardWithExpectedValues() =
        runCardInputWidgetTest {
            postalCodeEnabled = true

            setCardNumber(AMEX_WITH_SPACES)
            setExpiryDate(12, 2079)
            setCvcCode(CVC_VALUE_AMEX)
            setPostalCode(POSTAL_CODE_VALUE)

            assertThat(cardParams)
                .isEqualTo(
                    CardParams(
                        brand = CardBrand.AmericanExpress,
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

            assertThat(paymentMethodCard)
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
    fun addValues_thenClear_withPostalCodeDisabled_leavesAllTextFieldsEmpty() = runCardInputWidgetTest {
        postalCodeEnabled = false
        updateCardNumberAndIdle(VISA_NO_SPACES)

        setExpiryDate(12, 2079)
        WaitForFocusAndSetCvcCode(CVC_VALUE_AMEX)
        clear()

        assertThat(cardNumberEditText.fieldText)
            .isEmpty()
        assertThat(expiryDateEditText.fieldText)
            .isEmpty()
        assertThat(cvcEditText.fieldText)
            .isEmpty()

        assertThat(cardInputListener.focusedFields)
            .isEqualTo(
                listOf(
                    CardInputListener.FocusField.ExpiryDate,
                    CardInputListener.FocusField.Cvc,
                    CardInputListener.FocusField.CardNumber
                )
            )
    }

    @Test
    fun addValues_thenClear_withPostalCodeEnabled_leavesAllTextFieldsEmpty() = runCardInputWidgetTest {
        postalCodeEnabled = true
        setCardNumber(VISA_NO_SPACES)
        setExpiryDate(12, 2079)
        WaitForFocusAndSetCvcCode(CVC_VALUE_AMEX)
        WaitForFocusAndSetPostalCode(POSTAL_CODE_VALUE)
        clear()

        assertThat(cardNumberEditText.fieldText)
            .isEmpty()
        assertThat(expiryDateEditText.fieldText)
            .isEmpty()
        assertThat(cvcEditText.fieldText)
            .isEmpty()
        assertThat(postalCodeEditText.fieldText)
            .isEmpty()

        assertThat(cardInputListener.focusedFields)
            .isEqualTo(
                listOf(
                    CardInputListener.FocusField.Cvc,
                    CardInputListener.FocusField.ExpiryDate,
                    CardInputListener.FocusField.PostalCode,
                    CardInputListener.FocusField.CardNumber
                )
            )
    }

    @Test
    fun shouldIconShowBrand_whenCvcNotFocused_isAlwaysTrue() = runCardInputWidgetTest {
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
    fun shouldIconShowBrand_whenAmexAndCvCStringLengthNotFour_isFalse() = runCardInputWidgetTest {
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
    fun shouldIconShowBrand_whenAmexAndCvcStringLengthIsFour_isTrue() = runCardInputWidgetTest {
        assertThat(shouldIconShowBrand(CardBrand.AmericanExpress, true, CVC_VALUE_AMEX))
            .isTrue()
    }

    @Test
    fun shouldIconShowBrand_whenNotAmexAndCvcStringLengthIsNotThree_isFalse() = runCardInputWidgetTest {
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
    fun shouldIconShowBrand_whenNotAmexAndCvcStringLengthIsThree_isTrue() = runCardInputWidgetTest {
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
    fun shouldIconShowBrand_whenUnknownBrandAndCvcStringLengthIsFour_isTrue() = runCardInputWidgetTest {
        assertThat(shouldIconShowBrand(CardBrand.Unknown, true, "2124"))
            .isTrue()
    }

    @Test
    fun currentFields_equals_requiredFields_withPostalCodeDisabled() = runCardInputWidgetTest {
        postalCodeEnabled = false
        idleLooper()

        assertThat(requiredFields)
            .isEqualTo(currentFields.toHashSet())
    }

    @Test
    fun currentFields_notEquals_requiredFields_withPostalCodeEnabled() = runCardInputWidgetTest {
        postalCodeEnabled = true
        assertThat(requiredFields)
            .isNotEqualTo(currentFields.toHashSet())
    }

    @Test
    fun testCardValidCallback_withPostalCodeDefaultDisabled() = runCardInputWidgetTest {
        var currentIsValid = false
        var currentInvalidFields = emptySet<CardValidCallback.Fields>()
        setCardValidCallback { isValid, invalidFields ->
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

        setCardNumber(VISA_NO_SPACES)
        assertThat(currentIsValid)
            .isFalse()
        assertThat(currentInvalidFields)
            .containsExactly(CardValidCallback.Fields.Expiry, CardValidCallback.Fields.Cvc)

        expiryDateEditText.append("12")
        assertThat(currentIsValid)
            .isFalse()
        assertThat(currentInvalidFields)
            .containsExactly(CardValidCallback.Fields.Expiry, CardValidCallback.Fields.Cvc)

        expiryDateEditText.append("50")
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
    fun testCardValidCallback_withPostalCodeEnabledNotRequired() = runCardInputWidgetTest {
        var currentIsValid = false
        var currentInvalidFields = emptySet<CardValidCallback.Fields>()
        postalCodeEnabled = true
        postalCodeRequired = false
        setCardValidCallback { isValid, invalidFields ->
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
    }

    @Test
    fun testCardValidCallback_withPostalCodeEnabledAndRequired() = runCardInputWidgetTest {
        var currentIsValid = false
        var currentInvalidFields = emptySet<CardValidCallback.Fields>()
        postalCodeEnabled = true
        usZipCodeRequired = true
        setCardValidCallback { isValid, invalidFields ->
            currentIsValid = isValid
            currentInvalidFields = invalidFields
        }
        assertThat(currentIsValid)
            .isFalse()
        assertThat(currentInvalidFields)
            .containsExactly(
                CardValidCallback.Fields.Number,
                CardValidCallback.Fields.Expiry,
                CardValidCallback.Fields.Cvc,
                CardValidCallback.Fields.Postal
            )

        setCardNumber(VISA_NO_SPACES)
        expiryDateEditText.setText("1250")
        cvcEditText.setText("123")
        assertThat(currentIsValid).isFalse()
        assertThat(currentInvalidFields).containsExactly(CardValidCallback.Fields.Postal)

        postalCodeEditText.setText("12345")
        assertThat(currentIsValid).isTrue()
        assertThat(currentInvalidFields).isEmpty()

        postalCodeEditText.setText("123")
        assertThat(currentIsValid).isFalse()
        assertThat(currentInvalidFields).containsExactly(CardValidCallback.Fields.Postal)
    }

    @Test
    fun shouldShowErrorIcon_shouldBeUpdatedCorrectly() = runCardInputWidgetTest {
        setExpiryDate(12, 2030)
        setCvcCode(CVC_VALUE_COMMON)

        // show error icon when validating fields with invalid card number
        setCardNumber(VISA_NO_SPACES.take(6))
        assertThat(paymentMethodCreateParams)
            .isNull()
        assertThat(shouldShowErrorIcon)
            .isTrue()

        // don't show error icon after changing input
        setCardNumber(VISA_NO_SPACES.take(7))
        assertThat(shouldShowErrorIcon)
            .isFalse()

        // don't show error icon when validating fields with invalid card number
        assertThat(paymentMethodCreateParams)
            .isNull()
        setCardNumber(VISA_NO_SPACES)
        assertThat(paymentMethodCreateParams)
            .isNotNull()
        assertThat(shouldShowErrorIcon)
            .isFalse()
    }

    @Test
    fun `createHiddenCardText with 19 digit PAN`() = runCardInputWidgetTest {
        assertThat(
            createHiddenCardText(19)
        ).isEqualTo("0000 0000 0000 0000 ")
    }

    @Test
    fun `createHiddenCardText with 16 digit PAN`() = runCardInputWidgetTest {
        assertThat(
            createHiddenCardText(16)
        ).isEqualTo("0000 0000 0000 ")
    }

    @Test
    fun `createHiddenCardText with 15 digit PAN`() = runCardInputWidgetTest {
        assertThat(
            createHiddenCardText(15)
        ).isEqualTo("0000 000000 ")
    }

    @Test
    fun `createHiddenCardText with 14 digit PAN`() = runCardInputWidgetTest {
        assertThat(
            createHiddenCardText(14)
        ).isEqualTo("0000 000000 ")
    }

    @Test
    fun usZipCodeRequired_whenFalse_shouldSetPostalCodeHint() = runCardInputWidgetTest {
        usZipCodeRequired = false
        assertThat(postalCodeEditText.hint)
            .isEqualTo("Postal code")
    }

    @Test
    fun usZipCodeRequired_whenTrue_withInvalidZipCode_shouldReturnNullCard() = runCardInputWidgetTest {
        usZipCodeRequired = true
        assertThat(postalCodeEditText.hint)
            .isEqualTo("ZIP Code")

        setCardNumber(VISA_WITH_SPACES)
        expiryDateEditText.append("12")
        expiryDateEditText.append("50")
        cvcEditText.append(CVC_VALUE_COMMON)

        // invalid zipcode
        postalCodeEditText.setText(CVC_VALUE_AMEX)
        assertThat(cardParams)
            .isNull()
    }

    @Test
    fun usZipCodeRequired_whenTrue_withValidZipCode_shouldReturnNotNullCard() = runCardInputWidgetTest {
        usZipCodeRequired = true
        assertThat(postalCodeEditText.hint)
            .isEqualTo("ZIP Code")

        setCardNumber(VISA_WITH_SPACES)
        expiryDateEditText.append("12")
        expiryDateEditText.append("50")
        cvcEditText.append(CVC_VALUE_COMMON)

        // valid zipcode
        postalCodeEditText.setText(POSTAL_CODE_VALUE)

        assertThat(cardParams)
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
    fun usZipCodeRequired_whenFalse_shouldCallOnPostalCodeComplete() = runCardInputWidgetTest {
        usZipCodeRequired = false
        postalCodeEditText.setText(POSTAL_CODE_VALUE)
        assertThat(cardInputListener.onPostalCodeCompleteCalls).isEqualTo(1)
    }

    @Test
    fun usZipCodeRequired_whenTrue_withValidZip_shouldCallOnPostalCodeComplete() = runCardInputWidgetTest {
        usZipCodeRequired = true
        postalCodeEditText.setText(POSTAL_CODE_VALUE)
        assertThat(cardInputListener.onPostalCodeCompleteCalls).isEqualTo(1)
    }

    @Test
    fun postalCodeEnabled_whenFalse_shouldNotCallOnPostalCodeComplete() = runCardInputWidgetTest {
        postalCodeEnabled = false
        postalCodeEditText.setText("123")
        assertThat(cardInputListener.onPostalCodeCompleteCalls).isEqualTo(0)
    }

    @Test
    fun usZipCodeRequired_whenTrue_withInvalidZip_shouldNotCallOnPostalCodeComplete() = runCardInputWidgetTest {
        usZipCodeRequired = true
        postalCodeEditText.setText("1234")
        assertThat(cardInputListener.onPostalCodeCompleteCalls).isEqualTo(0)
    }

    @Test
    fun postalCode_whenTrue_withNonEmptyZip_shouldCallOnPostalCodeComplete() = runCardInputWidgetTest {
        postalCodeRequired = true
        postalCodeEditText.setText("1234")
        assertThat(cardInputListener.onPostalCodeCompleteCalls).isEqualTo(1)
    }

    @Test
    fun postalCode_whenTrue_withEmptyZip_shouldNotCallOnPostalCodeComplete() = runCardInputWidgetTest {
        postalCodeRequired = true
        postalCodeEditText.setText("")
        assertThat(cardInputListener.onPostalCodeCompleteCalls).isEqualTo(0)
    }

    @Test
    fun `setCvcLabel is not reset when card number entered`() = runCardInputWidgetTest {
        setCvcLabel("123")
        assertThat(cvcEditText.hint)
            .isEqualTo("123")
    }

    @Test
    fun `Verify on postal code focus change listeners trigger the callback`() = runCardInputWidgetTest {
        postalCodeEditText.getParentOnFocusChangeListener()
            .onFocusChange(postalCodeEditText, true)

        assertThat(cardInputListener.focusedFields)
            .contains(CardInputListener.FocusField.PostalCode)
    }

    @Test
    fun `Verify on cvc focus change listeners trigger the callback`() = runCardInputWidgetTest {
        cvcEditText.getParentOnFocusChangeListener()
            .onFocusChange(cvcEditText, true)

        assertThat(cardInputListener.focusedFields)
            .contains(CardInputListener.FocusField.Cvc)
    }

    @Test
    fun `Verify on expiration date focus change listeners trigger the callback`() = runCardInputWidgetTest {
        expiryDateEditText.getParentOnFocusChangeListener()
            .onFocusChange(expiryDateEditText, true)

        assertThat(cardInputListener.focusedFields)
            .contains(CardInputListener.FocusField.ExpiryDate)
    }

    @Test
    fun `Verify on card number focus change listeners trigger the callback`() = runCardInputWidgetTest {
        cardNumberEditText.getParentOnFocusChangeListener()
            .onFocusChange(cardNumberEditText, true)

        assertThat(cardInputListener.focusedFields)
            .contains(CardInputListener.FocusField.CardNumber)
    }

    @Test
    fun `Requiring postal code after setting CardValidCallback should still notify of change`() =
        runCardInputWidgetTest {
            val callback = mock<CardValidCallback>()
            setCardValidCallback(callback)

            postalCodeRequired = true
            postalCodeEditText.setText("54321")

            verify(callback, times(2)).onInputChanged(any(), any())
        }

    @Test
    fun `Removing postal code requirement keeps CardValidCallback notifications for the field`() =
        runCardInputWidgetTest {
            val callback = mock<CardValidCallback>()
            postalCodeRequired = true
            setCardValidCallback(callback)
            postalCodeRequired = false

            postalCodeEditText.setText("54321")

            verify(callback, times(2)).onInputChanged(any(), any())
        }

    @Test
    fun `Setting postal code requirements multiple times only sets callback once`() = runCardInputWidgetTest {
        val callback = mock<CardValidCallback>()
        setCardValidCallback(callback)

        postalCodeRequired = true
        postalCodeEnabled = true
        postalCodeEnabled = false
        postalCodeRequired = false
        postalCodeRequired = true
        postalCodeEnabled = true
        postalCodeEditText.setText("54321")

        // Called only when the callback is set and when the text is set.
        verify(callback, times(2)).onInputChanged(any(), any())
    }

    @Test
    fun `getBrand returns the right brands`() = runCardInputWidgetTest {
        setCardNumber(null)
        assertThat(brand).isEqualTo(CardBrand.Unknown)

        setCardNumber(VISA_NO_SPACES)
        assertThat(brand).isEqualTo(CardBrand.Visa)

        setCardNumber(CardNumberFixtures.MASTERCARD_NO_SPACES)
        assertThat(brand).isEqualTo(CardBrand.MasterCard)

        setCardNumber(AMEX_NO_SPACES)
        assertThat(brand).isEqualTo(CardBrand.AmericanExpress)

        setCardNumber(CardNumberFixtures.DISCOVER_NO_SPACES)
        assertThat(brand).isEqualTo(CardBrand.Discover)

        setCardNumber(CardNumberFixtures.JCB_NO_SPACES)
        assertThat(brand).isEqualTo(CardBrand.JCB)

        setCardNumber(DINERS_CLUB_14_NO_SPACES)
        assertThat(brand).isEqualTo(CardBrand.DinersClub)
    }

    @Test
    fun `Enabled but not required postal code should fire card valid callback when changed`() = runCardInputWidgetTest {
        val callback = mock<CardValidCallback>()
        setCardValidCallback(callback)

        postalCodeEnabled = true
        postalCodeRequired = false
        postalCodeEditText.setText("54321")

        // Called only when the callback is set and when the text is set.
        verify(callback, times(2)).onInputChanged(any(), any())
    }

    @Test
    fun `Adds no Networks field to card PM params if not CBC eligible`() {
        runCardInputWidgetTest(isCbcEligible = false) {
            postalCodeEnabled = false

            updateCardNumberAndIdle("4000 0025 0000 1001")
            expiryDateEditText.append("12")
            expiryDateEditText.append("50")
            cvcEditText.append("123")

            val networks = paymentMethodCreateParams?.card?.networks
            assertThat(networks).isNull()
        }
    }

    @Test
    fun `Adds correct Networks field to card PM params if customer does not select a network`() {
        runCardInputWidgetTest(isCbcEligible = true) {
            postalCodeEnabled = false

            updateCardNumberAndIdle("4000 0025 0000 1001")
            expiryDateEditText.append("12")
            expiryDateEditText.append("50")
            cvcEditText.append("123")

            val createCardParams = paymentMethodCreateParams?.card
            assertThat(createCardParams?.networks).isEqualTo(null)
        }
    }

    @Test
    fun `Adds correct Networks field to card PM params if customer selects a network`() {
        runCardInputWidgetTest(isCbcEligible = true) {
            postalCodeEnabled = false

            updateCardNumberAndIdle("4000 0025 0000 1001")
            expiryDateEditText.append("12")
            expiryDateEditText.append("50")
            cvcEditText.append("123")
            cardNumberEditText.requestFocus()

            onView(withId(R.id.card_brand_view)).perform(click())
            onData(anything()).inRoot(isPlatformPopup()).atPosition(1).perform(click())

            val expectedNetworks = PaymentMethodCreateParams.Card.Networks(
                preferred = "cartes_bancaires",
            )

            val createCardParams = paymentMethodCreateParams?.card
            assertThat(createCardParams?.networks).isEqualTo(expectedNetworks)
        }
    }

    @Test
    fun `Restores brand state correctly on activity recreation`() {
        runCardInputWidgetTest(
            isCbcEligible = true,
            block = {
                cardNumberEditText.setText("4000 0025 0000 1001")

                onView(withId(R.id.card_brand_view)).perform(click())
                onData(anything()).inRoot(isPlatformPopup()).atPosition(1).perform(click())

                expiryDateEditText.append("12")
                expiryDateEditText.append("50")
                cvcEditText.append("123")
            },
            afterRecreation = {
                assertThat(cardBrandView.brand).isEqualTo(CardBrand.CartesBancaires)
            },
        )
    }

    @Test
    fun `Restores onBehalfOf correctly on activity recreation`() {
        runCardInputWidgetTest(
            isCbcEligible = true,
            block = {
                onBehalfOf = "test"
            },
            afterRecreation = {
                assertThat(onBehalfOf).isEqualTo("test")
            },
        )
    }

    @Test
    fun `Re-fetches card brands when first eight are deleted and re-entered`() = runCardInputWidgetTest(
        true
    ) {
        cardNumberEditText.setText("4000 0026 0000 1001")
        assertThat(cardBrandView.possibleBrands.size).isEqualTo(0)
        updateCardNumberAndIdle("0000 1001")
        assertThat(cardBrandView.possibleBrands.size).isEqualTo(0)
        cardNumberEditText.setText("4000 0025 0000 1001")
        assertThat(cardBrandView.possibleBrands.size).isEqualTo(2)
    }

    private fun runCardInputWidgetTest(
        isCbcEligible: Boolean = false,
        afterRecreation: (CardInputWidget.() -> Unit)? = null,
        block: CardInputWidget.() -> Unit,
    ) {
        val activityScenario = ActivityScenario.launch<CardInputWidgetTestActivity>(
            Intent(context, CardInputWidgetTestActivity::class.java).apply {
                putExtra("args", CardInputWidgetTestActivity.Args(isCbcEligible = isCbcEligible))
            }
        )

        activityScenario.onActivity { activity ->
            activity.setWorkContext(testDispatcher)

            val widget = activity.findViewById<CardInputWidget>(CardInputWidgetTestActivity.VIEW_ID)
            widget.setCardInputListener(cardInputListener)
            widget.block()
        }

        if (afterRecreation != null) {
            activityScenario.recreate()

            activityScenario.onActivity { activity ->
                activity.setWorkContext(testDispatcher)

                val widget = activity.findViewById<CardInputWidget>(CardInputWidgetTestActivity.VIEW_ID)
                widget.setCardInputListener(cardInputListener)
                widget.afterRecreation()
            }
        }

        activityScenario.close()
    }

    private fun CardInputWidget.updateCardNumberAndIdle(cardNumber: String) {
        cardNumberEditText.setText(cardNumber)
        idleLooper()
    }

    private fun CardInputWidget.WaitForFocusAndSetCvcCode(cvcCode: String?) {
        idleLooper()
        setCvcCode(cvcCode)
    }

    private fun CardInputWidget.WaitForFocusAndSetPostalCode(postalCode: String?) {
        idleLooper()
        setPostalCode(postalCode)
    }

    private class FakeCardInputListener : CardInputListener {
        val focusedFields = mutableListOf<CardInputListener.FocusField>()
        var cardCompleteCalls = 0
        var expirationCompleteCalls = 0
        var cvcCompleteCalls = 0
        var onPostalCodeCompleteCalls = 0

        override fun onFocusChange(focusField: CardInputListener.FocusField) {
            focusedFields.add(focusField)
        }

        override fun onCardComplete() {
            cardCompleteCalls++
        }

        override fun onExpirationComplete() {
            expirationCompleteCalls++
        }

        override fun onCvcComplete() {
            cvcCompleteCalls++
        }

        override fun onPostalCodeComplete() {
            onPostalCodeCompleteCalls++
        }
    }

    private companion object {
        // Every Card made by the CardInputView should have the card widget token.
        private val ATTRIBUTION = setOf(LOGGING_TOKEN)

        private const val CVC_VALUE_COMMON = "123"
        private const val CVC_VALUE_AMEX = "1234"
        private const val POSTAL_CODE_VALUE = "94103"

        private const val SCREEN_WIDTH = 500
        private const val BRAND_ICON_WIDTH = 60
    }
}

internal class CardInputWidgetTestActivity : AppCompatActivity() {

    @Parcelize
    data class Args(
        val isCbcEligible: Boolean,
    ) : Parcelable

    private val args: Args by lazy {
        @Suppress("DEPRECATION")
        intent.getParcelableExtra("args")!!
    }

    private val cardInputWidget: CardInputWidget by lazy {
        CardInputWidget(this).apply {
            id = VIEW_ID

            layoutWidthCalculator = CardInputWidget.LayoutWidthCalculator { text, _ -> text.length * 10 }
            frameWidthSupplier = { 500 }

            val storeOwner = CardElementTestHelper.createViewModelStoreOwner(isCbcEligible = args.isCbcEligible)
            viewModelStoreOwner = storeOwner
            cardNumberEditText.viewModelStoreOwner = storeOwner

            // Set the width of the icon and its margin so that test calculations have
            // an expected value that is repeatable on all systems.
            cardBrandView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                width = 48
                marginEnd = 12
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.StripeDefaultTheme)

        val layout = FrameLayout(this).apply { addView(cardInputWidget) }
        setContentView(layout)
    }

    fun setWorkContext(workContext: CoroutineContext) {
        cardInputWidget.cardNumberEditText.workContext = workContext
    }

    companion object {
        const val VIEW_ID = 12345
    }
}
