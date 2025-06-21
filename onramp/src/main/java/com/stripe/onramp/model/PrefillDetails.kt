package com.stripe.onramp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents prefill details for Link signup.
 */
@Parcelize
data class PrefillDetails(
    val phoneNumber: String? = null,
    val country: String? = null,
    val fullName: String? = null
) : Parcelable