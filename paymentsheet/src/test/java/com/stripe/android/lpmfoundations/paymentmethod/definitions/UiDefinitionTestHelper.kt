package com.stripe.android.lpmfoundations.paymentmethod.definitions

import androidx.compose.runtime.Composable
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.ui.core.FormUI

@Composable
internal fun PaymentMethodDefinition.CreateFormUi(
    metadata: PaymentMethodMetadata,
    paymentMethodCreateParams: PaymentMethodCreateParams? = null,
    paymentMethodExtraParams: PaymentMethodExtraParams? = null,
    linkConfigurationCoordinator: LinkConfigurationCoordinator? = null,
) {
    val formElements = formElements(
        metadata = metadata,
        paymentMethodCreateParams = paymentMethodCreateParams,
        paymentMethodExtraParams = paymentMethodExtraParams,
        linkConfigurationCoordinator = linkConfigurationCoordinator,
    )
    FormUI(
        hiddenIdentifiers = emptySet(),
        enabled = true,
        elements = formElements,
        lastTextFieldIdentifier = null,
    )
}
