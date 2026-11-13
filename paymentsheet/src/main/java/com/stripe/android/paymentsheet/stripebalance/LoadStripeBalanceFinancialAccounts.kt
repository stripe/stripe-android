package com.stripe.android.paymentsheet.stripebalance

import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Loads the buyer's financial accounts while the Payment Element loads, so the Stripe balance form has them when it
 * appears. Returns `null` if Stripe balance won't be offered or its accounts couldn't be loaded in time, in which case
 * the Payment Element doesn't offer Stripe balance.
 */
internal fun interface LoadStripeBalanceFinancialAccounts {
    suspend operator fun invoke(
        initializationMode: PaymentElementLoader.InitializationMode,
        elementsSession: ElementsSession,
        customerMetadata: CustomerMetadata?,
        apiConfiguration: ApiConfiguration.State,
    ): StripeBalanceFinancialAccounts?
}

internal class DefaultLoadStripeBalanceFinancialAccounts @Inject constructor(
    private val stripeBalanceRepository: StripeBalanceRepository,
    private val errorReporter: ErrorReporter,
    private val durationProvider: DurationProvider,
    @ViewModelScope private val viewModelScope: CoroutineScope,
    @IOContext private val workContext: CoroutineContext,
) : LoadStripeBalanceFinancialAccounts {

    override suspend fun invoke(
        initializationMode: PaymentElementLoader.InitializationMode,
        elementsSession: ElementsSession,
        customerMetadata: CustomerMetadata?,
        apiConfiguration: ApiConfiguration.State,
    ): StripeBalanceFinancialAccounts? {
        val stripeIntent = elementsSession.stripeIntent
        if (PaymentMethod.Type.StripeBalance.code !in stripeIntent.paymentMethodTypes) {
            return null
        }
        if (initializationMode is PaymentElementLoader.InitializationMode.CheckoutSession) {
            // TODO(porter) Stripe balance: Support Checkout Sessions once CheckoutClient has its own endpoint to list
            //  the buyer's financial accounts. `v1/elements/stripe_balance/customer_session/financial_accounts`
            //  requires a CustomerSession.
            errorReporter.report(ErrorReporter.UnexpectedErrorEvent.STRIPE_BALANCE_CHECKOUT_SESSION_UNSUPPORTED)
            return null
        }
        // The accounts can only be listed with a CustomerSession
        val customerSession = customerMetadata as? CustomerMetadata.CustomerSession ?: return null
        // Without an amount, e.g. when setting up, the server lists every account without eligibility
        val paymentIntent = stripeIntent as? PaymentIntent
        val currency = paymentIntent?.currency

        // The request runs outside of loading, since timing out can't interrupt a blocking network call
        val request = viewModelScope.async(workContext) {
            stripeBalanceRepository.listFinancialAccounts(
                customerSessionClientSecret = customerSession.customerSessionClientSecret,
                ephemeralKeySecret = customerSession.ephemeralKeySecret,
                amount = paymentIntent?.amount,
                currency = currency,
                apiConfiguration = apiConfiguration,
            )
        }
        val result = durationProvider.measureDuration(
            DurationProvider.Key.PaymentSheetLoadFetchStripeBalanceFinancialAccounts
        ) {
            withTimeoutOrNull(TIMEOUT) { request.await() }
        } ?: run {
            request.cancel()
            Result.failure(StripeBalanceFinancialAccountsTimeoutException(TIMEOUT))
        }

        return result.fold(
            onSuccess = { accounts ->
                StripeBalanceFinancialAccounts(accounts = accounts, currency = currency)
            },
            onFailure = { error ->
                errorReporter.report(
                    errorEvent = ErrorReporter.ExpectedErrorEvent.STRIPE_BALANCE_FINANCIAL_ACCOUNTS_LOAD_FAILURE,
                    stripeException = StripeException.create(error),
                )
                null
            },
        )
    }

    private class StripeBalanceFinancialAccountsTimeoutException(timeout: Duration) :
        Exception("Stripe balance financial accounts didn't load within $timeout")

    private companion object {
        val TIMEOUT = 5.seconds
    }
}
