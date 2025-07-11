package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.FormElement

internal fun PaymentMethodDefinition.formElements(
    metadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
    paymentMethodCreateParams: PaymentMethodCreateParams? = null,
    paymentMethodOptionsParams: PaymentMethodOptionsParams? = null,
    paymentMethodExtraParams: PaymentMethodExtraParams? = null,
    initialLinkUserInput: UserInput? = null,
    linkConfigurationCoordinator: LinkConfigurationCoordinator? = null,
    setAsDefaultMatchesSaveForFutureUse: Boolean = false,
    autocompleteAddressInteractorFactory: AutocompleteAddressInteractor.Factory? = null,
): List<FormElement> {
    return requireNotNull(
        metadata.formElementsForCode(
            code = type.code,
            uiDefinitionFactoryArgumentsFactory = TestUiDefinitionFactoryArgumentsFactory.create(
                paymentMethodCreateParams = paymentMethodCreateParams,
                paymentMethodOptionsParams = paymentMethodOptionsParams,
                paymentMethodExtraParams = paymentMethodExtraParams,
                linkConfigurationCoordinator = linkConfigurationCoordinator,
                autocompleteAddressInteractorFactory = autocompleteAddressInteractorFactory,
                initialLinkUserInput = initialLinkUserInput,
                setAsDefaultMatchesSaveForFutureUse = setAsDefaultMatchesSaveForFutureUse,
            )
        )
    )
}
