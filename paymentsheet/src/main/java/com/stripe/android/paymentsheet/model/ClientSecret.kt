package com.stripe.android.paymentsheet.model

import android.os.Parcelable
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * Represents a client secret with resource-specific validation.
 */
internal sealed class ClientSecret : Parcelable {
    abstract val value: String
    abstract fun validate()
}

/**
 * Represents the client secret for a [PaymentIntent]
 */
@Parcelize
internal data class PaymentIntentClientSecret(
    override val value: String
) : ClientSecret() {
    override fun validate() {
        if (value.isBlank()) {
            throw IllegalArgumentException(
                "The PaymentIntent client_secret cannot be an empty string."
            )
        }
    }
}

/**
 * Represents the client secret for a [SetupIntent]
 */
@Parcelize
internal data class SetupIntentClientSecret(
    override val value: String
) : ClientSecret() {
    override fun validate() {
        if (value.isBlank()) {
            throw IllegalArgumentException(
                "The SetupIntent client_secret cannot be an empty string."
            )
        }
    }
}

/**
 * Represents the client secret for a Checkout Session.
 */
@Parcelize
internal data class CheckoutSessionClientSecret(
    override val value: String
) : ClientSecret() {
    @IgnoredOnParcel
    val sessionId: String = value.substringBefore(DELIMITER, missingDelimiterValue = "")

    init {
        validate()
    }

    override fun validate() {
        require(sessionId.isNotEmpty()) {
            "Invalid Checkout Session client secret. " +
                "Pass the client_secret from the Checkout Session created by your server."
        }
    }

    private companion object {
        const val DELIMITER = "_secret_"
    }
}
