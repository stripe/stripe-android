package com.stripe.android.paymentelement.confirmation.utils

import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.PaymentElementLoader

@OptIn(SharedPaymentTokenSessionPreview::class)
internal val PaymentElementLoader.InitializationMode.sellerBusinessName: String?
    get() {
        val deferredIntentInitializationMode =
            this as? PaymentElementLoader.InitializationMode.DeferredIntent ?: return null

        val intentBehavior =
            deferredIntentInitializationMode.intentConfiguration.intentBehavior as?
                PaymentSheet.IntentConfiguration.IntentBehavior.SharedPaymentToken ?: return null

        return intentBehavior.sellerDetails?.businessName
    }
