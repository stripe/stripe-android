package com.stripe.android.paymentsheet.flowcontroller

import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.model.ClientSecret
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.model.StripeIntentValidator
import com.stripe.android.paymentsheet.model.getSupportedSavedCustomerPMs
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.repositories.StripeIntentRepository
import com.stripe.android.ui.core.forms.resources.ResourceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
internal class DefaultFlowControllerInitializer @Inject constructor(
    private val prefsRepositoryFactory: @JvmSuppressWildcards
    (PaymentSheet.CustomerConfiguration?) -> PrefsRepository,
    private val googlePayRepositoryFactory: @JvmSuppressWildcards
    (GooglePayEnvironment) -> GooglePayRepository,
    private val stripeIntentRepository: StripeIntentRepository,
    private val stripeIntentValidator: StripeIntentValidator,
    private val customerRepository: CustomerRepository,
    private val resourceRepository: ResourceRepository,
    private val logger: Logger,
    @IOContext private val workContext: CoroutineContext
) : FlowControllerInitializer {

    override suspend fun init(
        clientSecret: ClientSecret,
        paymentSheetConfiguration: PaymentSheet.Configuration?
    ) = withContext(workContext) {
        val isGooglePayReady = isGooglePayReady(paymentSheetConfiguration)
        paymentSheetConfiguration?.customer?.let { customerConfig ->
            createWithCustomer(
                clientSecret,
                customerConfig,
                paymentSheetConfiguration,
                isGooglePayReady
            )
        } ?: createWithoutCustomer(
            clientSecret,
            paymentSheetConfiguration,
            isGooglePayReady
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

    private suspend fun createWithCustomer(
        clientSecret: ClientSecret,
        customerConfig: PaymentSheet.CustomerConfiguration,
        config: PaymentSheet.Configuration?,
        isGooglePayReady: Boolean
    ): FlowControllerInitializer.InitResult {
        val prefsRepository = prefsRepositoryFactory(customerConfig)

        return runCatching {
            retrieveStripeIntent(clientSecret)
        }.fold(
            onSuccess = { stripeIntent ->
                val paymentMethodTypes = getSupportedSavedCustomerPMs(
                    stripeIntent,
                    config,
                    resourceRepository.getLpmRepository()
                ).mapNotNull {
                    // The SDK is only able to parse customer LPMs
                    // that are hard coded in the SDK.
                    PaymentMethod.Type.fromCode(it.code)
                }
                customerRepository.getPaymentMethods(
                    customerConfig,
                    paymentMethodTypes
                ).filter { paymentMethod ->
                    paymentMethod.hasExpectedDetails()
                }.let { paymentMethods ->

                    setLastSavedPaymentMethod(prefsRepository, isGooglePayReady, paymentMethods)

                    FlowControllerInitializer.InitResult.Success(
                        InitData(
                            config = config,
                            clientSecret = clientSecret,
                            stripeIntent = stripeIntent,
                            paymentMethods = paymentMethods,
                            savedSelection = prefsRepository.getSavedSelection(isGooglePayReady),
                            isGooglePayReady = isGooglePayReady
                        )
                    )
                }
            },
            onFailure = {
                logger.error("Failure initializing FlowController", it)
                FlowControllerInitializer.InitResult.Failure(it)
            }
        )
    }

    private suspend fun createWithoutCustomer(
        clientSecret: ClientSecret,
        config: PaymentSheet.Configuration?,
        isGooglePayReady: Boolean
    ): FlowControllerInitializer.InitResult {
        return runCatching {
            retrieveStripeIntent(clientSecret)
        }.fold(
            onSuccess = { stripeIntent ->
                val savedSelection = if (isGooglePayReady) {
                    SavedSelection.GooglePay
                } else {
                    SavedSelection.None
                }

                FlowControllerInitializer.InitResult.Success(
                    InitData(
                        config = config,
                        clientSecret = clientSecret,
                        stripeIntent = stripeIntent,
                        paymentMethods = emptyList(),
                        savedSelection = savedSelection,
                        isGooglePayReady = isGooglePayReady
                    )
                )
            },
            onFailure = {
                logger.error("Failure initializing FlowController", it)
                FlowControllerInitializer.InitResult.Failure(it)
            }
        )
    }

    private suspend fun setLastSavedPaymentMethod(
        prefsRepository: PrefsRepository,
        isGooglePayReady: Boolean,
        paymentMethods: List<PaymentMethod>
    ) {
        if (prefsRepository.getSavedSelection(isGooglePayReady) == SavedSelection.None) {
            when {
                paymentMethods.isNotEmpty() -> {
                    PaymentSelection.Saved(paymentMethods.first())
                }
                isGooglePayReady -> {
                    PaymentSelection.GooglePay
                }
                else -> {
                    null
                }
            }?.let {
                prefsRepository.savePaymentSelection(it)
            }
        }
    }

    private suspend fun retrieveStripeIntent(
        clientSecret: ClientSecret
    ): StripeIntent {
        return stripeIntentValidator.requireValid(
            stripeIntentRepository.get(clientSecret)
        )
    }
}
