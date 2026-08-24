package com.stripe.android.paymentelement.embedded.sheet

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentsheet.FakeCustomerStateHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.verticalmode.FakeSavedPaymentMethodConfirmInteractor
import com.stripe.android.paymentsheet.verticalmode.SavedPaymentMethodConfirmInteractor
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class SavedPaymentMethodConfirmScreenFactoryTest {
    @Test
    fun `create passes initial selection to interactor factory`() = runTest {
        val interactorFactory = FakeInteractorFactory()
        val initialSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        val factory = createFactory(interactorFactory)

        factory.create(initialSelection)

        assertThat(interactorFactory.createCalls.awaitItem().initialSelection).isEqualTo(initialSelection)
        interactorFactory.validate()
    }

    @Test
    fun `create wires interactor selection updates to selection holder`() = runTest {
        val interactorFactory = FakeInteractorFactory()
        val selectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle())
        val factory = createFactory(
            interactorFactory = interactorFactory,
            selectionHolder = selectionHolder,
        )
        val updatedSelection = PaymentSelection.Saved(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_updated")
        )

        factory.create(PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD))
        interactorFactory.createCalls.awaitItem().updateSelection(updatedSelection)

        assertThat(selectionHolder.selection.value).isEqualTo(updatedSelection)
        interactorFactory.validate()
    }

    @Test
    fun `create derives live mode from payment method metadata`() = runTest {
        val interactorFactory = FakeInteractorFactory()
        val factory = createFactory(
            interactorFactory = interactorFactory,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    isLiveMode = true,
                )
            ),
        )

        val screen = factory.create(PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD))

        assertThat(screen.topBarState().value!!.showTestModeLabel).isFalse()
        interactorFactory.createCalls.awaitItem()
        interactorFactory.validate()
    }

    private fun createFactory(
        interactorFactory: SavedPaymentMethodConfirmInteractor.Factory,
        selectionHolder: DefaultEmbeddedSelectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle()),
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
    ): SavedPaymentMethodConfirmScreenFactory {
        return SavedPaymentMethodConfirmScreenFactory(
            interactorFactory = interactorFactory,
            paymentMethodMetadata = paymentMethodMetadata,
            sheetActivityStateHolder = FakeSheetActivityStateHolder(),
            confirmationHelper = FakeSheetActivityConfirmationHelper(),
            embeddedSelectionHolder = selectionHolder,
            customerStateHolder = FakeCustomerStateHolder(),
            launchMode = EmbeddedLaunchMode.Form(
                selectedPaymentMethodCode = "card",
            ),
        )
    }

    private class FakeInteractorFactory : SavedPaymentMethodConfirmInteractor.Factory {
        val createCalls = Turbine<CreateCall>()

        override fun create(
            initialSelection: PaymentSelection.Saved,
            updateSelection: (PaymentSelection.Saved) -> Unit,
        ): SavedPaymentMethodConfirmInteractor {
            createCalls.add(
                CreateCall(
                    initialSelection = initialSelection,
                    updateSelection = updateSelection,
                )
            )
            return FakeSavedPaymentMethodConfirmInteractor()
        }

        fun validate() {
            createCalls.ensureAllEventsConsumed()
        }

        data class CreateCall(
            val initialSelection: PaymentSelection.Saved,
            val updateSelection: (PaymentSelection.Saved) -> Unit,
        )
    }
}
