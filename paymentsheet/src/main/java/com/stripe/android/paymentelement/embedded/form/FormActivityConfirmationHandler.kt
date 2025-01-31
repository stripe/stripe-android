package com.stripe.android.paymentelement.embedded.form

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.toConfirmationOption
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.ui.PrimaryButtonProcessingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
internal class FormActivityConfirmationHandler @Inject constructor(
    private val initializationMode: PaymentElementLoader.InitializationMode,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    @ViewModelScope private val coroutineScope: CoroutineScope,
    confirmationHandlerFactory: ConfirmationHandler.Factory,
    private val logger: Logger,
    private val configuration: EmbeddedPaymentElement.Configuration,
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

    //var setResultAndDismiss: ((ConfirmationHandler.Result?) -> Unit)? = null

    init {
        coroutineScope.launch {
            confirmationHandler.state.collectLatest {
                val primaryState = when (it) {
                    is ConfirmationHandler.State.Complete -> {
                        when (it.result) {
                            is ConfirmationHandler.Result.Canceled -> PrimaryButtonProcessingState.Idle(null)
                            is ConfirmationHandler.Result.Failed -> PrimaryButtonProcessingState.Idle(it.result.message)
                            is ConfirmationHandler.Result.Succeeded -> {
                                delay(1500)
                                PrimaryButtonProcessingState.Completed
                            }
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
        selection: PaymentSelection
    ) {
        coroutineScope.launch {
            when (val result = confirmIntent(selection)) {
                is ConfirmationHandler.Result.Canceled -> {}
                is ConfirmationHandler.Result.Failed -> {
                    _error.value = result.message
                }
                is ConfirmationHandler.Result.Succeeded -> {
                    // probably log something here?
                }
                null -> {}
            }
        }
    }

    private suspend fun confirmIntent(
        selection: PaymentSelection
    ): ConfirmationHandler.Result? {
        return runCatching {
            val args = getArgs(selection) ?: return null
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
        selection: PaymentSelection
    ): ConfirmationHandler.Args? {
        val confirmationOption = selection.toConfirmationOption(
            configuration = configuration.asCommonConfiguration(),
            linkConfiguration = null
        ) ?: return null
        return ConfirmationHandler.Args(
            intent = paymentMethodMetadata.stripeIntent,
            confirmationOption = confirmationOption,
            appearance = configuration.appearance,
            initializationMode = initializationMode,
            shippingDetails = paymentMethodMetadata.shippingDetails
        )
    }
}
