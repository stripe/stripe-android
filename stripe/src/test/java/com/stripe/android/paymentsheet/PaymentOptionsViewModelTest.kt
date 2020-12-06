package com.stripe.android.paymentsheet

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.model.PaymentOptionViewState
import com.stripe.android.paymentsheet.model.PaymentSelection
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class PaymentOptionsViewModelTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val viewModel = PaymentOptionsViewModel(
        publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
        stripeAccountId = null,
        args = PaymentOptionsActivityStarter.Args.Guest(
            paymentIntent = PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            googlePayConfig = PaymentSheetGooglePayConfigFixtures.DEFAULT
        ),
        googlePayRepository = mock()
    )

    @Test
    fun `selectPaymentOption() when selection has been made should emit completion view state`() {
        var viewState: PaymentOptionViewState? = null
        viewModel.viewState.observeForever {
            viewState = it
        }
        viewModel.updateSelection(SELECTION_SAVED_PAYMENT_METHOD)

        viewModel.selectPaymentOption()

        assertThat(viewState)
            .isEqualTo(PaymentOptionViewState.Completed(SELECTION_SAVED_PAYMENT_METHOD))
    }

    @Test
    fun `selectPaymentOption() when selection has not been made should not emit`() {
        var viewState: PaymentOptionViewState? = null
        viewModel.viewState.observeForever {
            viewState = it
        }
        viewModel.selectPaymentOption()

        assertThat(viewState)
            .isNull()
    }

    private companion object {
        private val SELECTION_SAVED_PAYMENT_METHOD = PaymentSelection.Saved(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD
        )
    }
}
