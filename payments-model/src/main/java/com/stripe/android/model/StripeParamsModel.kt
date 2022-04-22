package com.stripe.android.model

import android.os.Parcelable

/**
 * Model for a Stripe API object creation parameters
 */
interface StripeParamsModel : Parcelable {
    fun toParamMap(): Map<String, Any>
}
