package com.stripe.android.paymentsheet.ui

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.ViewActionRecorder
import com.stripe.android.paymentsheet.forms.FormArgumentsFactory
import com.stripe.android.uicore.utils.stateFlowOf
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import kotlinx.coroutines.flow.StateFlow
import org.mockito.kotlin.mock

internal class FakeAddPaymentMethodInteractor(
    initialState: AddPaymentMethodInteractor.State,
    private val viewActionRecorder: ViewActionRecorder<AddPaymentMethodInteractor.ViewAction> = ViewActionRecorder(),
) : AddPaymentMethodInteractor {
    override val state: StateFlow<AddPaymentMethodInteractor.State> = stateFlowOf(initialState)
    override val isLiveMode: Boolean = true

    override fun handleViewAction(viewAction: AddPaymentMethodInteractor.ViewAction) {
        viewActionRecorder.record(viewAction)
    }

    override fun close() {
        // Do nothing.
    }

    companion object {
        fun createState(
            metadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            paymentMethodCode: PaymentMethodCode = metadata.supportedPaymentMethodTypes().first(),
        ): AddPaymentMethodInteractor.State {
            val formArguments = FormArgumentsFactory.create(
                paymentMethodCode = paymentMethodCode,
                metadata = metadata,
            )
            val uiDefinitionArgumentsFactory = UiDefinitionFactory.Arguments.Factory.Default(
                cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
                linkConfigurationCoordinator = null,
                onLinkInlineSignupStateChanged = { throw AssertionError("Not expected") },
            )

            return AddPaymentMethodInteractor.State(
                selectedPaymentMethodCode = paymentMethodCode,
                supportedPaymentMethods = metadata.sortedSupportedPaymentMethods(),
                arguments = formArguments,
                formElements = metadata.formElementsForCode(
                    code = paymentMethodCode,
                    uiDefinitionFactoryArgumentsFactory = uiDefinitionArgumentsFactory,
                ) ?: mock(),
                paymentSelection = null,
                processing = false,
                usBankAccountFormArguments = mock(),
            )
        }
    }
}
