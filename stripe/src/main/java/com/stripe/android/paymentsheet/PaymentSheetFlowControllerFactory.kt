package com.stripe.android.paymentsheet

import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentController
import com.stripe.android.PaymentSessionPrefs
import com.stripe.android.StripePaymentController
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentsheet.analytics.DefaultEventReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.SessionId
import com.stripe.android.paymentsheet.model.PaymentIntentValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

internal class PaymentSheetFlowControllerFactory(
    private val activity: ComponentActivity,
    private val stripeRepository: StripeRepository,
    private val publishableKey: String,
    private val stripeAccountId: String?,
    private val paymentSessionPrefs: PaymentSessionPrefs,
    private val workContext: CoroutineContext
) {
    private val sessionId = SessionId()
    private val paymentIntentValidator = PaymentIntentValidator()

    constructor(
        activity: ComponentActivity,
        workContext: CoroutineContext = Dispatchers.IO
    ) : this(
        activity,
        PaymentConfiguration.getInstance(activity),
        workContext
    )

    private constructor(
        activity: ComponentActivity,
        config: PaymentConfiguration,
        workContext: CoroutineContext
    ) : this(
        activity,
        StripeApiRepository(
            activity,
            config.publishableKey
        ),
        config.publishableKey,
        config.stripeAccountId,
        PaymentSessionPrefs.Default(activity),
        workContext
    )

    fun create(
        clientSecret: String,
        config: PaymentSheet.Configuration,
        onComplete: (PaymentSheet.FlowController.Result) -> Unit
    ) {
        val job = CoroutineScope(workContext).launch {
            dispatchResult(
                config.customer?.let { customerConfig ->
                    createWithCustomer(
                        clientSecret,
                        customerConfig,
                        config
                    )
                } ?: createWithoutCustomer(clientSecret, config),
                onComplete
            )
        }
        registerJob(job)
    }

    fun create(
        clientSecret: String,
        onComplete: (PaymentSheet.FlowController.Result) -> Unit
    ) {
        val job = CoroutineScope(workContext).launch {
            dispatchResult(
                createWithoutCustomer(
                    clientSecret,
                    config = null
                ),
                onComplete
            )
        }
        registerJob(job)
    }

    private suspend fun dispatchResult(
        result: Result,
        onComplete: (PaymentSheet.FlowController.Result) -> Unit
    ) = withContext(Dispatchers.Main) {
        when (result) {
            is Result.Success -> {
                onComplete(
                    PaymentSheet.FlowController.Result.Success(result.flowController)
                )
            }
            is Result.Failure -> {
                onComplete(
                    PaymentSheet.FlowController.Result.Failure(result.throwable)
                )
            }
        }
    }

    private suspend fun createWithCustomer(
        clientSecret: String,
        customerConfig: PaymentSheet.CustomerConfiguration,
        config: PaymentSheet.Configuration?
    ): Result {
        // load default payment option
        val defaultPaymentMethodId = paymentSessionPrefs.getPaymentMethodId(customerConfig.id)

        return runCatching {
            retrievePaymentIntent(clientSecret)
        }.fold(
            onSuccess = { paymentIntent ->
                val paymentMethodTypes = paymentIntent.paymentMethodTypes.mapNotNull {
                    PaymentMethod.Type.fromCode(it)
                }
                retrieveAllPaymentMethods(
                    types = paymentMethodTypes,
                    customerConfig
                ).let { paymentMethods ->
                    Result.Success(
                        DefaultPaymentSheetFlowController(
                            paymentController = createPaymentController(),
                            eventReporter = DefaultEventReporter(
                                mode = EventReporter.Mode.Custom,
                                sessionId,
                                activity
                            ),
                            args = DefaultPaymentSheetFlowController.Args(
                                clientSecret,
                                config
                            ),
                            publishableKey = publishableKey,
                            stripeAccountId = stripeAccountId,
                            paymentIntent = paymentIntent,
                            paymentMethodTypes = paymentMethodTypes,
                            paymentMethods = paymentMethods,
                            defaultPaymentMethodId = defaultPaymentMethodId,
                            sessionId = sessionId
                        )
                    )
                }
            },
            onFailure = {
                Result.Failure(it)
            }
        )
    }

    private suspend fun createWithoutCustomer(
        clientSecret: String,
        config: PaymentSheet.Configuration?
    ): Result {
        return runCatching {
            retrievePaymentIntent(clientSecret)
        }.fold(
            onSuccess = { paymentIntent ->
                val paymentMethodTypes = paymentIntent.paymentMethodTypes
                    .mapNotNull {
                        PaymentMethod.Type.fromCode(it)
                    }

                Result.Success(
                    DefaultPaymentSheetFlowController(
                        paymentController = createPaymentController(),
                        eventReporter = DefaultEventReporter(
                            mode = EventReporter.Mode.Custom,
                            sessionId,
                            activity
                        ),
                        publishableKey = publishableKey,
                        stripeAccountId = stripeAccountId,
                        args = DefaultPaymentSheetFlowController.Args(
                            clientSecret,
                            config = config
                        ),
                        paymentIntent = paymentIntent,
                        paymentMethodTypes = paymentMethodTypes,
                        paymentMethods = emptyList(),
                        sessionId = sessionId,
                        defaultPaymentMethodId = null
                    )
                )
            },
            onFailure = {
                Result.Failure(it)
            }
        )
    }

    private suspend fun retrieveAllPaymentMethods(
        types: List<PaymentMethod.Type>,
        customerConfig: PaymentSheet.CustomerConfiguration
    ): List<PaymentMethod> {
        return types.flatMap { type ->
            retrievePaymentMethodsByType(type, customerConfig)
        }
    }

    /**
     * Return empty list on failure.
     */
    private suspend fun retrievePaymentMethodsByType(
        type: PaymentMethod.Type,
        customerConfig: PaymentSheet.CustomerConfiguration
    ): List<PaymentMethod> {
        return runCatching {
            stripeRepository.getPaymentMethods(
                ListPaymentMethodsParams(
                    customerId = customerConfig.id,
                    paymentMethodType = type
                ),
                publishableKey,
                PRODUCT_USAGE,
                ApiRequest.Options(customerConfig.ephemeralKeySecret, stripeAccountId)
            )
        }.getOrDefault(emptyList())
    }

    private suspend fun retrievePaymentIntent(
        clientSecret: String
    ): PaymentIntent {
        return paymentIntentValidator.requireValid(
            requireNotNull(
                stripeRepository.retrievePaymentIntent(
                    clientSecret,
                    ApiRequest.Options(
                        publishableKey,
                        stripeAccountId
                    )
                )
            )
        )
    }

    private fun createPaymentController(): PaymentController {
        val config = PaymentConfiguration.getInstance(activity)
        val publishableKey = config.publishableKey
        val stripeAccountId = config.stripeAccountId
        return StripePaymentController(
            activity,
            publishableKey,
            stripeRepository,
            true
        )
    }

    private fun registerJob(job: Job) {
        activity.lifecycle.addObserver(
            object : LifecycleObserver {
                @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                fun onDestroy() {
                    job.cancel()
                }
            }
        )
    }

    sealed class Result {
        class Success(
            val flowController: PaymentSheet.FlowController
        ) : Result()

        class Failure(
            val throwable: Throwable
        ) : Result()
    }

    private companion object {
        private val PRODUCT_USAGE = setOf("PaymentSheet")
    }
}
