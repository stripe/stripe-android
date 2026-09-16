package com.stripe.android.identity.networking.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
internal data class VerificationPageNetworkingData(
    val features: VerificationPageNetworkingFeatures?
) : Parcelable

@Serializable
@Parcelize
internal data class VerificationPageNetworkingFeatures(
    @SerialName("vi_compatible")
    val viCompatible: Boolean?,
    @SerialName("vi_merchant_eligible")
    val viMerchantEligible: Boolean?,
    @SerialName("vi_merchant_enabled")
    val viMerchantEnabled: Boolean?,
    @SerialName("consumer_save_enabled")
    val consumerSaveEnabled: Boolean?,
    @SerialName("consumer_reuse_enabled")
    val consumerReuseEnabled: Boolean?,
    @SerialName("consumer_reuse_possible")
    val consumerReusePossible: Boolean?
) : Parcelable

internal enum class NetworkedIdentityRoute {
    OrdinaryIdentity,
    Reuse,
    Save,
    ResumeReuse,
    ResumeSave
}

/** Eligibility alone never enables an unfinished merchant-facing flow. */
internal val VerificationPage.networkedIdentityRoute: NetworkedIdentityRoute
    get() = networkedIdentity?.route ?: NetworkedIdentityRoute.OrdinaryIdentity
