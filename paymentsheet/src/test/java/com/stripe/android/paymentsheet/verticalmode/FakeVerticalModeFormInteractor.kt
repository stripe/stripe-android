package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.forms.FormArgumentsFactory
import com.stripe.android.uicore.utils.stateFlowOf
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import kotlinx.coroutines.flow.StateFlow
import org.mockito.Mockito.mock

internal class FakeVerticalModeFormInteractor private constructor(
    initialState: VerticalModeFormInteractor.State,
    override val isLiveMode: Boolean = true,
) : VerticalModeFormInteractor {
    override val state: StateFlow<VerticalModeFormInteractor.State> = stateFlowOf(initialState)

    override fun handleViewAction(viewAction: VerticalModeFormInteractor.ViewAction) {
        // No op.
    }

    override fun close() {
        throw AssertionError("Not expected")
    }

    companion object {
        fun create(
            paymentMethodCode: PaymentMethodCode,
            metadata: PaymentMethodMetadata,
            isProcessing: Boolean = false,
        ): VerticalModeFormInteractor {
            val formArguments = FormArgumentsFactory.create(
                paymentMethodCode = paymentMethodCode,
                metadata = metadata,
            )
            val uiDefinitionArgumentsFactory = UiDefinitionFactory.Arguments.Factory.Default(
                cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
                linkConfigurationCoordinator = null,
                onLinkInlineSignupStateChanged = { throw AssertionError("Not expected") },
            )

            return FakeVerticalModeFormInteractor(
                initialState = VerticalModeFormInteractor.State(
                    selectedPaymentMethodCode = paymentMethodCode,
                    isProcessing = isProcessing,
                    usBankAccountFormArguments = mock(),
                    formArguments = formArguments,
                    formElements = metadata.formElementsForCode(
                        code = paymentMethodCode,
                        uiDefinitionFactoryArgumentsFactory = uiDefinitionArgumentsFactory,
                    )!!,
                    headerInformation = metadata.formHeaderInformationForCode(
                        code = paymentMethodCode,
                        customerHasSavedPaymentMethods = false,
                    ),
                ),
            )
        }
    }
}
