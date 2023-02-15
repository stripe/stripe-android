package com.stripe.android.paymentsheet

import android.os.Parcelable
import com.stripe.android.paymentsheet.model.ClientSecret
import kotlinx.parcelize.Parcelize

internal sealed interface PaymentSheetOrigin : Parcelable {

    fun validate()

    @Parcelize
    data class Intent(val clientSecret: ClientSecret) : PaymentSheetOrigin {

        override fun validate() {
            clientSecret.validate()
        }
    }

    @Parcelize
    data class DeferredIntent(val options: com.stripe.android.model.DeferredIntent) : PaymentSheetOrigin {

        override fun validate() {
            // Nothing to do here
        }
    }
}

internal val PaymentSheetOrigin.clientSecret: ClientSecret?
    get() = (this as? PaymentSheetOrigin.Intent)?.clientSecret
