package com.stripe.android.paymentelement.embedded

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.BundleCompat
import com.stripe.android.checkout.analytics.CheckoutConfigurationAnalytics
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class EmbeddedActivityArgs(
    val paymentMethodMetadata: PaymentMethodMetadata,
    val configuration: EmbeddedPaymentElement.Configuration,
    val productUsage: Set<String>,
    val paymentElementCallbackIdentifier: String,
    val statusBarColor: Int?,
    val selection: PaymentSelection?,
    val previousNewSelections: Bundle,
    val customerState: CustomerState?,
    val linkAccountInfo: LinkAccountUpdate.Value,
    val promotions: List<PaymentMethodMessagePromotion>,
    val launchMode: EmbeddedLaunchMode,
    val presentationState: PresentationState,
    // Configuration analytics for the Checkout operations the sheet reports; null outside Checkout.
    val checkoutConfigurationAnalytics: CheckoutConfigurationAnalytics?,
) : Parcelable {
    internal enum class PresentationState {
        Loading,
        Ready,
    }

    companion object {
        internal const val EXTRA_ARGS: String = "extra_activity_args"

        fun fromIntent(intent: Intent): EmbeddedActivityArgs? {
            return intent.extras?.let { bundle ->
                BundleCompat.getParcelable(bundle, EXTRA_ARGS, EmbeddedActivityArgs::class.java)
            }
        }
    }
}
