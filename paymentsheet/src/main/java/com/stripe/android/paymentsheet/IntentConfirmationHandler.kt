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
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationContract
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationLauncher
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationLauncherFactory
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationResult
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateData
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.lang.IllegalStateException
import javax.inject.Provider

/**
 * This interface handles the process of confirming a [StripeIntent]. This interface can only handle confirming one
 * intent at a time.
 */
internal class IntentConfirmationHandler(
    private val intentConfirmationInterceptor: IntentConfirmationInterceptor,
    private val paymentLauncherFactory: (ActivityResultLauncher<PaymentLauncherContract.Args>) -> PaymentLauncher,
    private val bacsMandateConfirmationLauncherFactory: BacsMandateConfirmationLauncherFactory,
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val savedStateHandle: SavedStateHandle,
    private val errorReporter: ErrorReporter,
) {
    private var paymentLauncher: PaymentLauncher? = null
    private var externalPaymentMethodLauncher: ActivityResultLauncher<ExternalPaymentMethodInput>? = null
    private var bacsMandateConfirmationLauncher: BacsMandateConfirmationLauncher? = null

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

    private var completableResult: CompletableDeferred<Result>? = if (isAwaitingForPaymentResult()) {
        CompletableDeferred()
    } else {
        null
    }

    /**
     * Indicates if this handler has been reloaded from process death. This occurs if the handler was confirming
     * an intent before did not complete the process before process death.
     */
    val hasReloadedFromProcessDeath = isAwaitingForPaymentResult()

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

        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    paymentLauncher = null
                    externalPaymentMethodLauncher = null
                    bacsMandateConfirmationLauncher = null
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
        if (completableResult?.isActive == true) {
            return
        }

        currentArguments = arguments
        completableResult = CompletableDeferred()

        coroutineScope.launch {
            val paymentSelection = arguments.paymentSelection

            if (paymentSelection is PaymentSelection.ExternalPaymentMethod) {
                handleExternalPaymentMethod(paymentSelection)
            } else if (
                paymentSelection is PaymentSelection.New.GenericPaymentMethod &&
                paymentSelection.paymentMethodCreateParams.typeCode == PaymentMethod.Type.BacsDebit.code
            ) {
                launchBacsMandate(paymentSelection, arguments.appearance)
            } else {
                confirm(arguments)
            }
        }
    }

    /**
     * Waits for an intent result to be returned following a call to start an intent confirmation process through the
     * [start] method. If no such call has been made, will return null.
     *
     * @return result of intent confirmation process or null if not started.
     */
    suspend fun awaitIntentResult(): Result? {
        return completableResult?.await()
    }

    private suspend fun confirm(
        arguments: Args
    ) {
        val nextStep = intentConfirmationInterceptor.intercept(
            initializationMode = arguments.initializationMode,
            paymentSelection = arguments.paymentSelection,
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

    private fun handleExternalPaymentMethod(
        paymentSelection: PaymentSelection.ExternalPaymentMethod
    ) {
        ExternalPaymentMethodInterceptor.intercept(
            externalPaymentMethodType = paymentSelection.type,
            billingDetails = paymentSelection.billingDetails,
            onPaymentResult = ::onExternalPaymentMethodResult,
            externalPaymentMethodLauncher = externalPaymentMethodLauncher,
            errorReporter = errorReporter,
        )
    }

    private fun launchBacsMandate(
        paymentSelection: PaymentSelection.New.GenericPaymentMethod,
        appearance: PaymentSheet.Appearance,
    ) {
        BacsMandateData.fromPaymentSelection(paymentSelection)?.let { data ->
            runCatching {
                requireNotNull(bacsMandateConfirmationLauncher)
            }.onSuccess { launcher ->
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

        removeIsAwaitingForPaymentResult()
    }

    private fun onBacsMandateResult(result: BacsMandateConfirmationResult) {
        coroutineScope.launch {
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

    private fun onIntentResult(result: Result) {
        deferredIntentConfirmationType = null
        currentArguments = null

        completableResult?.complete(result)
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

    private fun storeIsAwaitingForPaymentResult() {
        savedStateHandle[AWAITING_PAYMENT_RESULT_KEY] = true
    }

    private fun removeIsAwaitingForPaymentResult() {
        savedStateHandle.remove<Boolean>(AWAITING_PAYMENT_RESULT_KEY)
    }

    private fun isAwaitingForPaymentResult(): Boolean {
        return savedStateHandle.get<Boolean>(AWAITING_PAYMENT_RESULT_KEY) ?: false
    }

    @Parcelize
    internal data class Args(
        val initializationMode: PaymentSheet.InitializationMode,
        val shippingDetails: AddressDetails?,
        val appearance: PaymentSheet.Appearance,
        val intent: StripeIntent,
        val paymentSelection: PaymentSelection?
    ) : Parcelable

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
    enum class ErrorType {
        /**
         * Fatal confirmation error that occurred while confirming a payment. This should never happen.
         */
        Fatal,

        /**
         * Indicates an error when processing a payment during the confirmation process.
         */
        Payment,

        /**
         * Indicates an internal process error occurred during the confirmation process.
         */
        Internal,

        /**
         * Indicates an error occurred when confirming with external payment methods
         */
        ExternalPaymentMethod,
    }

    class Factory(
        private val intentConfirmationInterceptor: IntentConfirmationInterceptor,
        private val paymentConfigurationProvider: Provider<PaymentConfiguration>,
        private val bacsMandateConfirmationLauncherFactory: BacsMandateConfirmationLauncherFactory,
        private val stripePaymentLauncherAssistedFactory: StripePaymentLauncherAssistedFactory,
        private val savedStateHandle: SavedStateHandle,
        private val statusBarColor: () -> Int?,
        private val application: Application,
        private val errorReporter: ErrorReporter,
    ) {
        fun create(scope: CoroutineScope): IntentConfirmationHandler {
            return IntentConfirmationHandler(
                bacsMandateConfirmationLauncherFactory = bacsMandateConfirmationLauncherFactory,
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
            )
        }
    }

    internal companion object {
        private const val AWAITING_PAYMENT_RESULT_KEY = "AwaitingPaymentResult"
        private const val DEFERRED_INTENT_CONFIRMATION_TYPE = "DeferredIntentConfirmationType"
        private const val ARGUMENTS_KEY = "IntentConfirmationArguments"
    }
}