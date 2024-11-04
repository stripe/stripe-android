package com.stripe.android.networking

import androidx.annotation.Keep
import com.stripe.android.core.networking.AnalyticsEvent

internal enum class PaymentAnalyticsEvent(val code: String) : AnalyticsEvent {
    // Token
    TokenCreate("token_creation"),

    // Payment Methods
    PaymentMethodCreate("payment_method_creation"),
    PaymentMethodUpdate("payment_method_update"),

    // Customer
    CustomerRetrieve("retrieve_customer"),
    CustomerRetrievePaymentMethods("retrieve_payment_methods"),
    CustomerAttachPaymentMethod("attach_payment_method"),
    CustomerDetachPaymentMethod("detach_payment_method"),
    CustomerDeleteSource("delete_source"),
    CustomerSetShippingInfo("set_shipping_info"),
    CustomerAddSource("add_source"),
    CustomerSetDefaultSource("default_source"),

    // Issuing
    IssuingRetrievePin("issuing_retrieve_pin"),
    IssuingUpdatePin("issuing_update_pin"),

    // Source
    SourceCreate("source_creation"),
    SourceRetrieve("retrieve_source"),

    // Payment Intents
    PaymentIntentConfirm("payment_intent_confirmation"),
    PaymentIntentRetrieve("payment_intent_retrieval"),
    PaymentIntentRetrieveOrdered("payment_intent_retrieval_ordered"),
    PaymentIntentCancelSource("payment_intent_cancel_source"),
    PaymentIntentRefresh("payment_intent_refresh"),

    // Setup Intents
    SetupIntentConfirm("setup_intent_confirmation"),
    SetupIntentRetrieve("setup_intent_retrieval"),
    SetupIntentRetrieveOrdered("setup_intent_retrieval_ordered"),
    SetupIntentCancelSource("setup_intent_cancel_source"),
    SetupIntentRefresh("setup_intent_refresh"),

    // Payment Launcher
    PaymentLauncherConfirmStarted("paymenthandler.confirm.started"),
    PaymentLauncherConfirmFinished("paymenthandler.confirm.finished"),
    PaymentLauncherNextActionStarted("paymenthandler.handle_next_action.started"),
    PaymentLauncherNextActionFinished("paymenthandler.handle_next_action.finished"),

    // File
    FileCreate("create_file"),

    // 3DS1
    Auth3ds1Sdk("3ds1_sdk"),
    Auth3ds1ChallengeStart("3ds1_challenge_start"),
    Auth3ds1ChallengeError("3ds1_challenge_error"),
    Auth3ds1ChallengeComplete("3ds1_challenge_complete"),

    // URL authentication method
    AuthWithWebView("auth_with_webview"),
    AuthWithCustomTabs("auth_with_customtabs"),
    AuthWithDefaultBrowser("auth_with_defaultbrowser"),

    // Return URL in confirmation request
    ConfirmReturnUrlNull("confirm_returnurl_null"),
    ConfirmReturnUrlDefault("confirm_returnurl_default"),
    ConfirmReturnUrlCustom("confirm_returnurl_custom"),

    // FPX
    FpxBankStatusesRetrieve("retrieve_fpx_bank_statuses"),

    // Get an arbitrary Stripe URL
    StripeUrlRetrieve("retrieve_stripe_url"),

    // 3DS2
    Auth3ds2RequestParamsFailed("3ds2_authentication_request_params_failed"),
    Auth3ds2Fingerprint("3ds2_fingerprint"),
    Auth3ds2Start("3ds2_authenticate"),
    Auth3ds2Frictionless("3ds2_frictionless_flow"),
    Auth3ds2ChallengePresented("3ds2_challenge_flow_presented"),
    Auth3ds2ChallengeCanceled("3ds2_challenge_flow_canceled"),
    Auth3ds2ChallengeCompleted("3ds2_challenge_flow_completed"),
    Auth3ds2ChallengeErrored("3ds2_challenge_flow_errored"),
    Auth3ds2ChallengeTimedOut("3ds2_challenge_flow_timed_out"),
    Auth3ds2Fallback("3ds2_fallback"),

    AuthRedirect("url_redirect_next_action"),
    AuthError("auth_error"),

    // Source Authentication
    AuthSourceStart("auth_source_start"),
    AuthSourceRedirect("auth_source_redirect"),
    AuthSourceResult("auth_source_result"),

    RadarSessionCreate("radar_session_create"),

    CardMetadataPublishableKeyAvailable("card_metadata_pk_available"),
    CardMetadataPublishableKeyUnavailable("card_metadata_pk_unavailable"),

    CardMetadataLoadedTooSlow("card_metadata_loaded_too_slow"),
    CardMetadataLoadFailure("card_metadata_load_failure"),
    CardMetadataMissingRange("card_metadata_missing_range");

    @Keep
    override fun toString(): String {
        return "$PREFIX.$code"
    }

    override val eventName: String
        get() = toString()

    private companion object {
        private const val PREFIX = "stripe_android"
    }
}
