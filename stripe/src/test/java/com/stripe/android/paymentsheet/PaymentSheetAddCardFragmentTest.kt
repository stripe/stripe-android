package com.stripe.android.paymentsheet

import android.widget.CheckBox
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
    fun `shouldSavePaymentMethod with customer config should default to true `() {
        createScenario().onFragment { fragment ->
            val activityViewModel = activityViewModel(
                fragment,
                PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY
            )
            assertThat(activityViewModel.shouldSavePaymentMethod)
                .isTrue()
        }
    }

    @Test
    fun `shouldSavePaymentMethod should be false when checkbox is unchecked`() {
        createScenario().onFragment { fragment ->
            val activityViewModel = activityViewModel(
                fragment,
                PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY
            )

            val checkbox = fragment.requireView().findViewById<CheckBox>(R.id.save_card_checkbox)
            checkbox.isChecked = false

            assertThat(activityViewModel.shouldSavePaymentMethod)
                .isFalse()
        }
    }

    @Test
    fun `shouldSavePaymentMethod without customer config should default to false`() {
        createScenario().onFragment { fragment ->
            val activityViewModel = activityViewModel(
                fragment,
                PaymentSheetFixtures.ARGS_WITHOUT_CUSTOMER
            )
            assertThat(activityViewModel.shouldSavePaymentMethod)
                .isTrue()
        }
    }

    @Test
    fun `paymentMethodParams with valid input should return object with expected billing details`() {
        createScenario().onFragment { fragment ->
            fragment.cardMultilineWidget.setCardNumber("4242424242424242")
            fragment.cardMultilineWidget.setExpiryDate(1, 2030)
            fragment.cardMultilineWidget.setCvcCode("123")
            fragment.billingAddressView.countryView.setText("United States")
            fragment.billingAddressView.postalCodeView.setText("94107")
            idleLooper()

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
    fun `when isGooglePayEnabled=true should configure Google Pay button`() {
        createScenario().onFragment { fragment ->
            val activityViewModel = activityViewModel(
                fragment,
                PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY
            )

            fragment.onConfigReady(
                AddPaymentMethodConfig(
                    paymentIntent = PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
                    paymentMethods = emptyList(),
                    isGooglePayReady = true
                )
            )
            idleLooper()

            val paymentSelections = mutableListOf<PaymentSelection>()
            activityViewModel.selection.observeForever { paymentSelection ->
                if (paymentSelection != null) {
                    paymentSelections.add(paymentSelection)
                }
            }

            assertThat(fragment.googlePayButton.isVisible)
                .isTrue()

            fragment.googlePayButton.performClick()
            idleLooper()

            assertThat(paymentSelections)
                .containsExactly(PaymentSelection.GooglePay)
        }
    }

    private fun activityViewModel(
        fragment: PaymentSheetAddCardFragment,
        args: PaymentSheetActivityStarter.Args
    ): PaymentSheetViewModel {
        return fragment.activityViewModels<PaymentSheetViewModel> {
            PaymentSheetViewModel.Factory(
                { fragment.requireActivity().application },
                { args }
            )
        }.value
    }

    private fun createScenario(): FragmentScenario<PaymentSheetAddCardFragment> {
        return launchFragmentInContainer<PaymentSheetAddCardFragment>(
            bundleOf(
                PaymentSheetActivity.EXTRA_STARTER_ARGS to PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY
            ),
            R.style.StripePaymentSheetDefaultTheme
        )
    }
}
