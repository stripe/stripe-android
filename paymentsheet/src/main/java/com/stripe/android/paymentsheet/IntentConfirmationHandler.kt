package com.stripe.android.paymentsheet

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.PaymentConfiguration
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContractV2
import com.stripe.android.googlepaylauncher.injection.GooglePayPaymentMethodLauncherFactory
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.paymentlauncher.InternalPaymentResult
import com.stripe.android.payments.paymentlauncher.PaymentLauncher
import com.stripe.android.payments.paymentlauncher.PaymentLauncherContract
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncherAssistedFactory
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.addresselement.toConfirmPaymentIntentShipping
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationContract
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationLauncher
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationLauncherFactory
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationResult
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateData
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
import kotlin.IllegalStateException
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
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val savedStateHandle: SavedStateHandle,
    private val errorReporter: ErrorReporter,
    private val logger: UserFacingLogger?,
) {
    private var paymentLauncher: PaymentLauncher? = null
    private var externalPaymentMethodLauncher: ActivityResultLauncher<ExternalPaymentMethodInput>? = null
    private var bacsMandateConfirmationLauncher: BacsMandateConfirmationLauncher? = null
    private var googlePayPaymentMethodLauncher:
        ActivityResultLauncher<GooglePayPaymentMethodLauncherContractV2.Args>? = null

    private var deferredIntentConfirmationType: DeferredIntentConfirmationType?
        get() = savedStateHandle[DEFERRED_INTENT_CONFIRMATION_TYPE]
        set(value) {
            savedStateHandle[DEFERRED_INTENT_CONFIRMATION_TYPE] = value
        }

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
                    onIntentResult(Result.Canceled(action = CancellationAction.None))
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
        paymentLauncher = paymentLauncherFactory(
            activityResultCaller.registerForActivityResult(
                PaymentLauncherContract(),
                ::onPaymentResult
            )
        )

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
                    paymentLauncher = null
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
    suspend fun awaitIntentResult(): Result? {
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
                arguments = arguments,
            )
        } else if (
            confirmationOption is PaymentConfirmationOption.New &&
            confirmationOption.createParams.typeCode == PaymentMethod.Type.BacsDebit.code
        ) {
            launchBacsMandate(confirmationOption, arguments.appearance)
        } else {
            confirm(arguments)
        }
    }

    private suspend fun confirm(
        arguments: Args
    ) {
        _state.value = State.Confirming

        val confirmationOption = arguments.confirmationOption

        if (confirmationOption is PaymentConfirmationOption.ExternalPaymentMethod) {
            confirmExternalPaymentMethod(confirmationOption)
        } else {
            confirmIntent(arguments)
        }
    }

    private suspend fun confirmIntent(
        arguments: Args
    ) {
        val nextStep = intentConfirmationInterceptor.intercept(
            initializationMode = arguments.initializationMode,
            confirmationOption = arguments.confirmationOption,
            shippingValues = arguments.shippingDetails?.toConfirmPaymentIntentShipping(),
            context = context,
        )

        deferredIntentConfirmationType = nextStep.deferredIntentConfirmationType

        when (nextStep) {
            is IntentConfirmationInterceptor.NextStep.HandleNextAction -> {
                handleNextAction(
                    clientSecret = nextStep.clientSecret,
                    stripeIntent = arguments.intent,
                )
            }
            is IntentConfirmationInterceptor.NextStep.Confirm -> {
                confirmStripeIntent(nextStep.confirmParams)
            }
            is IntentConfirmationInterceptor.NextStep.Fail -> {
                onIntentResult(
                    Result.Failed(
                        cause = nextStep.cause,
                        message = nextStep.message,
                        type = ErrorType.Internal,
                    )
                )
            }
            is IntentConfirmationInterceptor.NextStep.Complete -> {
                onIntentResult(
                    Result.Succeeded(
                        intent = arguments.intent,
                        deferredIntentConfirmationType = nextStep.deferredIntentConfirmationType,
                    )
                )
            }
        }
    }

    private fun handleNextAction(
        clientSecret: String,
        stripeIntent: StripeIntent,
    ) = withPaymentLauncher { launcher ->
        /*
         * In case of process death, we should store that we waiting for a payment result to return from a
         * payment confirmation activity
         */
        storeIsAwaitingForPaymentResult()

        when (stripeIntent) {
            is PaymentIntent -> {
                launcher.handleNextActionForPaymentIntent(clientSecret)
            }
            is SetupIntent -> {
                launcher.handleNextActionForSetupIntent(clientSecret)
            }
        }
    }

    private fun confirmStripeIntent(
        confirmStripeIntentParams: ConfirmStripeIntentParams
    ) = withPaymentLauncher { launcher ->
        /*
         * In case of process death, we should store that we waiting for a payment result to return from a
         * payment confirmation activity
         */
        storeIsAwaitingForPaymentResult()

        when (confirmStripeIntentParams) {
            is ConfirmPaymentIntentParams -> {
                launcher.confirm(confirmStripeIntentParams)
            }
            is ConfirmSetupIntentParams -> {
                launcher.confirm(confirmStripeIntentParams)
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
        arguments: Args,
    ) {
        if (googlePay.config.merchantCurrencyCode == null && !arguments.initializationMode.isProcessingPayment) {
            val message = "GooglePayConfig.currencyCode is required in order to use " +
                "Google Pay when processing a Setup Intent"

            logger?.logWarningWithoutPii(message)

            onIntentResult(
                Result.Failed(
                    cause = IllegalStateException(message),
                    message = R.string.stripe_something_went_wrong.resolvableString,
                    type = ErrorType.MerchantIntegration,
                )
            )

            return
        }

        val activityLauncher = runCatching {
            requireNotNull(googlePayPaymentMethodLauncher)
        }.getOrElse {
            onIntentResult(
                Result.Failed(
                    cause = it,
                    message = R.string.stripe_something_went_wrong.resolvableString,
                    type = ErrorType.Internal
                )
            )

            return
        }

        val factory = runCatching {
            requireNotNull(googlePayPaymentMethodLauncherFactory)
        }.getOrElse {
            onIntentResult(
                Result.Failed(
                    cause = it,
                    message = R.string.stripe_something_went_wrong.resolvableString,
                    type = ErrorType.Internal
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

        val intent = arguments.intent

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
            skipReadyCheck = true
        )
    }

    private fun launchBacsMandate(
        confirmationOption: PaymentConfirmationOption.New,
        appearance: PaymentSheet.Appearance,
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
                    appearance = appearance
                )
            }.onFailure { cause ->
                onIntentResult(
                    Result.Failed(
                        cause = cause,
                        message = R.string.stripe_something_went_wrong.resolvableString,
                        type = ErrorType.Internal
                    )
                )
            }
        } ?: run {
            onIntentResult(
                Result.Failed(
                    cause = IllegalArgumentException(
                        "Given payment selection could not be converted to Bacs data!"
                    ),
                    message = R.string.stripe_something_went_wrong.resolvableString,
                    type = ErrorType.Internal
                )
            )
        }
    }

    private fun onPaymentResult(result: InternalPaymentResult) {
        val intentResult = when (result) {
            is InternalPaymentResult.Completed -> Result.Succeeded(
                intent = result.intent,
                deferredIntentConfirmationType = deferredIntentConfirmationType
            )
            is InternalPaymentResult.Failed -> Result.Failed(
                cause = result.throwable,
                message = result.throwable.stripeErrorMessage(),
                type = ErrorType.Payment,
            )
            is InternalPaymentResult.Canceled -> Result.Canceled(action = CancellationAction.InformCancellation)
        }

        onIntentResult(intentResult)
    }

    private fun onBacsMandateResult(result: BacsMandateConfirmationResult) {
        coroutineScope.launch {
            removeIsAwaitingForPreConfirmResult()

            when (result) {
                is BacsMandateConfirmationResult.Confirmed -> currentArguments?.let { arguments ->
                    confirm(arguments)
                }
                is BacsMandateConfirmationResult.ModifyDetails -> onIntentResult(
                    Result.Canceled(
                        action = CancellationAction.ModifyPaymentDetails
                    )
                )
                is BacsMandateConfirmationResult.Cancelled -> onIntentResult(
                    Result.Canceled(
                        action = CancellationAction.None
                    )
                )
            }
        }
    }

    private fun onExternalPaymentMethodResult(result: PaymentResult) {
        val intentResult = currentArguments?.let { arguments ->
            when (result) {
                is PaymentResult.Completed -> Result.Succeeded(
                    intent = arguments.intent,
                    deferredIntentConfirmationType = null
                )
                is PaymentResult.Failed -> Result.Failed(
                    cause = result.throwable,
                    message = result.throwable.stripeErrorMessage(),
                    type = ErrorType.ExternalPaymentMethod,
                )
                is PaymentResult.Canceled -> Result.Canceled(action = CancellationAction.None)
            }
        } ?: run {
            val cause = IllegalStateException("Arguments should have been initialized before handling EPM result!")

            Result.Failed(
                cause = cause,
                message = cause.stripeErrorMessage(),
                type = ErrorType.ExternalPaymentMethod,
            )
        }

        onIntentResult(intentResult)
    }

    private fun onGooglePayResult(result: GooglePayPaymentMethodLauncher.Result) {
        coroutineScope.launch {
            when (result) {
                is GooglePayPaymentMethodLauncher.Result.Completed -> {
                    currentArguments?.let { arguments ->
                        val confirmationOption = PaymentConfirmationOption.Saved(
                            paymentMethod = result.paymentMethod,
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
                        Result.Failed(
                            cause = result.error,
                            message = when (result.errorCode) {
                                GooglePayPaymentMethodLauncher.NETWORK_ERROR ->
                                    com.stripe.android.R.string.stripe_failure_connection_error.resolvableString
                                else -> com.stripe.android.R.string.stripe_internal_error.resolvableString
                            },
                            type = ErrorType.GooglePay(result.errorCode),
                        )
                    )
                }
                is GooglePayPaymentMethodLauncher.Result.Canceled -> {
                    onIntentResult(Result.Canceled(action = CancellationAction.InformCancellation))
                }
            }
        }
    }

    private fun onIntentResult(result: Result) {
        deferredIntentConfirmationType = null
        currentArguments = null

        _state.value = State.Complete(result)

        removeIsAwaitingForPaymentResult()
        removeIsAwaitingForPreConfirmResult()
    }

    private fun withPaymentLauncher(action: (PaymentLauncher) -> Unit) {
        paymentLauncher?.let(action) ?: run {
            onIntentResult(
                Result.Failed(
                    cause = IllegalArgumentException(
                        "No 'PaymentLauncher' instance was created before starting confirmation. " +
                            "Did you call register?"
                    ),
                    message = resolvableString(R.string.stripe_something_went_wrong),
                    type = ErrorType.Fatal,
                )
            )
        }
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

    private val PaymentSheet.InitializationMode.isProcessingPayment: Boolean
        get() = when (this) {
            is PaymentSheet.InitializationMode.PaymentIntent -> true
            is PaymentSheet.InitializationMode.SetupIntent -> false
            is PaymentSheet.InitializationMode.DeferredIntent -> {
                intentConfiguration.mode is PaymentSheet.IntentConfiguration.Mode.Payment
            }
        }

    @Parcelize
    internal data class Args(
        val initializationMode: PaymentSheet.InitializationMode,
        val shippingDetails: AddressDetails?,
        val appearance: PaymentSheet.Appearance,
        val intent: StripeIntent,
        val confirmationOption: PaymentConfirmationOption?
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
         * Indicates that the handler has completed confirming a payment and contains a [Result] regarding
         * the confirmation process final result.
         */
        data class Complete(
            val result: Result,
        ) : State
    }

    /**
     * Defines the result types that [IntentConfirmationHandler] can return after completing the confirmation process.
     */
    sealed interface Result {
        /**
         * Indicates that the confirmation process was canceled by the customer.
         */
        data class Canceled(
            val action: CancellationAction
        ) : Result

        /**
         * Indicates that the confirmation process has been successfully completed. A [StripeIntent] with an updated
         * state is returned as part of the result as well.
         */
        data class Succeeded(
            val intent: StripeIntent,
            val deferredIntentConfirmationType: DeferredIntentConfirmationType?
        ) : Result

        /**
         * Indicates that the confirmation process has failed. A cause and potentially a resolvable message are
         * returned as part of the result.
         */
        data class Failed(
            val cause: Throwable,
            val message: ResolvableString,
            val type: ErrorType,
        ) : Result
    }

    /**
     * Action to perform if a user cancels a running confirmation process.
     */
    enum class CancellationAction {
        /**
         * This actions means the user has cancels a critical confirmation step and that the user should be notified
         * of the cancellation if relevant.
         */
        InformCancellation,

        /**
         * This action means that the user has asked to modify the payment details of their selected payment option.
         */
        ModifyPaymentDetails,

        /**
         * Means no action should be taken if the user cancels a step in the confirmation process.
         */
        None,
    }

    /**
     * Types of errors that can occur when confirming an intent.
     */
    sealed interface ErrorType {
        /**
         * Fatal confirmation error that occurred while confirming a payment. This should never happen.
         */
        data object Fatal : ErrorType

        /**
         * Indicates an error when processing a payment during the confirmation process.
         */
        data object Payment : ErrorType

        /**
         * Indicates an internal process error occurred during the confirmation process.
         */
        data object Internal : ErrorType

        /**
         * Indicates a merchant integration error occurred during the confirmation process.
         */
        data object MerchantIntegration : ErrorType

        /**
         * Indicates an error occurred when confirming with external payment methods
         */
        data object ExternalPaymentMethod : ErrorType

        /**
         * Indicates an error occurred when confirming with Google Pay
         */
        data class GooglePay(val errorCode: Int) : ErrorType
    }

    class Factory(
        private val intentConfirmationInterceptor: IntentConfirmationInterceptor,
        private val paymentConfigurationProvider: Provider<PaymentConfiguration>,
        private val bacsMandateConfirmationLauncherFactory: BacsMandateConfirmationLauncherFactory,
        private val stripePaymentLauncherAssistedFactory: StripePaymentLauncherAssistedFactory,
        private val googlePayPaymentMethodLauncherFactory: GooglePayPaymentMethodLauncherFactory?,
        private val savedStateHandle: SavedStateHandle,
        private val statusBarColor: () -> Int?,
        private val application: Application,
        private val errorReporter: ErrorReporter,
        private val logger: UserFacingLogger?,
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
                context = application,
                coroutineScope = scope,
                errorReporter = errorReporter,
                savedStateHandle = savedStateHandle,
                logger = logger,
            )
        }
    }

    internal companion object {
        private const val AWAITING_PRE_CONFIRM_RESULT_KEY = "AwaitingPreConfirmResult"
        private const val AWAITING_PAYMENT_RESULT_KEY = "AwaitingPaymentResult"
        private const val DEFERRED_INTENT_CONFIRMATION_TYPE = "DeferredIntentConfirmationType"
        private const val ARGUMENTS_KEY = "IntentConfirmationArguments"
    }
}
