package com.stripe.android.paymentelement.confirmation

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContractV2
import com.stripe.android.googlepaylauncher.injection.GooglePayPaymentMethodLauncherFactory
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

internal class GooglePayConfirmationDefinition(
    private val googlePayPaymentMethodLauncherFactory: GooglePayPaymentMethodLauncherFactory?,
    private val userFacingLogger: UserFacingLogger?,
) : ConfirmationDefinition<
    ConfirmationHandler.Option.GooglePay,
    ActivityResultLauncher<GooglePayPaymentMethodLauncherContractV2.Args>,
    GooglePayConfirmationDefinition.Args,
    GooglePayPaymentMethodLauncher.Result,
    > {
    override val key: String = "GooglePay"

    override val confirmationFlow: ConfirmationDefinition.ConfirmationFlow =
        ConfirmationDefinition.ConfirmationFlow.Preconfirm

    override fun option(confirmationOption: ConfirmationHandler.Option): ConfirmationHandler.Option.GooglePay? {
        return confirmationOption as? ConfirmationHandler.Option.GooglePay
    }

    override fun canConfirm(
        confirmationOption: ConfirmationHandler.Option.GooglePay,
        intent: StripeIntent
    ): Boolean = true

    override suspend fun action(
        confirmationOption: ConfirmationHandler.Option.GooglePay,
        intent: StripeIntent
    ): ConfirmationDefinition.ConfirmationAction<Args> {
        if (
            confirmationOption.config.merchantCurrencyCode == null &&
            !confirmationOption.initializationMode.isProcessingPayment
        ) {
            val message = "GooglePayConfig.currencyCode is required in order to use " +
                "Google Pay when processing a Setup Intent"

            userFacingLogger?.logWarningWithoutPii(message)

            return ConfirmationDefinition.ConfirmationAction.Fail(
                cause = IllegalStateException(message),
                message = R.string.stripe_something_went_wrong.resolvableString,
                errorType = ConfirmationHandler.Result.Failed.ErrorType.MerchantIntegration,
            )
        }

        val factory = runCatching {
            requireNotNull(googlePayPaymentMethodLauncherFactory)
        }.getOrElse {
            return ConfirmationDefinition.ConfirmationAction.Fail(
                cause = it,
                message = R.string.stripe_something_went_wrong.resolvableString,
                errorType = ConfirmationHandler.Result.Failed.ErrorType.Internal
            )
        }

        return ConfirmationDefinition.ConfirmationAction.Launch(
            launcherArguments = Args(
                factory = factory,
                config = confirmationOption.config,
            ),
            deferredIntentConfirmationType = null,
        )
    }

    override fun createLauncher(
        activityResultCaller: ActivityResultCaller,
        onResult: (GooglePayPaymentMethodLauncher.Result) -> Unit
    ): ActivityResultLauncher<GooglePayPaymentMethodLauncherContractV2.Args> {
        return activityResultCaller.registerForActivityResult(
            GooglePayPaymentMethodLauncherContractV2(),
            onResult
        )
    }

    override fun launch(
        launcher: ActivityResultLauncher<GooglePayPaymentMethodLauncherContractV2.Args>,
        arguments: Args,
        confirmationOption: ConfirmationHandler.Option.GooglePay,
        intent: StripeIntent,
    ) {
        val config = arguments.config

        val googlePayLauncher = createGooglePayLauncher(
            factory = arguments.factory,
            activityLauncher = launcher,
            config = config,
        )

        googlePayLauncher.present(
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

    override fun toResult(
        confirmationOption: ConfirmationHandler.Option.GooglePay,
        deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        intent: StripeIntent,
        result: GooglePayPaymentMethodLauncher.Result,
    ): ConfirmationDefinition.Result {
        return when (result) {
            is GooglePayPaymentMethodLauncher.Result.Completed -> {
                val nextConfirmationOption = ConfirmationHandler.Option.PaymentMethod.Saved(
                    paymentMethod = result.paymentMethod,
                    initializationMode = confirmationOption.initializationMode,
                    shippingDetails = confirmationOption.shippingDetails,
                    optionsParams = null,
                )

                ConfirmationDefinition.Result.Succeeded(
                    confirmationOption = nextConfirmationOption,
                    intent = intent,
                    deferredIntentConfirmationType = null,
                )
            }
            is GooglePayPaymentMethodLauncher.Result.Failed -> {
                ConfirmationDefinition.Result.Failed(
                    cause = result.error,
                    message = when (result.errorCode) {
                        GooglePayPaymentMethodLauncher.NETWORK_ERROR ->
                            com.stripe.android.R.string.stripe_failure_connection_error.resolvableString
                        else -> com.stripe.android.R.string.stripe_internal_error.resolvableString
                    },
                    type = ConfirmationHandler.Result.Failed.ErrorType.GooglePay(result.errorCode),
                )
            }
            is GooglePayPaymentMethodLauncher.Result.Canceled -> {
                ConfirmationDefinition.Result.Canceled(
                    action = ConfirmationHandler.Result.Canceled.Action.InformCancellation,
                )
            }
        }
    }

    private fun createGooglePayLauncher(
        factory: GooglePayPaymentMethodLauncherFactory,
        activityLauncher: ActivityResultLauncher<GooglePayPaymentMethodLauncherContractV2.Args>,
        config: ConfirmationHandler.Option.GooglePay.Config,
    ): GooglePayPaymentMethodLauncher {
        return factory.create(
            lifecycleScope = CoroutineScope(Dispatchers.Default),
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

    private fun StripeIntent.asPaymentIntent(): PaymentIntent? {
        return this as? PaymentIntent
    }

    class Args(
        val factory: GooglePayPaymentMethodLauncherFactory,
        val config: ConfirmationHandler.Option.GooglePay.Config,
    )

    private val PaymentElementLoader.InitializationMode.isProcessingPayment: Boolean
        get() = when (this) {
            is PaymentElementLoader.InitializationMode.PaymentIntent -> true
            is PaymentElementLoader.InitializationMode.SetupIntent -> false
            is PaymentElementLoader.InitializationMode.DeferredIntent -> {
                intentConfiguration.mode is PaymentSheet.IntentConfiguration.Mode.Payment
            }
        }
}
