package com.stripe.android.paymentsheet.state

import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.model.StripeIntentValidator
import com.stripe.android.paymentsheet.model.getSupportedSavedCustomerPMs
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.ui.core.forms.resources.LpmRepository.ServerSpecState
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * Loads the information necessary to display [PaymentSheet], either directly or via
 * [PaymentSheet.FlowController].
 */
internal interface PaymentSheetLoader {

    suspend fun load(
        initializationMode: PaymentSheet.InitializationMode,
        paymentSheetConfiguration: PaymentSheet.Configuration? = null
    ): Result

    sealed class Result {
        data class Success(val state: PaymentSheetState.Full) : Result()
        class Failure(val throwable: Throwable) : Result()
    }
}

@Singleton
internal class DefaultPaymentSheetLoader @Inject constructor(
    private val prefsRepositoryFactory: @JvmSuppressWildcards (PaymentSheet.CustomerConfiguration?) -> PrefsRepository,
    private val googlePayRepositoryFactory: @JvmSuppressWildcards (GooglePayEnvironment) -> GooglePayRepository,
    private val elementsSessionRepository: ElementsSessionRepository,
    private val stripeIntentValidator: StripeIntentValidator,
    private val customerRepository: CustomerRepository,
    private val lpmRepository: LpmRepository,
    private val logger: Logger,
    private val eventReporter: EventReporter,
    @IOContext private val workContext: CoroutineContext,
) : PaymentSheetLoader {

    override suspend fun load(
        initializationMode: PaymentSheet.InitializationMode,
        paymentSheetConfiguration: PaymentSheet.Configuration?
    ): PaymentSheetLoader.Result = withContext(workContext) {
        val isGooglePayReady = isGooglePayReady(paymentSheetConfiguration)

        runCatching {
            retrieveElementsSession(
                initializationMode = initializationMode,
                configuration = paymentSheetConfiguration,
            )
        }.fold(
            onSuccess = { stripeIntent ->
                create(
                    stripeIntent = stripeIntent,
                    customerConfig = paymentSheetConfiguration?.customer,
                    config = paymentSheetConfiguration,
                    isGooglePayReady = isGooglePayReady,
                )
            },
            onFailure = {
                logger.error("Failure initializing FlowController", it)
                PaymentSheetLoader.Result.Failure(it)
            }
        )
    }

    private suspend fun isGooglePayReady(
        paymentSheetConfiguration: PaymentSheet.Configuration?
    ): Boolean {
        return paymentSheetConfiguration?.googlePay?.environment?.let { environment ->
            googlePayRepositoryFactory(
                when (environment) {
                    PaymentSheet.GooglePayConfiguration.Environment.Production ->
                        GooglePayEnvironment.Production
                    PaymentSheet.GooglePayConfiguration.Environment.Test ->
                        GooglePayEnvironment.Test
                }
            )
        }?.isReady()?.first() ?: false
    }

    private suspend fun create(
        stripeIntent: StripeIntent,
        customerConfig: PaymentSheet.CustomerConfiguration?,
        config: PaymentSheet.Configuration?,
        isGooglePayReady: Boolean,
    ): PaymentSheetLoader.Result = coroutineScope {
        val prefsRepository = prefsRepositoryFactory(customerConfig)

        val paymentMethods = async {
            if (customerConfig != null) {
                retrieveCustomerPaymentMethods(
                    stripeIntent,
                    config,
                    customerConfig
                )
            } else {
                emptyList()
            }
        }

        val savedSelection = async {
            retrieveSavedPaymentSelection(
                prefsRepository,
                isGooglePayReady,
                paymentMethods.await()
            )
        }

        return@coroutineScope PaymentSheetLoader.Result.Success(
            PaymentSheetState.Full(
                config = config,
                stripeIntent = stripeIntent,
                customerPaymentMethods = paymentMethods.await(),
                savedSelection = savedSelection.await(),
                isGooglePayReady = isGooglePayReady,
                newPaymentSelection = null,
            )
        )
    }

    private suspend fun retrieveCustomerPaymentMethods(
        stripeIntent: StripeIntent,
        config: PaymentSheet.Configuration?,
        customerConfig: PaymentSheet.CustomerConfiguration
    ): List<PaymentMethod> {
        val paymentMethodTypes = getSupportedSavedCustomerPMs(
            stripeIntent,
            config,
            lpmRepository,
        ).mapNotNull {
            // The SDK is only able to parse customer LPMs
            // that are hard coded in the SDK.
            PaymentMethod.Type.fromCode(it.code)
        }

        return customerRepository.getPaymentMethods(
            customerConfig,
            paymentMethodTypes
        ).filter { paymentMethod ->
            paymentMethod.hasExpectedDetails() &&
                // PayPal isn't supported yet as a saved payment method (backend limitation).
                paymentMethod.type != PaymentMethod.Type.PayPal
        }
    }

    private suspend fun retrieveSavedPaymentSelection(
        prefsRepository: PrefsRepository,
        isGooglePayReady: Boolean,
        paymentMethods: List<PaymentMethod>
    ): SavedSelection {
        val savedSelection = prefsRepository.getSavedSelection(isGooglePayReady)
        if (savedSelection != SavedSelection.None) {
            return savedSelection
        }

        // No saved selection has been set yet, so we'll initialize it with a default
        // value based on which payment methods are available.
        val paymentSelection = determineDefaultPaymentSelection(
            isGooglePayReady,
            paymentMethods
        )

        prefsRepository.savePaymentSelection(paymentSelection)

        return prefsRepository.getSavedSelection(isGooglePayReady)
    }

    private fun determineDefaultPaymentSelection(
        isGooglePayReady: Boolean,
        paymentMethods: List<PaymentMethod>
    ): PaymentSelection? {
        return when {
            paymentMethods.isNotEmpty() -> PaymentSelection.Saved(paymentMethods.first())
            isGooglePayReady -> PaymentSelection.GooglePay
            else -> null
        }
    }

    private suspend fun retrieveElementsSession(
        initializationMode: PaymentSheet.InitializationMode,
        configuration: PaymentSheet.Configuration?,
    ): StripeIntent {
        val elementsSession = elementsSessionRepository.get(
            initializationMode = initializationMode,
            configuration = configuration,
        )

        lpmRepository.update(
            stripeIntent = elementsSession.stripeIntent,
            serverLpmSpecs = elementsSession.paymentMethodSpecs,
        )

        if (lpmRepository.serverSpecLoadingState is ServerSpecState.ServerNotParsed) {
            eventReporter.onLpmSpecFailure()
        }

        return stripeIntentValidator.requireValid(elementsSession.stripeIntent)
    }
}
