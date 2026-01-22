package com.stripe.android.paymentelement.confirmation.utils

import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.StripeIntent
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

internal fun IntegrationMetadata.toInitializationMode(
    intent: StripeIntent
): PaymentElementLoader.InitializationMode? {
    return when (this) {
        is IntegrationMetadata.IntentFirst -> {
            if (intent is PaymentIntent) {
                PaymentElementLoader.InitializationMode.PaymentIntent(clientSecret)
            } else {
                PaymentElementLoader.InitializationMode.SetupIntent(clientSecret)
            }
        }
        is IntegrationMetadata.DeferredIntent -> {
            PaymentElementLoader.InitializationMode.DeferredIntent(intentConfiguration)
        }
        is IntegrationMetadata.CheckoutSession -> {
            PaymentElementLoader.InitializationMode.CheckoutSession(clientSecret)
        }
        is IntegrationMetadata.CryptoOnramp -> {
            PaymentElementLoader.InitializationMode.CryptoOnramp
        }
        is IntegrationMetadata.CustomerSheet -> null
    }
}
