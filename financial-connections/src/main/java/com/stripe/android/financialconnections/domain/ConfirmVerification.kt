package com.stripe.android.financialconnections.domain

import com.stripe.android.core.exception.StripeException
import com.stripe.android.financialconnections.repository.FinancialConnectionsConsumerSessionRepository
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.VerificationType
import javax.inject.Inject

internal class ConfirmVerification @Inject constructor(
    private val consumerSessionRepository: FinancialConnectionsConsumerSessionRepository
) {

    suspend fun sms(
        consumerSessionClientSecret: String,
        verificationCode: String,
    ): ConsumerSession = kotlin.runCatching {
        consumerSessionRepository.confirmConsumerVerification(
            consumerSessionClientSecret = consumerSessionClientSecret,
            verificationCode = verificationCode,
            type = VerificationType.SMS
        )
    }.fold(
        onSuccess = { it },
        onFailure = { throw it.toDomainException(VerificationType.SMS) }
    )

    suspend fun email(
        consumerSessionClientSecret: String,
        verificationCode: String,
    ): ConsumerSession = kotlin.runCatching {
        consumerSessionRepository.confirmConsumerVerification(
            consumerSessionClientSecret = consumerSessionClientSecret,
            verificationCode = verificationCode,
            type = VerificationType.EMAIL
        )
    }.fold(
        onSuccess = { it },
        onFailure = { throw it.toDomainException(VerificationType.EMAIL) }
    )

    /**
     * Convert Exception to [OTPError] (a soft error related to OTP verification) or
     * returns the original exception.
     */
    private fun Throwable.toDomainException(
        verificationCode: VerificationType
    ): Throwable = when (val code = (this as? StripeException)?.stripeError?.code ?: "") {
        "consumer_verification_code_invalid" -> OTPError(code, OTPError.Type.CODE_INVALID)
        "consumer_session_expired",
        "consumer_verification_expired",
        "consumer_verification_max_attempts_exceeded" -> when (verificationCode) {
            VerificationType.EMAIL -> OTPError(code, OTPError.Type.EMAIL_CODE_EXPIRED)
            VerificationType.SMS -> OTPError(code, OTPError.Type.SMS_CODE_EXPIRED)
        }

        else -> this
    }

    /**
     * A soft error related to OTP verification.
     */
    class OTPError(
        message: String,
        val type: Type
    ) : Throwable(message = message) {
        enum class Type {
            EMAIL_CODE_EXPIRED,
            SMS_CODE_EXPIRED,
            CODE_INVALID,
        }
    }
}
