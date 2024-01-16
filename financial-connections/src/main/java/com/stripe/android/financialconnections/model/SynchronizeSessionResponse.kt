package com.stripe.android.financialconnections.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
internal data class SynchronizeSessionResponse(
    @SerialName("manifest")
    val manifest: FinancialConnectionsSessionManifest,
    @SerialName("text")
    val text: TextUpdate? = null,
    @SerialName("visual")
    val visual: VisualUpdate,
) : Parcelable

@Serializable
@Parcelize
internal data class VisualUpdate(
    // Indicates whether the logo should be removed from most panes
    @SerialName("reduced_branding")
    val reducedBranding: Boolean,
    @SerialName("reduce_manual_entry_prominence_in_errors")
    val reducedManualEntryProminenceInErrors: Boolean,
    @SerialName("merchant_logo")
    val merchantLogos: List<String>
) : Parcelable
