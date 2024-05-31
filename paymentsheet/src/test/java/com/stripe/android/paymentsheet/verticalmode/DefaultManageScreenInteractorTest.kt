package com.stripe.android.paymentsheet.verticalmode

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
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

    private fun createInitialState(
        paymentMethods: List<PaymentMethod>?,
        currentSelection: PaymentSelection?,
    ): ManageScreenInteractor.State {
        return DefaultManageScreenInteractor.computeInitialState(
            paymentMethods = paymentMethods,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
            ),
            selection = currentSelection,
            providePaymentMethodName = { it ?: "Missing name" },
            isEditing = false,
        )
    }
}
