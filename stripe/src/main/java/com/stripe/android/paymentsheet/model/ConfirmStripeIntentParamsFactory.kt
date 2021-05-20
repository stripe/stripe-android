package com.stripe.android.paymentsheet.model

import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams

/**
 * Factory interface for creating [ConfirmPaymentIntentParams] or [ConfirmSetupIntentParams].
 */
internal interface ConfirmStripeIntentParamsFactory<T : ConfirmStripeIntentParams> {

    fun create(paymentSelection: PaymentSelection.Saved): T

    fun create(paymentSelection: PaymentSelection.New): T
}
