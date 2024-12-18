package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.ViewActionRecorder
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
internal class FakePaymentMethodVerticalLayoutInteractor(
    initialState: PaymentMethodVerticalLayoutInteractor.State,
    initialShowsWalletsHeader: Boolean = false,
    private val viewActionRecorder: ViewActionRecorder<PaymentMethodVerticalLayoutInteractor.ViewAction>
) : PaymentMethodVerticalLayoutInteractor {
    companion object {
        fun create(
            paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            initialShowsWalletsHeader: Boolean = true,
            selection: PaymentSelection? = null,
            viewActionRecorder: ViewActionRecorder<PaymentMethodVerticalLayoutInteractor.ViewAction> =
                ViewActionRecorder()
        ): FakePaymentMethodVerticalLayoutInteractor {
            val displayablePaymentMethods = paymentMethodMetadata.sortedSupportedPaymentMethods()
                .map { supportedPaymentMethod ->
                    supportedPaymentMethod.asDisplayablePaymentMethod(
                        customerSavedPaymentMethods = emptyList(),
                        incentive = null,
                        onClick = { throw AssertionError("Not expected.") },
                    )
                }
            val initialState = PaymentMethodVerticalLayoutInteractor.State(
                displayablePaymentMethods = displayablePaymentMethods,
                isProcessing = false,
                selection = selection,
                displayedSavedPaymentMethod = null,
                availableSavedPaymentMethodAction =
                PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.MANAGE_ALL,
                rowType = Embedded.RowStyle.FloatingButton.default
            )
            return FakePaymentMethodVerticalLayoutInteractor(
                initialState = initialState,
                initialShowsWalletsHeader = initialShowsWalletsHeader,
                viewActionRecorder = viewActionRecorder,
            )
        }
    }

    override val isLiveMode: Boolean = true
    override val state: StateFlow<PaymentMethodVerticalLayoutInteractor.State> = stateFlowOf(initialState)
    override val showsWalletsHeader: StateFlow<Boolean> = stateFlowOf(initialShowsWalletsHeader)

    override fun handleViewAction(viewAction: PaymentMethodVerticalLayoutInteractor.ViewAction) {
        viewActionRecorder.record(viewAction)
    }
}
