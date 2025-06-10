package com.stripe.android.paymentelement.confirmation.lpms

import com.stripe.android.paymentelement.confirmation.lpms.foundations.CreateIntentFactory
import com.stripe.android.paymentelement.confirmation.lpms.foundations.LpmAssertionParams
import com.stripe.android.paymentelement.confirmation.lpms.foundations.LpmNetworkTestActivity
import com.stripe.android.paymentelement.confirmation.lpms.foundations.assertIntentConfirmed
import com.stripe.android.paymentelement.confirmation.lpms.foundations.network.MerchantCountry

internal interface TestType {
    suspend fun createIntent(
        country: MerchantCountry,
        factory: CreateIntentFactory
    ): Result<CreateIntentFactory.CreateIntentData>

    suspend fun assert(
        activity: LpmNetworkTestActivity,
        params: LpmAssertionParams
    )

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

    override suspend fun assert(activity: LpmNetworkTestActivity, params: LpmAssertionParams) {
        assertIntentConfirmed(activity, params)
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

    override suspend fun assert(activity: LpmNetworkTestActivity, params: LpmAssertionParams) {
        assertIntentConfirmed(activity, params)
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

    override suspend fun assert(activity: LpmNetworkTestActivity, params: LpmAssertionParams) {
        assertIntentConfirmed(activity, params)
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

    override suspend fun assert(activity: LpmNetworkTestActivity, params: LpmAssertionParams) {
        assertIntentConfirmed(activity, params)
    }
}
