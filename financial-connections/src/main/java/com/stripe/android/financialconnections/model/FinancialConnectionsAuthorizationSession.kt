package com.stripe.android.financialconnections.model

import android.os.Parcelable
import com.stripe.android.core.model.serializers.EnumIgnoreUnknownSerializer
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

    @Serializable(with = Flow.Serializer::class)
    enum class Flow(val value: String) {
        @SerialName("direct")
        DIRECT("direct"),

        @SerialName("direct_webview")
        DIRECT_WEBVIEW("direct_webview"),

        @SerialName("finicity_connect_v2_fix")
        FINICITY_CONNECT_V2_FIX("finicity_connect_v2_fix"),

        @SerialName("finicity_connect_v2_lite")
        FINICITY_CONNECT_V2_LITE("finicity_connect_v2_lite"),

        @SerialName("finicity_connect_v2_oauth")
        FINICITY_CONNECT_V2_OAUTH("finicity_connect_v2_oauth"),

        @SerialName("finicity_connect_v2_oauth_redirect")
        FINICITY_CONNECT_V2_OAUTH_REDIRECT("finicity_connect_v2_oauth_redirect"),

        @SerialName("finicity_connect_v2_oauth_webview")
        FINICITY_CONNECT_V2_OAUTH_WEBVIEW("finicity_connect_v2_oauth_webview"),

        @SerialName("mx_connect")
        MX_CONNECT("mx_connect"),

        @SerialName("mx_oauth")
        MX_OAUTH("mx_oauth"),

        @SerialName("mx_oauth_app_to_app")
        MX_OAUTH_APP2APP("mx_oauth_app_to_app"),

        @SerialName("mx_oauth_redirect")
        MX_OAUTH_REDIRECT("mx_oauth_redirect"),

        @SerialName("mx_oauth_webview")
        MX_OAUTH_WEBVIEW("mx_oauth_webview"),

        @SerialName("testmode")
        TESTMODE("testmode"),

        @SerialName("testmode_oauth")
        TESTMODE_OAUTH("testmode_oauth"),

        @SerialName("testmode_oauth_webview")
        TESTMODE_OAUTH_WEBVIEW("testmode_oauth_webview"),

        @SerialName("truelayer_oauth")
        TRUELAYER_OAUTH("truelayer_oauth"),

        @SerialName("truelayer_oauth_handoff")
        TRUELAYER_OAUTH_HANDOFF("truelayer_oauth_handoff"),

        @SerialName("truelayer_oauth_webview")
        TRUELAYER_OAUTH_WEBVIEW("truelayer_oauth_webview"),

        @SerialName("wells_fargo")
        WELLS_FARGO("wells_fargo"),

        @SerialName("wells_fargo_webview")
        WELLS_FARGO_WEBVIEW("wells_fargo_webview"),

        @SerialName(value = "unknown")
        UNKNOWN("unknown");

        internal object Serializer :
            EnumIgnoreUnknownSerializer<Flow>(entries.toTypedArray(), UNKNOWN)
    }
}

@Parcelize
@Serializable
internal data class Display(
    @SerialName("text")
    val text: TextUpdate? = null
) : Parcelable
