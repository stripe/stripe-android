package com.stripe.android.utils

import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.networking.RequestCredentials
import com.stripe.android.model.MobileCardElementConfig
import com.stripe.android.testing.AbsFakeStripeRepository
import kotlinx.coroutines.channels.Channel

class FakeCardElementConfigRepository : AbsFakeStripeRepository() {

    private val channel = Channel<Result<MobileCardElementConfig>>()

    fun enqueueEligible() {
        val config = MobileCardElementConfig(
            cardBrandChoice = MobileCardElementConfig.CardBrandChoice(eligible = true),
        )
        val result = Result.success(config)
        channel.trySend(result)
    }

    fun enqueueNotEligible() {
        val config = MobileCardElementConfig(
            cardBrandChoice = MobileCardElementConfig.CardBrandChoice(eligible = false),
        )
        val result = Result.success(config)
        channel.trySend(result)
    }

    fun enqueueFailure() {
        val result = Result.failure<MobileCardElementConfig>(APIConnectionException())
        channel.trySend(result)
    }

    override suspend fun retrieveCardElementConfig(
        requestOptions: RequestCredentials,
        params: Map<String, String>?,
        idempotencyKey: String?,
    ): Result<MobileCardElementConfig> {
        return channel.receive()
    }
}
