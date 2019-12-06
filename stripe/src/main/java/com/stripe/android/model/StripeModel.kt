package com.stripe.android.model

import android.os.Parcelable

/**
 * Model for a Stripe API object.
 */
interface StripeModel : Parcelable {
    override fun hashCode(): Int

    override fun equals(other: Any?): Boolean
}
