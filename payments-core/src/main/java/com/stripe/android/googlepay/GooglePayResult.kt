package com.stripe.android.googlepay

import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import androidx.core.os.bundleOf
import com.google.android.gms.common.api.Status
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.ShippingInformation
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize

internal sealed class GooglePayResult : ActivityStarter.Result {
    override fun toBundle(): Bundle {
        return bundleOf(ActivityStarter.Result.EXTRA to this)
    }

    @Parcelize
    data class Error(
        val exception: Throwable,
        val googlePayStatus: Status? = null,
        val paymentMethod: PaymentMethod? = null,
        val shippingInformation: ShippingInformation? = null
    ) : GooglePayResult() {
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
    ) : GooglePayResult()

    @Parcelize
    object Canceled : GooglePayResult()

    @Parcelize
    object Unavailable : GooglePayResult()

    companion object {
        /**
         * @return the [Result] object from the given `Intent`
         */
        @JvmStatic
        fun fromIntent(intent: Intent?): GooglePayResult {
            val result = intent?.getParcelableExtra<GooglePayResult>(ActivityStarter.Result.EXTRA)
            return result ?: Error(
                exception = IllegalStateException(
                    "Error while processing result from Google Pay."
                )
            )
        }
    }
}
