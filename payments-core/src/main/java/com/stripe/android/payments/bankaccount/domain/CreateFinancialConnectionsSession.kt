package com.stripe.android.payments.bankaccount.domain

import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.CreateFinancialConnectionsSessionForDeferredPaymentParams
import com.stripe.android.model.CreateFinancialConnectionsSessionParams
import com.stripe.android.model.FinancialConnectionsSession
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.VerificationMethodParam
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.bankaccount.CollectBankAccountConfiguration
import javax.inject.Inject

internal class CreateFinancialConnectionsSession @Inject constructor(
    private val stripeRepository: StripeRepository
) {

    /**
     * Creates a [FinancialConnectionsSession] for the given [PaymentIntent] secret.
     */
    suspend fun forPaymentIntent(
        publishableKey: String,
        clientSecret: String,
        stripeAccountId: String?,
        configuration: CollectBankAccountConfiguration,
    ): Result<FinancialConnectionsSession> {
        val paymentIntentClientSecretResult = runCatching {
            PaymentIntent.ClientSecret(clientSecret)
        }

        return paymentIntentClientSecretResult.mapCatching { paymentIntentClientSecret ->
            stripeRepository.createPaymentIntentFinancialConnectionsSession(
                paymentIntentId = paymentIntentClientSecret.paymentIntentId,
                params = configuration.toCreateSessionParams(clientSecret),
                requestOptions = ApiRequest.Options(
                    publishableKey,
                    stripeAccountId
                )
            ).getOrThrow()
        }
    }

    /**
     * Creates a [FinancialConnectionsSession] for the given [SetupIntent] secret.
     */
    suspend fun forSetupIntent(
        publishableKey: String,
        clientSecret: String,
        stripeAccountId: String?,
        configuration: CollectBankAccountConfiguration,
    ): Result<FinancialConnectionsSession> {
        val setupIntentClientSecretResult = runCatching {
            SetupIntent.ClientSecret(clientSecret)
        }

        return setupIntentClientSecretResult.mapCatching { setupIntentClientSecret ->
            stripeRepository.createSetupIntentFinancialConnectionsSession(
                setupIntentId = setupIntentClientSecret.setupIntentId,
                params = configuration.toCreateSessionParams(clientSecret),
                requestOptions = ApiRequest.Options(
                    publishableKey,
                    stripeAccountId
                )
            ).getOrThrow()
        }
    }

    /**
     * Creates a [FinancialConnectionsSession] for deferred payments.
     *
     * @param elementsSessionId the elements session id
     *
     * The params below are only used for payment intents
     * @param amount the amount of the payment
     * @param currency the currency of the payment
     */
    suspend fun forDeferredPayments(
        publishableKey: String,
        stripeAccountId: String?,
        elementsSessionId: String,
        customerId: String?,
        onBehalfOf: String?,
        amount: Int?,
        currency: String?
    ): Result<FinancialConnectionsSession> {
        return stripeRepository.createFinancialConnectionsSessionForDeferredPayments(
            params = CreateFinancialConnectionsSessionForDeferredPaymentParams(
                uniqueId = elementsSessionId,
                verificationMethod = VerificationMethodParam.Automatic,
                customer = customerId,
                onBehalfOf = onBehalfOf,
                amount = amount,
                currency = currency
            ),
            requestOptions = ApiRequest.Options(
                publishableKey,
                stripeAccountId
            )
        )
    }

    private fun CollectBankAccountConfiguration.toCreateSessionParams(
        clientSecret: String
    ): CreateFinancialConnectionsSessionParams = when (this) {
        is CollectBankAccountConfiguration.USBankAccount -> {
            CreateFinancialConnectionsSessionParams(
                clientSecret = clientSecret,
                customerName = name,
                customerEmailAddress = email
            )
        }

        is CollectBankAccountConfiguration.InstantDebits -> {
            TODO("Instant Debits not supported yet. This will create a FCSession for ID.")
        }
    }
}
