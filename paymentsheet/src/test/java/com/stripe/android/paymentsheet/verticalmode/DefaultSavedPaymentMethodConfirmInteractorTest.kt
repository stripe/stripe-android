package com.stripe.android.paymentsheet.verticalmode

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.spms.SavedPaymentMethodLinkFormHelper
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.uicore.elements.FormElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DefaultSavedPaymentMethodConfirmInteractorTest {

    @Test
    fun `when link form helper state is updated, selection is updated`() = runTest {
        val linkFormHelper = FakeSavedPaymentMethodLinkFormHelper(
            initialState = SavedPaymentMethodLinkFormHelper.State.Unused
        )
        val updateSelectionCalls = Turbine<PaymentSelection.Saved>()
        val testScope = TestScope()

        getDefaultSavedPaymentMethodConfirmInteractor(
            linkFormHelper = linkFormHelper,
            updateSelection = { updateSelectionCalls.add(it) },
            coroutineScope = testScope,
        )

        val userInput = UserInput.SignIn(email = "test@example.com")
        linkFormHelper.updateState(
            SavedPaymentMethodLinkFormHelper.State.Complete(
                userInput = userInput
            )
        )

        testScope.advanceUntilIdle()

        val updatedSelection = updateSelectionCalls.awaitItem()
        assertThat(updatedSelection.linkInput).isEqualTo(userInput)

        updateSelectionCalls.ensureAllEventsConsumed()
    }

    private fun getDefaultSavedPaymentMethodConfirmInteractor(
        linkFormHelper: SavedPaymentMethodLinkFormHelper = FakeSavedPaymentMethodLinkFormHelper(),
        updateSelection: (PaymentSelection.Saved) -> Unit = {},
        coroutineScope: CoroutineScope,
    ): DefaultSavedPaymentMethodConfirmInteractor {
        val paymentMethod = PaymentMethodFactory.card()
        return DefaultSavedPaymentMethodConfirmInteractor(
            initialSelection = PaymentSelection.Saved(paymentMethod),
            displayName = "Card".resolvableString,
            savedPaymentMethodLinkFormHelper = linkFormHelper,
            updateSelection = updateSelection,
            coroutineScope = coroutineScope,
        )
    }

    private class FakeSavedPaymentMethodLinkFormHelper(
        initialState: SavedPaymentMethodLinkFormHelper.State = SavedPaymentMethodLinkFormHelper.State.Unused,
        override val formElement: FormElement? = null,
    ) : SavedPaymentMethodLinkFormHelper {
        private val _state = MutableStateFlow(initialState)
        override val state: StateFlow<SavedPaymentMethodLinkFormHelper.State> = _state.asStateFlow()

        fun updateState(newState: SavedPaymentMethodLinkFormHelper.State) {
            _state.value = newState
        }
    }
}
