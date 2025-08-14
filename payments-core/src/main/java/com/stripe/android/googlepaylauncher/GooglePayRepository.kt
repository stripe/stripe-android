package com.stripe.android.googlepaylauncher

import android.content.Context
import androidx.annotation.RestrictTo
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentsClient
import com.stripe.android.CardBrandFilter
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.StripeException
import com.stripe.android.payments.core.analytics.ErrorReporter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

fun interface GooglePayRepository {
    fun isReady(): Flow<Boolean>

    object Disabled : GooglePayRepository {
        override fun isReady(): Flow<Boolean> = flowOf(false)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        private val defaultFactory = DefaultGooglePayAvailabilityClient.Factory()

        @Volatile
        var googlePayAvailabilityClientFactory: GooglePayAvailabilityClient.Factory = defaultFactory

        fun resetFactory() {
            googlePayAvailabilityClientFactory = defaultFactory
        }
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
    private val errorReporter: ErrorReporter,
    private val logger: Logger = Logger.noop(),
    private val cardBrandFilter: CardBrandFilter = DefaultCardBrandFilter,
) : GooglePayRepository {

    @Inject
    internal constructor(
        context: Context,
        googlePayConfig: GooglePayPaymentMethodLauncher.Config,
        logger: Logger,
        errorReporter: ErrorReporter,
        cardBrandFilter: CardBrandFilter
    ) : this(
        context.applicationContext,
        googlePayConfig.environment,
        googlePayConfig.billingAddressConfig.convert(),
        googlePayConfig.existingPaymentMethodRequired,
        googlePayConfig.allowCreditCards,
        DefaultPaymentsClientFactory(context),
        errorReporter,
        logger,
        cardBrandFilter
    )

    private val googlePayJsonFactory = GooglePayJsonFactory(context, cardBrandFilter = cardBrandFilter)

    private val googlePayAvailabilityClient: GooglePayAvailabilityClient by lazy {
        GooglePayRepository.googlePayAvailabilityClientFactory.create(
            paymentsClient = paymentsClientFactory.create(environment)
        )
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
            errorReporter.report(
                ErrorReporter.UnexpectedErrorEvent.GOOGLE_PAY_JSON_REQUEST_PARSING,
                StripeException.create(it)
            )

            logger.error("Google Pay json parsing failed.", it)

            return false
        }

        val isReady = runCatching {
            googlePayAvailabilityClient.isReady(request)
        }.onFailure {
            errorReporter.report(
                ErrorReporter.ExpectedErrorEvent.GOOGLE_PAY_IS_READY_API_CALL,
                StripeException.create(it)
            )

            logger.error("Google Pay check failed.", it)
        }.getOrDefault(false)

        logger.info("Google Pay ready? $isReady")

        return isReady
    }
}
