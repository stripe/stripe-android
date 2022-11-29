package com.stripe.android.paymentsheet.state

import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethod.Type.Link
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.ClientSecret
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.model.StripeIntentValidator
import com.stripe.android.paymentsheet.model.getSupportedSavedCustomerPMs
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.repositories.StripeIntentRepository
import com.stripe.android.paymentsheet.repositories.initializeRepositoryAndGetStripeIntent
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.ui.core.forms.resources.ResourceRepository
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
        clientSecret: ClientSecret,
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
    private val stripeIntentRepository: StripeIntentRepository,
    private val stripeIntentValidator: StripeIntentValidator,
    private val customerRepository: CustomerRepository,
    private val lpmResourceRepository: ResourceRepository<LpmRepository>,
    private val logger: Logger,
    private val eventReporter: EventReporter,
    @IOContext private val workContext: CoroutineContext,
) : PaymentSheetLoader {

    override suspend fun load(
        clientSecret: ClientSecret,
        paymentSheetConfiguration: PaymentSheet.Configuration?
    ) = withContext(workContext) {
        val isGooglePayReady = isGooglePayReady(paymentSheetConfiguration)
        runCatching {
            retrieveStripeIntent(clientSecret)
        }.fold(
            onSuccess = { stripeIntent ->
                val isLinkReady = stripeIntent.paymentMethodTypes.contains(Link.code)

                create(
                    clientSecret = clientSecret,
                    stripeIntent = stripeIntent,
                    customerConfig = paymentSheetConfiguration?.customer,
                    config = paymentSheetConfiguration,
                    isGooglePayReady = isGooglePayReady,
                    isLinkReady = isLinkReady
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
        clientSecret: ClientSecret,
        stripeIntent: StripeIntent,
        customerConfig: PaymentSheet.CustomerConfiguration?,
        config: PaymentSheet.Configuration?,
        isGooglePayReady: Boolean,
        isLinkReady: Boolean
    ): PaymentSheetLoader.Result {
        val prefsRepository = prefsRepositoryFactory(customerConfig)

        val paymentMethods = if (customerConfig != null) {
            retrieveCustomerPaymentMethods(
                stripeIntent,
                config,
                customerConfig
            )
        } else {
            emptyList()
        }

        val savedSelection = retrieveSavedPaymentSelection(
            prefsRepository,
            isGooglePayReady,
            isLinkReady,
            paymentMethods
        )

        return PaymentSheetLoader.Result.Success(
            PaymentSheetState.Full(
                config = config,
                clientSecret = clientSecret,
                stripeIntent = stripeIntent,
                customerPaymentMethods = paymentMethods,
                savedSelection = savedSelection,
                isGooglePayReady = isGooglePayReady,
                isLinkEnabled = isLinkReady,
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
            lpmResourceRepository.getRepository()
        ).mapNotNull {
            // The SDK is only able to parse customer LPMs
            // that are hard coded in the SDK.
            PaymentMethod.Type.fromCode(it.code)
        }

        return customerRepository.getPaymentMethods(
            customerConfig,
            paymentMethodTypes
        ).filter { paymentMethod ->
            paymentMethod.hasExpectedDetails()
        }
    }

    private suspend fun retrieveSavedPaymentSelection(
        prefsRepository: PrefsRepository,
        isGooglePayReady: Boolean,
        isLinkReady: Boolean,
        paymentMethods: List<PaymentMethod>
    ): SavedSelection {
        val savedSelection = prefsRepository.getSavedSelection(isGooglePayReady, isLinkReady)
        if (savedSelection != SavedSelection.None) {
            return savedSelection
        }

        // No saved selection has been set yet, so we'll initialize it with a default
        // value based on which payment methods are available.
        val paymentSelection = determineDefaultPaymentSelection(
            isGooglePayReady,
            isLinkReady,
            paymentMethods
        )

        prefsRepository.savePaymentSelection(paymentSelection)

        return prefsRepository.getSavedSelection(isGooglePayReady, isLinkReady)
    }

    private fun determineDefaultPaymentSelection(
        isGooglePayReady: Boolean,
        isLinkReady: Boolean,
        paymentMethods: List<PaymentMethod>
    ): PaymentSelection? {
        return when {
            paymentMethods.isNotEmpty() -> PaymentSelection.Saved(paymentMethods.first())
            isLinkReady -> PaymentSelection.Link
            isGooglePayReady -> PaymentSelection.GooglePay
            else -> null
        }
    }

    private suspend fun retrieveStripeIntent(
        clientSecret: ClientSecret
    ) = stripeIntentValidator.requireValid(
        initializeRepositoryAndGetStripeIntent(
            lpmResourceRepository,
            stripeIntentRepository,
            clientSecret,
            eventReporter
        )
    )
}
