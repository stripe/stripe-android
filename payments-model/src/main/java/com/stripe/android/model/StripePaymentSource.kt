package com.stripe.android.model

import android.os.Parcelable

/**
 * Represents an object that has an ID field that can be used to create payments with Stripe.
 */
interface StripePaymentSource : Parcelable {
    val id: String?
}
