package com.stripe.android.financialconnections.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class UserFacingEventResponse(
    @SerialName("type")
    val type: String,
    @SerialName("institution_selected")
    val institutionSelected: InstitutionSelected? = null,
    @SerialName("error")
    val error: Error? = null,
    @SerialName("success")
    val success: Success? = null
) {
    @Serializable
    data class InstitutionSelected(
        @SerialName("institution_name")
        val institutionName: String
    )

    @Serializable
    data class Error(
        @SerialName("error_code")
        val errorCode: String
    )

    @Serializable
    data class Success(
        @SerialName("manual_entry")
        val manualEntry: Boolean
    )
}
