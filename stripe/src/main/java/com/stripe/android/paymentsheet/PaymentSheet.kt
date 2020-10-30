package com.stripe.android.paymentsheet

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.core.os.bundleOf
import com.stripe.android.model.PaymentIntent
import com.stripe.android.view.ActivityStarter
import com.stripe.android.view.AddPaymentMethodActivityStarter.Result
import kotlinx.android.parcel.Parcelize

internal class PaymentSheet internal constructor(
    private val args: PaymentSheetActivityStarter.Args
) {
    /**
     * Create PaymentSheet with a Customer
     */
    constructor(
        clientSecret: String,
        ephemeralKey: String,
        customerId: String
    ) : this(
        PaymentSheetActivityStarter.Args.Default(
            clientSecret,
            ephemeralKey,
            customerId
        )
    )

    /**
     * Create PaymentSheet without a Customer
     */
    constructor(
        clientSecret: String
    ) : this(
        PaymentSheetActivityStarter.Args.Guest(clientSecret)
    )

    fun confirm(activity: ComponentActivity, callback: (CompletionStatus) -> Unit) {
        // TODO: Use ActivityResultContract and call callback instead of using onActivityResult
        // when androidx.activity:1.2.0 hits GA
        PaymentSheetActivityStarter(activity)
            .startForResult(args)
    }

    internal sealed class CompletionStatus : Parcelable {
        @Parcelize
        data class Succeeded(
            val paymentIntent: PaymentIntent
        ) : CompletionStatus()

        @Parcelize
        data class Failed(
            val error: Throwable,
            val paymentIntent: PaymentIntent?
        ) : CompletionStatus()

        @Parcelize
        data class Cancelled(
            val mostRecentError: Throwable?,
            val paymentIntent: PaymentIntent?
        ) : CompletionStatus()
    }

    @Parcelize
    internal data class Result(val status: CompletionStatus) : ActivityStarter.Result {
        override fun toBundle(): Bundle {
            return bundleOf(ActivityStarter.Result.EXTRA to this)
        }

        companion object {
            @JvmStatic
            fun fromIntent(intent: Intent?): Result? {
                return intent?.getParcelableExtra(ActivityStarter.Result.EXTRA)
            }
        }
    }
}
