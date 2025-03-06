package com.stripe.android.utils

import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.MobileCardElementConfig
import com.stripe.android.testing.AbsFakeStripeRepository

class FakeCardElementConfigRepository : AbsFakeStripeRepository() {

    private var result = Result.success(
        value = MobileCardElementConfig(
            cardBrandChoice = MobileCardElementConfig.CardBrandChoice(eligible = true),
        )
    )

    fun enqueueEligible() {
        val config = MobileCardElementConfig(
            cardBrandChoice = MobileCardElementConfig.CardBrandChoice(eligible = true),
        )
        this.result = Result.success(config)
    }

    fun enqueueNotEligible() {
        val config = MobileCardElementConfig(
            cardBrandChoice = MobileCardElementConfig.CardBrandChoice(eligible = false),
        )
        this.result = Result.success(config)
    }

    fun enqueueFailure() {
        result = Result.failure(APIConnectionException())
    }

    override suspend fun retrieveCardElementConfig(
        requestOptions: ApiRequest.Options,
        params: Map<String, String>?
    ): Result<MobileCardElementConfig> {
        return result
    }
}
