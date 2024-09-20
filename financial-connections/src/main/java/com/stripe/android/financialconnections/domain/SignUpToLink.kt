package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.repository.FinancialConnectionsConsumerSessionRepository
import com.stripe.android.model.ConsumerSessionSignup
import javax.inject.Inject

internal fun interface SignUpToLink {
    suspend operator fun invoke(
        email: String,
        phoneNumber: String,
        country: String,
    ): ConsumerSessionSignup
}

internal class RealSignUpToLink @Inject constructor(
    private val repository: FinancialConnectionsConsumerSessionRepository,
) : SignUpToLink {

    override suspend fun invoke(
        email: String,
        phoneNumber: String,
        country: String,
    ): ConsumerSessionSignup {
        return repository.signUp(email, phoneNumber, country)
    }
}
