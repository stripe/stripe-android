package com.stripe.android.googlepay

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.ColorInt
import androidx.core.os.bundleOf
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
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

    /**
     * Args to start [StripeGooglePayActivity] and collect payment data. If successful, the
     * result will be returned through [Result.PaymentData].
     */
    @Parcelize
    data class Args(
        var paymentIntent: PaymentIntent,
        var config: GooglePayConfig,
        @ColorInt val statusBarColor: Int?
    ) : ActivityStarter.Args {

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

internal fun Status.getErrorMessage(): String? {
    return when (this) {
        Status.RESULT_SUCCESS -> null
        Status.RESULT_INTERNAL_ERROR, Status.RESULT_CANCELED, Status.RESULT_DEAD_CLIENT -> "An internal error occurred."
        else -> {
            when (this.statusCode) {
                CommonStatusCodes.API_NOT_CONNECTED, // "The client attempted to call a method from an API that failed to connect."
                CommonStatusCodes.CANCELED, // -> "The result was canceled either due to client disconnect or PendingResult.cancel()."
                CommonStatusCodes.DEVELOPER_ERROR, // -> "The application is misconfigured."
                CommonStatusCodes.ERROR, // -> "The operation failed with no more detailed information."
                CommonStatusCodes.INTERRUPTED, // -> "A blocking call was interrupted while waiting and did not run to completion."
                CommonStatusCodes.INVALID_ACCOUNT, // -> "The client attempted to connect to the service with an invalid account name specified."
                CommonStatusCodes.SERVICE_DISABLED, // -> "This constant is deprecated. This case handled during connection, not during API requests. No results should be returned with this status code."
                CommonStatusCodes.SERVICE_VERSION_UPDATE_REQUIRED, // -> " This constant is deprecated.This case handled during connection, not during API requests . No results should be returned with this status code."
                CommonStatusCodes.SUCCESS, // -> "The operation was successful.SUCCESS_CACHE The operation was successful, but was used the device's cache."
                CommonStatusCodes.INTERNAL_ERROR -> "An internal error occurred."
                CommonStatusCodes.RESOLUTION_REQUIRED -> "Completing the operation requires some form of resolution."
                CommonStatusCodes.NETWORK_ERROR -> "A network error occurred."
                CommonStatusCodes.SIGN_IN_REQUIRED -> "The client attempted to connect to the service but the user is not signed in."
                CommonStatusCodes.TIMEOUT -> "Timed out while awaiting the result."
                else -> {
                    "An internal error occurred."
                }
            }
        }
    }
}
