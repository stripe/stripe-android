package com.stripe.android.googlepay

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import androidx.fragment.app.Fragment
import com.google.android.gms.common.api.Status
import com.stripe.android.PaymentIntentResult
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.ShippingInformation
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize

internal class StripeGooglePayLauncher : ActivityStarter<StripeGooglePayActivity, StripeGooglePayLauncher.Args> {
    constructor(activity: Activity) : super(
        activity,
        StripeGooglePayActivity::class.java,
        REQUEST_CODE,
        intentFlags = Intent.FLAG_ACTIVITY_NO_ANIMATION
    )

    constructor(fragment: Fragment) : super(
        fragment,
        StripeGooglePayActivity::class.java,
        REQUEST_CODE,
        intentFlags = Intent.FLAG_ACTIVITY_NO_ANIMATION
    )

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
            fun fromIntent(intent: Intent): Result? {
                return intent.getParcelableExtra(ActivityStarter.Result.EXTRA)
            }
        }
    }

    companion object {
        const val REQUEST_CODE: Int = 9004
    }
}
