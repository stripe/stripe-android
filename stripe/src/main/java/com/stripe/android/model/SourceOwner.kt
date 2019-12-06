package com.stripe.android.model

import kotlinx.android.parcel.Parcelize

/**
 * Model for a [owner](https://stripe.com/docs/api#source_object-owner) object
 * in the Source api.
 */
@Parcelize
data class SourceOwner internal constructor(
    val address: Address?,
    val email: String?,
    val name: String?,
    val phone: String?,
    val verifiedAddress: Address?,
    val verifiedEmail: String?,
    val verifiedName: String?,
    val verifiedPhone: String?
) : StripeModel
