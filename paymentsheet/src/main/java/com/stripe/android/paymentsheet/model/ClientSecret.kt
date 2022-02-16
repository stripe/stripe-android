package com.stripe.android.paymentsheet.model

import android.os.Parcelable
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import kotlinx.parcelize.Parcelize
import java.security.InvalidParameterException

/**
 * Represents the client secret for a [SetupIntent] or [PaymentIntent]
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
            throw InvalidParameterException(
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
            throw InvalidParameterException(
                "The SetupIntent client_secret cannot be an empty string."
            )
        }
    }
}
