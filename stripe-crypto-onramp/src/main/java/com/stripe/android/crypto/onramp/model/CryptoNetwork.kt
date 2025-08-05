package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supported crypto networks for wallet address registration.
 *
 */
@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class CryptoNetwork(val value: String) {
    @SerialName("bitcoin")
    Bitcoin("bitcoin"),

    @SerialName("ethereum")
    Ethereum("ethereum"),

    @SerialName("solana")
    Solana("solana"),

    @SerialName("polygon")
    Polygon("polygon"),

    @SerialName("stellar")
    Stellar("stellar"),

    @SerialName("avalanche")
    Avalanche("avalanche"),

    @SerialName("base")
    Base("base"),

    @SerialName("aptos")
    Aptos("aptos"),

    @SerialName("optimism")
    Optimism("optimism"),

    @SerialName("worldchain")
    Worldchain("worldchain"),

    @SerialName("xrpl")
    Xrpl("xrpl")
}
