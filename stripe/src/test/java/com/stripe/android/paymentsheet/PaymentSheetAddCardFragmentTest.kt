package com.stripe.android.paymentsheet

import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.R
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.AddPaymentMethodConfig
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.utils.TestUtils.idleLooper
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentSheetAddCardFragmentTest {

    @Before
    fun setup() {
        PaymentConfiguration.init(
            ApplicationProvider.getApplicationContext(),
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
    }

    @Test
    fun `paymentMethodParams with valid input should return object with expected billing details`() {
        createScenario().onFragment { fragment ->
            fragment.cardMultilineWidget.setCardNumber("4242424242424242")
            fragment.cardMultilineWidget.setExpiryDate(1, 2030)
            fragment.cardMultilineWidget.setCvcCode("123")
            fragment.billingAddressView.countryView.setText("United States")
            fragment.billingAddressView.postalCodeView.setText("94107")

            val params = requireNotNull(fragment.paymentMethodParams)
            assertThat(params.billingDetails)
                .isEqualTo(
                    PaymentMethod.BillingDetails(
                        Address(
                            country = "US",
                            postalCode = "94107"
                        )
                    )
                )
        }
    }

    @Test
    fun `selection without customer config and valid card entered should create expected PaymentSelection`() {
        createScenario(PaymentSheetFixtures.ARGS_WITHOUT_CUSTOMER).onFragment { fragment ->
            fragment.saveCardCheckbox.isChecked = false

            val activityViewModel = activityViewModel(
                fragment,
                PaymentSheetFixtures.ARGS_WITHOUT_CUSTOMER
            )

            var paymentSelection: PaymentSelection? = null
            activityViewModel.selection.observeForever {
                paymentSelection = it
            }

            fragment.saveCardCheckbox.isChecked = true

            fragment.cardMultilineWidget.setCardNumber("4242424242424242")
            fragment.cardMultilineWidget.setExpiryDate(1, 2030)
            fragment.cardMultilineWidget.setCvcCode("123")
            fragment.billingAddressView.countryView.setText("United States")
            fragment.billingAddressView.postalCodeView.setText("94107")

            val newPaymentSelection = paymentSelection as PaymentSelection.New.Card
            assertThat(newPaymentSelection.shouldSavePaymentMethod)
                .isFalse()
        }
    }

    @Test
    fun `selection when save card checkbox enabled and then valid card entered should create expected PaymentSelection`() {
        createScenario().onFragment { fragment ->
            fragment.saveCardCheckbox.isChecked = false

            val activityViewModel = activityViewModel(fragment)

            var paymentSelection: PaymentSelection? = null
            activityViewModel.selection.observeForever {
                paymentSelection = it
            }

            fragment.saveCardCheckbox.isChecked = true

            fragment.cardMultilineWidget.setCardNumber("4242424242424242")
            fragment.cardMultilineWidget.setExpiryDate(1, 2030)
            fragment.cardMultilineWidget.setCvcCode("123")
            fragment.billingAddressView.countryView.setText("United States")
            fragment.billingAddressView.postalCodeView.setText("94107")
            idleLooper()

            val newPaymentSelection = paymentSelection as? PaymentSelection.New.Card
            assertThat(newPaymentSelection?.shouldSavePaymentMethod)
                .isTrue()
        }
    }

    @Test
    fun `selection when valid card entered and then save card checkbox enabled should create expected PaymentSelection`() {
        createScenario().onFragment { fragment ->
            fragment.saveCardCheckbox.isChecked = false

            val activityViewModel = activityViewModel(fragment)

            var paymentSelection: PaymentSelection? = null
            activityViewModel.selection.observeForever {
                paymentSelection = it
            }

            fragment.cardMultilineWidget.setCardNumber("4242424242424242")
            fragment.cardMultilineWidget.setExpiryDate(1, 2030)
            fragment.cardMultilineWidget.setCvcCode("123")
            fragment.billingAddressView.countryView.setText("United States")
            fragment.billingAddressView.postalCodeView.setText("94107")

            fragment.saveCardCheckbox.isChecked = true

            val newPaymentSelection = paymentSelection as PaymentSelection.New.Card
            assertThat(newPaymentSelection.shouldSavePaymentMethod)
                .isTrue()
        }
    }

    @Test
    fun `when isGooglePayEnabled=true should configure Google Pay button`() {
        createScenario().onFragment { fragment ->
            val activityViewModel = activityViewModel(fragment)

            fragment.onConfigReady(
                AddPaymentMethodConfig(
                    paymentIntent = PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
                    paymentMethods = emptyList(),
                    isGooglePayReady = true
                )
            )
            val paymentSelections = mutableListOf<PaymentSelection>()
            activityViewModel.selection.observeForever { paymentSelection ->
                if (paymentSelection != null) {
                    paymentSelections.add(paymentSelection)
                }
            }

            assertThat(fragment.googlePayButton.isVisible)
                .isTrue()

            fragment.googlePayButton.performClick()

            assertThat(paymentSelections)
                .containsExactly(PaymentSelection.GooglePay)
        }
    }

    private fun activityViewModel(
        fragment: PaymentSheetAddCardFragment,
        args: PaymentSheetActivityStarter.Args = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY
    ): PaymentSheetViewModel {
        return fragment.activityViewModels<PaymentSheetViewModel> {
            PaymentSheetViewModel.Factory(
                { fragment.requireActivity().application },
                { args }
            )
        }.value
    }

    private fun createScenario(
        args: PaymentSheetActivityStarter.Args = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY
    ): FragmentScenario<PaymentSheetAddCardFragment> {
        return launchFragmentInContainer<PaymentSheetAddCardFragment>(
            bundleOf(
                PaymentSheetActivity.EXTRA_STARTER_ARGS to args
            ),
            R.style.StripePaymentSheetDefaultTheme
        )
    }
}
