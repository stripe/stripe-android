package com.stripe.android.identity.networking.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
internal data class VerificationPageNetworkingData(
    // #TODO - Networked Identity: Merchant email needs the SDK verification_page field contract
    // and an email-screen design decision; do not assume the public provided_details.email shape.
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
) : Parcelable {
    val route: NetworkedIdentityRoute
        get() = when {
            viCompatible != true || viMerchantEligible != true || viMerchantEnabled != true ->
                NetworkedIdentityRoute.OrdinaryIdentity
            consumerReuseEnabled == true && consumerReusePossible == true -> NetworkedIdentityRoute.Reuse
            consumerSaveEnabled == true -> NetworkedIdentityRoute.Save
            else -> NetworkedIdentityRoute.OrdinaryIdentity
        }
}

internal enum class NetworkedIdentityRoute {
    OrdinaryIdentity,
    Reuse,
    Save
}

/** Eligibility alone never enables an unfinished merchant-facing flow. */
internal val VerificationPage.networkedIdentityRoute: NetworkedIdentityRoute
    get() = networkingData?.features?.route ?: NetworkedIdentityRoute.OrdinaryIdentity
