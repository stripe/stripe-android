package com.stripe.android.paymentsheet.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import kotlinx.parcelize.Parcelize
import java.security.InvalidParameterException

/**
 * Represents the client secret for a [SetupIntent] or [PaymentIntent]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
sealed class ClientSecret : Parcelable {
    abstract val value: String
    abstract fun validate()
}

/**
 * Represents the client secret for a [PaymentIntent]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
@Parcelize
data class PaymentIntentClientSecret(
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
@Parcelize
data class SetupIntentClientSecret(
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
