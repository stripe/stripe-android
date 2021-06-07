package com.stripe.android.paymentsheet

import android.content.Context
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.R
import com.stripe.android.databinding.FragmentPaymentsheetAddPaymentMethodBinding
import com.stripe.android.databinding.PrimaryButtonBinding
import com.stripe.android.databinding.StripeGooglePayButtonBinding
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheetViewModel.CheckoutIdentifier
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.model.FragmentConfigFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSheetViewState
import com.stripe.android.paymentsheet.ui.PaymentSheetFragmentFactory
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.utils.TestUtils.idleLooper
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentSheetAddPaymentMethodFragmentTest {
    private val eventReporter = mock<EventReporter>()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        PaymentConfiguration.init(
            context,
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
    }

    @Test
    fun `when processing google pay should be disabled`() {
        createFragment { fragment, viewBinding ->
            fragment.sheetViewModel._processing.value = true
            assertThat(viewBinding.googlePayButton.isEnabled).isFalse()
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
    fun `when back to Ready state should update PaymentSelection`() {
        createFragment { fragment, viewBinding ->
            val paymentSelections = mutableListOf<PaymentSelection?>()
            fragment.sheetViewModel.selection.observeForever { paymentSelection ->
                paymentSelections.add(paymentSelection)
            }

            assertThat(viewBinding.googlePayButton.isVisible)
                .isTrue()

            // Start with null PaymentSelection because the card entered is invalid
            assertThat(paymentSelections.size)
                .isEqualTo(1)
            assertThat(paymentSelections[0])
                .isNull()

            viewBinding.googlePayButton.performClick()

            // Updates PaymentSelection to Google Pay
            assertThat(paymentSelections.size)
                .isEqualTo(2)
            assertThat(paymentSelections[1])
                .isEqualTo(PaymentSelection.GooglePay)

            fragment.sheetViewModel._viewState.value = PaymentSheetViewState.Reset(null)

            // Back to Ready state, should return to null PaymentSelection
            assertThat(paymentSelections.size)
                .isEqualTo(3)
            assertThat(paymentSelections[2])
                .isNull()
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
            assertThat((fragment.sheetViewModel.paymentSheetResult.value as PaymentSheetResult.Failed).error.message)
                .isEqualTo("Failed to start add payment option fragment.")
        }
    }

    @Test
    fun `google pay button state updated on start processing`() {
        createFragment(PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY) { fragment, viewBinding ->
            fragment.sheetViewModel.checkoutIdentifier = CheckoutIdentifier.AddFragmentTopGooglePay
            fragment.sheetViewModel._viewState.value = PaymentSheetViewState.StartProcessing

            val googlePayButton =
                StripeGooglePayButtonBinding.bind(viewBinding.googlePayButton)
            val googlePayPrimaryComponent =
                PrimaryButtonBinding.bind(googlePayButton.primaryButton)
            val googlePayIconComponent = googlePayButton.googlePayButtonIcon
            assertThat(googlePayButton.primaryButton.isVisible).isTrue()
            assertThat(googlePayIconComponent.isVisible).isFalse()
            assertThat(googlePayPrimaryComponent.label.text).isEqualTo(
                fragment.getString(R.string.stripe_paymentsheet_primary_button_processing)
            )
        }
    }

    @Test
    fun `google pay button error message displayed`() {
        createFragment(PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY) { fragment, viewBinding ->
            fragment.sheetViewModel.checkoutIdentifier = CheckoutIdentifier.AddFragmentTopGooglePay
            fragment.sheetViewModel._viewState.value =
                PaymentSheetViewState.Reset(BaseSheetViewModel.UserErrorMessage("This is my test error message"))

            assertThat(viewBinding.message.text.toString()).isEqualTo("This is my test error message")

            fragment.sheetViewModel._viewState.value = PaymentSheetViewState.Reset(null)

            assertThat(viewBinding.message.text.toString()).isEqualTo("")
        }
    }

    @Test
    fun `google pay button state updated on finish processing`() {
        createFragment(PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY) { fragment, viewBinding ->
            fragment.sheetViewModel.checkoutIdentifier = CheckoutIdentifier.AddFragmentTopGooglePay

            var finishProcessingCalled = false
            fragment.sheetViewModel._viewState.value =
                PaymentSheetViewState.FinishProcessing {
                    finishProcessingCalled = true
                }

            idleLooper()

            val googlePayButton =
                StripeGooglePayButtonBinding.bind(viewBinding.googlePayButton)
            val googlePayIconComponent = googlePayButton.googlePayButtonIcon
            assertThat(googlePayButton.primaryButton.isVisible).isTrue()
            assertThat(googlePayIconComponent.isVisible).isFalse()
            assertThat(finishProcessingCalled).isTrue()
        }
    }

    private fun createFragment(
        args: PaymentSheetContract.Args = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY,
        fragmentConfig: FragmentConfig? = FragmentConfigFixtures.DEFAULT,
        stripeIntent: StripeIntent? = mock<PaymentIntent>(),
        onReady: (PaymentSheetAddPaymentMethodFragment, FragmentPaymentsheetAddPaymentMethodBinding) -> Unit
    ) {
        launchFragmentInContainer<PaymentSheetAddPaymentMethodFragment>(
            bundleOf(
                PaymentSheetActivity.EXTRA_FRAGMENT_CONFIG to fragmentConfig,
                PaymentSheetActivity.EXTRA_STARTER_ARGS to args
            ),
            R.style.StripePaymentSheetDefaultTheme,
            factory = PaymentSheetFragmentFactory(eventReporter),
            initialState = Lifecycle.State.INITIALIZED
        ).onFragment { fragment ->
            // Mock sheetViewModel loading the StripeIntent before the Fragment is created
            fragment.sheetViewModel._stripeIntent.value = stripeIntent
        }.moveToState(Lifecycle.State.STARTED)
            .onFragment { fragment ->
                onReady(
                    fragment,
                    FragmentPaymentsheetAddPaymentMethodBinding.bind(
                        requireNotNull(fragment.view)
                    )
                )
            }
    }
}
