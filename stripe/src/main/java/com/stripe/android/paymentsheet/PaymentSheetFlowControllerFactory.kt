package com.stripe.android.paymentsheet

import android.content.Context
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentSessionPrefs
import com.stripe.android.StripeApiRepository
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.StripeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

internal class PaymentSheetFlowControllerFactory(
    private val stripeRepository: StripeRepository,
    private val config: PaymentConfiguration,
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
        StripeApiRepository(
            context,
            config.publishableKey,
            workContext = workContext
        ),
        config,
        PaymentSessionPrefs.Default(context),
        workContext
    )

    fun create(
        clientSecret: String,
        ephemeralKey: String,
        customerId: String,
        onComplete: (PaymentSheetFlowController.Result) -> Unit
    ) {
        CoroutineScope(workContext).launch {
            dispatchResult(
                createWithDefaultArgs(clientSecret, ephemeralKey, customerId),
                onComplete
            )
        }
    }

    fun create(
        clientSecret: String,
        onComplete: (PaymentSheetFlowController.Result) -> Unit
    ) {
        CoroutineScope(workContext).launch {
            dispatchResult(
                createWithGuestArgs(clientSecret),
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
        customerId: String
    ): Result {
        val result = runCatching {
            stripeRepository.getPaymentMethods(
                ListPaymentMethodsParams(
                    customerId = customerId,
                    paymentMethodType = PaymentMethod.Type.Card
                ),
                config.publishableKey,
                PRODUCT_USAGE,
                ApiRequest.Options(ephemeralKey, config.stripeAccountId)
            )
        }

        val defaultPaymentMethodId = paymentSessionPrefs.getPaymentMethodId(customerId)

        // load default payment option

        return result.fold(
            onSuccess = { paymentMethods ->
                Result.Success(
                    DefaultPaymentSheetFlowController(
                        PaymentSheetActivityStarter.Args.Default(
                            clientSecret,
                            ephemeralKey,
                            customerId
                        ),
                        paymentMethods,
                        defaultPaymentMethodId
                    )
                )
            },
            onFailure = {
                Result.Failure(it)
            }
        )
    }

    private suspend fun createWithGuestArgs(
        clientSecret: String
    ): Result {
        return Result.Success(
            DefaultPaymentSheetFlowController(
                PaymentSheetActivityStarter.Args.Guest(
                    clientSecret
                ),
                emptyList(),
                null
            )
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
