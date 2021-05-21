package com.stripe.android.paymentsheet.model

import android.os.Parcelable
import com.stripe.android.StripeIntentResult
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.PaymentFlowResultProcessor
import kotlinx.parcelize.Parcelize

/**
 * Represents the client secret for a [SetupIntent] or [PaymentIntent]
 */
internal sealed class ClientSecret : Parcelable {
    abstract val value: String

    abstract fun createConfirmParamsFactory():
        ConfirmStripeIntentParamsFactory<ConfirmStripeIntentParams>

    abstract suspend fun processPaymentFlowResultWithProcessor(
        paymentFlowResult: PaymentFlowResult.Unvalidated,
        processor: PaymentFlowResultProcessor
    ): StripeIntentResult<*>
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

    override suspend fun processPaymentFlowResultWithProcessor(
        paymentFlowResult: PaymentFlowResult.Unvalidated,
        processor: PaymentFlowResultProcessor
    ) = processor.processPaymentIntent(paymentFlowResult)
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

    override suspend fun processPaymentFlowResultWithProcessor(
        paymentFlowResult: PaymentFlowResult.Unvalidated,
        processor: PaymentFlowResultProcessor
    ) = processor.processSetupIntent(paymentFlowResult)
}
