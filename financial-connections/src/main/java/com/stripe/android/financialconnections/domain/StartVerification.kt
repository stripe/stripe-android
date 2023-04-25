package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.repository.FinancialConnectionsConsumerSessionRepository
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.CustomEmailType
import com.stripe.android.model.VerificationType
import javax.inject.Inject

internal class StartVerification @Inject constructor(
    private val consumerSessionRepository: FinancialConnectionsConsumerSessionRepository
) {

    suspend fun sms(
        consumerSessionClientSecret: String,
    ): ConsumerSession = consumerSessionRepository.startConsumerVerification(
        consumerSessionClientSecret = consumerSessionClientSecret,
        connectionsMerchantName = null,
        customEmailType = null,
        type = VerificationType.SMS,
    )

    suspend fun email(
        consumerSessionClientSecret: String,
        businessName: String?,
    ): ConsumerSession = consumerSessionRepository.startConsumerVerification(
        consumerSessionClientSecret = consumerSessionClientSecret,
        customEmailType = CustomEmailType.NETWORKED_CONNECTIONS_OTP_EMAIL,
        connectionsMerchantName = businessName,
        type = VerificationType.EMAIL,
    )
}
