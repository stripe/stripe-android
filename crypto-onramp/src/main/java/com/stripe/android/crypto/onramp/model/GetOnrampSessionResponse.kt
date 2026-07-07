package com.stripe.android.crypto.onramp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a response from the `/v1/crypto/internal/onramp_session` endpoint.
 */
@Serializable
internal data class GetOnrampSessionResponse(
    /**
     * The onramp session's unique identifier.
     * `cos_XXXXXXXXX`
     */
    val id: String,
    /**
     * The onramp session client secret.
     * `cos_XXXXXXXXX_secret_XXXXXXXXX`
     */
    @SerialName("client_secret")
    val clientSecret: String,
    /**
     * The PaymentIntent client secret associated with this onramp session.
     */
    @SerialName("payment_intent_client_secret")
    val paymentIntentClientSecret: String? = null,
    /**
     * Transaction details for this onramp session.
     */
    @SerialName("transaction_details")
    val transactionDetails: OnrampSessionTransactionDetails? = null
)

@Serializable
internal data class OnrampSessionTransactionDetails(
    @SerialName("wallet_address")
    val walletAddress: String? = null,
    @SerialName("destination_network")
    val destinationNetwork: String? = null,
    @SerialName("last_error")
    val lastError: String? = null,
) {
    val destinationCryptoNetwork: CryptoNetwork?
        get() = destinationNetwork?.let { value ->
            CryptoNetwork.values().firstOrNull { it.value == value }
        }
}
