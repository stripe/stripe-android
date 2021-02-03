package com.stripe.android.paymentsheet

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.SessionId
import com.stripe.android.paymentsheet.model.PaymentSelection
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class PaymentOptionsViewModelTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val eventReporter = mock<EventReporter>()
    private val viewModel = PaymentOptionsViewModel(
        args = PaymentOptionContract.Args(
            paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            paymentMethods = emptyList(),
            sessionId = SessionId(),
            config = PaymentSheetFixtures.CONFIG_GOOGLEPAY,
            isGooglePayReady = true,
            newCard = null
        ),
        prefsRepository = FakePrefsRepository(),
        eventReporter = eventReporter
    )

    @Test
    fun `onUserSelection() when selection has been made should emit on userSelection`() {
        var paymentSelection: PaymentSelection? = null
        viewModel.userSelection.observeForever {
            paymentSelection = it
        }
        viewModel.updateSelection(SELECTION_SAVED_PAYMENT_METHOD)

        viewModel.onUserSelection()

        assertThat(paymentSelection)
            .isEqualTo(SELECTION_SAVED_PAYMENT_METHOD)
        verify(eventReporter).onSelectPaymentOption(SELECTION_SAVED_PAYMENT_METHOD)
    }

    @Test
    fun `onUserSelection() when selection has not been made should not emit`() {
        var paymentSelection: PaymentSelection? = null
        viewModel.userSelection.observeForever {
            paymentSelection = it
        }
        viewModel.onUserSelection()

        assertThat(paymentSelection)
            .isNull()
    }

    @Test
    fun `getPaymentOptionResult() after selection is set should return Succeeded`() {
        viewModel.updateSelection(SELECTION_SAVED_PAYMENT_METHOD)

        assertThat(
            viewModel.getPaymentOptionResult()
        ).isEqualTo(
            PaymentOptionResult.Succeeded(SELECTION_SAVED_PAYMENT_METHOD)
        )
    }

    @Test
    fun `getPaymentOptionResult() when selection is not set should return Canceled`() {
        assertThat(
            viewModel.getPaymentOptionResult()
        ).isEqualTo(
            PaymentOptionResult.Canceled(null)
        )
    }

    private companion object {
        private val SELECTION_SAVED_PAYMENT_METHOD = PaymentSelection.Saved(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD
        )
    }
}
