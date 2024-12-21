package com.stripe.android.financialconnections.domain

import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.VerificationType
import javax.inject.Inject

internal class LookupConsumerAndStartVerification @Inject constructor(
    private val lookupAccount: LookupAccount,
    private val startVerification: StartVerification,
) {

    sealed interface Result {
        data class Success(val consumerSession: ConsumerSession) : Result
        data object ConsumerNotFound : Result
        data class LookupError(val error: Throwable) : Result
        data class VerificationError(val error: Throwable) : Result
    }

    suspend operator fun invoke(
        email: String,
        businessName: String?,
        verificationType: VerificationType,
    ): Result {
        return runCatching {
            lookupAccount(email)
        }.fold(
            onSuccess = { session ->
                if (session.exists) {
                    runCatching {
                        val consumerSecret = session.consumerSession!!.clientSecret
                        when (verificationType) {
                            VerificationType.EMAIL -> startVerification.email(
                                consumerSessionClientSecret = consumerSecret,
                                businessName = businessName
                            )
                            VerificationType.SMS -> startVerification.sms(
                                consumerSessionClientSecret = consumerSecret
                            )
                        }
                    }.fold(
                        onSuccess = { Result.Success(it) },
                        onFailure = { Result.VerificationError(it) }
                    )
                } else {
                    Result.ConsumerNotFound
                }
            },
            onFailure = {
                Result.LookupError(it)
            },
        )
    }
}
