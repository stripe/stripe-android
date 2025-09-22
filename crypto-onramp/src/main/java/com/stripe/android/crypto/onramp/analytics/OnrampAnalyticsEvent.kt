package com.stripe.android.crypto.onramp.analytics

import com.stripe.android.core.exception.safeAnalyticsMessage
import com.stripe.android.crypto.onramp.model.CryptoNetwork
import com.stripe.android.crypto.onramp.model.PaymentMethodType
import com.stripe.android.utils.filterNotNullValues

/**
 * Event definitions for Crypto Onramp.
 */
internal sealed class OnrampAnalyticsEvent(
    private val name: String,
    val params: Map<String, String>? = null,
) {

    val eventName = "$EVENT_PREFIX.$name"

    data object SessionCreated : OnrampAnalyticsEvent(
        name = "session_created"
    )

    class LinkAccountLookupCompleted(
        hasLinkAccount: Boolean
    ) : OnrampAnalyticsEvent(
        name = "link_account_lookup_completed",
        params = mapOf(
            "has_link_account" to hasLinkAccount.toString()
        )
    )

    data object LinkRegistrationCompleted : OnrampAnalyticsEvent(
        name = "link_registration_completed"
    )

    data object LinkPhoneNumberUpdated : OnrampAnalyticsEvent(
        name = "link_phone_number_updated"
    )

    data object LinkUserAuthenticationStarted : OnrampAnalyticsEvent(
        name = "link_user_authentication_started"
    )

    data object LinkUserAuthenticationCompleted : OnrampAnalyticsEvent(
        name = "link_user_authentication_completed"
    )

    data object LinkAuthorizationStarted : OnrampAnalyticsEvent(
        name = "link_authorization_started"
    )

    class LinkAuthorizationCompleted(
        consented: Boolean
    ) : OnrampAnalyticsEvent(
        name = "link_authorization_completed",
        params = mapOf(
            "consented" to consented.toString()
        )
    )

    data object IdentityVerificationStarted : OnrampAnalyticsEvent(
        name = "identity_verification_started"
    )

    data object IdentityVerificationCompleted : OnrampAnalyticsEvent(
        name = "identity_verification_completed"
    )

    data object KycInfoSubmitted : OnrampAnalyticsEvent(
        name = "kyc_info_submitted"
    )

    class WalletRegistered(
        network: CryptoNetwork
    ) : OnrampAnalyticsEvent(
        name = "wallet_registered",
        params = mapOf(
            "network" to network.value
        )
    )

    class CollectPaymentMethodStarted(
        paymentMethodType: PaymentMethodType?,
    ) : OnrampAnalyticsEvent(
        name = "collect_payment_method_started",
        params = mapOf(
            "payment_method_type" to paymentMethodType?.value
        ).filterNotNullValues()
    )

    class CollectPaymentMethodCompleted(
        paymentMethodType: PaymentMethodType?
    ) : OnrampAnalyticsEvent(
        name = "collect_payment_method_completed",
        params = mapOf(
            "payment_method_type" to paymentMethodType?.value
        ).filterNotNullValues()
    )

    class CryptoPaymentTokenCreated(
        paymentMethodType: PaymentMethodType?
    ) : OnrampAnalyticsEvent(
        name = "crypto_payment_token_created",
        params = mapOf(
            "payment_method_type" to paymentMethodType?.value
        ).filterNotNullValues()
    )

    class CheckoutStarted(
        onrampSessionId: String,
        paymentMethodType: PaymentMethodType?
    ) : OnrampAnalyticsEvent(
        name = "checkout_started",
        params = mapOf(
            "onramp_session_id" to onrampSessionId,
            "payment_method_type" to paymentMethodType?.value
        ).filterNotNullValues()
    )

    class CheckoutCompleted(
        onrampSessionId: String,
        paymentMethodType: PaymentMethodType?,
        requiredAction: Boolean
    ) : OnrampAnalyticsEvent(
        name = "checkout_completed",
        params = mapOf(
            "onramp_session_id" to onrampSessionId,
            "payment_method_type" to paymentMethodType?.value,
            "required_action" to requiredAction.toString()
        ).filterNotNullValues()
    )

    data object LinkLogout : OnrampAnalyticsEvent(
        name = "link_logout"
    )

    class ErrorOccurred(
        operation: Operation,
        error: Throwable
    ) : OnrampAnalyticsEvent(
        name = "error_occurred",
        params = mapOf(
            "operation_name" to operation.value,
            "error_message" to error.safeAnalyticsMessage
        )
    ) {
        enum class Operation(val value: String) {
            Configure("configure"),
            HasLinkAccount("has_link_account"),
            RegisterLinkUser("register_link_user"),
            UpdatePhoneNumber("update_phone_number"),
            AuthenticateUser("authenticate_user"),
            Authorize("authorize"),
            CollectPaymentMethod("collect_payment_method"),
            AttachKycInfo("attach_kyc_info"),
            VerifyIdentity("verify_identity"),
            RegisterWalletAddress("register_wallet_address"),
            CreateCryptoPaymentToken("create_crypto_payment_token"),
            PerformCheckout("perform_checkout"),
            LogOut("log_out")
        }
    }

    override fun toString(): String {
        return "OnrampAnalyticsEvent(name='$name', params=$params)"
    }
}

private const val EVENT_PREFIX = "onramp"
