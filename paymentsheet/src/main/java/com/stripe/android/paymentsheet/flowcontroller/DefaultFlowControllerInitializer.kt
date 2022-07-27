package com.stripe.android.paymentsheet.flowcontroller

import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.model.PaymentMethod
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
    val eventReporter: EventReporter,
    @IOContext private val workContext: CoroutineContext
) : FlowControllerInitializer {

    override suspend fun init(
        clientSecret: ClientSecret,
        paymentSheetConfiguration: PaymentSheet.Configuration?
    ) = withContext(workContext) {
        val isGooglePayReady = isGooglePayReady(paymentSheetConfiguration)
        runCatching {
            retrieveStripeIntent(clientSecret)
        }.fold(
            onSuccess = { stripeIntent ->
                val isLinkReady = LinkPaymentLauncher.LINK_ENABLED &&
                    stripeIntent.paymentMethodTypes.contains(PaymentMethod.Type.Link.code)

                paymentSheetConfiguration?.customer?.let { customerConfig ->
                    createWithCustomer(
                        clientSecret,
                        stripeIntent,
                        customerConfig,
                        paymentSheetConfiguration,
                        isGooglePayReady,
                        isLinkReady
                    )
                } ?: createWithoutCustomer(
                    clientSecret,
                    stripeIntent,
                    paymentSheetConfiguration,
                    isGooglePayReady,
                    isLinkReady
                )
            },
            onFailure = {
                logger.error("Failure initializing FlowController", it)
                FlowControllerInitializer.InitResult.Failure(it)
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

    private suspend fun createWithCustomer(
        clientSecret: ClientSecret,
        stripeIntent: StripeIntent,
        customerConfig: PaymentSheet.CustomerConfiguration,
        config: PaymentSheet.Configuration?,
        isGooglePayReady: Boolean,
        isLinkReady: Boolean
    ): FlowControllerInitializer.InitResult {
        val prefsRepository = prefsRepositoryFactory(customerConfig)

        val paymentMethodTypes = getSupportedSavedCustomerPMs(
            stripeIntent,
            config,
            resourceRepository.getLpmRepository()
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
        }.let { paymentMethods ->
            setLastSavedPaymentMethod(
                prefsRepository,
                isGooglePayReady,
                isLinkReady,
                paymentMethods
            )

            FlowControllerInitializer.InitResult.Success(
                InitData(
                    config = config,
                    clientSecret = clientSecret,
                    stripeIntent = stripeIntent,
                    paymentMethods = paymentMethods,
                    savedSelection = prefsRepository.getSavedSelection(
                        isGooglePayReady,
                        isLinkReady
                    ),
                    isGooglePayReady = isGooglePayReady
                )
            )
        }
    }

    private fun createWithoutCustomer(
        clientSecret: ClientSecret,
        stripeIntent: StripeIntent,
        config: PaymentSheet.Configuration?,
        isGooglePayReady: Boolean,
        isLinkReady: Boolean
    ): FlowControllerInitializer.InitResult {
        val savedSelection = if (isLinkReady) {
            SavedSelection.Link
        } else if (isGooglePayReady) {
            SavedSelection.GooglePay
        } else {
            SavedSelection.None
        }

        return FlowControllerInitializer.InitResult.Success(
            InitData(
                config = config,
                clientSecret = clientSecret,
                stripeIntent = stripeIntent,
                paymentMethods = emptyList(),
                savedSelection = savedSelection,
                isGooglePayReady = isGooglePayReady
            )
        )
    }

    private suspend fun setLastSavedPaymentMethod(
        prefsRepository: PrefsRepository,
        isGooglePayReady: Boolean,
        isLinkReady: Boolean,
        paymentMethods: List<PaymentMethod>
    ) {
        if (
            prefsRepository.getSavedSelection(isGooglePayReady, isLinkReady) == SavedSelection.None
        ) {
            when {
                paymentMethods.isNotEmpty() -> PaymentSelection.Saved(paymentMethods.first())
                isLinkReady -> PaymentSelection.Link
                isGooglePayReady -> PaymentSelection.GooglePay
                else -> null
            }?.let {
                prefsRepository.savePaymentSelection(it)
            }
        }
    }

    private suspend fun retrieveStripeIntent(
        clientSecret: ClientSecret
    ) = stripeIntentValidator.requireValid(
        initializeRepositoryAndGetStripeIntent(
            resourceRepository,
            stripeIntentRepository,
            clientSecret,
            eventReporter
        )
    )
}
