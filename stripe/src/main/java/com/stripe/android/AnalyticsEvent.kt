package com.stripe.android

internal enum class AnalyticsEvent(private val code: String) {
    TokenCreate("token_creation"),

    // Payment Methods
    PaymentMethodCreate("payment_method_creation"),

    // Customer Methods
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

    // Payment Intents
    PaymentIntentConfirm("payment_intent_confirmation"),
    PaymentIntentRetrieve("payment_intent_retrieval"),
    PaymentIntentCancelSource("payment_intent_cancel_source"),

    // Setup Intents
    SetupIntentConfirm("setup_intent_confirmation"),
    SetupIntentRetrieve("setup_intent_retrieval"),
    SetupIntentCancelSource("setup_intent_cancel_source"),

    // File
    FileCreate("create_file"),

    Auth3ds1Sdk("3ds1_sdk"),

    // 3DS2
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
    AuthError("auth_error");

    override fun toString(): String {
        return "$PREFIX.$code"
    }

    private companion object {
        private const val PREFIX = "stripe_android"
    }
}
