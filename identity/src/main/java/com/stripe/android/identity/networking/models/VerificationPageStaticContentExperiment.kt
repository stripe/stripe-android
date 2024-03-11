package com.stripe.android.identity.networking.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
internal data class VerificationPageStaticContentExperiment(
    @SerialName("experiment_name")
    val experimentName: String,
    @SerialName("event_name")
    val eventName: String,
    @SerialName("event_metadata")
    val eventMetadata: Map<String, String>
) : Parcelable
