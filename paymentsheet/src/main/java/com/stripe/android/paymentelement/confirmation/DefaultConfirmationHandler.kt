package com.stripe.android.paymentelement.confirmation

import android.app.Activity
import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.PaymentConfiguration
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
import com.stripe.android.paymentelement.confirmation.bacs.BacsConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.epms.ExternalPaymentMethodConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayConfirmationOption
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationInterceptor
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.paymentlauncher.PaymentLauncher
import com.stripe.android.payments.paymentlauncher.PaymentLauncherContract
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncherAssistedFactory
import com.stripe.android.paymentsheet.ExternalPaymentMethodInterceptor
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationLauncherFactory
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
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import kotlin.time.Duration.Companion.seconds

/**
 * This interface handles the process of confirming a [StripeIntent]. This interface can only handle confirming one
 * intent at a time.
 */
internal class DefaultConfirmationHandler(
    private val intentConfirmationInterceptor: IntentConfirmationInterceptor,
    private val paymentLauncherFactory: (ActivityResultLauncher<PaymentLauncherContract.Args>) -> PaymentLauncher,
    private val bacsMandateConfirmationLauncherFactory: BacsMandateConfirmationLauncherFactory,
    private val googlePayPaymentMethodLauncherFactory: GooglePayPaymentMethodLauncherFactory?,
    private val coroutineScope: CoroutineScope,
    private val savedStateHandle: SavedStateHandle,
    private val errorReporter: ErrorReporter,
    private val logger: UserFacingLogger?
) : ConfirmationHandler {
    private val intentConfirmationRegistry = ConfirmationRegistry(
        confirmationDefinitions = listOf(
            IntentConfirmationDefinition(
                intentConfirmationInterceptor = intentConfirmationInterceptor,
                paymentLauncherFactory = paymentLauncherFactory,
            ),
            ExternalPaymentMethodConfirmationDefinition(
                externalPaymentMethodConfirmHandlerProvider = {
                    ExternalPaymentMethodInterceptor.externalPaymentMethodConfirmHandler
                },
                errorReporter = errorReporter,
            ),
            BacsConfirmationDefinition(
                bacsMandateConfirmationLauncherFactory = bacsMandateConfirmationLauncherFactory,
            )
        )
    )

    private val confirmationMediators = intentConfirmationRegistry.createConfirmationMediators(savedStateHandle)

    private var googlePayPaymentMethodLauncher:
        ActivityResultLauncher<GooglePayPaymentMethodLauncherContractV2.Args>? = null

    private var currentArguments: ConfirmationHandler.Args?
        get() = savedStateHandle[ARGUMENTS_KEY]
        set(value) {
            savedStateHandle[ARGUMENTS_KEY] = value
        }

    private val isAwaitingForResultData = retrieveIsAwaitingForResultData()

    /**
     * Indicates if this handler has been reloaded from process death. This occurs if the handler was confirming
     * an intent before did not complete the process before process death.
     */
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
                    onIntentResult(
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
        confirmationMediators.forEach { mediator ->
            mediator.register(activityResultCaller, ::onResult)
        }

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
                    googlePayPaymentMethodLauncher = null
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

        if (currentState is ConfirmationHandler.State.Confirming) {
            return
        }

        _state.value = ConfirmationHandler.State.Confirming(arguments.confirmationOption)

        currentArguments = arguments

        coroutineScope.launch {
            confirm(arguments)
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
            is ConfirmationHandler.State.Confirming -> {
                val complete = _state.firstInstanceOf<ConfirmationHandler.State.Complete>()

                complete.result
            }
        }
    }

    private suspend fun confirm(
        arguments: ConfirmationHandler.Args,
    ) {
        currentArguments = arguments

        _state.value = ConfirmationHandler.State.Confirming(arguments.confirmationOption)

        when (val confirmationOption = arguments.confirmationOption) {
            is GooglePayConfirmationOption -> launchGooglePay(
                googlePay = confirmationOption,
                intent = arguments.intent,
            )
            else -> confirm(confirmationOption, arguments.intent)
        }
    }

    private suspend fun confirm(
        confirmationOption: ConfirmationHandler.Option,
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
                onIntentResult(
                    ConfirmationHandler.Result.Failed(
                        cause = action.cause,
                        message = action.message,
                        type = action.errorType,
                    )
                )
            }
            is ConfirmationMediator.Action.Complete -> {
                onIntentResult(
                    ConfirmationHandler.Result.Succeeded(
                        intent = intent,
                        deferredIntentConfirmationType = action.deferredIntentConfirmationType,
                    )
                )
            }
        }
    }

    private fun launchGooglePay(
        googlePay: GooglePayConfirmationOption,
        intent: StripeIntent,
    ) {
        if (googlePay.config.merchantCurrencyCode == null && !googlePay.initializationMode.isProcessingPayment) {
            val message = "GooglePayConfig.currencyCode is required in order to use " +
                "Google Pay when processing a Setup Intent"

            logger?.logWarningWithoutPii(message)

            onIntentResult(
                ConfirmationHandler.Result.Failed(
                    cause = IllegalStateException(message),
                    message = R.string.stripe_something_went_wrong.resolvableString,
                    type = ConfirmationHandler.Result.Failed.ErrorType.MerchantIntegration,
                )
            )

            return
        }

        val activityLauncher = runCatching {
            requireNotNull(googlePayPaymentMethodLauncher)
        }.getOrElse {
            onIntentResult(
                ConfirmationHandler.Result.Failed(
                    cause = it,
                    message = R.string.stripe_something_went_wrong.resolvableString,
                    type = ConfirmationHandler.Result.Failed.ErrorType.Internal
                )
            )

            return
        }

        val factory = runCatching {
            requireNotNull(googlePayPaymentMethodLauncherFactory)
        }.getOrElse {
            onIntentResult(
                ConfirmationHandler.Result.Failed(
                    cause = it,
                    message = R.string.stripe_something_went_wrong.resolvableString,
                    type = ConfirmationHandler.Result.Failed.ErrorType.Internal
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

        storeIsAwaitingForResult(
            option = googlePay,
            receivesResultInProcess = true,
        )

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
        config: GooglePayConfirmationOption.Config,
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

    private fun onGooglePayResult(result: GooglePayPaymentMethodLauncher.Result) {
        coroutineScope.launch {
            removeIsAwaitingForResult()

            when (result) {
                is GooglePayPaymentMethodLauncher.Result.Completed -> {
                    val arguments = currentArguments
                    val paymentMethod = arguments?.confirmationOption as? GooglePayConfirmationOption

                    paymentMethod?.let { option ->
                        val confirmationOption = PaymentMethodConfirmationOption.Saved(
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
                        ConfirmationHandler.Result.Failed(
                            cause = result.error,
                            message = when (result.errorCode) {
                                GooglePayPaymentMethodLauncher.NETWORK_ERROR ->
                                    com.stripe.android.R.string.stripe_failure_connection_error.resolvableString
                                else -> com.stripe.android.R.string.stripe_internal_error.resolvableString
                            },
                            type = ConfirmationHandler.Result.Failed.ErrorType.GooglePay(result.errorCode),
                        )
                    )
                }
                is GooglePayPaymentMethodLauncher.Result.Canceled -> {
                    onIntentResult(
                        ConfirmationHandler.Result.Canceled(
                            action = ConfirmationHandler.Result.Canceled.Action.InformCancellation,
                        )
                    )
                }
            }
        }
    }

    private fun onResult(result: ConfirmationDefinition.Result) {
        val confirmationResult = when (result) {
            is ConfirmationDefinition.Result.NextStep -> {
                coroutineScope.launch {
                    confirm(
                        arguments = ConfirmationHandler.Args(
                            intent = result.intent,
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

        onIntentResult(confirmationResult)
    }

    private fun onIntentResult(result: ConfirmationHandler.Result) {
        currentArguments = null

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
        @Named(STATUS_BAR_COLOR_PROVIDER) private val statusBarColor: () -> Int?,
        private val errorReporter: ErrorReporter,
        private val logger: UserFacingLogger?
    ) : ConfirmationHandler.Factory {
        override fun create(scope: CoroutineScope): ConfirmationHandler {
            return DefaultConfirmationHandler(
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
        private const val AWAITING_CONFIRMATION_RESULT_KEY = "AwaitingConfirmationResult"
        private const val ARGUMENTS_KEY = "PaymentConfirmationArguments"
    }
}
