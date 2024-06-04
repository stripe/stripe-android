package com.stripe.android.identity.networking.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
internal data class VerificationPageRequirements(
    @SerialName("missing")
    val missing: List<Requirement>
) : Parcelable
