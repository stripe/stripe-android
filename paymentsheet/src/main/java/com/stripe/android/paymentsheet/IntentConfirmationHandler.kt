package com.stripe.android.paymentsheet

import android.app.Activity
import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.PaymentConfiguration
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContractV2
import com.stripe.android.googlepaylauncher.injection.GooglePayPaymentMethodLauncherFactory
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.paymentlauncher.PaymentLauncher
import com.stripe.android.payments.paymentlauncher.PaymentLauncherContract
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncherAssistedFactory
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationContract
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationLauncher
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationLauncherFactory
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationResult
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateData
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Provider
import kotlin.time.Duration.Companion.seconds

/**
 * This interface handles the process of confirming a [StripeIntent]. This interface can only handle confirming one
 * intent at a time.
 */
internal class IntentConfirmationHandler(
    private val intentConfirmationInterceptor: IntentConfirmationInterceptor,
    private val paymentLauncherFactory: (ActivityResultLauncher<PaymentLauncherContract.Args>) -> PaymentLauncher,
    private val bacsMandateConfirmationLauncherFactory: BacsMandateConfirmationLauncherFactory,
    private val googlePayPaymentMethodLauncherFactory: GooglePayPaymentMethodLauncherFactory?,
    private val coroutineScope: CoroutineScope,
    private val savedStateHandle: SavedStateHandle,
    private val errorReporter: ErrorReporter,
    private val logger: UserFacingLogger?
) {
    private val intentConfirmationRegistry = PaymentConfirmationRegistry(
        confirmationDefinitions = listOf(
            IntentConfirmationDefinition(
                intentConfirmationInterceptor,
                paymentLauncherFactory,
            )
        )
    )

    private val confirmationMediators = intentConfirmationRegistry.createConfirmationMediators(savedStateHandle)

    private var externalPaymentMethodLauncher: ActivityResultLauncher<ExternalPaymentMethodInput>? = null
    private var bacsMandateConfirmationLauncher: BacsMandateConfirmationLauncher? = null
    private var googlePayPaymentMethodLauncher:
        ActivityResultLauncher<GooglePayPaymentMethodLauncherContractV2.Args>? = null

    private var currentArguments: Args?
        get() = savedStateHandle[ARGUMENTS_KEY]
        set(value) {
            savedStateHandle[ARGUMENTS_KEY] = value
        }

    private val hasReloadedWhileAwaitingPreConfirm = isAwaitingForPreConfirmResult()
    private val hasReloadedWhileAwaitingConfirm = isAwaitingForPaymentResult()

    /**
     * Indicates if this handler has been reloaded from process death. This occurs if the handler was confirming
     * an intent before did not complete the process before process death.
     */
    val hasReloadedFromProcessDeath = hasReloadedWhileAwaitingPreConfirm || hasReloadedWhileAwaitingConfirm

    private val _state = MutableStateFlow(
        if (hasReloadedWhileAwaitingPreConfirm) {
            State.Preconfirming(
                confirmationOption = currentArguments?.confirmationOption,
                inPreconfirmFlow = true,
            )
        } else if (hasReloadedWhileAwaitingConfirm) {
            State.Confirming
        } else {
            State.Idle
        }
    )
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        if (hasReloadedWhileAwaitingConfirm) {
            coroutineScope.launch {
                delay(1.seconds)

                if (_state.value is State.Confirming) {
                    onIntentResult(PaymentConfirmationResult.Canceled(action = PaymentCancellationAction.None))
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
    fun register(activityResultCaller: ActivityResultCaller, lifecycleOwner: LifecycleOwner) {
        confirmationMediators.forEach { mediator ->
            mediator.register(activityResultCaller, ::onIntentResult)
        }

        externalPaymentMethodLauncher = activityResultCaller.registerForActivityResult(
            ExternalPaymentMethodContract(errorReporter),
            ::onExternalPaymentMethodResult
        )

        val bacsActivityResultLauncher = activityResultCaller.registerForActivityResult(
            BacsMandateConfirmationContract(),
            ::onBacsMandateResult
        )

        bacsMandateConfirmationLauncher = bacsMandateConfirmationLauncherFactory.create(bacsActivityResultLauncher)

        googlePayPaymentMethodLauncher = activityResultCaller.registerForActivityResult(
            GooglePayPaymentMethodLauncherContractV2(),
            ::onGooglePayResult
        )

        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    confirmationMediators.forEach { mediator ->
                        mediator.unregister()
                    }
                    externalPaymentMethodLauncher = null
                    bacsMandateConfirmationLauncher = null
                    googlePayPaymentMethodLauncher = null
                    bacsActivityResultLauncher.unregister()
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
    fun start(
        arguments: Args,
    ) {
        val currentState = _state.value

        if (currentState is State.Preconfirming || currentState is State.Confirming) {
            return
        }

        _state.value = State.Preconfirming(
            confirmationOption = arguments.confirmationOption,
            inPreconfirmFlow = false,
        )

        currentArguments = arguments

        coroutineScope.launch {
            preconfirm(arguments)
        }
    }

    /**
     * Waits for an intent result to be returned following a call to start an intent confirmation process through the
     * [start] method. If no such call has been made, will return null.
     *
     * @return result of intent confirmation process or null if not started.
     */
    suspend fun awaitIntentResult(): PaymentConfirmationResult? {
        return when (val state = _state.value) {
            is State.Idle -> null
            is State.Complete -> state.result
            is State.Preconfirming,
            is State.Confirming -> {
                val complete = _state.firstInstanceOf<State.Complete>()

                complete.result
            }
        }
    }

    private suspend fun preconfirm(
        arguments: Args
    ) {
        val confirmationOption = arguments.confirmationOption

        if (confirmationOption is PaymentConfirmationOption.GooglePay) {
            launchGooglePay(
                googlePay = confirmationOption,
                intent = arguments.intent,
            )
        } else if (confirmationOption is PaymentConfirmationOption.BacsPaymentMethod) {
            launchBacsMandate(confirmationOption)
        } else {
            confirm(arguments)
        }
    }

    private suspend fun confirm(
        arguments: Args
    ) {
        currentArguments = arguments

        _state.value = State.Confirming

        val confirmationOption = arguments.confirmationOption

        if (confirmationOption is PaymentConfirmationOption.ExternalPaymentMethod) {
            confirmExternalPaymentMethod(confirmationOption)
        } else {
            confirm(confirmationOption, arguments.intent)
        }
    }

    private suspend fun confirm(
        confirmationOption: PaymentConfirmationOption,
        intent: StripeIntent,
    ) {
        val mediator = confirmationMediators.find { mediator ->
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

            onIntentResult(
                PaymentConfirmationResult.Failed(
                    cause = IllegalStateException(
                        "Attempted to confirm invalid ${confirmationOption::class.qualifiedName} confirmation type"
                    ),
                    message = R.string.stripe_something_went_wrong.resolvableString,
                    type = PaymentConfirmationErrorType.Internal,
                )
            )

            return
        }

        when (val action = mediator.action(confirmationOption, intent)) {
            is PaymentConfirmationMediator.Action.Launch -> {
                storeIsAwaitingForPaymentResult()

                action.launch()
            }
            is PaymentConfirmationMediator.Action.Fail -> {
                onIntentResult(
                    PaymentConfirmationResult.Failed(
                        cause = action.cause,
                        message = action.message,
                        type = action.errorType,
                    )
                )
            }
            is PaymentConfirmationMediator.Action.Complete -> {
                onIntentResult(
                    PaymentConfirmationResult.Succeeded(
                        intent = intent,
                        deferredIntentConfirmationType = action.deferredIntentConfirmationType,
                    )
                )
            }
        }
    }

    private fun confirmExternalPaymentMethod(
        confirmationOption: PaymentConfirmationOption.ExternalPaymentMethod
    ) {
        /*
         * In case of process death, we should store that we waiting for a payment result to return from a
         * payment confirmation activity
         */
        storeIsAwaitingForPaymentResult()

        ExternalPaymentMethodInterceptor.intercept(
            externalPaymentMethodType = confirmationOption.type,
            billingDetails = confirmationOption.billingDetails,
            onPaymentResult = ::onExternalPaymentMethodResult,
            externalPaymentMethodLauncher = externalPaymentMethodLauncher,
            errorReporter = errorReporter,
        )
    }

    private fun launchGooglePay(
        googlePay: PaymentConfirmationOption.GooglePay,
        intent: StripeIntent,
    ) {
        if (googlePay.config.merchantCurrencyCode == null && !googlePay.initializationMode.isProcessingPayment) {
            val message = "GooglePayConfig.currencyCode is required in order to use " +
                "Google Pay when processing a Setup Intent"

            logger?.logWarningWithoutPii(message)

            onIntentResult(
                PaymentConfirmationResult.Failed(
                    cause = IllegalStateException(message),
                    message = R.string.stripe_something_went_wrong.resolvableString,
                    type = PaymentConfirmationErrorType.MerchantIntegration,
                )
            )

            return
        }

        val activityLauncher = runCatching {
            requireNotNull(googlePayPaymentMethodLauncher)
        }.getOrElse {
            onIntentResult(
                PaymentConfirmationResult.Failed(
                    cause = it,
                    message = R.string.stripe_something_went_wrong.resolvableString,
                    type = PaymentConfirmationErrorType.Internal
                )
            )

            return
        }

        val factory = runCatching {
            requireNotNull(googlePayPaymentMethodLauncherFactory)
        }.getOrElse {
            onIntentResult(
                PaymentConfirmationResult.Failed(
                    cause = it,
                    message = R.string.stripe_something_went_wrong.resolvableString,
                    type = PaymentConfirmationErrorType.Internal
                )
            )

            return
        }

        val config = googlePay.config

        val launcher = createGooglePayLauncher(
            factory = factory,
            activityLauncher = activityLauncher,
            config = config,
        )

        storeIsAwaitingForPreConfirmResult()

        _state.value = State.Preconfirming(confirmationOption = googlePay, inPreconfirmFlow = true)

        launcher.present(
            currencyCode = intent.asPaymentIntent()?.currency
                ?: config.merchantCurrencyCode.orEmpty(),
            amount = when (intent) {
                is PaymentIntent -> intent.amount ?: 0L
                is SetupIntent -> config.customAmount ?: 0L
            },
            transactionId = intent.id,
            label = config.customLabel,
        )
    }

    private fun createGooglePayLauncher(
        factory: GooglePayPaymentMethodLauncherFactory,
        activityLauncher: ActivityResultLauncher<GooglePayPaymentMethodLauncherContractV2.Args>,
        config: PaymentConfirmationOption.GooglePay.Config,
    ): GooglePayPaymentMethodLauncher {
        return factory.create(
            lifecycleScope = coroutineScope,
            config = GooglePayPaymentMethodLauncher.Config(
                environment = when (config.environment) {
                    PaymentSheet.GooglePayConfiguration.Environment.Production -> GooglePayEnvironment.Production
                    else -> GooglePayEnvironment.Test
                },
                merchantCountryCode = config.merchantCountryCode,
                merchantName = config.merchantName,
                isEmailRequired = config.billingDetailsCollectionConfiguration.collectsEmail,
                billingAddressConfig = config.billingDetailsCollectionConfiguration.toBillingAddressConfig(),
            ),
            readyCallback = {
                // Do nothing since we are skipping the ready check below
            },
            activityResultLauncher = activityLauncher,
            skipReadyCheck = true,
            cardBrandFilter = config.cardBrandFilter
        )
    }

    private fun launchBacsMandate(
        confirmationOption: PaymentConfirmationOption.BacsPaymentMethod,
    ) {
        BacsMandateData.fromConfirmationOption(confirmationOption)?.let { data ->
            runCatching {
                requireNotNull(bacsMandateConfirmationLauncher)
            }.onSuccess { launcher ->
                _state.value = State.Preconfirming(
                    confirmationOption = confirmationOption,
                    inPreconfirmFlow = true,
                )

                storeIsAwaitingForPreConfirmResult()

                launcher.launch(
                    data = data,
                    appearance = confirmationOption.appearance
                )
            }.onFailure { cause ->
                onIntentResult(
                    PaymentConfirmationResult.Failed(
                        cause = cause,
                        message = R.string.stripe_something_went_wrong.resolvableString,
                        type = PaymentConfirmationErrorType.Internal
                    )
                )
            }
        } ?: run {
            onIntentResult(
                PaymentConfirmationResult.Failed(
                    cause = IllegalArgumentException(
                        "Given payment selection could not be converted to Bacs data!"
                    ),
                    message = R.string.stripe_something_went_wrong.resolvableString,
                    type = PaymentConfirmationErrorType.Internal
                )
            )
        }
    }

    private fun onBacsMandateResult(result: BacsMandateConfirmationResult) {
        coroutineScope.launch {
            removeIsAwaitingForPreConfirmResult()

            when (result) {
                is BacsMandateConfirmationResult.Confirmed -> {
                    val arguments = currentArguments
                    val bacs = arguments?.confirmationOption as? PaymentConfirmationOption.BacsPaymentMethod

                    bacs?.let { bacsPaymentMethod ->
                        confirm(
                            arguments.copy(
                                confirmationOption = PaymentConfirmationOption.PaymentMethod.New(
                                    initializationMode = bacsPaymentMethod.initializationMode,
                                    shippingDetails = bacsPaymentMethod.shippingDetails,
                                    createParams = bacsPaymentMethod.createParams,
                                    optionsParams = null,
                                    shouldSave = false,
                                )
                            )
                        )
                    }
                }
                is BacsMandateConfirmationResult.ModifyDetails -> onIntentResult(
                    PaymentConfirmationResult.Canceled(
                        action = PaymentCancellationAction.ModifyPaymentDetails
                    )
                )
                is BacsMandateConfirmationResult.Cancelled -> onIntentResult(
                    PaymentConfirmationResult.Canceled(
                        action = PaymentCancellationAction.None
                    )
                )
            }
        }
    }

    private fun onExternalPaymentMethodResult(result: PaymentResult) {
        val intentResult = currentArguments?.let { arguments ->
            when (result) {
                is PaymentResult.Completed -> PaymentConfirmationResult.Succeeded(
                    intent = arguments.intent,
                    deferredIntentConfirmationType = null,
                )
                is PaymentResult.Failed -> PaymentConfirmationResult.Failed(
                    cause = result.throwable,
                    message = result.throwable.stripeErrorMessage(),
                    type = PaymentConfirmationErrorType.ExternalPaymentMethod,
                )
                is PaymentResult.Canceled -> PaymentConfirmationResult.Canceled(action = PaymentCancellationAction.None)
            }
        } ?: run {
            val cause = IllegalStateException("Arguments should have been initialized before handling EPM result!")

            PaymentConfirmationResult.Failed(
                cause = cause,
                message = cause.stripeErrorMessage(),
                type = PaymentConfirmationErrorType.ExternalPaymentMethod,
            )
        }

        onIntentResult(intentResult)
    }

    private fun onGooglePayResult(result: GooglePayPaymentMethodLauncher.Result) {
        coroutineScope.launch {
            when (result) {
                is GooglePayPaymentMethodLauncher.Result.Completed -> {
                    val arguments = currentArguments
                    val paymentMethod = arguments?.confirmationOption as? PaymentConfirmationOption.GooglePay

                    paymentMethod?.let { option ->
                        val confirmationOption = PaymentConfirmationOption.PaymentMethod.Saved(
                            paymentMethod = result.paymentMethod,
                            initializationMode = option.initializationMode,
                            shippingDetails = option.shippingDetails,
                            optionsParams = null,
                        )

                        confirm(
                            arguments.copy(
                                confirmationOption = confirmationOption,
                            )
                        )
                    }
                }
                is GooglePayPaymentMethodLauncher.Result.Failed -> {
                    onIntentResult(
                        PaymentConfirmationResult.Failed(
                            cause = result.error,
                            message = when (result.errorCode) {
                                GooglePayPaymentMethodLauncher.NETWORK_ERROR ->
                                    com.stripe.android.R.string.stripe_failure_connection_error.resolvableString
                                else -> com.stripe.android.R.string.stripe_internal_error.resolvableString
                            },
                            type = PaymentConfirmationErrorType.GooglePay(result.errorCode),
                        )
                    )
                }
                is GooglePayPaymentMethodLauncher.Result.Canceled -> {
                    onIntentResult(
                        PaymentConfirmationResult.Canceled(
                            action = PaymentCancellationAction.InformCancellation
                        )
                    )
                }
            }
        }
    }

    private fun onIntentResult(result: PaymentConfirmationResult) {
        currentArguments = null

        _state.value = State.Complete(result)

        removeIsAwaitingForPaymentResult()
        removeIsAwaitingForPreConfirmResult()
    }

    private fun storeIsAwaitingForPreConfirmResult() {
        savedStateHandle[AWAITING_PRE_CONFIRM_RESULT_KEY] = true
    }

    private fun removeIsAwaitingForPreConfirmResult() {
        savedStateHandle.remove<Boolean>(AWAITING_PRE_CONFIRM_RESULT_KEY)
    }

    private fun isAwaitingForPreConfirmResult(): Boolean {
        return savedStateHandle.get<Boolean>(AWAITING_PRE_CONFIRM_RESULT_KEY) ?: false
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

    private fun StripeIntent.asPaymentIntent(): PaymentIntent? {
        return this as? PaymentIntent
    }

    private suspend inline fun <reified T> Flow<*>.firstInstanceOf(): T {
        return first {
            it is T
        } as T
    }

    private val PaymentElementLoader.InitializationMode.isProcessingPayment: Boolean
        get() = when (this) {
            is PaymentElementLoader.InitializationMode.PaymentIntent -> true
            is PaymentElementLoader.InitializationMode.SetupIntent -> false
            is PaymentElementLoader.InitializationMode.DeferredIntent -> {
                intentConfiguration.mode is PaymentSheet.IntentConfiguration.Mode.Payment
            }
        }

    @Parcelize
    internal data class Args(
        val intent: StripeIntent,
        val confirmationOption: PaymentConfirmationOption
    ) : Parcelable

    /**
     * Defines the state types that [IntentConfirmationHandler] can be in with regards to payment confirmation.
     */
    sealed interface State {
        /**
         * Indicates that the handler is currently idle. This is normally the initial state of the handler unless the
         * handler is reloaded after being destroyed by process death while confirming an intent.
         */
        data object Idle : State

        /**
         * Indicates the the handler is currently performing pre-confirmation steps before starting confirmation of
         * a payment.
         */
        data class Preconfirming(
            val confirmationOption: PaymentConfirmationOption?,
            val inPreconfirmFlow: Boolean,
        ) : State

        /**
         * Indicates the the handler is currently confirming a payment.
         */
        data object Confirming : State

        /**
         * Indicates that the handler has completed confirming a payment and contains a [PaymentConfirmationResult]
         * regarding the confirmation process final result.
         */
        data class Complete(
            val result: PaymentConfirmationResult,
        ) : State
    }

    class Factory(
        private val intentConfirmationInterceptor: IntentConfirmationInterceptor,
        private val paymentConfigurationProvider: Provider<PaymentConfiguration>,
        private val bacsMandateConfirmationLauncherFactory: BacsMandateConfirmationLauncherFactory,
        private val stripePaymentLauncherAssistedFactory: StripePaymentLauncherAssistedFactory,
        private val googlePayPaymentMethodLauncherFactory: GooglePayPaymentMethodLauncherFactory?,
        private val savedStateHandle: SavedStateHandle,
        private val statusBarColor: () -> Int?,
        private val errorReporter: ErrorReporter,
        private val logger: UserFacingLogger?
    ) {
        fun create(scope: CoroutineScope): IntentConfirmationHandler {
            return IntentConfirmationHandler(
                bacsMandateConfirmationLauncherFactory = bacsMandateConfirmationLauncherFactory,
                googlePayPaymentMethodLauncherFactory = googlePayPaymentMethodLauncherFactory,
                paymentLauncherFactory = { hostActivityLauncher ->
                    stripePaymentLauncherAssistedFactory.create(
                        publishableKey = { paymentConfigurationProvider.get().publishableKey },
                        stripeAccountId = { paymentConfigurationProvider.get().stripeAccountId },
                        hostActivityLauncher = hostActivityLauncher,
                        statusBarColor = statusBarColor(),
                        includePaymentSheetNextHandlers = true,
                    )
                },
                intentConfirmationInterceptor = intentConfirmationInterceptor,
                coroutineScope = scope,
                errorReporter = errorReporter,
                savedStateHandle = savedStateHandle,
                logger = logger
            )
        }
    }

    internal companion object {
        private const val AWAITING_PRE_CONFIRM_RESULT_KEY = "AwaitingPreConfirmResult"
        private const val AWAITING_PAYMENT_RESULT_KEY = "AwaitingPaymentResult"
        private const val ARGUMENTS_KEY = "PaymentConfirmationArguments"
    }
}
