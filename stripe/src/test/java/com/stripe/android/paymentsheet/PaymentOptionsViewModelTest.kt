package com.stripe.android.paymentsheet

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodCreateParamsFixtures.DEFAULT_CARD
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.PaymentOptionsViewModel.TransitionTarget
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.SessionId
import com.stripe.android.paymentsheet.model.FragmentConfigFixtures
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
    private val newCard = PaymentSelection.New.Card(
        DEFAULT_CARD,
        CardBrand.Discover,
        false
    )
    private val args = PaymentOptionContract.Args(
        paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
        paymentMethods = emptyList(),
        sessionId = SessionId(),
        config = PaymentSheetFixtures.CONFIG_GOOGLEPAY,
        isGooglePayReady = true,
        newCard = null,
        statusBarColor = PaymentSheetFixtures.STATUS_BAR_COLOR
    )
    private val viewModel = PaymentOptionsViewModel(
        args = args,
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

    @Test
    fun `resolveTransitionTarget no new card`() {

        val viewModel = PaymentOptionsViewModel(
            args = args.copy(newCard = null),
            prefsRepository = FakePrefsRepository(),
            eventReporter = eventReporter
        )

        var transitionTarget: TransitionTarget? = null
        viewModel.transition.observeForever {
            transitionTarget = it
        }

        // no customer, no new card, no paymentMethods
        val fragmentConfig = FragmentConfigFixtures.DEFAULT
        viewModel.resolveTransitionTarget(fragmentConfig)

        assertThat(transitionTarget).isNull()
    }

    @Test
    fun `resolveTransitionTarget new card saved`() {

        val viewModel = PaymentOptionsViewModel(
            args = args.copy(
                newCard = newCard.copy(
                    shouldSavePaymentMethod = true
                )
            ),
            prefsRepository = FakePrefsRepository(),
            eventReporter = eventReporter
        )

        val transitionTarget: MutableList<TransitionTarget?> = mutableListOf()
        viewModel.transition.observeForever {
            transitionTarget.add(it)
        }

        val fragmentConfig = FragmentConfigFixtures.DEFAULT
        viewModel.resolveTransitionTarget(fragmentConfig)

        assertThat(transitionTarget).containsExactly(null)
    }

    @Test
    fun `resolveTransitionTarget new card NOT saved`() {
        val newCard = newCard.copy(
            shouldSavePaymentMethod = false
        )
        val viewModel = PaymentOptionsViewModel(
            args = args.copy(
                newCard = newCard
            ),
            prefsRepository = FakePrefsRepository(),
            eventReporter = eventReporter
        )
        viewModel.updateSelection(newCard)

        val transitionTarget: MutableList<TransitionTarget?> = mutableListOf()
        viewModel.transition.observeForever {
            transitionTarget.add(it)
        }

        val fragmentConfig = FragmentConfigFixtures.DEFAULT
        viewModel.resolveTransitionTarget(fragmentConfig)
        assertThat(transitionTarget).hasSize(2)
        assertThat(transitionTarget[1]).isInstanceOf(TransitionTarget.AddPaymentMethodFull::class.java)

        viewModel.resolveTransitionTarget(fragmentConfig)
        assertThat(transitionTarget).hasSize(2)
    }

    private companion object {
        private val SELECTION_SAVED_PAYMENT_METHOD = PaymentSelection.Saved(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD
        )
    }
}
