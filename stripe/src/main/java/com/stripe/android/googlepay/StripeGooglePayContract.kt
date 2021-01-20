package com.stripe.android.googlepay

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import androidx.activity.result.contract.ActivityResultContract
import com.google.android.gms.common.api.Status
import com.stripe.android.PaymentIntentResult
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.ShippingInformation
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import java.lang.IllegalStateException

internal class StripeGooglePayContract :
    ActivityResultContract<StripeGooglePayContract.Args, StripeGooglePayContract.Result>() {

    override fun createIntent(
        context: Context,
        input: Args?
    ): Intent {
        return Intent(context, StripeGooglePayActivity::class.java)
            .putExtra(ActivityStarter.Args.EXTRA, input)
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): Result {
        return Result.fromIntent(intent)
    }

    @Parcelize
    data class Args(
        internal var environment: StripeGooglePayEnvironment,

        internal var paymentIntent: PaymentIntent,

        /**
         * ISO 3166-1 alpha-2 country code where the transaction is processed.
         */
        internal var countryCode: String,

        /**
         * Set to true to request an email address.
         */
        internal var isEmailRequired: Boolean = false,

        internal var merchantName: String? = null
    ) : ActivityStarter.Args {

        companion object {
            @JvmSynthetic
            internal fun create(intent: Intent): Args {
                return requireNotNull(intent.getParcelableExtra(ActivityStarter.Args.EXTRA))
            }
        }
    }

    sealed class Result : ActivityStarter.Result {
        override fun toBundle(): Bundle {
            return Bundle().also {
                it.putParcelable(ActivityStarter.Result.EXTRA, this)
            }
        }

        @Parcelize
        class Error(
            val exception: Throwable,
            val googlePayStatus: Status? = null,
            val paymentMethod: PaymentMethod? = null,
            val shippingInformation: ShippingInformation? = null
        ) : Result() {
            companion object : Parceler<Error> {
                override fun create(parcel: Parcel): Error {
                    return Error(
                        exception = parcel.readSerializable() as Throwable,
                        googlePayStatus = parcel.readParcelable(Status::class.java.classLoader)
                    )
                }

                override fun Error.write(parcel: Parcel, flags: Int) {
                    parcel.writeSerializable(exception)
                    parcel.writeParcelable(googlePayStatus, flags)
                }
            }
        }

        @Parcelize
        class PaymentIntent(
            val paymentIntentResult: PaymentIntentResult
        ) : Result()

        @Parcelize
        object Canceled : Result()

        @Parcelize
        object Unavailable : Result()

        companion object {
            /**
             * @return the [Result] object from the given `Intent`
             */
            @JvmStatic
            fun fromIntent(intent: Intent?): Result {
                val result = intent?.getParcelableExtra<Result>(ActivityStarter.Result.EXTRA)
                return result ?: Error(
                    exception = IllegalStateException(
                        "Error while processing result from Google Pay."
                    )
                )
            }
        }
    }
}
