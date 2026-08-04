package com.stripe.android.paymentelement.embedded

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.content.EmbeddedSheetLauncher
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState

internal class FakeEmbeddedSheetLauncher : EmbeddedSheetLauncher {
    override fun launchForm(
        code: String,
        paymentMethodMetadata: PaymentMethodMetadata,
        configuration: EmbeddedPaymentElement.Configuration?,
        customerState: CustomerState?,
        promotion: PaymentMethodMessagePromotion?,
    ) {
        error("Not expected.")
    }

    override fun launchManage(
        paymentMethodMetadata: PaymentMethodMetadata,
        customerState: CustomerState,
        selection: PaymentSelection?,
        configuration: EmbeddedPaymentElement.Configuration?,
    ) {
        error("Not expected.")
    }

    override fun launchPaymentOptions(
        paymentMethodMetadata: PaymentMethodMetadata,
        customerState: CustomerState?,
        selection: PaymentSelection?,
        configuration: EmbeddedPaymentElement.Configuration?,
    ) {
        error("Not expected.")
    }
}
