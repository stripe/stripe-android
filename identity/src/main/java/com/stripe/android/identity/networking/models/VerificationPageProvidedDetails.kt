package com.stripe.android.identity.networking.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/** Optional merchant details decoded using the current iOS SDK VerificationPage contract. */
@Serializable
@Parcelize
internal data class VerificationPageProvidedDetails(
    val email: String?
) : Parcelable {
    override fun toString(): String = "VerificationPageProvidedDetails([redacted])"
}
