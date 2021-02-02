package com.stripe.android.paymentsheet

import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.R
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.model.FragmentConfigFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.PaymentSheetFragmentFactory
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentSheetAddCardFragmentTest {
    private val eventReporter = mock<EventReporter>()

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

            val newPaymentSelection = paymentSelection as PaymentSelection.New.Card
            assertThat(newPaymentSelection.shouldSavePaymentMethod)
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

            fragment.onConfigReady(FragmentConfigFixtures.DEFAULT)
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

    @Test
    fun `onConfigReady() should update header text`() {
        createScenario().onFragment { fragment ->
            fragment.onConfigReady(FragmentConfigFixtures.DEFAULT)

            assertThat(fragment.addCardHeader.text.toString())
                .isEqualTo("Pay $10.99 using")
        }
    }

    @Test
    fun `checkbox text should reflect merchant display name`() {
        createScenario().onFragment { fragment ->
            assertThat(fragment.saveCardCheckbox.text)
                .isEqualTo("Save this card for future Widget Store payments")
        }
    }

    @Test
    fun `started fragment should report onShowNewPaymentOptionForm() event`() {
        createScenario().onFragment {
            verify(eventReporter).onShowNewPaymentOptionForm()
        }
    }

    @Test
    fun `fragment started without FragmentConfig should emit fatal`() {
        createScenario(
            fragmentConfig = null
        ).onFragment { fragment ->
            assertThat(fragment.sheetViewModel.fatal.value?.message)
                .isEqualTo("Failed to start add payment option fragment.")
        }
    }

    private fun activityViewModel(
        fragment: PaymentSheetAddCardFragment,
        args: PaymentSheetContract.Args = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY
    ): PaymentSheetViewModel {
        return fragment.activityViewModels<PaymentSheetViewModel> {
            PaymentSheetViewModel.Factory(
                { fragment.requireActivity().application },
                { args }
            )
        }.value
    }

    private fun createScenario(
        args: PaymentSheetContract.Args = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY,
        fragmentConfig: FragmentConfig? = FragmentConfigFixtures.DEFAULT
    ): FragmentScenario<PaymentSheetAddCardFragment> {
        return launchFragmentInContainer<PaymentSheetAddCardFragment>(
            bundleOf(
                PaymentSheetActivity.EXTRA_FRAGMENT_CONFIG to fragmentConfig,
                PaymentSheetActivity.EXTRA_STARTER_ARGS to args
            ),
            R.style.StripePaymentSheetDefaultTheme,
            factory = PaymentSheetFragmentFactory(eventReporter)
        )
    }
}
