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
import com.stripe.android.R
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
         * Total monetary value of the transaction.
         *
         * The value of this field is represented in the [smallest currency unit](https://stripe.com/docs/currencies#zero-decimal).
         * For example, when [currencyCode] is `"USD"`, a value of `100` represents 100 cents ($1.00).
         */
        internal var amount: Int?,

        /**
         * ISO 3166-1 alpha-2 country code where the transaction is processed.
         */
        internal var countryCode: String,

        /**
         * ISO 4217 alphabetic currency code.
         */
        internal var currencyCode: String,

        /**
         * Set to true to request an email address.
         */
        internal var isEmailRequired: Boolean = false,

        /**
         * Merchant name encoded as UTF-8.
         */
        internal var merchantName: String? = null,

        /**
         * A unique ID that identifies a transaction attempt. Merchants may use an existing ID or
         * generate a specific one for Google Pay transaction attempts. This field is required
         * when you send callbacks to the Google Transaction Events API.
         */
        internal var transactionId: String? = null
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
         * See [Args]
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

internal fun StripeGooglePayContract.Result.Error.getErrorResourceID(): Int? {
    return when (googlePayStatus) {
        Status.RESULT_SUCCESS -> null
        Status.RESULT_INTERNAL_ERROR, Status.RESULT_CANCELED, Status.RESULT_DEAD_CLIENT, null ->
            R.string.stripe_google_pay_error_internal
        else -> {
            when (googlePayStatus.statusCode) {
                CommonStatusCodes.API_NOT_CONNECTED, // "The client attempted to call a method from an API that failed to connect."
                CommonStatusCodes.CANCELED, // -> "The result was canceled either due to client disconnect or PendingResult.cancel()."
                CommonStatusCodes.DEVELOPER_ERROR, // -> "The application is misconfigured."
                CommonStatusCodes.ERROR, // -> "The operation failed with no more detailed information."
                CommonStatusCodes.INTERRUPTED, // -> "A blocking call was interrupted while waiting and did not run to completion."
                CommonStatusCodes.INVALID_ACCOUNT, // -> "The client attempted to connect to the service with an invalid account name specified."
                CommonStatusCodes.SERVICE_DISABLED, // -> "This constant is deprecated. This case handled during connection, not during API requests. No results should be returned with this status code."
                CommonStatusCodes.SERVICE_VERSION_UPDATE_REQUIRED, // -> " This constant is deprecated.This case handled during connection, not during API requests . No results should be returned with this status code."
                CommonStatusCodes.SUCCESS, // -> "The operation was successful.SUCCESS_CACHE The operation was successful, but was used the device's cache."
                CommonStatusCodes.INTERNAL_ERROR -> R.string.stripe_google_pay_error_internal
                CommonStatusCodes.RESOLUTION_REQUIRED -> R.string.stripe_google_pay_error_resolution_required
                CommonStatusCodes.NETWORK_ERROR -> R.string.stripe_failure_connection_error
                CommonStatusCodes.SIGN_IN_REQUIRED -> R.string.stripe_failure_reason_authentication
                CommonStatusCodes.TIMEOUT -> R.string.stripe_failure_reason_timed_out
                else -> {
                    R.string.stripe_google_pay_error_internal
                }
            }
        }
    }
}
