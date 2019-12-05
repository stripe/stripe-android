package com.stripe.android.model

import android.os.Parcelable

/**
 * Model for a Stripe API object.
 */
abstract class StripeModel : Parcelable {
    abstract override fun hashCode(): Int

    abstract override fun equals(other: Any?): Boolean
}
