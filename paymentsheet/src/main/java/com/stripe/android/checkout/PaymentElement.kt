package com.stripe.android.checkout

import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import com.stripe.android.paymentelement.CheckoutSessionPreview
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PaymentElement @Inject internal constructor() {

    @Composable
    fun PaymentOptionsContent() {
        TODO("Not yet implemented")
    }

    fun presentPaymentOptions() {
        TODO("Not yet implemented")
    }

    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Configuration {
        private var embeddedViewDisplaysMandateText: Boolean = true
        private var billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration =
            BillingDetailsCollectionConfiguration()

        fun embeddedViewDisplaysMandateText(
            embeddedViewDisplaysMandateText: Boolean
        ): Configuration = apply {
            this.embeddedViewDisplaysMandateText = embeddedViewDisplaysMandateText
        }

        fun billingDetailsCollectionConfiguration(
            billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration
        ): Configuration = apply {
            this.billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration
        }

        @Parcelize
        internal data class State(
            val embeddedViewDisplaysMandateText: Boolean,
            val billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration.State,
        ) : Parcelable

        internal fun build(): State = State(
            embeddedViewDisplaysMandateText = embeddedViewDisplaysMandateText,
            billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration.build(),
        )
    }
}
