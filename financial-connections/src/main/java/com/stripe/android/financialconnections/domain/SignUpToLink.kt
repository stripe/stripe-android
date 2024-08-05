package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.repository.FinancialConnectionsConsumerSessionRepository
import com.stripe.android.model.ConsumerSessionSignup
import java.util.Locale
import javax.inject.Inject

internal fun interface SignUpToLink {
    suspend operator fun invoke(
        email: String,
        phoneNumber: String,
        country: String,
    ): ConsumerSessionSignup
}

internal class RealSignUpToLink @Inject constructor(
    private val consumerRepository: FinancialConnectionsConsumerSessionRepository,
) : SignUpToLink {

    override suspend operator fun invoke(
        email: String,
        phoneNumber: String,
        country: String,
    ): ConsumerSessionSignup {
        return consumerRepository.signUp(
            email = email,
            phoneNumber = phoneNumber,
            country = country,
            locale = Locale.getDefault(),
        )
    }
}
