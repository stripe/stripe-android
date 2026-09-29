package com.stripe.android.paymentelement.embedded.content

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded

internal object EmbeddedContentHelperStateFactory {
    fun create(
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        embeddedAppearance: Embedded = Embedded(Embedded.RowStyle.FlatWithRadio.default),
        embeddedViewDisplaysMandateText: Boolean = true,
        configuration: EmbeddedPaymentElement.Configuration =
            EmbeddedPaymentElement.Configuration.Builder("Example, Inc.")
                .appearance(PaymentSheet.Appearance(embeddedAppearance = embeddedAppearance))
                .build(),
    ): EmbeddedContentHelperStateHolder.State = EmbeddedContentHelperStateHolder.State(
        paymentMethodMetadata = paymentMethodMetadata,
        embeddedViewDisplaysMandateText = embeddedViewDisplaysMandateText,
        configuration = configuration,
    )
}
