package com.stripe.android.paymentsheet

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.SessionId
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.ViewState
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
            newCard = null,
            statusBarColor = PaymentSheetFixtures.STATUS_BAR_COLOR
        ),
        prefsRepository = FakePrefsRepository(),
        eventReporter = eventReporter
    )

    @Test
    fun `onUserSelection() when selection has been made should emit on userSelection`() {
        var viewState: ViewState? = null
        viewModel.viewState.observeForever {
            viewState = it
        }
        viewModel.updateSelection(SELECTION_SAVED_PAYMENT_METHOD)

        viewModel.onUserSelection()

        assertThat((viewState as ViewState.PaymentOptions.Finished).result)
            .isEqualTo(PaymentOptionResult.Succeeded(SELECTION_SAVED_PAYMENT_METHOD))
        verify(eventReporter).onSelectPaymentOption(SELECTION_SAVED_PAYMENT_METHOD)
    }

    @Test
    fun `onUserSelection() when new card selection with no save should be finished view state`() {
        var viewState: ViewState? = null
        viewModel.viewState.observeForever {
            viewState = it
        }
        viewModel.updateSelection(NEW_REQUEST_DONT_SAVE_PAYMENT_SELECTION)

        viewModel.onUserSelection()

        assertThat((viewState as ViewState.PaymentOptions.Finished).result)
            .isEqualTo(PaymentOptionResult.Succeeded(NEW_REQUEST_DONT_SAVE_PAYMENT_SELECTION))
        verify(eventReporter).onSelectPaymentOption(NEW_REQUEST_DONT_SAVE_PAYMENT_SELECTION)
    }

    @Test
    fun `onUserSelection() when new card selection with save view state should be confirm - save`() {
        val viewState: MutableList<ViewState?> = mutableListOf()
        viewModel.viewState.observeForever {
            viewState.add(it)
        }
        viewModel.updateSelection(NEW_REQUEST_SAVE_PAYMENT_SELECTION)

        viewModel.onUserSelection()

        assertThat(viewState[0]).isInstanceOf(ViewState.PaymentOptions.Ready::class.java)

        val paymentOptionResultSucceeded = (viewState[1] as ViewState.PaymentOptions.Completed)
            .result as PaymentOptionResult.Succeeded
        assertThat((paymentOptionResultSucceeded).paymentSelection)
            .isEqualTo(NEW_REQUEST_SAVE_PAYMENT_SELECTION)
        verify(eventReporter).onSelectPaymentOption(paymentOptionResultSucceeded.paymentSelection)
    }

    @Test
    fun `onUserSelection() when selection has not been made should not emit`() {
        var viewState: ViewState? = null
        viewModel.viewState.observeForever {
            viewState = it
        }
        viewModel.onUserSelection()

        assertThat(viewState).isInstanceOf(ViewState.PaymentOptions.Ready::class.java)
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
        private val paymentMethodCreateParams: PaymentMethodCreateParams = mock()
        private val NEW_REQUEST_SAVE_PAYMENT_SELECTION = PaymentSelection.New.Card(
            paymentMethodCreateParams,
            CardBrand.Visa,
            true,
        )
        private val NEW_REQUEST_DONT_SAVE_PAYMENT_SELECTION = PaymentSelection.New.Card(
            paymentMethodCreateParams,
            CardBrand.Visa,
            true,
        )
    }
}
