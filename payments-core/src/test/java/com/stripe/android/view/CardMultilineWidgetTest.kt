package com.stripe.android.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import com.google.android.material.textfield.TextInputLayout
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.CardNumberFixtures.AMEX_NO_SPACES
import com.stripe.android.CardNumberFixtures.AMEX_WITH_SPACES
import com.stripe.android.CardNumberFixtures.CO_BRAND_CARTES_MASTERCARD_NO_SPACES
import com.stripe.android.CardNumberFixtures.CO_BRAND_CARTES_MASTERCARD_WITH_SPACES
import com.stripe.android.CardNumberFixtures.DINERS_CLUB_14_NO_SPACES
import com.stripe.android.CardNumberFixtures.DINERS_CLUB_14_WITH_SPACES
import com.stripe.android.CardNumberFixtures.DISCOVER_NO_SPACES
import com.stripe.android.CardNumberFixtures.JCB_NO_SPACES
import com.stripe.android.CardNumberFixtures.MASTERCARD_NO_SPACES
import com.stripe.android.CardNumberFixtures.VISA_NO_SPACES
import com.stripe.android.CardNumberFixtures.VISA_WITH_SPACES
import com.stripe.android.PaymentConfiguration
import com.stripe.android.R
import com.stripe.android.cards.AccountRangeFixtures
import com.stripe.android.cards.DefaultCardAccountRangeStore
import com.stripe.android.model.Address
import com.stripe.android.model.BinFixtures
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.testharness.ViewTestUtils
import com.stripe.android.utils.CardElementTestHelper
import com.stripe.android.utils.TestUtils.idleLooper
import com.stripe.android.utils.createTestActivityRule
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.parcelize.Parcelize
import org.hamcrest.CoreMatchers.anything
import org.hamcrest.Matchers
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import java.util.Calendar
import kotlin.coroutines.CoroutineContext
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Test class for [CardMultilineWidget].
 */
@RunWith(RobolectricTestRunner::class)
internal class CardMultilineWidgetTest {
    private val testDispatcher = StandardTestDispatcher()

    private val fullCardListener: CardInputListener = mock()
    private val noZipCardListener: CardInputListener = mock()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val accountRangeStore = DefaultCardAccountRangeStore(context)

