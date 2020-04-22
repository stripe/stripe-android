package com.stripe.android

import android.os.Parcel
import android.os.Parcelable
import com.stripe.android.model.PaymentIntent

class PaymentIntentResult internal constructor(
    paymentIntent: PaymentIntent,
    @Outcome outcome: Int = 0
) : StripeIntentResult<PaymentIntent>(paymentIntent, outcome) {
    internal constructor(parcel: Parcel) : this(
        paymentIntent = requireNotNull(
            parcel.readParcelable<PaymentIntent>(PaymentIntent::class.java.classLoader)
        ),
        outcome = parcel.readInt()
    )

    companion object CREATOR : Parcelable.Creator<PaymentIntentResult> {
        override fun createFromParcel(parcel: Parcel): PaymentIntentResult {
            return PaymentIntentResult(parcel)
        }

        override fun newArray(size: Int): Array<PaymentIntentResult?> {
            return arrayOfNulls<PaymentIntentResult?>(size)
        }
    }
}
