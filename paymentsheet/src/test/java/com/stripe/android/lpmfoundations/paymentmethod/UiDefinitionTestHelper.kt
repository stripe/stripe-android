package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.uicore.elements.FormElement

internal fun PaymentMethodDefinition.formElements(
    metadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
    paymentMethodCreateParams: PaymentMethodCreateParams? = null,
    paymentMethodExtraParams: PaymentMethodExtraParams? = null,
    linkConfigurationCoordinator: LinkConfigurationCoordinator? = null,
): List<FormElement> {
    return requireNotNull(
        metadata.formElementsForCode(
            code = type.code,
            uiDefinitionFactoryArgumentsFactory = TestUiDefinitionFactoryArgumentsFactory.create(
                paymentMethodCreateParams = paymentMethodCreateParams,
                paymentMethodExtraParams = paymentMethodExtraParams,
                linkConfigurationCoordinator = linkConfigurationCoordinator,
            )
        )
    )
}
