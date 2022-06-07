package com.stripe.android.financialconnections.model

import android.os.Parcelable
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount.SupportedPaymentMethodTypes
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 *
 *
 * @param allowManualEntry
 * @param consentRequired
 * @param customManualEntryHandling
 * @param disableLinkMoreAccounts
 * @param id
 * @param instantVerificationDisabled
 * @param institutionSearchDisabled
 * @param livemode
 * @param manualEntryUsesMicrodeposits
 * @param mobileHandoffEnabled
 * @param nextPane
 * @param permissions
 * @param product
 * @param singleAccount
 * @param useSingleSortSearch
 * @param accountDisconnectionMethod
 * @param accountholderCustomerEmailAddress
 * @param accountholderIsLinkConsumer
 * @param activeAuthSession
 * @param activeInstitution
 * @param businessName
 * @param cancelUrl
 * @param connectPlatformName
 * @param connectedAccountName
 * @param hostedAuthUrl
 * @param initialInstitution
 * @param isEndUserFacing
 * @param isLinkWithStripe
 * @param isNetworkingUserFlow
 * @param isStripeDirect
 * @param modalCustomization
 * @param paymentMethodType
 * @param successUrl
 * @param theme
 */
@Suppress("MaxLineLength")
@Serializable
@Parcelize
internal data class FinancialConnectionsSessionManifest(

    @SerialName(value = "allow_manual_entry")
    val allowManualEntry: Boolean,

    @SerialName(value = "consent_required")
    val consentRequired: Boolean,

    @SerialName(value = "custom_manual_entry_handling")
    val customManualEntryHandling: Boolean,

    @SerialName(value = "disable_link_more_accounts")
    val disableLinkMoreAccounts: Boolean,

    @SerialName(value = "id")
    val id: String,

    @SerialName(value = "instant_verification_disabled")
    val instantVerificationDisabled: Boolean,

    @SerialName(value = "institution_search_disabled")
    val institutionSearchDisabled: Boolean,

    @SerialName(value = "livemode")
    val livemode: Boolean,

    @SerialName(value = "manual_entry_uses_microdeposits")
    val manualEntryUsesMicrodeposits: Boolean,

    @SerialName(value = "mobile_handoff_enabled")
    val mobileHandoffEnabled: Boolean,

    @SerialName(value = "next_pane")
    val nextPane: NextPane,

    @SerialName(value = "permissions")
    val permissions: List<FinancialConnectionsAccount.Permissions>,

    @SerialName(value = "product")
    val product: Product,

    @SerialName(value = "single_account")
    val singleAccount: Boolean,

    @SerialName(value = "use_single_sort_search")
    val useSingleSortSearch: Boolean,

    @SerialName(value = "account_disconnection_method")
    val accountDisconnectionMethod: AccountDisconnectionMethod? = null,

    @SerialName(value = "accountholder_customer_email_address")
    val accountholderCustomerEmailAddress: String? = null,

    @SerialName(value = "accountholder_is_link_consumer")
    val accountholderIsLinkConsumer: Boolean? = null,

    @SerialName(value = "active_auth_session")
    val activeAuthSession: FinancialConnectionsAuthorizationSession? = null,

    @SerialName(value = "active_institution")
    val activeInstitution: FinancialConnectionsInstitution? = null,

    @SerialName(value = "business_name")
    val businessName: String? = null,

    @SerialName(value = "cancel_url")
    val cancelUrl: String,

    @SerialName(value = "connect_platform_name")
    val connectPlatformName: String? = null,

    @SerialName(value = "connected_account_name")
    val connectedAccountName: String? = null,

    @SerialName(value = "hosted_auth_url")
    val hostedAuthUrl: String,

    @SerialName(value = "initial_institution")
    val initialInstitution: FinancialConnectionsInstitution? = null,

    @SerialName(value = "is_end_user_facing")
    val isEndUserFacing: Boolean? = null,

    @SerialName(value = "is_link_with_stripe")
    val isLinkWithStripe: Boolean? = null,

    @SerialName(value = "is_networking_user_flow")
    val isNetworkingUserFlow: Boolean? = null,

    @SerialName(value = "is_stripe_direct")
    val isStripeDirect: Boolean? = null,

    @SerialName(value = "modal_customization")
    val modalCustomization: Map<String, Boolean>? = null,

    @SerialName(value = "payment_method_type")
    val paymentMethodType: SupportedPaymentMethodTypes? = null,

    @SerialName(value = "success_url")
    val successUrl: String,

) : Parcelable {

    /**
     *
     *
     * Values: ACCOUNT_PICKER,ATTACH_LINKED_PAYMENT_ACCOUNT,AUTH_OPTIONS,CONSENT,INSTITUTION_PICKER,
     * LINK_CONSENT,LINK_LOGIN,MANUAL_ENTRY,MANUAL_ENTRY_SUCCESS,NETWORKING_LINK_LOGIN_WARMUP,
     * NETWORKING_LINK_SIGNUP_PANE,NETWORKING_LINK_VERIFICATION,PARTNER_AUTH,SUCCESS,UNEXPECTED_ERROR
     */
    @Serializable
    enum class NextPane(val value: String) {
        @SerialName(value = "account_picker")
        ACCOUNT_PICKER("account_picker"),

        @SerialName(value = "attach_linked_payment_account")
        ATTACH_LINKED_PAYMENT_ACCOUNT("attach_linked_payment_account"),

        @SerialName(value = "auth_options")
        AUTH_OPTIONS("auth_options"),

        @SerialName(value = "consent")
        CONSENT("consent"),

        @SerialName(value = "institution_picker")
        INSTITUTION_PICKER("institution_picker"),

        @SerialName(value = "link_consent")
        LINK_CONSENT("link_consent"),

        @SerialName(value = "link_login")
        LINK_LOGIN("link_login"),

        @SerialName(value = "manual_entry")
        MANUAL_ENTRY("manual_entry"),

        @SerialName(value = "manual_entry_success")
        MANUAL_ENTRY_SUCCESS("manual_entry_success"),

        @SerialName(value = "networking_link_login_warmup")
        NETWORKING_LINK_LOGIN_WARMUP("networking_link_login_warmup"),

        @SerialName(value = "networking_link_signup_pane")
        NETWORKING_LINK_SIGNUP_PANE("networking_link_signup_pane"),

        @SerialName(value = "networking_link_verification")
        NETWORKING_LINK_VERIFICATION("networking_link_verification"),

        @SerialName(value = "partner_auth")
        PARTNER_AUTH("partner_auth"),

        @SerialName(value = "success")
        SUCCESS("success"),

        @SerialName(value = "unexpected_error")
        UNEXPECTED_ERROR("unexpected_error");
    }

    /**
     *
     *
     * Values: BILLPAY,CANARY,CAPITAL,CAPITAL_HOSTED,DASHBOARD,DIRECT_ONBOARDING,DIRECT_SETTINGS,
     * EMERALD,EXPRESS_ONBOARDING,EXTERNAL_API,ISSUING,LCPM,LINK_WITH_NETWORKING,
     * OPAL,PAYMENT_FLOWS,RESERVE_APPEALS,STANDARD_ONBOARDING,STRIPE_CARD,SUPPORT_SITE
     */
    @Serializable
    enum class Product(val value: String) {
        @SerialName(value = "billpay")
        BILLPAY("billpay"),

        @SerialName(value = "canary")
        CANARY("canary"),

        @SerialName(value = "capital")
        CAPITAL("capital"),

        @SerialName(value = "capital_hosted")
        CAPITAL_HOSTED("capital_hosted"),

        @SerialName(value = "dashboard")
        DASHBOARD("dashboard"),

        @SerialName(value = "direct_onboarding")
        DIRECT_ONBOARDING("direct_onboarding"),

        @SerialName(value = "direct_settings")
        DIRECT_SETTINGS("direct_settings"),

        @SerialName(value = "emerald")
        EMERALD("emerald"),

        @SerialName(value = "express_onboarding")
        EXPRESS_ONBOARDING("express_onboarding"),

        @SerialName(value = "external_api")
        EXTERNAL_API("external_api"),

        @SerialName(value = "issuing")
        ISSUING("issuing"),

        @SerialName(value = "lcpm")
        LCPM("lcpm"),

        @SerialName(value = "link_with_networking")
        LINK_WITH_NETWORKING("link_with_networking"),

        @SerialName(value = "opal")
        OPAL("opal"),

        @SerialName(value = "payment_flows")
        PAYMENT_FLOWS("payment_flows"),

        @SerialName(value = "reserve_appeals")
        RESERVE_APPEALS("reserve_appeals"),

        @SerialName(value = "standard_onboarding")
        STANDARD_ONBOARDING("standard_onboarding"),

        @SerialName(value = "stripe_card")
        STRIPE_CARD("stripe_card"),

        @SerialName(value = "support_site")
        SUPPORT_SITE("support_site");
    }

    /**
     *
     *
     * Values: DASHBOARD,EMAIL,SUPPORT
     */
    @Serializable
    enum class AccountDisconnectionMethod(val value: String) {
        @SerialName(value = "dashboard")
        DASHBOARD("dashboard"),

        @SerialName(value = "email")
        EMAIL("email"),

        @SerialName(value = "support")
        SUPPORT("support");
    }

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
    data class FinancialConnectionsAuthorizationSession(

        @SerialName(value = "id")
        val id: String,

        @SerialName(value = "next_pane")
        val nextPane: NextPane,

        @SerialName(value = "flow")
        val flow: Flow? = null,

        @SerialName(value = "institution_skip_account_selection")
        val institutionSkipAccountSelection: Boolean? = null,

        @SerialName(value = "show_partner_disclosure")
        val showPartnerDisclosure: Boolean? = null,

        @SerialName(value = "skip_account_selection")
        val skipAccountSelection: Boolean? = null,

        @SerialName(value = "url")
        val url: String? = null,

        @SerialName(value = "url_qr_code")
        val urlQrCode: String? = null

    ) : Parcelable {

        /**
         *
         *
         * Values: FINICITY_CONNECT_V2_FIX,FINICITY_CONNECT_V2_LITE,FINICITY_CONNECT_V2_OAUTH,
         * FINICITY_CONNECT_V2_OAUTH_REDIRECT,FINICITY_CONNECT_V2_OAUTH_WEBVIEW,MX_CONNECT,
         * MX_OAUTH,MX_OAUTH_REDIRECT,MX_OAUTH_WEBVIEW,TESTMODE,TESTMODE_OAUTH,
         * TESTMODE_OAUTH_WEBVIEW,TRUELAYER_OAUTH,TRUELAYER_OAUTH_HANDOFF,
         * TRUELAYER_OAUTH_WEBVIEW,WELLS_FARGO,WELLS_FARGO_WEBVIEW
         */
        @Serializable
        enum class Flow(val value: String) {
            @SerialName(value = "finicity_connect_v2_fix")
            FINICITY_CONNECT_V2_FIX("finicity_connect_v2_fix"),

            @SerialName(value = "finicity_connect_v2_lite")
            FINICITY_CONNECT_V2_LITE("finicity_connect_v2_lite"),

            @SerialName(value = "finicity_connect_v2_oauth")
            FINICITY_CONNECT_V2_OAUTH("finicity_connect_v2_oauth"),

            @SerialName(value = "finicity_connect_v2_oauth_redirect")
            FINICITY_CONNECT_V2_OAUTH_REDIRECT("finicity_connect_v2_oauth_redirect"),

            @SerialName(value = "finicity_connect_v2_oauth_webview")
            FINICITY_CONNECT_V2_OAUTH_WEBVIEW("finicity_connect_v2_oauth_webview"),

            @SerialName(value = "mx_connect")
            MX_CONNECT("mx_connect"),

            @SerialName(value = "mx_oauth")
            MX_OAUTH("mx_oauth"),

            @SerialName(value = "mx_oauth_redirect")
            MX_OAUTH_REDIRECT("mx_oauth_redirect"),

            @SerialName(value = "mx_oauth_webview")
            MX_OAUTH_WEBVIEW("mx_oauth_webview"),

            @SerialName(value = "testmode")
            TESTMODE("testmode"),

            @SerialName(value = "testmode_oauth")
            TESTMODE_OAUTH("testmode_oauth"),

            @SerialName(value = "testmode_oauth_webview")
            TESTMODE_OAUTH_WEBVIEW("testmode_oauth_webview"),

            @SerialName(value = "truelayer_oauth")
            TRUELAYER_OAUTH("truelayer_oauth"),

            @SerialName(value = "truelayer_oauth_handoff")
            TRUELAYER_OAUTH_HANDOFF("truelayer_oauth_handoff"),

            @SerialName(value = "truelayer_oauth_webview")
            TRUELAYER_OAUTH_WEBVIEW("truelayer_oauth_webview"),

            @SerialName(value = "wells_fargo")
            WELLS_FARGO("wells_fargo"),

            @SerialName(value = "wells_fargo_webview")
            WELLS_FARGO_WEBVIEW("wells_fargo_webview");
        }
    }
}
