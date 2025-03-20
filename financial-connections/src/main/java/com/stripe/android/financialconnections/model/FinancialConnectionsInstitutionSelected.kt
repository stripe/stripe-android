package com.stripe.android.financialconnections.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
internal data class FinancialConnectionsInstitutionSelected(
    @SerialName("manifest")
    val manifest: FinancialConnectionsSessionManifest,
    @SerialName("text")
    val text: TextUpdate? = null,
) : Parcelable
