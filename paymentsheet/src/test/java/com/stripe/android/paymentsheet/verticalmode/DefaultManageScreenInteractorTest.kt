package com.stripe.android.paymentsheet.verticalmode

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test

class DefaultManageScreenInteractorTest {

    @Test
    fun initializeState_nullCurrentSelection() {
        val paymentMethods = PaymentMethodFixtures.createCards(2).plus(
            // This is here because an easy bug to write would be selecting a PM with a null ID when the current
            // selection is also null
            PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = null)
        )
        val initialState = createInitialState(paymentMethods, currentSelection = null)

        assertThat(initialState.currentSelection).isNull()
        assertThat(initialState.paymentMethods).hasSize(3)
    }

    @Test
    fun initializeState_currentSelectionFoundCorrectly() {
        val paymentMethods = PaymentMethodFixtures.createCards(2)
        val initialState = createInitialState(
            paymentMethods,
            currentSelection = PaymentSelection.Saved(paymentMethods[0])
        )

        assertThat(initialState.currentSelection?.paymentMethod).isEqualTo(paymentMethods[0])
    }

    @Test
    fun initializeState_noCurrentSelectionIfEditing() {
        val paymentMethods = PaymentMethodFixtures.createCards(2)
        val initialState = createInitialState(
            paymentMethods,
            currentSelection = PaymentSelection.Saved(paymentMethods[0]),
            isEditing = true,
        )

        assertThat(initialState.currentSelection).isNull()
    }

    private val notImplemented: () -> Nothing = { throw AssertionError("Not implemented") }

    private fun createInitialState(
        initialPaymentMethods: List<PaymentMethod>?,
        currentSelection: PaymentSelection?,
        isEditing: Boolean = false,
    ): ManageScreenInteractor.State {
        val paymentMethods = MutableStateFlow(initialPaymentMethods)
        val selection = MutableStateFlow(currentSelection)
        val editing = MutableStateFlow(isEditing)

        return DefaultManageScreenInteractor(
            paymentMethods = paymentMethods,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
            ),
            selection = selection,
            editing = editing,
            providePaymentMethodName = { it ?: "Missing name" },
            onSelectPaymentMethod = { notImplemented() },
            onDeletePaymentMethod = { notImplemented() },
            onEditPaymentMethod = { notImplemented() },
            navigateBack = { notImplemented() },
        ).state.value
    }
}
