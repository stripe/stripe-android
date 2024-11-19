package com.stripe.android.paymentelement.confirmation

import android.app.Activity
import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.StripeIntent
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
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * This interface handles the process of confirming a [StripeIntent]. This interface can only handle confirming one
 * intent at a time.
 */
internal class DefaultConfirmationHandler(
    private val mediators: List<ConfirmationMediator<*, *, *, *>>,
    private val coroutineScope: CoroutineScope,
    private val savedStateHandle: SavedStateHandle,
    private val errorReporter: ErrorReporter,
) : ConfirmationHandler {
    private val hasReloadedWhileAwaitingPreConfirm = isAwaitingForPreConfirmResult()
    private val hasReloadedWhileAwaitingConfirm = isAwaitingForPaymentResult()

    /**
     * Indicates if this handler has been reloaded from process death. This occurs if the handler was confirming
     * an intent before did not complete the process before process death.
     */
    override val hasReloadedFromProcessDeath = hasReloadedWhileAwaitingPreConfirm || hasReloadedWhileAwaitingConfirm

    private val _state = MutableStateFlow(
        if (hasReloadedWhileAwaitingPreConfirm) {
            ConfirmationHandler.State.Preconfirming(
                confirmationType = isAwaitingForPreConfirmType(),
                inPreconfirmFlow = true,
            )
        } else if (hasReloadedWhileAwaitingConfirm) {
            ConfirmationHandler.State.Confirming
        } else {
            ConfirmationHandler.State.Idle
        }
    )
    override val state: StateFlow<ConfirmationHandler.State> = _state.asStateFlow()

    init {
        if (hasReloadedWhileAwaitingConfirm) {
            coroutineScope.launch {
                delay(1.seconds)

                if (_state.value is ConfirmationHandler.State.Confirming) {
                    onResult(
                        ConfirmationHandler.Result.Canceled(
                            action = ConfirmationHandler.Result.Canceled.Action.None,
                        )
                    )
                }
            }
        }
    }

    /**
     * Registers activities tied to confirmation process to the lifecycle.
     *
     * @param activityResultCaller a class that can call [Activity.startActivityForResult]-style APIs
     * @param lifecycleOwner a class tied to an Android [Lifecycle]
     */
    override fun register(activityResultCaller: ActivityResultCaller, lifecycleOwner: LifecycleOwner) {
        mediators.forEach { mediator ->
            mediator.register(activityResultCaller, ::onDefinitionResult)
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

    /**
     * Starts the confirmation process with a given [Args] instance. Result from this method can be received
     * from [awaitIntentResult]. This method cannot return a result since the confirmation process can be handed
     * off to another [Activity] to handle after starting it.
     *
     * @param arguments arguments required to confirm a Stripe intent
     */
    override fun start(
        arguments: ConfirmationHandler.Args,
    ) {
        val currentState = _state.value

        if (
            currentState is ConfirmationHandler.State.Preconfirming ||
            currentState is ConfirmationHandler.State.Confirming
        ) {
            return
        }

        _state.value = ConfirmationHandler.State.Preconfirming(
            confirmationType = arguments.confirmationOption.confirmationType,
            inPreconfirmFlow = false,
        )

        coroutineScope.launch {
            confirm(arguments.confirmationOption, arguments.intent)
        }
    }

    /**
     * Waits for an intent result to be returned following a call to start an intent confirmation process through the
     * [start] method. If no such call has been made, will return null.
     *
     * @return result of intent confirmation process or null if not started.
     */
    override suspend fun awaitIntentResult(): ConfirmationHandler.Result? {
        return when (val state = _state.value) {
            is ConfirmationHandler.State.Idle -> null
            is ConfirmationHandler.State.Complete -> state.result
            is ConfirmationHandler.State.Preconfirming,
            is ConfirmationHandler.State.Confirming -> {
                val complete = _state.firstInstanceOf<ConfirmationHandler.State.Complete>()

                complete.result
            }
        }
    }

    private suspend fun confirm(
        confirmationOption: ConfirmationHandler.Option,
        intent: StripeIntent,
    ) {
        val mediator = mediators.find { mediator -> mediator.canConfirm(confirmationOption, intent) } ?: run {
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

            onResult(
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

        if (mediator.confirmationFlow == ConfirmationDefinition.ConfirmationFlow.Confirm) {
            _state.value = ConfirmationHandler.State.Confirming
        }

        when (val action = mediator.action(confirmationOption, intent)) {
            is ConfirmationMediator.Action.Launch -> {
                when (mediator.confirmationFlow) {
                    ConfirmationDefinition.ConfirmationFlow.Preconfirm -> {
                        _state.value = ConfirmationHandler.State.Preconfirming(
                            confirmationType = confirmationOption.confirmationType,
                            inPreconfirmFlow = true,
                        )

                        storeIsAwaitingForPreConfirmResult(confirmationOption.confirmationType)
                    }
                    ConfirmationDefinition.ConfirmationFlow.Confirm -> storeIsAwaitingForPaymentResult()
                }

                action.launch()
            }
            is ConfirmationMediator.Action.Fail -> {
                onResult(
                    ConfirmationHandler.Result.Failed(
                        cause = action.cause,
                        message = action.message,
                        type = action.errorType,
                    )
                )
            }
            is ConfirmationMediator.Action.Complete -> {
                onResult(
                    ConfirmationHandler.Result.Succeeded(
                        intent = intent,
                        deferredIntentConfirmationType = action.deferredIntentConfirmationType,
                    )
                )
            }
        }
    }

    private fun onResult(result: ConfirmationHandler.Result) {
        _state.value = ConfirmationHandler.State.Complete(result)

        removeIsAwaitingForPaymentResult()
        removeIsAwaitingForPreConfirmResult()
    }

    private fun onDefinitionResult(definitionResult: ConfirmationDefinition.Result) {
        removeIsAwaitingForPaymentResult()
        removeIsAwaitingForPreConfirmResult()

        when (definitionResult) {
            is ConfirmationDefinition.Result.Succeeded -> {
                when (_state.value) {
                    is ConfirmationHandler.State.Preconfirming -> coroutineScope.launch {
                        confirm(
                            confirmationOption = definitionResult.confirmationOption,
                            intent = definitionResult.intent,
                        )
                    }
                    is ConfirmationHandler.State.Confirming -> {
                        onResult(
                            ConfirmationHandler.Result.Succeeded(
                                intent = definitionResult.intent,
                                deferredIntentConfirmationType = definitionResult.deferredIntentConfirmationType,
                            )
                        )
                    }
                    else -> Unit
                }
            }
            is ConfirmationDefinition.Result.Failed -> {
                onResult(
                    ConfirmationHandler.Result.Failed(
                        cause = definitionResult.cause,
                        message = definitionResult.message,
                        type = definitionResult.type,
                    )
                )
            }
            is ConfirmationDefinition.Result.Canceled -> {
                onResult(
                    ConfirmationHandler.Result.Canceled(definitionResult.action)
                )
            }
        }
    }

    private fun storeIsAwaitingForPreConfirmResult(type: ConfirmationHandler.Option.Type) {
        savedStateHandle[AWAITING_PRE_CONFIRM_RESULT_KEY] = true
        savedStateHandle[AWAITING_PRE_CONFIRM_TYPE_KEY] = type
    }

    private fun removeIsAwaitingForPreConfirmResult() {
        savedStateHandle.remove<Boolean>(AWAITING_PRE_CONFIRM_RESULT_KEY)
        savedStateHandle.remove<ConfirmationHandler.Option.Type>(AWAITING_PRE_CONFIRM_TYPE_KEY)
    }

    private fun isAwaitingForPreConfirmResult(): Boolean {
        return savedStateHandle.get<Boolean>(AWAITING_PRE_CONFIRM_RESULT_KEY) ?: false
    }

    private fun isAwaitingForPreConfirmType(): ConfirmationHandler.Option.Type? {
        return savedStateHandle.get<ConfirmationHandler.Option.Type>(AWAITING_PRE_CONFIRM_RESULT_KEY)
    }

    private fun storeIsAwaitingForPaymentResult() {
        savedStateHandle[AWAITING_PAYMENT_RESULT_KEY] = true
    }

    private fun removeIsAwaitingForPaymentResult() {
        savedStateHandle.remove<Boolean>(AWAITING_PAYMENT_RESULT_KEY)
    }

    private fun isAwaitingForPaymentResult(): Boolean {
        return savedStateHandle.get<Boolean>(AWAITING_PAYMENT_RESULT_KEY) ?: false
    }

    private suspend inline fun <reified T> Flow<*>.firstInstanceOf(): T {
        return first {
            it is T
        } as T
    }

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
        private const val AWAITING_PRE_CONFIRM_TYPE_KEY = "AwaitingPreConfirmType"
        private const val AWAITING_PRE_CONFIRM_RESULT_KEY = "AwaitingPreConfirmResult"
        private const val AWAITING_PAYMENT_RESULT_KEY = "AwaitingPaymentResult"
    }
}
