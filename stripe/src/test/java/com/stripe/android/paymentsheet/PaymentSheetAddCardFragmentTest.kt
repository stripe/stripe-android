package com.stripe.android.paymentsheet

import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.R
import com.stripe.android.databinding.FragmentPaymentsheetAddCardBinding
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
        createFragment { fragment, viewBinding ->
            viewBinding.cardMultilineWidget.setCardNumber("4242424242424242")
            viewBinding.cardMultilineWidget.setExpiryDate(1, 2030)
            viewBinding.cardMultilineWidget.setCvcCode("123")
            viewBinding.billingAddress.countryView.setText("United States")
            viewBinding.billingAddress.postalCodeView.setText("94107")

            val paymentSelections = mutableListOf<PaymentSelection>()
            fragment.sheetViewModel.selection.observeForever { paymentSelection ->
                if (paymentSelection != null) {
                    paymentSelections.add(paymentSelection)
                }
            }

            val newCard = paymentSelections.first() as PaymentSelection.New.Card
            assertThat(newCard.paymentMethodCreateParams.billingDetails)
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
        createFragment(PaymentSheetFixtures.ARGS_WITHOUT_CUSTOMER) { fragment, viewBinding ->
            viewBinding.saveCardCheckbox.isChecked = false

            var paymentSelection: PaymentSelection? = null
            fragment.sheetViewModel.selection.observeForever {
                paymentSelection = it
            }

            viewBinding.saveCardCheckbox.isChecked = true

            viewBinding.cardMultilineWidget.setCardNumber("4242424242424242")
            viewBinding.cardMultilineWidget.setExpiryDate(1, 2030)
            viewBinding.cardMultilineWidget.setCvcCode("123")
            viewBinding.billingAddress.countryView.setText("United States")
            viewBinding.billingAddress.postalCodeView.setText("94107")

            val newPaymentSelection = paymentSelection as PaymentSelection.New.Card
            assertThat(newPaymentSelection.shouldSavePaymentMethod)
                .isFalse()
        }
    }

    @Test
    fun `selection when save card checkbox enabled and then valid card entered should create expected PaymentSelection`() {
        createFragment { fragment, viewBinding ->
            viewBinding.saveCardCheckbox.isChecked = false

            var paymentSelection: PaymentSelection? = null
            fragment.sheetViewModel.selection.observeForever {
                paymentSelection = it
            }

            viewBinding.saveCardCheckbox.isChecked = true

            viewBinding.cardMultilineWidget.setCardNumber("4242424242424242")
            viewBinding.cardMultilineWidget.setExpiryDate(1, 2030)
            viewBinding.cardMultilineWidget.setCvcCode("123")
            viewBinding.billingAddress.countryView.setText("United States")
            viewBinding.billingAddress.postalCodeView.setText("94107")

            val newPaymentSelection = paymentSelection as PaymentSelection.New.Card
            assertThat(newPaymentSelection.shouldSavePaymentMethod)
                .isTrue()
        }
    }

    @Test
    fun `selection when valid card entered and then save card checkbox enabled should create expected PaymentSelection`() {
        createFragment { fragment, viewBinding ->
            viewBinding.saveCardCheckbox.isChecked = false

            var paymentSelection: PaymentSelection? = null
            fragment.sheetViewModel.selection.observeForever {
                paymentSelection = it
            }

            viewBinding.cardMultilineWidget.setCardNumber("4242424242424242")
            viewBinding.cardMultilineWidget.setExpiryDate(1, 2030)
            viewBinding.cardMultilineWidget.setCvcCode("123")
            viewBinding.billingAddress.countryView.setText("United States")
            viewBinding.billingAddress.postalCodeView.setText("94107")

            viewBinding.saveCardCheckbox.isChecked = true

            val newPaymentSelection = paymentSelection as PaymentSelection.New.Card
            assertThat(newPaymentSelection.shouldSavePaymentMethod)
                .isTrue()
        }
    }

    @Test
    fun `when isGooglePayEnabled=true should configure Google Pay button`() {
        createFragment { fragment, viewBinding ->
            val paymentSelections = mutableListOf<PaymentSelection>()
            fragment.sheetViewModel.selection.observeForever { paymentSelection ->
                if (paymentSelection != null) {
                    paymentSelections.add(paymentSelection)
                }
            }

            assertThat(viewBinding.googlePayButton.isVisible)
                .isTrue()

            viewBinding.googlePayButton.performClick()

            assertThat(paymentSelections)
                .containsExactly(PaymentSelection.GooglePay)
        }
    }

    @Test
    fun `onConfigReady() should update header text`() {
        createFragment { _, viewBinding ->
            assertThat(viewBinding.addCardHeader.text.toString())
                .isEqualTo("Pay $10.99 using")
        }
    }

    @Test
    fun `checkbox text should reflect merchant display name`() {
        createFragment { _, viewBinding ->
            assertThat(viewBinding.saveCardCheckbox.text)
                .isEqualTo("Save this card for future Widget Store payments")
        }
    }

    @Test
    fun `started fragment should report onShowNewPaymentOptionForm() event`() {
        createFragment { _, _ ->
            verify(eventReporter).onShowNewPaymentOptionForm()
        }
    }

    @Test
    fun `fragment started without FragmentConfig should emit fatal`() {
        createFragment(
            fragmentConfig = null
        ) { fragment, _ ->
            assertThat(fragment.sheetViewModel.fatal.value?.message)
                .isEqualTo("Failed to start add payment option fragment.")
        }
    }

    @Test
    fun `cardErrors should react to input validity`() {
        createFragment { _, viewBinding ->
            assertThat(viewBinding.cardErrors.isVisible)
                .isFalse()

            viewBinding.cardMultilineWidget.setCardNumber("4242424242424249")
            viewBinding.cardMultilineWidget.setExpiryDate(1, 2010)
            assertThat(viewBinding.cardErrors.text)
                .isEqualTo("Your card's number is invalid.")
            assertThat(viewBinding.cardErrors.isVisible)
                .isTrue()

            viewBinding.cardMultilineWidget.setCardNumber("4242424242424242")
            assertThat(viewBinding.cardErrors.text)
                .isEqualTo("Your card's expiration year is invalid.")
            assertThat(viewBinding.cardErrors.isVisible)
                .isTrue()

            viewBinding.cardMultilineWidget.setExpiryDate(1, 2030)
            assertThat(viewBinding.cardErrors.text.toString())
                .isEmpty()
            assertThat(viewBinding.cardErrors.isVisible)
                .isFalse()
        }
    }

    private fun createFragment(
        args: PaymentSheetContract.Args = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY,
        fragmentConfig: FragmentConfig? = FragmentConfigFixtures.DEFAULT,
        onReady: (PaymentSheetAddCardFragment, FragmentPaymentsheetAddCardBinding) -> Unit
    ) {
        launchFragmentInContainer<PaymentSheetAddCardFragment>(
            bundleOf(
                PaymentSheetActivity.EXTRA_FRAGMENT_CONFIG to fragmentConfig,
                PaymentSheetActivity.EXTRA_STARTER_ARGS to args
            ),
            R.style.StripePaymentSheetDefaultTheme,
            factory = PaymentSheetFragmentFactory(eventReporter)
        ).onFragment { fragment ->
            onReady(
                fragment,
                FragmentPaymentsheetAddCardBinding.bind(
                    requireNotNull(fragment.view)
                )
            )
        }
    }
}
