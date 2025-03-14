@file:SuppressWarnings("unused")

package com.stripe.android.financialconnections.model

import android.os.Parcelable
import com.stripe.android.core.model.serializers.EnumIgnoreUnknownSerializer
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
 * @param manualEntryMode
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
 */
@Serializable
@Parcelize
internal data class FinancialConnectionsSessionManifest(

    @SerialName(value = "allow_manual_entry")
    val allowManualEntry: Boolean,

    @SerialName(value = "consent_required")
    val consentRequired: Boolean,

    @SerialName(value = "consent_acquired_at")
    val consentAcquiredAt: String?,

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

    @SerialName(value = "app_verification_enabled")
    val appVerificationEnabled: Boolean,

    @SerialName(value = "livemode")
    val livemode: Boolean,

    @SerialName(value = "manual_entry_uses_microdeposits")
    val manualEntryUsesMicrodeposits: Boolean,

    @SerialName(value = "mobile_handoff_enabled")
    val mobileHandoffEnabled: Boolean,

    @SerialName(value = "next_pane")
    val nextPane: Pane,

    @SerialName(value = "manual_entry_mode")
    val manualEntryMode: ManualEntryMode,

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

    @SerialName(value = "accountholder_phone_number")
    val accountholderPhoneNumber: String? = null,

    @SerialName(value = "accountholder_token")
    val accountholderToken: String? = null,

    @SerialName(value = "active_auth_session")
    val activeAuthSession: FinancialConnectionsAuthorizationSession? = null,

    @SerialName(value = "active_institution")
    val activeInstitution: FinancialConnectionsInstitution? = null,

    @SerialName(value = "assignment_event_id")
    val assignmentEventId: String? = null,

    @SerialName(value = "business_name")
    val businessName: String? = null,

    @SerialName(value = "cancel_url")
    val cancelUrl: String? = null,

    @SerialName(value = "connect_platform_name")
    val connectPlatformName: String? = null,

    @SerialName(value = "connected_account_name")
    val connectedAccountName: String? = null,

    @SerialName(value = "experiment_assignments")
    val experimentAssignments: Map<String, String>? = null,

    @SerialName(value = "display_text")
    val displayText: TextUpdate? = null,

    @SerialName(value = "features")
    val features: Map<String, Boolean>? = null,

    @SerialName(value = "hosted_auth_url")
    val hostedAuthUrl: String? = null,

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

    @SerialName(value = "link_account_session_cancellation_behavior")
    val linkAccountSessionCancellationBehavior: LinkAccountSessionCancellationBehavior? = null,

    @SerialName(value = "modal_customization")
    val modalCustomization: Map<String, Boolean>? = null,

    @SerialName(value = "payment_method_type")
    val paymentMethodType: SupportedPaymentMethodTypes? = null,

    @SerialName(value = "step_up_authentication_required")
    val stepUpAuthenticationRequired: Boolean? = null,

    @SerialName(value = "success_url")
    val successUrl: String? = null,

    @SerialName("skip_success_pane")
    val skipSuccessPane: Boolean? = null,

    @SerialName("theme")
    val theme: Theme? = null,
) : Parcelable {

    val consentAcquired: Boolean
        get() = !consentRequired || consentAcquiredAt != null

    /**
     *
     *
     * Values: ACCOUNT_PICKER,ATTACH_LINKED_PAYMENT_ACCOUNT,AUTH_OPTIONS,CONSENT,INSTITUTION_PICKER,
     * LINK_CONSENT,LINK_LOGIN,MANUAL_ENTRY,MANUAL_ENTRY_SUCCESS,NETWORKING_LINK_LOGIN_WARMUP,
     * NETWORKING_LINK_SIGNUP_PANE,NETWORKING_LINK_VERIFICATION,PARTNER_AUTH,SUCCESS,UNEXPECTED_ERROR
     */
    @Serializable(with = Pane.Serializer::class)
    enum class Pane(val value: String) {
        @SerialName(value = "account_picker")
        ACCOUNT_PICKER("account_picker"),

        @SerialName(value = "attach_linked_payment_account")
        ATTACH_LINKED_PAYMENT_ACCOUNT("attach_linked_payment_account"),

        @SerialName(value = "auth_options")
        AUTH_OPTIONS("auth_options"),

        @SerialName(value = "consent")
        CONSENT("consent"),

        @SerialName(value = "bank_auth_repair")
        BANK_AUTH_REPAIR("bank_auth_repair"),

        @SerialName(value = "id_consent_content")
        ID_CONSENT_CONTENT("id_consent_content"),

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

        @SerialName(value = "networking_link_step_up_verification")
        LINK_STEP_UP_VERIFICATION("networking_link_step_up_verification"),

        @SerialName(value = "partner_auth")
        PARTNER_AUTH("partner_auth"),

        @SerialName(value = "success")
        SUCCESS("success"),

        @SerialName(value = "unexpected_error")
        UNEXPECTED_ERROR("unexpected_error"),

        // CLIENT SIDE PANES
        @SerialName(value = "link_account_picker")
        LINK_ACCOUNT_PICKER("link_account_picker"),

        @SerialName(value = "partner_auth_drawer")
        PARTNER_AUTH_DRAWER("partner_auth_drawer"),

        @SerialName(value = "networking_save_to_link_verification")
        NETWORKING_SAVE_TO_LINK_VERIFICATION("networking_save_to_link_verification"),

        @SerialName(value = "notice")
        NOTICE("notice"),

        @SerialName(value = "reset")
        RESET("reset"),

        @SerialName(value = "account_update_required")
        ACCOUNT_UPDATE_REQUIRED("account_update_required"),

        @SerialName(value = "exit")
        EXIT("exit");

        internal object Serializer :
            EnumIgnoreUnknownSerializer<Pane>(entries.toTypedArray(), UNEXPECTED_ERROR)
    }

    @Serializable(with = Product.Serializer::class)
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

        @SerialName(value = "instant_debits")
        INSTANT_DEBITS("instant_debits"),

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
        SUPPORT_SITE("support_site"),

        @SerialName(value = "unknown")
        UNKNOWN("unknown");

        internal object Serializer :
            EnumIgnoreUnknownSerializer<Product>(entries.toTypedArray(), UNKNOWN)
    }

    /**
     *
     *
     * Values: DASHBOARD,EMAIL,SUPPORT
     */
    @Serializable(with = AccountDisconnectionMethod.Serializer::class)
    enum class AccountDisconnectionMethod(val value: String) {
        @SerialName(value = "dashboard")
        DASHBOARD("dashboard"),

        @SerialName(value = "email")
        EMAIL("email"),

        @SerialName(value = "support")
        SUPPORT("support"),

        @SerialName(value = "link")
        LINK("link"),

        @SerialName(value = "unknown")
        UNKNOWN("unknown");

        internal object Serializer :
            EnumIgnoreUnknownSerializer<AccountDisconnectionMethod>(
                entries.toTypedArray(),
                UNKNOWN
            )
    }

    /**
     *
     *
     * Values: SILENT_SUCCESS,USER_ERROR
     */
    @Serializable(with = LinkAccountSessionCancellationBehavior.Serializer::class)
    enum class LinkAccountSessionCancellationBehavior(val value: String) {
        @SerialName(value = "treat_as_silent_success")
        SILENT_SUCCESS("treat_as_silent_success"),

        @SerialName(value = "treat_as_user_error")
        USER_ERROR("treat_as_user_error"),

        @SerialName(value = "unknown")
        UNKNOWN("unknown");

        internal object Serializer :
            EnumIgnoreUnknownSerializer<LinkAccountSessionCancellationBehavior>(
                entries.toTypedArray(),
                UNKNOWN
            )
    }

    @Serializable(with = FinancialConnectionsSessionManifest.Theme.Serializer::class)
    enum class Theme {

        @SerialName(value = "light")
        LIGHT,

        @SerialName(value = "dashboard_light")
        DASHBOARD_LIGHT,

        @SerialName(value = "link_light")
        LINK_LIGHT;

        internal object Serializer : EnumIgnoreUnknownSerializer<Theme>(
            values = entries.toTypedArray(),
            defaultValue = LIGHT,
        )
    }
}
