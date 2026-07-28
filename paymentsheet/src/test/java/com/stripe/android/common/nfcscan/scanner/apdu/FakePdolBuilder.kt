package com.stripe.android.common.nfcscan.scanner.apdu

import app.cash.turbine.Turbine
import com.stripe.android.common.nfcscan.scanner.apdu.pdol.PdolBuilder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory

internal class FakePdolBuilder(
    private val pdolData: ByteArray = byteArrayOf(),
) : PdolBuilder {
    val fromTemplateCalls = Turbine<FromTemplateCall>()

    override fun fromTemplate(
        paymentMethodMetadata: PaymentMethodMetadata,
        template: ByteArray,
    ): ByteArray {
        fromTemplateCalls.add(
            FromTemplateCall(
                paymentMethodMetadata = paymentMethodMetadata,
                template = template,
            ),
        )
        return pdolData
    }

    fun ensureAllEventsConsumed() {
        fromTemplateCalls.ensureAllEventsConsumed()
    }

    data class FromTemplateCall(
        val paymentMethodMetadata: PaymentMethodMetadata,
        val template: ByteArray,
    )
}

internal fun defaultPaymentMethodMetadata(): PaymentMethodMetadata {
    return PaymentMethodMetadataFactory.create()
}
