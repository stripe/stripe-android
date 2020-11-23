package com.stripe.android.paymentsheet

import android.content.Context
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentController
import com.stripe.android.PaymentSessionPrefs
import com.stripe.android.StripePaymentController
import com.stripe.android.model.ListPaymentMethodsParams
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
                publishableKey,
                PRODUCT_USAGE,
                ApiRequest.Options(ephemeralKey, stripeAccountId)
            )
        }

        val defaultPaymentMethodId = paymentSessionPrefs.getPaymentMethodId(customerId)

        // load default payment option

        return result.fold(
            onSuccess = { paymentMethods ->
                Result.Success(
                    DefaultPaymentSheetFlowController(
                        createPaymentController(),
                        publishableKey,
                        stripeAccountId,
                        DefaultPaymentSheetFlowController.Args.Default(
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
                createPaymentController(),
                publishableKey,
                stripeAccountId,
                DefaultPaymentSheetFlowController.Args.Guest(
                    clientSecret
                ),
                emptyList(),
                null
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
