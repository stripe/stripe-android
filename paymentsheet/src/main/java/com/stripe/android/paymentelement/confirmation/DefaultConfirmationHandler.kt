package com.stripe.android.paymentelement.confirmation

import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import androidx.annotation.ColorInt
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.googlepaylauncher.injection.GooglePayPaymentMethodLauncherFactory
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.bacs.BacsConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.epms.ExternalPaymentMethodConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationInterceptor
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncherAssistedFactory
import com.stripe.android.paymentsheet.ExternalPaymentMethodInterceptor
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationLauncherFactory
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
import javax.inject.Named
import javax.inject.Provider
import kotlin.time.Duration.Companion.seconds

internal class DefaultConfirmationHandler(
    private val mediators: List<ConfirmationMediator<*, *, *, *>>,
    private val coroutineScope: CoroutineScope,
    private val savedStateHandle: SavedStateHandle,
    private val errorReporter: ErrorReporter,
) : ConfirmationHandler {
    private val isAwaitingForResultData = retrieveIsAwaitingForResultData()

    override val hasReloadedFromProcessDeath = isAwaitingForResultData != null

    private val _state = MutableStateFlow(
        isAwaitingForResultData?.let { data ->
            ConfirmationHandler.State.Confirming(data.confirmationOption)
        } ?: ConfirmationHandler.State.Idle
    )
    override val state: StateFlow<ConfirmationHandler.State> = _state.asStateFlow()

    init {
        if (hasReloadedFromProcessDeath) {
            coroutineScope.launch {
                delay(1.seconds)

                if (
                    _state.value is ConfirmationHandler.State.Confirming &&
                    isAwaitingForResultData?.receivesResultInProcess != true
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
            confirm(
                intent = arguments.intent,
                confirmationOption = arguments.confirmationOption,
            )
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
        confirmationOption: ConfirmationHandler.Option,
        intent: StripeIntent,
    ) {
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

        when (val action = mediator.action(confirmationOption, intent)) {
            is ConfirmationMediator.Action.Launch -> {
                storeIsAwaitingForResult(
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
                        intent = intent,
                        deferredIntentConfirmationType = action.deferredIntentConfirmationType,
                    )
                )
            }
        }
    }

    private fun onResult(result: ConfirmationDefinition.Result) {
        val confirmationResult = when (result) {
            is ConfirmationDefinition.Result.NextStep -> {
                coroutineScope.launch {
                    confirm(
                        intent = result.intent,
                        confirmationOption = result.confirmationOption,
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
        option: ConfirmationHandler.Option,
        receivesResultInProcess: Boolean,
    ) {
        savedStateHandle[AWAITING_CONFIRMATION_RESULT_KEY] = AwaitingConfirmationResultData(
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

    private suspend inline fun <reified T> Flow<*>.firstInstanceOf(): T {
        return first {
            it is T
        } as T
    }

    @Parcelize
    data class AwaitingConfirmationResultData(
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
        private val intentConfirmationInterceptor: IntentConfirmationInterceptor,
        private val paymentConfigurationProvider: Provider<PaymentConfiguration>,
        private val bacsMandateConfirmationLauncherFactory: BacsMandateConfirmationLauncherFactory,
        private val stripePaymentLauncherAssistedFactory: StripePaymentLauncherAssistedFactory,
        private val googlePayPaymentMethodLauncherFactory: GooglePayPaymentMethodLauncherFactory?,
        private val savedStateHandle: SavedStateHandle,
        @Named(STATUS_BAR_COLOR) @ColorInt private val statusBarColor: Int?,
        private val errorReporter: ErrorReporter,
        private val logger: UserFacingLogger?
    ) : ConfirmationHandler.Factory {
        override fun create(scope: CoroutineScope): ConfirmationHandler {
            val mediators = ConfirmationRegistry(
                listOfNotNull(
                    IntentConfirmationDefinition(
                        intentConfirmationInterceptor = intentConfirmationInterceptor,
                        paymentLauncherFactory = { hostActivityLauncher ->
                            stripePaymentLauncherAssistedFactory.create(
                                publishableKey = { paymentConfigurationProvider.get().publishableKey },
                                stripeAccountId = { paymentConfigurationProvider.get().stripeAccountId },
                                hostActivityLauncher = hostActivityLauncher,
                                statusBarColor = statusBarColor,
                                includePaymentSheetNextHandlers = true,
                            )
                        },
                    ),
                    ExternalPaymentMethodConfirmationDefinition(
                        externalPaymentMethodConfirmHandlerProvider = {
                            ExternalPaymentMethodInterceptor.externalPaymentMethodConfirmHandler
                        },
                        errorReporter = errorReporter,
                    ),
                    BacsConfirmationDefinition(
                        bacsMandateConfirmationLauncherFactory = bacsMandateConfirmationLauncherFactory,
                    ),
                    googlePayPaymentMethodLauncherFactory?.let {
                        GooglePayConfirmationDefinition(
                            googlePayPaymentMethodLauncherFactory = it,
                            userFacingLogger = logger,
                        )
                    }
                )
            ).createConfirmationMediators(savedStateHandle)

            return DefaultConfirmationHandler(
                mediators = mediators,
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
