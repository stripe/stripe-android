package com.stripe.android.paymentelement.taptoadd

import com.stripe.android.paymentelement.CreateCardPresentSetupIntentCallback
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.TapToAddPreview
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.PaymentSheet

@OptIn(TapToAddPreview::class)
internal class TapToAddIntegrationBuilder {
    private var _createIntentCallback: CreateIntentCallback? = null
    private val createIntentCallback: CreateIntentCallback
        get() = _createIntentCallback ?: throw IllegalStateException("No create intent callback!")

    private var _createCardPresentSetupIntentCallback: CreateCardPresentSetupIntentCallback? = null
    private val createCardPresentSetupIntentCallback: CreateCardPresentSetupIntentCallback
        get() = _createCardPresentSetupIntentCallback
            ?: throw IllegalStateException("No create card present setup intent callback!")

    fun createCardPresentSetupIntentCallback(
        createCardPresentSetupIntentCallback: CreateCardPresentSetupIntentCallback?
    ) = apply {
        this._createCardPresentSetupIntentCallback = createCardPresentSetupIntentCallback
    }

    fun createIntentCallback(createIntentCallback: CreateIntentCallback?) = apply {
        this._createIntentCallback = createIntentCallback
    }

    fun applyToPaymentSheetBuilder(builder: PaymentSheet.Builder) {
        builder
            .createIntentCallback(createIntentCallback)
            .createCardPresentSetupIntentCallback(createCardPresentSetupIntentCallback)
    }

    fun applyToFlowControllerBuilder(builder: PaymentSheet.FlowController.Builder) {
        builder
            .createIntentCallback(createIntentCallback)
            .createCardPresentSetupIntentCallback(createCardPresentSetupIntentCallback)
    }

    fun applyToEmbeddedBuilder(builder: EmbeddedPaymentElement.Builder) {
        builder
            .createCardPresentSetupIntentCallback(createCardPresentSetupIntentCallback)
    }
}
