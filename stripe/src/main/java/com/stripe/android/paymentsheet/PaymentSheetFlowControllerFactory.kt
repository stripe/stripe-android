package com.stripe.android.paymentsheet

import android.content.Context
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

internal class PaymentSheetFlowControllerFactory(
    private val context: Context,
    private val stripeRepository: StripeRepository,
    private val publishableKey: String,
    private val stripeAccountId: String?,
    private val paymentSessionPrefs: PaymentSessionPrefs,
    private val workContext: CoroutineContext
) {
    constructor(
        context: Context,
        workContext: CoroutineContext = Dispatchers.IO
    ) : this(
        context,
        PaymentConfiguration.getInstance(context),
        workContext
    )

    private constructor(
        context: Context,
        config: PaymentConfiguration,
        workContext: CoroutineContext
    ) : this(
        context,
        StripeApiRepository(
            context,
            config.publishableKey
        ),
        config.publishableKey,
        config.stripeAccountId,
        PaymentSessionPrefs.Default(context),
        workContext
    )

    fun create(
        clientSecret: String,
        ephemeralKey: String,
        customerId: String,
        googlePayConfig: PaymentSheetGooglePayConfig? = null,
        onComplete: (PaymentSheetFlowController.Result) -> Unit
    ) {
        CoroutineScope(workContext).launch {
            dispatchResult(
                createWithDefaultArgs(
                    clientSecret,
                    ephemeralKey,
                    customerId,
                    googlePayConfig
                ),
                onComplete
            )
        }
    }

    fun create(
        clientSecret: String,
        googlePayConfig: PaymentSheetGooglePayConfig? = null,
        onComplete: (PaymentSheetFlowController.Result) -> Unit
    ) {
        CoroutineScope(workContext).launch {
            dispatchResult(
                createWithGuestArgs(
                    clientSecret,
                    googlePayConfig
                ),
                onComplete
            )
        }
    }

    private suspend fun dispatchResult(
        result: Result,
        onComplete: (PaymentSheetFlowController.Result) -> Unit
    ) = withContext(Dispatchers.Main) {
        when (result) {
            is Result.Success -> {
                onComplete(
                    PaymentSheetFlowController.Result.Success(result.flowController)
                )
            }
            is Result.Failure -> {
                onComplete(
                    PaymentSheetFlowController.Result.Failure(result.throwable)
                )
            }
        }
    }

    private suspend fun createWithDefaultArgs(
        clientSecret: String,
        ephemeralKey: String,
        customerId: String,
        googlePayConfig: PaymentSheetGooglePayConfig? = null
    ): Result {
        // load default payment option
        val defaultPaymentMethodId = paymentSessionPrefs.getPaymentMethodId(customerId)

        return runCatching {
            requireNotNull(retrievePaymentIntent(clientSecret))
        }.fold(
            onSuccess = { paymentIntent ->
                val paymentMethodTypes = paymentIntent.paymentMethodTypes.mapNotNull {
                    PaymentMethod.Type.fromCode(it)
                }
                retrieveAllPaymentMethods(
                    types = paymentMethodTypes,
                    customerId = customerId,
                    ephemeralKey = ephemeralKey
                ).let { paymentMethods ->
                    Result.Success(
                        DefaultPaymentSheetFlowController(
                            paymentController = createPaymentController(),
                            args = DefaultPaymentSheetFlowController.Args.Default(
                                clientSecret,
                                ephemeralKey,
                                customerId
                            ),
                            publishableKey = publishableKey,
                            stripeAccountId = stripeAccountId,
                            paymentMethodTypes = paymentMethodTypes,
                            paymentMethods = paymentMethods,
                            googlePayConfig = googlePayConfig,
                            defaultPaymentMethodId = defaultPaymentMethodId
                        )
                    )
                }
            },
            onFailure = {
                Result.Failure(it)
            }
        )
    }

    private suspend fun createWithGuestArgs(
        clientSecret: String,
        googlePayConfig: PaymentSheetGooglePayConfig? = null
    ): Result {
        return runCatching {
            requireNotNull(retrievePaymentIntent(clientSecret))
        }.fold(
            onSuccess = { paymentIntent ->
                val paymentMethodTypes = paymentIntent.paymentMethodTypes
                    .mapNotNull {
                        PaymentMethod.Type.fromCode(it)
                    }

                Result.Success(
                    DefaultPaymentSheetFlowController(
                        createPaymentController(),
                        publishableKey,
                        stripeAccountId,
                        DefaultPaymentSheetFlowController.Args.Guest(
                            clientSecret
                        ),
                        paymentMethodTypes = paymentMethodTypes,
                        paymentMethods = emptyList(),
                        googlePayConfig = googlePayConfig,
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
        customerId: String,
        ephemeralKey: String
    ): List<PaymentMethod> {
        return types.flatMap { type ->
            retrievePaymentMethodsByType(
                type,
                customerId,
                ephemeralKey
            )
        }
    }

    /**
     * Return empty list on failure.
     */
    private suspend fun retrievePaymentMethodsByType(
        type: PaymentMethod.Type,
        customerId: String,
        ephemeralKey: String
    ): List<PaymentMethod> {
        return runCatching {
            stripeRepository.getPaymentMethods(
                ListPaymentMethodsParams(
                    customerId = customerId,
                    paymentMethodType = type
                ),
                publishableKey,
                PRODUCT_USAGE,
                ApiRequest.Options(ephemeralKey, stripeAccountId)
            )
        }.getOrDefault(emptyList())
    }

    private suspend fun retrievePaymentIntent(
        clientSecret: String
    ): PaymentIntent? {
        return stripeRepository.retrievePaymentIntent(
            clientSecret,
            ApiRequest.Options(
                publishableKey,
                stripeAccountId
            )
        )
    }

    private fun createPaymentController(): PaymentController {
        val config = PaymentConfiguration.getInstance(context)
        val publishableKey = config.publishableKey
        val stripeAccountId = config.stripeAccountId
        return StripePaymentController(
            context,
            publishableKey,
            stripeRepository,
            true
        )
    }

    sealed class Result {
        class Success(
            val flowController: PaymentSheetFlowController
        ) : Result()

        class Failure(
            val throwable: Throwable
        ) : Result()
    }

    private companion object {
        private val PRODUCT_USAGE = setOf("PaymentSheet")
    }
}
