package com.stripe.android.financialconnections.model

import com.stripe.android.financialconnections.model.LinkAccountSessionPaymentAccount.MicrodepositVerificationMethod.UNKNOWN
import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class LinkAccountSessionPaymentAccount(

    @SerialName(value = "id")
    @Required
    val id: String,

    @SerialName(value = "microdeposit_verification_method")
    val microdepositVerificationMethod: MicrodepositVerificationMethod = UNKNOWN,

    @SerialName(value = "networking_successful")
    val networkingSuccessful: Boolean? = null,

    @SerialName(value = "next_pane")
    val nextPane: FinancialConnectionsSessionManifest.Pane? = null

) {

    @Serializable
    enum class MicrodepositVerificationMethod(val value: String) {
        @SerialName(value = "amounts")
        AMOUNTS("amounts"),

        @SerialName(value = "descriptor_code")
        DESCRIPTOR_CODE("descriptor_code"),

        @SerialName(value = "unknown")
        UNKNOWN("unknown")
    }
}