    @get:Rule
    val testActivityRule = createTestActivityRule<CardMultilineWidgetTestActivity>()

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
    fun testExistence() = runCardMultilineWidgetTest {
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
    fun onCreate_setsCorrectHintForExpiry() = runCardMultilineWidgetTest {
        val shortExpiryContainer = cardMultilineWidget
            .findViewById<TextInputLayout>(R.id.tl_expiry)

        val longExpiryContainer = noZipCardMultilineWidget
            .findViewById<TextInputLayout>(R.id.tl_expiry)

        val shortExpiryHint = cardMultilineWidget
            .resources.getString(R.string.stripe_expiry_label_short)
        val longExpiryHint = cardMultilineWidget
            .resources.getString(R.string.stripe_acc_label_expiry_date)

        assertThat(shortExpiryContainer.hint)
            .isEqualTo(shortExpiryHint)
        assertThat(longExpiryContainer.hint)
            .isEqualTo(longExpiryHint)
    }

    @Test
    fun getPaymentMethodParams_whenInputIsValidAmexAndNoZipRequiredAnd4DigitCvc_returnsFullCardAndExpectedLogging() =
        runCardMultilineWidgetTest {
            noZipGroup.cardNumberEditText.setText(AMEX_WITH_SPACES)
            noZipGroup.expiryDateEditText.append("12")
            noZipGroup.expiryDateEditText.append("50")
            noZipGroup.cvcEditText.append("1234")

            assertThat(noZipCardMultilineWidget.paymentMethodCreateParams)
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
    fun getPaymentMethodParams_whenInputIsValidAmexAndNoZipRequiredAnd3DigitCvc_returnsFullCardAndExpectedLogging() =
        runCardMultilineWidgetTest {
            noZipGroup.cardNumberEditText.setText(AMEX_WITH_SPACES)
            noZipGroup.expiryDateEditText.append("12")
            noZipGroup.expiryDateEditText.append("50")
            noZipGroup.cvcEditText.append(CVC_VALUE_COMMON)

            assertThat(noZipCardMultilineWidget.paymentMethodCreateParams)
                .isEqualTo(
                    PaymentMethodCreateParams.create(
                        PaymentMethodCreateParams.Card(
                            number = AMEX_NO_SPACES,
                            cvc = CVC_VALUE_COMMON,
                            expiryMonth = 12,
                            expiryYear = 2050,
                            attribution = ATTRIBUTION
                        )
                    )
                )
        }

    @Test
    fun getPaymentMethodCreateParams_shouldReturnExpectedObject() = runCardMultilineWidgetTest {
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
    fun paymentMethodCard_whenInputIsValidVisaWithZip_returnsCardAndBillingDetails() = runCardMultilineWidgetTest {
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
    fun paymentMethodCard_whenInputHasPrefNewtworks_returnsCardObjectWithNetworks() = runCardMultilineWidgetTest {
        cardMultilineWidget.cardNumberEditText.setText(CO_BRAND_CARTES_MASTERCARD_WITH_SPACES)
        cardMultilineWidget.expiryDateEditText.append("12")
        cardMultilineWidget.expiryDateEditText.append("50")
        cardMultilineWidget.cvcEditText.append(CVC_VALUE_COMMON)
        cardMultilineWidget.postalCodeEditText.append(POSTAL_CODE_VALUE)
        cardMultilineWidget.setPreferredNetworks(listOf(CardBrand.CartesBancaires))

        assertThat(cardMultilineWidget.paymentMethodCard)
            .isEqualTo(
                PaymentMethodCreateParams.Card(
                    number = CO_BRAND_CARTES_MASTERCARD_NO_SPACES,
                    cvc = CVC_VALUE_COMMON,
                    expiryMonth = 12,
                    expiryYear = 2050,
                    attribution = ATTRIBUTION,
                    networks = PaymentMethodCreateParams.Card.Networks(
                        preferred = CardBrand.CartesBancaires.code
                    )
                )
            )
    }

    @Test
    fun paymentMethodCreateParams_whenPostalCodeIsRequiredAndValueIsBlank_returnsNull() = runCardMultilineWidgetTest {
        cardMultilineWidget.setShouldShowPostalCode(true)
        cardMultilineWidget.setCardInputListener(fullCardListener)
        cardMultilineWidget.postalCodeRequired = true

        fullGroup.cardNumberEditText.setText(VISA_WITH_SPACES)
        fullGroup.expiryDateEditText.append("12")
        fullGroup.expiryDateEditText.append("50")
        fullGroup.cvcEditText.append(CVC_VALUE_COMMON)
        fullGroup.postalCodeEditText.setText("")

        assertThat(cardMultilineWidget.paymentMethodCreateParams)
            .isNull()
        verify(fullCardListener, never()).onPostalCodeComplete()
    }

    @Test
    fun paymentMethodCreateParams_whenPostalCodeIsRequiredAndValueIsNotBlank_returnsNotNull() =
        runCardMultilineWidgetTest {
            cardMultilineWidget.setShouldShowPostalCode(true)
            cardMultilineWidget.setCardInputListener(fullCardListener)
            cardMultilineWidget.postalCodeRequired = true

            fullGroup.cardNumberEditText.setText(VISA_WITH_SPACES)
            fullGroup.expiryDateEditText.append("12")
            fullGroup.expiryDateEditText.append("50")
            fullGroup.cvcEditText.append(CVC_VALUE_COMMON)
            fullGroup.postalCodeEditText.setText("1234")

            assertThat(cardMultilineWidget.paymentMethodCreateParams)
                .isNotNull()
            verify(fullCardListener).onPostalCodeComplete()
        }

    @Test
    fun paymentMethodCreateParams_whenPostalCodeIsNotRequiredAndValueIsBlank_returnsNotNull() =
        runCardMultilineWidgetTest {
            cardMultilineWidget.setShouldShowPostalCode(true)
            cardMultilineWidget.postalCodeRequired = false
            cardMultilineWidget.setCardInputListener(fullCardListener)

            fullGroup.cardNumberEditText.setText(VISA_WITH_SPACES)
            fullGroup.expiryDateEditText.append("12")
            fullGroup.expiryDateEditText.append("50")
            fullGroup.cvcEditText.append(CVC_VALUE_COMMON)

            assertThat(cardMultilineWidget.paymentMethodCreateParams)
                .isNotNull()
            verify(fullCardListener, never()).onPostalCodeComplete()
        }

    @Test
    fun paymentMethodCard_whenInputIsValidVisaAndNoZipRequired_returnsFullCard() = runCardMultilineWidgetTest {
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
    fun paymentMethodCard_whenInputIsValidAmexAndNoZipRequiredAnd4DigitCvc_returnsFullCard() =
        runCardMultilineWidgetTest {
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
    fun paymentMethodCard_whenInputIsValidAmexAndNoZipRequiredAnd3DigitCvc_returnsFullCard() =
        runCardMultilineWidgetTest {
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
    fun initView_whenZipRequired_secondRowContainsThreeVisibleElements() = runCardMultilineWidgetTest {
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
    fun clear_whenZipRequiredAndAllFieldsEntered_clearsAllfields() = runCardMultilineWidgetTest {
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
    fun clear_whenFieldsInErrorState_clearsFieldsAndHidesErrors() = runCardMultilineWidgetTest {
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
    fun setCvcLabel_shouldShowCustomLabelIfPresent() = runCardMultilineWidgetTest {
        cardMultilineWidget.setCvcLabel("my cool cvc")
        assertThat(fullGroup.cvcInputLayout.hint)
            .isEqualTo("my cool cvc")

        cardMultilineWidget.setCvcLabel(null)
        assertThat(fullGroup.cvcInputLayout.hint)
            .isEqualTo("CVC")
    }

    @Test
    fun setCvcPlaceholderText_shouldShowCustomPlaceholderTextIfPresent() = runCardMultilineWidgetTest {
        cardMultilineWidget.setCvcPlaceholderText("my cool placeholder")
        assertThat(fullGroup.cvcInputLayout.placeholderText)
            .isEqualTo("my cool placeholder")

        cardMultilineWidget.setCvcPlaceholderText(null)
        assertThat(fullGroup.cvcInputLayout.placeholderText)
            .isEqualTo("123")
    }

    @Test
    fun initView_whenZipRequiredThenSetToHidden_secondRowLosesPostalCodeAndAdjustsMargin() =
        runCardMultilineWidgetTest {
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
    fun initView_whenNoZipRequired_secondRowContainsTwoVisibleElements() = runCardMultilineWidgetTest {
        assertThat(noZipGroup.expiryDateEditText.visibility)
            .isEqualTo(View.VISIBLE)
        assertThat(noZipGroup.cvcEditText.visibility)
            .isEqualTo(View.VISIBLE)
        assertThat(noZipGroup.postalCodeInputLayout.visibility)
            .isEqualTo(View.GONE)
    }

    @Test
    fun initView_whenZipHiddenThenSetToRequired_secondRowAddsPostalCodeAndAdjustsMargin() = runCardMultilineWidgetTest {
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
    fun onCompleteCardNumber_whenValid_shiftsFocusToExpiryDate() = runCardMultilineWidgetTest {
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

        verify(fullCardListener, never()).onPostalCodeComplete()
        verify(noZipCardListener, never()).onPostalCodeComplete()
    }

    @Test
    fun onCompleteExpiry_whenValid_shiftsFocusToCvc() = runCardMultilineWidgetTest {
        cardMultilineWidget.setCardInputListener(fullCardListener)
        noZipCardMultilineWidget.setCardInputListener(noZipCardListener)

        fullGroup.expiryDateEditText.append("12")
        fullGroup.expiryDateEditText.append("50")
        idleLooper()
        verify(fullCardListener).onExpirationComplete()
        verify(fullCardListener).onFocusChange(CardInputListener.FocusField.Cvc)
        assertThat(fullGroup.cvcEditText.hasFocus())
            .isTrue()

        noZipGroup.expiryDateEditText.append("12")
        noZipGroup.expiryDateEditText.append("50")
        idleLooper()
        verify(noZipCardListener).onExpirationComplete()
        verify(noZipCardListener).onFocusChange(CardInputListener.FocusField.Cvc)
        assertThat(noZipGroup.cvcEditText.hasFocus())
            .isTrue()

        verify(fullCardListener, never()).onPostalCodeComplete()
        verify(noZipCardListener, never()).onPostalCodeComplete()
    }

    @Test
    fun onCompleteCvc_whenValid_shiftsFocusOnlyIfPostalCodeShown() = runCardMultilineWidgetTest {
        cardMultilineWidget.setCardInputListener(fullCardListener)
        noZipCardMultilineWidget.setCardInputListener(noZipCardListener)

        fullGroup.cardNumberEditText.setText(VISA_WITH_SPACES)
        fullGroup.expiryDateEditText.append("12")
        fullGroup.expiryDateEditText.append("50")
        fullGroup.cvcEditText.append(CVC_VALUE_COMMON)
        idleLooper()
        verify(fullCardListener).onCvcComplete()
        verify(fullCardListener).onFocusChange(CardInputListener.FocusField.PostalCode)
        assertThat(fullGroup.postalCodeEditText.hasFocus())
            .isTrue()

        noZipGroup.cardNumberEditText.setText(VISA_WITH_SPACES)
        noZipGroup.expiryDateEditText.append("12")
        noZipGroup.expiryDateEditText.append("50")
        noZipGroup.cvcEditText.append(CVC_VALUE_COMMON)
        idleLooper()
        verify(noZipCardListener).onCvcComplete()
        verify(noZipCardListener, never()).onFocusChange(CardInputListener.FocusField.PostalCode)
        assertThat(noZipGroup.cvcEditText.hasFocus())
            .isTrue()

        verify(fullCardListener, never()).onPostalCodeComplete()
        verify(noZipCardListener, never()).onPostalCodeComplete()
    }

    @Test
    fun deleteWhenEmpty_fromExpiry_withPostalCode_shiftsToCardNumber() = runCardMultilineWidgetTest {
        cardMultilineWidget.setCardInputListener(fullCardListener)
        fullGroup.cardNumberEditText.setText(VISA_WITH_SPACES)
        idleLooper()

        assertThat(fullGroup.expiryDateEditText.hasFocus())
            .isTrue()
        ViewTestUtils.sendDeleteKeyEvent(fullGroup.expiryDateEditText)

        verify(fullCardListener).onFocusChange(CardInputListener.FocusField.CardNumber)
        assertThat(fullGroup.cardNumberEditText.hasFocus())
            .isTrue()
        assertThat(fullGroup.cardNumberEditText.text?.toString())
            .isEqualTo(VISA_WITH_SPACES.take(VISA_WITH_SPACES.length - 1))

        verify(fullCardListener, never()).onPostalCodeComplete()

        verify(fullCardListener, never()).onPostalCodeComplete()
    }

    @Test
    fun deleteWhenEmpty_fromExpiry_withoutPostalCode_shiftsToCardNumber() = runCardMultilineWidgetTest {
        noZipCardMultilineWidget.setCardInputListener(noZipCardListener)
        noZipGroup.cardNumberEditText.setText(VISA_WITH_SPACES)
        idleLooper()

        assertThat(noZipGroup.expiryDateEditText.hasFocus())
            .isTrue()
        ViewTestUtils.sendDeleteKeyEvent(noZipGroup.expiryDateEditText)

        verify(noZipCardListener).onFocusChange(CardInputListener.FocusField.CardNumber)
        assertThat(noZipGroup.cardNumberEditText.hasFocus())
            .isTrue()
        assertThat(noZipGroup.cardNumberEditText.text?.toString())
            .isEqualTo(VISA_WITH_SPACES.take(VISA_WITH_SPACES.length - 1))

        verify(noZipCardListener, never()).onPostalCodeComplete()
    }

    @Test
    fun deleteWhenEmpty_fromCvc_shiftsToExpiry() = runCardMultilineWidgetTest {
        cardMultilineWidget.setCardInputListener(fullCardListener)
        noZipCardMultilineWidget.setCardInputListener(noZipCardListener)

        fullGroup.expiryDateEditText.append("12")
        fullGroup.expiryDateEditText.append("50")
        idleLooper()

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
        idleLooper()

        assertThat(noZipGroup.cvcEditText.hasFocus())
            .isTrue()
        ViewTestUtils.sendDeleteKeyEvent(noZipGroup.cvcEditText)

        verify(noZipCardListener).onFocusChange(CardInputListener.FocusField.ExpiryDate)
        assertThat(noZipGroup.expiryDateEditText.hasFocus())
            .isTrue()
        assertThat(noZipGroup.expiryDateEditText.fieldText)
            .isEqualTo("12/5")

        verify(fullCardListener, never()).onPostalCodeComplete()
        verify(noZipCardListener, never()).onPostalCodeComplete()
    }

    @Test
    fun deleteWhenEmpty_fromPostalCode_shiftsToCvc() = runCardMultilineWidgetTest {
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
    fun validateCardNumber_whenValid_doesNotShowError() = runCardMultilineWidgetTest {
        cardMultilineWidget.setCardNumber(VISA_WITH_SPACES)

        val isValid = cardMultilineWidget.validateCardNumber()
        val shouldShowError = fullGroup.cardNumberEditText.shouldShowError

        assertThat(isValid)
            .isTrue()
        assertThat(shouldShowError)
            .isFalse()
    }

    @Test
    fun validateCardNumber_whenInvalid_setsShowError() = runCardMultilineWidgetTest {
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
    fun onFinishInflate_shouldSetPostalCodeInputLayoutHint() = runCardMultilineWidgetTest {
        assertThat(cardMultilineWidget.postalInputLayout.hint)
            .isEqualTo("Postal code")
    }

    @Test
    fun setEnabled_setsEnabledPropertyOnAllChildWidgets() = runCardMultilineWidgetTest {
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
    fun testCardValidCallback() = runCardMultilineWidgetTest {
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
    fun testCardValidCallback_usZipCodeRequired() = runCardMultilineWidgetTest {
        cardMultilineWidget.usZipCodeRequired = true

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
                CardValidCallback.Fields.Cvc,
                CardValidCallback.Fields.Postal
            )

        cardMultilineWidget.setCardNumber(VISA_NO_SPACES)
        assertThat(currentIsValid)
            .isFalse()
        assertThat(currentInvalidFields)
            .containsExactly(
                CardValidCallback.Fields.Expiry,
                CardValidCallback.Fields.Cvc,
                CardValidCallback.Fields.Postal
            )

        fullGroup.expiryDateEditText.append("12")
        assertThat(currentIsValid)
            .isFalse()
        assertThat(currentInvalidFields)
            .containsExactly(
                CardValidCallback.Fields.Expiry,
                CardValidCallback.Fields.Cvc,
                CardValidCallback.Fields.Postal
            )

        fullGroup.expiryDateEditText.append("50")
        assertThat(currentIsValid)
            .isFalse()
        assertThat(currentInvalidFields)
            .containsExactly(
                CardValidCallback.Fields.Cvc,
                CardValidCallback.Fields.Postal
            )

        fullGroup.cvcEditText.append("123")
        assertThat(currentIsValid)
            .isFalse()
        assertThat(currentInvalidFields)
            .containsExactly(CardValidCallback.Fields.Postal)

        fullGroup.postalCodeEditText.append("1")
        assertThat(currentIsValid)
            .isFalse()
        assertThat(currentInvalidFields)
            .containsExactly(CardValidCallback.Fields.Postal)

        fullGroup.postalCodeEditText.append("2345")
        assertThat(currentIsValid)
            .isTrue()
        assertThat(currentInvalidFields)
            .isEmpty()
    }

    @Test
    fun testCardValidCallback_postalCodeRequired() = runCardMultilineWidgetTest {
        cardMultilineWidget.postalCodeRequired = true

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
                CardValidCallback.Fields.Cvc,
                CardValidCallback.Fields.Postal
            )

        cardMultilineWidget.setCardNumber(VISA_NO_SPACES)
        assertThat(currentIsValid)
            .isFalse()
        assertThat(currentInvalidFields)
            .containsExactly(
                CardValidCallback.Fields.Expiry,
                CardValidCallback.Fields.Cvc,
                CardValidCallback.Fields.Postal
            )

        fullGroup.expiryDateEditText.append("12")
        assertThat(currentIsValid)
            .isFalse()
        assertThat(currentInvalidFields)
            .containsExactly(
                CardValidCallback.Fields.Expiry,
                CardValidCallback.Fields.Cvc,
                CardValidCallback.Fields.Postal
            )

        fullGroup.expiryDateEditText.append("50")
        assertThat(currentIsValid)
            .isFalse()
        assertThat(currentInvalidFields)
            .containsExactly(
                CardValidCallback.Fields.Cvc,
                CardValidCallback.Fields.Postal
            )

        fullGroup.cvcEditText.append("123")
        assertThat(currentIsValid)
            .isFalse()
        assertThat(currentInvalidFields)
            .containsExactly(CardValidCallback.Fields.Postal)

        fullGroup.postalCodeEditText.setText("A")
        assertThat(currentIsValid)
            .isTrue()
        assertThat(currentInvalidFields)
            .isEmpty()
    }

    @Test
    fun shouldShowErrorIcon_shouldBeUpdatedCorrectly() = runCardMultilineWidgetTest {
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
    fun `getBrand returns the right brands`() = runCardMultilineWidgetTest {
        cardMultilineWidget.setCardNumber(null)
        assertThat(cardMultilineWidget.brand).isEqualTo(CardBrand.Unknown)

        cardMultilineWidget.setCardNumber(VISA_NO_SPACES)
        assertThat(cardMultilineWidget.brand).isEqualTo(CardBrand.Visa)

        cardMultilineWidget.setCardNumber(MASTERCARD_NO_SPACES)
        assertThat(cardMultilineWidget.brand).isEqualTo(CardBrand.MasterCard)

        cardMultilineWidget.setCardNumber(AMEX_NO_SPACES)
        assertThat(cardMultilineWidget.brand).isEqualTo(CardBrand.AmericanExpress)

        cardMultilineWidget.setCardNumber(DISCOVER_NO_SPACES)
        assertThat(cardMultilineWidget.brand).isEqualTo(CardBrand.Discover)

        cardMultilineWidget.setCardNumber(JCB_NO_SPACES)
        assertThat(cardMultilineWidget.brand).isEqualTo(CardBrand.JCB)

        cardMultilineWidget.setCardNumber(DINERS_CLUB_14_NO_SPACES)
        assertThat(cardMultilineWidget.brand).isEqualTo(CardBrand.DinersClub)
    }

    @Test
    fun `Returns the correct create params when user selects no brand in CBC flow`() {
        runCardMultilineWidgetTest(isCbcEligible = true) {
            cardMultilineWidget.setCardNumber("4000002500001001")
            cardMultilineWidget.setExpiryDate(12, 2030)
            cardMultilineWidget.setCvcCode("123")

            val cardParams = cardMultilineWidget.paymentMethodCard
            assertThat(cardParams?.networks?.preferred).isNull()
        }
    }

    @Test
    fun `Returns the correct create params when user selects a brand in CBC flow`() {
        runCardMultilineWidgetTest(isCbcEligible = true) {
            cardMultilineWidget.setCardNumber("4000002500001001")
            cardMultilineWidget.setExpiryDate(12, 2030)
            cardMultilineWidget.setCvcCode("123")
            cardMultilineWidget.cardBrandView.tag = "card_brand_view"

            onView(withTagValue(Matchers.`is`("card_brand_view"))).perform(click())
            onData(anything()).inRoot(isPlatformPopup()).atPosition(1).perform(click())

            val cardParams = cardMultilineWidget.paymentMethodCard
            assertThat(cardParams?.networks?.preferred).isEqualTo(CardBrand.CartesBancaires.code)
        }
    }

    private fun runCardMultilineWidgetTest(
        isCbcEligible: Boolean = false,
        block: TestContext.() -> Unit,
    ) {
        val activityScenario = ActivityScenario.launch<CardMultilineWidgetTestActivity>(
            Intent(context, CardMultilineWidgetTestActivity::class.java).apply {
                putExtra("args", CardMultilineWidgetTestActivity.Args(isCbcEligible = isCbcEligible))
            }
        )

        activityScenario.onActivity { activity ->
            activity.setWorkContext(testDispatcher)

            val cardMultilineWidget =
                activity.findViewById<CardMultilineWidget>(CardMultilineWidgetTestActivity.VIEW_ID)
            cardMultilineWidget.setCardInputListener(fullCardListener)

            val noZipWidget = activity.findViewById<CardMultilineWidget>(CardMultilineWidgetTestActivity.NO_ZIP_VIEW_ID)
            noZipWidget.setCardInputListener(noZipCardListener)

            val testContext = TestContext(cardMultilineWidget, noZipWidget, activity.fullGroup, activity.noZipGroup)
            block(testContext)
        }

        activityScenario.close()
    }

    private class TestContext(
        val cardMultilineWidget: CardMultilineWidget,
        val noZipCardMultilineWidget: CardMultilineWidget,
        val fullGroup: WidgetControlGroup,
        val noZipGroup: WidgetControlGroup,
    )

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

internal class CardMultilineWidgetTestActivity : AppCompatActivity() {

    @Parcelize
    data class Args(
        val isCbcEligible: Boolean,
    ) : Parcelable

    private val args: Args by lazy {
        @Suppress("DEPRECATION")
        intent.getParcelableExtra("args")!!
    }

    private val cardMultilineWidget: CardMultilineWidget by lazy {
        CardMultilineWidget(this, shouldShowPostalCode = true).apply {
            id = VIEW_ID

            val storeOwner = CardElementTestHelper.createViewModelStoreOwner(isCbcEligible = args.isCbcEligible)
            viewModelStoreOwner = storeOwner
            cardNumberEditText.viewModelStoreOwner = storeOwner
        }
    }

    private val noZipCardMultilineWidget: CardMultilineWidget by lazy {
        CardMultilineWidget(this, shouldShowPostalCode = false).apply {
            id = NO_ZIP_VIEW_ID

            val storeOwner = CardElementTestHelper.createViewModelStoreOwner(isCbcEligible = args.isCbcEligible)
            viewModelStoreOwner = storeOwner
            cardNumberEditText.viewModelStoreOwner = storeOwner
        }
    }

    lateinit var fullGroup: CardMultilineWidgetTest.WidgetControlGroup
    lateinit var noZipGroup: CardMultilineWidgetTest.WidgetControlGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.StripeDefaultTheme)

        val layout = LinearLayout(this).apply {
            addView(cardMultilineWidget)
            addView(noZipCardMultilineWidget)
        }
        setContentView(layout)
    }

    fun setWorkContext(workContext: CoroutineContext) {
        fullGroup = CardMultilineWidgetTest.WidgetControlGroup(cardMultilineWidget, workContext)
        noZipGroup = CardMultilineWidgetTest.WidgetControlGroup(noZipCardMultilineWidget, workContext)
    }

    companion object {
        const val VIEW_ID = 12345
        const val NO_ZIP_VIEW_ID = 12346
    }
}
