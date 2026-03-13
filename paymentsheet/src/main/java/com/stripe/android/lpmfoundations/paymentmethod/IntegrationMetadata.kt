package com.stripe.android.lpmfoundations.paymentmethod

import android.os.Parcelable
import com.stripe.android.paymentsheet.PaymentSheet.IntentConfiguration
import kotlinx.parcelize.Parcelize

internal sealed class IntegrationMetadata : Parcelable {
    @Parcelize
    data class IntentFirst(
        val clientSecret: String,
    ) : IntegrationMetadata()

    sealed class DeferredIntent : IntegrationMetadata() {
        abstract val intentConfiguration: IntentConfiguration

        @Parcelize
        data class WithPaymentMethod(
            override val intentConfiguration: IntentConfiguration,
        ) : DeferredIntent()

        @Parcelize
        data class WithSharedPaymentToken(
            override val intentConfiguration: IntentConfiguration,
        ) : DeferredIntent()

        @Parcelize
        data class WithConfirmationToken(
            override val intentConfiguration: IntentConfiguration,
        ) : DeferredIntent()
    }

    // CustomerSheet doesn't really fit the bill of any of the other integrations, so making it's own, even though it's
    // not ideal.
    @Parcelize
    data class CustomerSheet(
        val attachmentStyle: AttachmentStyle,
    ) : IntegrationMetadata() {
        enum class AttachmentStyle {
            SetupIntent,
            CreateAttach
        }
    }

    @Parcelize
    object CryptoOnramp : IntegrationMetadata()

    @Parcelize
    data class CheckoutSession(val id: String) : IntegrationMetadata()
}
