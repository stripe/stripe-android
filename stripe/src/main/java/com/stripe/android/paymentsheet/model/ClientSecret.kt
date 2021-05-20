package com.stripe.android.paymentsheet.model

import android.os.Parcelable
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import kotlinx.parcelize.Parcelize

/**
 * Represents the client secret for a [SetupIntent] or [PaymentIntent]
 */
internal sealed class ClientSecret : Parcelable {
    abstract val value: String
    abstract fun createConfirmParamsFactory(): ConfirmStripeIntentParamsFactory<*>
}

/**
 * Represents the client secret for a [PaymentIntent]
 */
@Parcelize
internal data class PaymentIntentClientSecret(
    override val value: String
) : ClientSecret() {
    override fun createConfirmParamsFactory() =
        ConfirmPaymentIntentParamsFactory(this)
}

/**
 * Represents the client secret for a [SetupIntent]
 */
@Parcelize
internal data class SetupIntentClientSecret(
    override val value: String
) : ClientSecret() {
    override fun createConfirmParamsFactory() =
        ConfirmSetupIntentParamsFactory(this)
}
