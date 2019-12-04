package com.stripe.android.model

import kotlinx.android.parcel.Parcelize

/**
 * Model for a
 * [receiver](https://stripe.com/docs/api/sources/object#source_object-receiver) object
 * in the Sources API. Present if the [Source] is a receiver.
 */
@Parcelize
data class SourceReceiver internal constructor(
    val address: String?,
    val amountCharged: Long,
    val amountReceived: Long,
    val amountReturned: Long
) : StripeModel
