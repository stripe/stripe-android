package com.stripe.android.paymentsheet.flowcontroller

import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.model.ClientSecret
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.model.StripeIntentValidator
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import com.stripe.android.paymentsheet.repositories.PaymentMethodsRepository
import com.stripe.android.paymentsheet.repositories.StripeIntentRepository
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

internal class DefaultFlowControllerInitializer(
    private val prefsRepositoryFactory: (String, Boolean) -> PrefsRepository,
    private val isGooglePayReadySupplier: suspend (PaymentSheet.GooglePayConfiguration.Environment?) -> Boolean,
    private val workContext: CoroutineContext
) : FlowControllerInitializer {
    private val stripeIntentValidator = StripeIntentValidator()

    private lateinit var stripeIntentRepository: StripeIntentRepository
    private lateinit var paymentMethodsRepository: PaymentMethodsRepository

    override suspend fun init(
        clientSecret: ClientSecret,
        stripeIntentRepository: StripeIntentRepository,
        paymentMethodsRepository: PaymentMethodsRepository,
        paymentSheetConfiguration: PaymentSheet.Configuration?
    ) = withContext(workContext) {
        this@DefaultFlowControllerInitializer.stripeIntentRepository = stripeIntentRepository
        this@DefaultFlowControllerInitializer.paymentMethodsRepository = paymentMethodsRepository

        val isGooglePayReady =
            clientSecret is PaymentIntentClientSecret && paymentSheetConfiguration?.let {
                isGooglePayReadySupplier(it.googlePay?.environment)
            } ?: false
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

    private suspend fun createWithCustomer(
        clientSecret: ClientSecret,
        customerConfig: PaymentSheet.CustomerConfiguration,
        config: PaymentSheet.Configuration?,
        isGooglePayReady: Boolean
    ): FlowControllerInitializer.InitResult {
        val prefsRepository = prefsRepositoryFactory(
            customerConfig.id,
            isGooglePayReady
        )

        return runCatching {
            retrieveStripeIntent(clientSecret)
        }.fold(
            onSuccess = { stripeIntent ->
                val paymentMethodTypes = stripeIntent.paymentMethodTypes.mapNotNull {
                    PaymentMethod.Type.fromCode(it)
                }.filter {
                    SupportedPaymentMethod.supportedSavedPaymentMethods.contains(it.code)
                }
                retrieveAllPaymentMethods(
                    types = paymentMethodTypes,
                    customerConfig
                ).filter { paymentMethod ->
                    paymentMethod.hasExpectedDetails()
                }.let { paymentMethods ->

                    setLastSavedPaymentMethod(prefsRepository, isGooglePayReady, paymentMethods)

                    FlowControllerInitializer.InitResult.Success(
                        InitData(
                            config = config,
                            clientSecret = clientSecret,
                            stripeIntent = stripeIntent,
                            paymentMethodTypes = paymentMethodTypes,
                            paymentMethods = paymentMethods,
                            savedSelection = prefsRepository.getSavedSelection(),
                            isGooglePayReady = isGooglePayReady
                        )
                    )
                }
            },
            onFailure = {
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
                val paymentMethodTypes = stripeIntent.paymentMethodTypes
                    .mapNotNull {
                        PaymentMethod.Type.fromCode(it)
                    }

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
                        paymentMethodTypes = paymentMethodTypes,
                        paymentMethods = emptyList(),
                        savedSelection = savedSelection,
                        isGooglePayReady = isGooglePayReady
                    )
                )
            },
            onFailure = {
                FlowControllerInitializer.InitResult.Failure(it)
            }
        )
    }

    private suspend fun setLastSavedPaymentMethod(
        prefsRepository: PrefsRepository,
        isGooglePayReady: Boolean,
        paymentMethods: List<PaymentMethod>
    ) {
        if (prefsRepository.getSavedSelection() == SavedSelection.None) {
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

    private suspend fun retrieveAllPaymentMethods(
        types: List<PaymentMethod.Type>,
        customerConfig: PaymentSheet.CustomerConfiguration
    ): List<PaymentMethod> {
        return types.flatMap { type ->
            paymentMethodsRepository.get(customerConfig, type)
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
