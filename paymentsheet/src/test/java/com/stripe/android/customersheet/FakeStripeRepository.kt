package com.stripe.android.customersheet

import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.SetupIntent
import com.stripe.android.testing.AbsFakeStripeRepository

class FakeStripeRepository(
    private val createPaymentMethodResult: Result<PaymentMethod> = Result.failure(NotImplementedError()),
    private val confirmSetupIntentResult: Result<SetupIntent> = Result.failure(NotImplementedError())
) : AbsFakeStripeRepository() {

    override suspend fun createPaymentMethod(
        paymentMethodCreateParams: PaymentMethodCreateParams,
        options: ApiRequest.Options
    ): Result<PaymentMethod> {
        return createPaymentMethodResult
    }

    override suspend fun confirmSetupIntent(
        confirmSetupIntentParams: ConfirmSetupIntentParams,
        options: ApiRequest.Options,
        expandFields: List<String>
    ): Result<SetupIntent> {
        return confirmSetupIntentResult
    }
}
