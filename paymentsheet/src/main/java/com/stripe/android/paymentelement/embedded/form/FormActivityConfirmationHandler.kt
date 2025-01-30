package com.stripe.android.paymentelement.embedded.form

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.ui.PrimaryButtonProcessingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class FormActivityConfirmationHandler @Inject constructor(
    private val intentConfiguration: PaymentSheet.IntentConfiguration,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    @ViewModelScope private val coroutineScope: CoroutineScope,
    confirmationHandlerFactory: ConfirmationHandler.Factory,
    private val logger: Logger
) {

    private val confirmationHandler = confirmationHandlerFactory.create(coroutineScope)
    val state = confirmationHandler.state

    // Move this out likely
    private val _primaryButtonProcessingState: MutableStateFlow<PrimaryButtonProcessingState> =
        MutableStateFlow(PrimaryButtonProcessingState.Idle(null))
    val primaryButtonProcessingState: StateFlow<PrimaryButtonProcessingState> = _primaryButtonProcessingState

    private val _error: MutableStateFlow<ResolvableString?> = MutableStateFlow(null)
    val error: StateFlow<ResolvableString?> = _error

    fun register(activityResultCaller: ActivityResultCaller, lifecycleOwner: LifecycleOwner) {
        confirmationHandler.register(activityResultCaller, lifecycleOwner)
    }

    var setResultAndDismiss: ((ConfirmationHandler.Result?) -> Unit)? = null

    init {
        coroutineScope.launch {
            confirmationHandler.state.collectLatest {
                val primaryState = when (it) {
                    is ConfirmationHandler.State.Complete -> {
                        when (it.result) {
                            is ConfirmationHandler.Result.Canceled -> PrimaryButtonProcessingState.Idle(null)
                            is ConfirmationHandler.Result.Failed -> PrimaryButtonProcessingState.Idle(it.result.message)
                            is ConfirmationHandler.Result.Succeeded -> PrimaryButtonProcessingState.Completed
                        }
                        PrimaryButtonProcessingState.Completed
                    }
                    is ConfirmationHandler.State.Confirming -> PrimaryButtonProcessingState.Processing
                    ConfirmationHandler.State.Idle -> PrimaryButtonProcessingState.Idle(null)
                }
                _primaryButtonProcessingState.value = primaryState
            }
        }
    }

    fun confirm(
        createParams: PaymentMethodCreateParams,
        optionParams: PaymentMethodOptionsParams?,
        shouldSave: Boolean
    ) {
        coroutineScope.launch {
            when (val result = confirmIntent(createParams, optionParams, shouldSave)) {
                is ConfirmationHandler.Result.Canceled -> {}
                is ConfirmationHandler.Result.Failed -> {
                    _error.value = result.message
                }
                is ConfirmationHandler.Result.Succeeded -> {
                    delay(1000)
                    setResultAndDismiss?.invoke(result)
                }
                null -> {}
            }
        }
    }

    private suspend fun confirmIntent(
        createParams: PaymentMethodCreateParams,
        optionParams: PaymentMethodOptionsParams?,
        shouldSave: Boolean
    ): ConfirmationHandler.Result? {
        return runCatching {
            val args = getArgs(createParams, optionParams, shouldSave)
            confirmationHandler.start(args)
            confirmationHandler.awaitResult()
        }.getOrElse {
            logger.error(
                msg = "DefaultFormActivityConfirmationHandler: Failed to confirm payment"
            )
            ConfirmationHandler.Result.Failed(
                Exception(),
                "Something went wrong".resolvableString,
                ConfirmationHandler.Result.Failed.ErrorType.Internal
            )
        }
    }

    private fun getArgs(
        createParams: PaymentMethodCreateParams,
        optionParams: PaymentMethodOptionsParams?,
        shouldSave: Boolean
    ): ConfirmationHandler.Args {
        return ConfirmationHandler.Args(
            intent = paymentMethodMetadata.stripeIntent,
            confirmationOption = PaymentMethodConfirmationOption.New(
                createParams = createParams,
                optionsParams = optionParams,
                shouldSave = shouldSave
            ),
            appearance = PaymentSheet.Appearance(),
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(intentConfiguration),
            shippingDetails = paymentMethodMetadata.shippingDetails
        )
    }
}
