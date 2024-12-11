package com.stripe.android.paymentelement.confirmation

import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

internal class DefaultConfirmationHandler(
    private val mediators: List<ConfirmationMediator<*, *, *, *>>,
    private val coroutineScope: CoroutineScope,
    private val savedStateHandle: SavedStateHandle,
    private val errorReporter: ErrorReporter,
) : ConfirmationHandler {
    private val isInitiallyAwaitingForResultData = retrieveIsAwaitingForResultData()

    override val hasReloadedFromProcessDeath = isInitiallyAwaitingForResultData != null

    private val _state = MutableStateFlow(
        isInitiallyAwaitingForResultData?.let { data ->
            ConfirmationHandler.State.Confirming(data.confirmationOption)
        } ?: ConfirmationHandler.State.Idle
    )
    override val state: StateFlow<ConfirmationHandler.State> = _state.asStateFlow()

    init {
        if (hasReloadedFromProcessDeath) {
            coroutineScope.launch {
                delay(1.seconds)

                val isStillAwaitingForResultData = retrieveIsAwaitingForResultData()

                if (
                    isStillAwaitingForResultData != null &&
                    isStillAwaitingForResultData.key == isInitiallyAwaitingForResultData?.key &&
                    !isStillAwaitingForResultData.receivesResultInProcess
                ) {
                    onHandlerResult(
                        ConfirmationHandler.Result.Canceled(
                            action = ConfirmationHandler.Result.Canceled.Action.None,
                        )
                    )
                }
            }
        }
    }

    override fun register(activityResultCaller: ActivityResultCaller, lifecycleOwner: LifecycleOwner) {
        mediators.forEach { mediator ->
            mediator.register(activityResultCaller, ::onResult)
        }

        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    mediators.forEach { mediator ->
                        mediator.unregister()
                    }
                    super.onDestroy(owner)
                }
            }
        )
    }

    override fun start(
        arguments: ConfirmationHandler.Args,
    ) {
        val currentState = _state.value

        if (currentState is ConfirmationHandler.State.Confirming) {
            return
        }

        _state.value = ConfirmationHandler.State.Confirming(arguments.confirmationOption)

        coroutineScope.launch {
            confirm(arguments)
        }
    }

    override suspend fun awaitResult(): ConfirmationHandler.Result? {
        return when (val state = _state.value) {
            is ConfirmationHandler.State.Idle -> null
            is ConfirmationHandler.State.Complete -> state.result
            is ConfirmationHandler.State.Confirming -> {
                val complete = _state.firstInstanceOf<ConfirmationHandler.State.Complete>()

                complete.result
            }
        }
    }

    private suspend fun confirm(
        arguments: ConfirmationHandler.Args,
    ) {
        val confirmationOption = arguments.confirmationOption

        _state.value = ConfirmationHandler.State.Confirming(confirmationOption)

        val mediator = mediators.find { mediator ->
            mediator.canConfirm(confirmationOption)
        } ?: run {
            errorReporter.report(
                errorEvent = ErrorReporter
                    .UnexpectedErrorEvent
                    .INTENT_CONFIRMATION_HANDLER_INVALID_PAYMENT_CONFIRMATION_OPTION,
                stripeException = StripeException.create(
                    throwable = IllegalStateException(
                        "Attempting to confirm intent for invalid confirmation option: $confirmationOption"
                    )
                ),
            )

            onHandlerResult(
                ConfirmationHandler.Result.Failed(
                    cause = IllegalStateException(
                        "Attempted to confirm invalid ${confirmationOption::class.qualifiedName} confirmation type"
                    ),
                    message = R.string.stripe_something_went_wrong.resolvableString,
                    type = ConfirmationHandler.Result.Failed.ErrorType.Internal,
                )
            )

            return
        }

        when (val action = mediator.action(confirmationOption, arguments.toParameters())) {
            is ConfirmationMediator.Action.Launch -> {
                storeIsAwaitingForResult(
                    key = mediator.key,
                    option = confirmationOption,
                    receivesResultInProcess = action.receivesResultInProcess,
                )

                action.launch()
            }
            is ConfirmationMediator.Action.Fail -> {
                onHandlerResult(
                    ConfirmationHandler.Result.Failed(
                        cause = action.cause,
                        message = action.message,
                        type = action.errorType,
                    )
                )
            }
            is ConfirmationMediator.Action.Complete -> {
                onHandlerResult(
                    ConfirmationHandler.Result.Succeeded(
                        intent = action.intent,
                        deferredIntentConfirmationType = action.deferredIntentConfirmationType,
                    )
                )
            }
        }
    }

    private fun onResult(result: ConfirmationDefinition.Result) {
        val confirmationResult = when (result) {
            is ConfirmationDefinition.Result.NextStep -> {
                removeIsAwaitingForResult()

                coroutineScope.launch {
                    val parameters = result.parameters

                    confirm(
                        arguments = ConfirmationHandler.Args(
                            intent = parameters.intent,
                            shippingDetails = parameters.shippingDetails,
                            appearance = parameters.appearance,
                            initializationMode = parameters.initializationMode,
                            confirmationOption = result.confirmationOption,
                        )
                    )
                }

                return
            }
            is ConfirmationDefinition.Result.Succeeded -> ConfirmationHandler.Result.Succeeded(
                intent = result.intent,
                deferredIntentConfirmationType = result.deferredIntentConfirmationType,
            )
            is ConfirmationDefinition.Result.Failed -> ConfirmationHandler.Result.Failed(
                cause = result.cause,
                type = result.type,
                message = result.message,
            )
            is ConfirmationDefinition.Result.Canceled -> ConfirmationHandler.Result.Canceled(
                action = result.action,
            )
        }

        onHandlerResult(confirmationResult)
    }

    private fun onHandlerResult(result: ConfirmationHandler.Result) {
        _state.value = ConfirmationHandler.State.Complete(result)

        removeIsAwaitingForResult()
    }

    private fun storeIsAwaitingForResult(
        key: String,
        option: ConfirmationHandler.Option,
        receivesResultInProcess: Boolean,
    ) {
        savedStateHandle[AWAITING_CONFIRMATION_RESULT_KEY] = AwaitingConfirmationResultData(
            key = key,
            confirmationOption = option,
            receivesResultInProcess = receivesResultInProcess,
        )
    }

    private fun removeIsAwaitingForResult() {
        savedStateHandle.remove<AwaitingConfirmationResultData>(AWAITING_CONFIRMATION_RESULT_KEY)
    }

    private fun retrieveIsAwaitingForResultData(): AwaitingConfirmationResultData? {
        return savedStateHandle.get<AwaitingConfirmationResultData>(AWAITING_CONFIRMATION_RESULT_KEY)
    }

    private fun ConfirmationHandler.Args.toParameters(): ConfirmationDefinition.Parameters {
        return ConfirmationDefinition.Parameters(
            appearance = appearance,
            shippingDetails = shippingDetails,
            initializationMode = initializationMode,
            intent = intent
        )
    }

    private suspend inline fun <reified T> Flow<*>.firstInstanceOf(): T {
        return first {
            it is T
        } as T
    }

    @Parcelize
    data class AwaitingConfirmationResultData(
        val key: String,
        val confirmationOption: ConfirmationHandler.Option,
        /*
         * Indicates the user receives the result within the process of the app. For example, Bacs & Google Pay open
         * sheets in front of `PaymentSheet` and `FlowController`. During process death, these sheets and the activity
         * hosting the products will be re-initialized, meaning we have to wait for the sheet to be closed and a result
         * to be received before continuing the confirmation process since the result is guaranteed.
         */
        val receivesResultInProcess: Boolean,
    ) : Parcelable

    class Factory @Inject constructor(
        private val registry: ConfirmationRegistry,
        private val savedStateHandle: SavedStateHandle,
        private val errorReporter: ErrorReporter,
    ) : ConfirmationHandler.Factory {
        override fun create(scope: CoroutineScope): ConfirmationHandler {
            return DefaultConfirmationHandler(
                mediators = registry.createConfirmationMediators(savedStateHandle),
                coroutineScope = scope,
                errorReporter = errorReporter,
                savedStateHandle = savedStateHandle,
            )
        }
    }

    internal companion object {
        private const val AWAITING_CONFIRMATION_RESULT_KEY = "AwaitingConfirmationResult"
    }
}
