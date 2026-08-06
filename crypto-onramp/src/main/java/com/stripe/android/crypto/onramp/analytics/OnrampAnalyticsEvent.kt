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
    val params: Map<String, Any?>? = null,
    private val eventPrefix: String = ONRAMP_EVENT_PREFIX,
) {

    val eventName = "$eventPrefix.$name"

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

    data object LinkUserAuthenticationWithTokenCompleted : OnrampAnalyticsEvent(
        name = "link_user_authentication_with_token_completed"
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

    data object KycVerificationStarted : OnrampAnalyticsEvent(
        name = "kyc_info_verification_started"
    )

    data object KycVerificationCompleted : OnrampAnalyticsEvent(
        name = "kyc_info_verification_completed"
    )

    data object MissingIdentifiersRetrieved : OnrampAnalyticsEvent(
        name = "identifier_requirements_retrieved"
    )

    class IdentifiersSubmitted(completed: Boolean) : OnrampAnalyticsEvent(
        name = "identifiers_submitted",
        params = mapOf(
            "completed" to completed.toString()
        )
    )

    data object UserAttestationStarted : OnrampAnalyticsEvent(
        name = "user_attestation_started"
    )

    data object UserAttestationCompleted : OnrampAnalyticsEvent(
        name = "user_attestation_completed"
    )

    class WalletRegistered(
        network: CryptoNetwork
    ) : OnrampAnalyticsEvent(
        name = "wallet_registered",
        params = mapOf(
            "network" to network.value
        )
    )

    class WalletOwnershipChallengeRetrieved(
        network: CryptoNetwork
    ) : OnrampAnalyticsEvent(
        name = "wallet_ownership_challenge_retrieved",
        params = mapOf(
            "network" to network.value
        )
    )

    class WalletOwnershipVerified(
        network: CryptoNetwork
    ) : OnrampAnalyticsEvent(
        name = "wallet_ownership_verified",
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

    data object SamsungPayInitialized : OnrampAnalyticsEvent(
        name = "initialized",
        eventPrefix = SAMSUNG_PAY_EVENT_PREFIX,
    )

    class SamsungPayAvailable(
        available: Boolean,
        status: Int,
    ) : OnrampAnalyticsEvent(
        name = "available",
        params = mapOf(
            "available" to available,
            "status" to status,
        ),
        eventPrefix = SAMSUNG_PAY_EVENT_PREFIX,
    )

    data object SamsungPayPresented : OnrampAnalyticsEvent(
        name = "presented",
        eventPrefix = SAMSUNG_PAY_EVENT_PREFIX,
    )

    data object SamsungPayCanceled : OnrampAnalyticsEvent(
        name = "canceled",
        eventPrefix = SAMSUNG_PAY_EVENT_PREFIX,
    )

    data object SamsungPayObtainCredentialsSuccess : OnrampAnalyticsEvent(
        name = "obtain_credentials.success",
        eventPrefix = SAMSUNG_PAY_EVENT_PREFIX,
    )

    class SamsungPayObtainCredentialsFailed(
        errorCode: Int,
    ) : OnrampAnalyticsEvent(
        name = "obtain_credentials.failed",
        params = mapOf("error_code" to errorCode),
        eventPrefix = SAMSUNG_PAY_EVENT_PREFIX,
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
            AuthenticateUserWithAuthToken("authenticate_user_with_auth_token"),
            Authorize("authorize"),
            CollectPaymentMethod("collect_payment_method"),
            AttachKycInfo("attach_kyc_info"),
            VerifyIdentity("verify_identity"),
            RegisterWalletAddress("register_wallet_address"),
            GetWalletOwnershipChallenge("get_wallet_ownership_challenge"),
            SubmitWalletOwnershipSignature("submit_wallet_ownership_signature"),
            CreateCryptoPaymentToken("create_crypto_payment_token"),
            PerformCheckout("perform_checkout"),
            LogOut("log_out"),
            VerifyKyc("verify_kyc_info"),
            RetrieveMissingIdentifiers("retrieve_missing_identifiers"),
            SubmitIdentifiers("submit_identifiers"),
            PresentUserAttestation("present_user_attestation")
        }
    }

    override fun toString(): String {
        return "OnrampAnalyticsEvent(name='$name', params=$params)"
    }
}

private const val ONRAMP_EVENT_PREFIX = "onramp"
private const val SAMSUNG_PAY_EVENT_PREFIX = "samsung_pay"
