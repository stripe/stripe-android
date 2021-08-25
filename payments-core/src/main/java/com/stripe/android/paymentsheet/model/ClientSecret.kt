package com.stripe.android.paymentsheet.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import kotlinx.parcelize.Parcelize

/**
 * Represents the client secret for a [SetupIntent] or [PaymentIntent]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
sealed class ClientSecret : Parcelable {
    abstract val value: String
}

/**
 * Represents the client secret for a [PaymentIntent]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
@Parcelize
data class PaymentIntentClientSecret(
    override val value: String
) : ClientSecret()

/**
 * Represents the client secret for a [SetupIntent]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
@Parcelize
data class SetupIntentClientSecret(
    override val value: String
) : ClientSecret()
