package com.stripe.android.googlepay

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.bundleOf
import com.google.android.gms.common.api.Status
import com.stripe.android.PaymentIntentResult
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.ShippingInformation
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize

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

    sealed class Args : ActivityStarter.Args {
        abstract var paymentIntent: PaymentIntent
        abstract var config: GooglePayConfig

        /**
         * Args to start [StripeGooglePayActivity] and collect payment data. If successful, the
         * result will be returned through [Result.PaymentData].
         */
        @Parcelize
        data class PaymentData(
            override var paymentIntent: PaymentIntent,
            override var config: GooglePayConfig
        ) : Args()

        /**
         * Args to start [StripeGooglePayActivity] and confirm the [paymentIntent] with the
         * selected payment data. If successful, the result will be returned through
         * [Result.PaymentIntent].
         */
        @Parcelize
        data class ConfirmPaymentIntent(
            override var paymentIntent: PaymentIntent,
            override var config: GooglePayConfig
        ) : Args()

        companion object {
            @JvmSynthetic
            internal fun create(intent: Intent): Args? {
                return intent.getParcelableExtra(ActivityStarter.Args.EXTRA)
            }
        }
    }

    @Parcelize
    data class GooglePayConfig(
        var environment: StripeGooglePayEnvironment,

        /**
         * ISO 3166-1 alpha-2 country code where the transaction is processed.
         */
        internal var countryCode: String,

        /**
         * Set to true to request an email address.
         */
        internal var isEmailRequired: Boolean = false,

        internal var merchantName: String? = null
    ) : Parcelable

    sealed class Result : ActivityStarter.Result {
        override fun toBundle(): Bundle {
            return bundleOf(ActivityStarter.Result.EXTRA to this)
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

        /**
         * See [Args.ConfirmPaymentIntent]
         */
        @Parcelize
        data class PaymentIntent(
            val paymentIntentResult: PaymentIntentResult
        ) : Result()

        /**
         * See [Args.PaymentData]
         */
        @Parcelize
        data class PaymentData(
            val paymentMethod: PaymentMethod,
            val shippingInformation: ShippingInformation?
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
