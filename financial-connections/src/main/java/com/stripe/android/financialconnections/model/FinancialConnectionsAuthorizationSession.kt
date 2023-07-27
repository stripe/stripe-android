package com.stripe.android.financialconnections.model

import android.os.Parcelable
import com.stripe.android.financialconnections.domain.Display
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 *
 *
 * @param id
 * @param nextPane
 * @param flow
 * @param institutionSkipAccountSelection
 * @param showPartnerDisclosure
 * @param skipAccountSelection
 * @param url
 * @param urlQrCode
 */
@Serializable
@Parcelize
@Suppress("ConstructorParameterNaming")
internal data class FinancialConnectionsAuthorizationSession(

    @SerialName(value = "id")
    val id: String,

    @SerialName(value = "next_pane")
    val nextPane: FinancialConnectionsSessionManifest.Pane,

    @SerialName(value = "flow")
    val flow: String? = null,

    @SerialName(value = "institution_skip_account_selection")
    val institutionSkipAccountSelection: Boolean? = null,

    @SerialName(value = "show_partner_disclosure")
    val showPartnerDisclosure: Boolean? = null,

    @SerialName(value = "skip_account_selection")
    val skipAccountSelection: Boolean? = null,

    @SerialName(value = "url")
    val url: String? = null,

    @SerialName(value = "url_qr_code")
    val urlQrCode: String? = null,

    @SerialName(value = "is_oauth")
    private val _isOAuth: Boolean? = false,

    @SerialName(value = "display")
    val display: Display? = null
) : Parcelable {

    val isOAuth: Boolean
        get() = _isOAuth ?: false
}
