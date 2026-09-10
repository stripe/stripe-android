package com.stripe.android.paymentelement.embedded

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.BundleCompat
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.PaymentSheetContract
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class EmbeddedActivityArgs(
    val paymentMethodMetadata: PaymentMethodMetadata?,
    val configuration: EmbeddedPaymentElement.Configuration,
    val productUsage: Set<String>,
    val paymentElementCallbackIdentifier: String,
    val statusBarColor: Int?,
    val selection: PaymentSelection?,
    val previousNewSelections: Bundle,
    val customerState: CustomerState?,
    val promotions: List<PaymentMethodMessagePromotion>,
    val launchMode: EmbeddedLaunchMode,
    val activityConfiguration: ActivityConfiguration,
    val presentationState: PresentationState,
) : Parcelable {
    @Parcelize
    internal sealed class ActivityConfiguration : Parcelable {
        @Parcelize
        data object Embedded : ActivityConfiguration()

        @Parcelize
        data class PaymentSheet(
            val args: PaymentSheetContract.Args,
        ) : ActivityConfiguration()

        @Parcelize
        data class PaymentOptions(
            val initialSelection: PaymentSelection?,
            val initialLinkAccount: LinkAccountUpdate.Value,
            val productUsageTokens: Set<String>,
        ) : ActivityConfiguration()

        val eventReporterMode: EventReporter.Mode
            get() = when (this) {
                Embedded -> EventReporter.Mode.Embedded
                is PaymentOptions -> EventReporter.Mode.Custom
                is PaymentSheet -> EventReporter.Mode.Complete
            }

        val productUsage: Set<String>
            get() = when (this) {
                Embedded -> setOf("EmbeddedPaymentElement")
                is PaymentOptions -> productUsageTokens
                is PaymentSheet -> setOf("PaymentSheet")
            }

        val initialLinkAccountInfo: LinkAccountUpdate.Value
            get() = when (this) {
                is PaymentOptions -> initialLinkAccount
                Embedded,
                is PaymentSheet -> LinkAccountUpdate.Value(null)
            }
    }

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
