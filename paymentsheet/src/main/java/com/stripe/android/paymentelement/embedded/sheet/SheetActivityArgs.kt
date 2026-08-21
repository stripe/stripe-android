package com.stripe.android.paymentelement.embedded.sheet

import android.content.Intent
import android.os.Parcelable
import androidx.core.os.BundleCompat
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentsheet.PaymentOptionContract
import com.stripe.android.paymentsheet.PaymentSheetContract
import com.stripe.android.paymentsheet.analytics.EventReporter
import kotlinx.parcelize.Parcelize

/**
 * Internal envelope used by [EmbeddedSheetActivity] to host every sheet presentation surface.
 *
 * The individual contracts deliberately retain their input and output types. This envelope only
 * selects the runtime that renders the request inside the shared activity.
 */
@Parcelize
internal sealed class SheetActivityArgs : Parcelable {
    @Parcelize
    data class Embedded(val args: EmbeddedActivityArgs) : SheetActivityArgs()

    @Parcelize
    data class PaymentSheet(val args: PaymentSheetContract.Args) : SheetActivityArgs()

    @Parcelize
    data class PaymentOptions(val args: PaymentOptionContract.Args) : SheetActivityArgs()

    val eventReporterMode: EventReporter.Mode
        get() = when (this) {
            is Embedded -> EventReporter.Mode.Embedded
            is PaymentOptions -> EventReporter.Mode.Custom
            is PaymentSheet -> EventReporter.Mode.Complete
        }

    val productUsage: Set<String>
        get() = when (this) {
            is Embedded -> setOf("EmbeddedPaymentElement")
            is PaymentOptions -> args.productUsage
            is PaymentSheet -> setOf("PaymentSheet")
        }

    val initialLinkAccountInfo: LinkAccountUpdate.Value
        get() = when (this) {
            is PaymentOptions -> args.linkAccountInfo
            is Embedded,
            is PaymentSheet -> LinkAccountUpdate.Value(null)
        }

    companion object {
        const val EXTRA_ARGS: String = "com.stripe.android.paymentelement.embedded.sheet.extra_args"

        fun fromIntent(intent: Intent): SheetActivityArgs? {
            return intent.extras?.let { bundle ->
                BundleCompat.getParcelable(bundle, EXTRA_ARGS, SheetActivityArgs::class.java)
            }
        }
    }
}
