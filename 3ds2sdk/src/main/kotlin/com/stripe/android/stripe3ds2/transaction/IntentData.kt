package com.stripe.android.stripe3ds2.transaction

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class IntentData(
    /**
     * The client_secret of a PaymentIntent or SetupIntent.
     */
    val clientSecret: String,

    /**
     * The 3DS2 Source Id for this transaction.
     */
    val sourceId: String,

    /**
     * The publishable key.
     */
    val publishableKey: String,

    /**
     * Optional connected account id.
     */
    val accountId: String? = null
) : Parcelable {

    internal companion object {
        val EMPTY = IntentData("", "", "", null)
    }
}
