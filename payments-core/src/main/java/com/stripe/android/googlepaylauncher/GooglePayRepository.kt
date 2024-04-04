package com.stripe.android.googlepaylauncher

import android.content.Context
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentsClient
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.core.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

fun interface GooglePayRepository {
    fun isReady(): Flow<Boolean>

    object Disabled : GooglePayRepository {
        override fun isReady(): Flow<Boolean> = flowOf(false)
    }
}

/**
 * The default implementation of [GooglePayRepository].  Using the individual values as parameters
 * so it can be shared with [GooglePayLauncher] and [GooglePayPaymentMethodLauncher]
 */
@Singleton
internal class DefaultGooglePayRepository(
    private val context: Context,
    private val environment: GooglePayEnvironment,
    private val billingAddressParameters: GooglePayJsonFactory.BillingAddressParameters,
    private val existingPaymentMethodRequired: Boolean,
    private val allowCreditCards: Boolean,
    private val paymentsClientFactory: PaymentsClientFactory = DefaultPaymentsClientFactory(context),
    private val logger: Logger = Logger.noop(),
) : GooglePayRepository {

    @Inject
    internal constructor(
        context: Context,
        googlePayConfig: GooglePayPaymentMethodLauncher.Config,
        logger: Logger,
    ) : this(
        context.applicationContext,
        googlePayConfig.environment,
        googlePayConfig.billingAddressConfig.convert(),
        googlePayConfig.existingPaymentMethodRequired,
        googlePayConfig.allowCreditCards,
        DefaultPaymentsClientFactory(context),
        logger
    )

    private val googlePayJsonFactory = GooglePayJsonFactory(context)

    private val paymentsClient: PaymentsClient by lazy {
        paymentsClientFactory.create(environment)
    }

    /**
     * @return a [Flow] that represents the result of a [PaymentsClient.isReadyToPay] operation.
     *
     * See [Google Pay API docs](https://developers.google.com/android/reference/com/google/android/gms/wallet/PaymentsClient#isReadyToPay(com.google.android.gms.wallet.IsReadyToPayRequest))
     * for more details.
     */
    override fun isReady(): Flow<Boolean> = flow {
        emit(isReadyAsync())
    }

    private suspend fun isReadyAsync(): Boolean {
        val request = runCatching {
            IsReadyToPayRequest.fromJson(
                googlePayJsonFactory.createIsReadyToPayRequest(
                    billingAddressParameters = billingAddressParameters,
                    existingPaymentMethodRequired = existingPaymentMethodRequired,
                    allowCreditCards = allowCreditCards
                ).toString()
            )
        }.getOrElse {
            // TODO (samer-stripe): Add unexpected error event here
            logger.error("Google Pay json parsing failed.", it)

            return false
        }

        val isReady = runCatching {
            paymentsClient.isReadyToPay(request).await()
        }.onFailure {
            // TODO (samer-stripe): Add error event here
            logger.error("Google Pay check failed.", it)
        }.getOrDefault(false)

        logger.info("Google Pay ready? $isReady")

        return isReady
    }
}
