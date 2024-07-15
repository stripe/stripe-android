package com.stripe.android.financialconnections.utils

import com.stripe.android.financialconnections.repository.FinancialConnectionsConsumerSessionRepository
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.CustomEmailType
import com.stripe.android.model.VerificationType

abstract class FakeFinancialConnectionsConsumerSessionRepository : FinancialConnectionsConsumerSessionRepository {

    override suspend fun getCachedConsumerSession(): ConsumerSession? {
        TODO("Not yet implemented")
    }

    override suspend fun lookupConsumerSession(email: String, clientSecret: String): ConsumerSessionLookup {
        TODO("Not yet implemented")
    }

    override suspend fun startConsumerVerification(
        consumerSessionClientSecret: String,
        connectionsMerchantName: String?,
        type: VerificationType,
        customEmailType: CustomEmailType?
    ): ConsumerSession {
        TODO("Not yet implemented")
    }

    override suspend fun confirmConsumerVerification(
        consumerSessionClientSecret: String,
        verificationCode: String,
        type: VerificationType
    ): ConsumerSession {
        TODO("Not yet implemented")
    }
}
