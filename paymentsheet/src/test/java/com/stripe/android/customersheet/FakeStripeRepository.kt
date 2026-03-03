package com.stripe.android.customersheet

import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.testing.AbsFakeStripeRepository

class FakeStripeRepository(
    private val createPaymentMethodResult: Result<PaymentMethod> = Result.failure(NotImplementedError()),
    private val retrieveSetupIntent: Result<SetupIntent> = Result.failure(NotImplementedError()),
    private val retrieveIntent: Result<StripeIntent> = Result.failure(NotImplementedError()),
) : AbsFakeStripeRepository() {

    override suspend fun createPaymentMethod(
        paymentMethodCreateParams: PaymentMethodCreateParams,
        options: ApiRequest.Options
    ): Result<PaymentMethod> {
        return createPaymentMethodResult
    }

    override suspend fun retrieveSetupIntent(
        clientSecret: String,
        options: ApiRequest.Options,
        expandFields: List<String>
    ): Result<SetupIntent> {
        return retrieveSetupIntent
    }

    override suspend fun retrieveStripeIntent(
        clientSecret: String,
        options: ApiRequest.Options,
        expandFields: List<String>
    ): Result<StripeIntent> {
        return retrieveIntent
    }
}
