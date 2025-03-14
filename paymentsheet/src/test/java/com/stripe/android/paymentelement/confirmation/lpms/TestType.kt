package com.stripe.android.paymentelement.confirmation.lpms

import com.stripe.android.paymentelement.confirmation.lpms.foundations.CreateIntentFactory
import com.stripe.android.paymentelement.confirmation.lpms.foundations.network.MerchantCountry

internal interface TestType {
    suspend fun createIntent(
        country: MerchantCountry,
        factory: CreateIntentFactory
    ): Result<CreateIntentFactory.CreateIntentData>

    companion object {
        fun default(): List<TestType> {
            return listOf(
                PaymentIntentTestType(
                    amount = 5050,
                    currency = "USD",
                    createWithSetupFutureUsage = false
                ),
                PaymentIntentTestType(
                    amount = 5050,
                    currency = "USD",
                    createWithSetupFutureUsage = true
                ),
                SetupIntentTestType,
                DeferredPaymentIntentTestType(
                    amount = 5050,
                    currency = "USD",
                    createWithSetupFutureUsage = false
                ),
                DeferredPaymentIntentTestType(
                    amount = 5050,
                    currency = "USD",
                    createWithSetupFutureUsage = true
                ),
                DeferredSetupIntentTestType,
            )
        }
    }
}

internal data class PaymentIntentTestType(
    val amount: Int,
    val currency: String,
    val createWithSetupFutureUsage: Boolean,
) : TestType {
    override suspend fun createIntent(
        country: MerchantCountry,
        factory: CreateIntentFactory,
    ): Result<CreateIntentFactory.CreateIntentData> {
        return factory.createPaymentIntent(
            country = country,
            amount = amount,
            currency = currency,
            createWithSetupFutureUsage = createWithSetupFutureUsage,
        )
    }
}

internal data object SetupIntentTestType : TestType {
    override suspend fun createIntent(
        country: MerchantCountry,
        factory: CreateIntentFactory,
    ): Result<CreateIntentFactory.CreateIntentData> {
        return factory.createSetupIntent(
            country = country,
        )
    }
}

internal data class DeferredPaymentIntentTestType(
    val amount: Int,
    val currency: String,
    val createWithSetupFutureUsage: Boolean,
) : TestType {
    override suspend fun createIntent(
        country: MerchantCountry,
        factory: CreateIntentFactory,
    ): Result<CreateIntentFactory.CreateIntentData> {
        return factory.createDeferredPaymentIntent(
            country = country,
            amount = amount,
            currency = currency,
            createWithSetupFutureUsage = createWithSetupFutureUsage,
        )
    }
}

internal data object DeferredSetupIntentTestType : TestType {
    override suspend fun createIntent(
        country: MerchantCountry,
        factory: CreateIntentFactory,
    ): Result<CreateIntentFactory.CreateIntentData> {
        return factory.createDeferredSetupIntent(
            country = country,
        )
    }
}
