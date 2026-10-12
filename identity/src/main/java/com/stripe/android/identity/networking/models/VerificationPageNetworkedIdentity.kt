package com.stripe.android.identity.networking.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// #TODO - Networked Identity: Confirm the draft v8 bootstrap contract before production routing.
@Serializable
@Parcelize
internal data class VerificationPageNetworkedIdentity(
    @SerialName("save_available")
    val saveAvailable: Boolean? = null,
    @SerialName("reuse_available")
    val reuseAvailable: Boolean? = null,
    @SerialName("lookup_email")
    val lookupEmail: String? = null,
    /** Prefills Link sign-up only; never used to look up or select a Link account. */
    @SerialName("phone_number_autofill_hint")
    val phoneNumberAutofillHint: String? = null,
    val state: State? = null,
) : Parcelable {
    val route: NetworkedIdentityRoute
        get() = when {
            state?.skipped != false || state.consented == null -> NetworkedIdentityRoute.OrdinaryIdentity
            state.consented -> when (state.direction) {
                "consumer_to_merchant" -> NetworkedIdentityRoute.ResumeReuse
                "merchant_to_consumer" -> NetworkedIdentityRoute.ResumeSave
                else -> NetworkedIdentityRoute.OrdinaryIdentity
            }
            state.direction != null -> NetworkedIdentityRoute.OrdinaryIdentity
            reuseAvailable == true -> NetworkedIdentityRoute.Reuse
            saveAvailable == true -> NetworkedIdentityRoute.Save
            else -> NetworkedIdentityRoute.OrdinaryIdentity
        }

    override fun toString(): String = "VerificationPageNetworkedIdentity([redacted])"

    @Serializable
    @Parcelize
    data class State(
        val consented: Boolean? = null,
        val skipped: Boolean? = null,
        val direction: String? = null,
    ) : Parcelable
}

internal enum class NetworkedIdentityRoute {
    OrdinaryIdentity,
    Reuse,
    Save,
    ResumeReuse,
    ResumeSave
}

internal val VerificationPage.networkedIdentityRoute: NetworkedIdentityRoute
    get() = networkedIdentity?.route ?: NetworkedIdentityRoute.OrdinaryIdentity
