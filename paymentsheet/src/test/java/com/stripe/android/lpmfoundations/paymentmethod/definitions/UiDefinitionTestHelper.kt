package com.stripe.android.lpmfoundations.paymentmethod.definitions

import androidx.compose.runtime.Composable
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.ui.core.FormUI
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor

@Composable
internal fun PaymentMethodDefinition.CreateFormUi(
    metadata: PaymentMethodMetadata,
    paymentMethodCreateParams: PaymentMethodCreateParams? = null,
    paymentMethodExtraParams: PaymentMethodExtraParams? = null,
    initialLinkUserInput: UserInput? = null,
    linkConfigurationCoordinator: LinkConfigurationCoordinator? = null,
    autocompleteAddressInteractorFactory: AutocompleteAddressInteractor.Factory? = null
) {
    val formElements = formElements(
        metadata = metadata,
        paymentMethodCreateParams = paymentMethodCreateParams,
        paymentMethodExtraParams = paymentMethodExtraParams,
        initialLinkUserInput = initialLinkUserInput,
        linkConfigurationCoordinator = linkConfigurationCoordinator,
        autocompleteAddressInteractorFactory = autocompleteAddressInteractorFactory,
    )
    FormUI(
        hiddenIdentifiers = emptySet(),
        enabled = true,
        elements = formElements,
        lastTextFieldIdentifier = null,
    )
}
