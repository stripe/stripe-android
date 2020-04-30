package com.stripe.android

import android.os.Parcel
import android.os.Parcelable
import com.stripe.android.model.SetupIntent

/**
 * A model representing the result of a [SetupIntent] confirmation via [Stripe.confirmSetupIntent]
 * or handling of next actions via [Stripe.handleNextActionForSetupIntent].
 */
class SetupIntentResult internal constructor(
    setupIntent: SetupIntent,
    @Outcome outcome: Int = 0
) : StripeIntentResult<SetupIntent>(setupIntent, outcome) {
    internal constructor(parcel: Parcel) : this(
        setupIntent = requireNotNull(
            parcel.readParcelable<SetupIntent>(SetupIntent::class.java.classLoader)
        ),
        outcome = parcel.readInt()
    )

    companion object CREATOR : Parcelable.Creator<SetupIntentResult> {
        override fun createFromParcel(parcel: Parcel): SetupIntentResult {
            return SetupIntentResult(parcel)
        }

        override fun newArray(size: Int): Array<SetupIntentResult?> {
            return arrayOfNulls<SetupIntentResult?>(size)
        }
    }
}
