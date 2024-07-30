package com.stripe.android.financialconnections.repository

import com.stripe.android.model.ConsumerSession

internal class TestConsumerSessionRepository : ConsumerSessionRepository {

    private var consumerSession: CachedConsumerSession? = null

    override fun provideConsumerSession(): CachedConsumerSession? = consumerSession

    override fun storeConsumerSession(consumerSession: ConsumerSession?) {
        this.consumerSession = consumerSession?.toCached()
    }
}
