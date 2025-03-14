package com.stripe.android.paymentelement.confirmation.lpms

import com.stripe.android.paymentelement.confirmation.lpms.foundations.CreateIntentFactory
import com.stripe.android.paymentelement.confirmation.lpms.foundations.network.MerchantCountry

internal interface TestType {
    suspend fun createIntent(
        country: MerchantCountry,
        factory: CreateIntentFactory
    ): Result<CreateIntentFactory.CreateIntentData>

    companion object {
        fun all(
            amount: Int = 5050,
            currency: String = "USD"
        ): List<Array<TestType>> {
            return listOf(
                PaymentIntentTestType(
                    amount = amount,
                    currency = currency,
                    createWithSetupFutureUsage = false
                ),
                PaymentIntentTestType(
                    amount = 5050,
                    currency = currency,
                    createWithSetupFutureUsage = true
                ),
                SetupIntentTestType,
                DeferredPaymentIntentTestType(
                    amount = amount,
                    currency = currency,
                    createWithSetupFutureUsage = false
                ),
                DeferredPaymentIntentTestType(
                    amount = amount,
                    currency = currency,
                    createWithSetupFutureUsage = true
                ),
                DeferredSetupIntentTestType,
            ).map {
                arrayOf(it)
            }
        }
    }
}

internal data class PaymentIntentTestType(
    private val amount: Int,
    private val currency: String,
    private val createWithSetupFutureUsage: Boolean,
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
    private val amount: Int,
    private val currency: String,
    private val createWithSetupFutureUsage: Boolean,
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
