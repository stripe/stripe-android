package com.stripe.android.paymentelement.confirmation.gpay

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
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import com.stripe.android.R as PaymentsCoreR

internal class GooglePayConfirmationDefinition(
    private val googlePayPaymentMethodLauncherFactory: GooglePayPaymentMethodLauncherFactory,
    private val userFacingLogger: UserFacingLogger?,
) : ConfirmationDefinition<
    GooglePayConfirmationOption,
    ActivityResultLauncher<GooglePayPaymentMethodLauncherContractV2.Args>,
    Unit,
    GooglePayPaymentMethodLauncher.Result,
    > {
    override val key: String = "GooglePay"

    override fun option(confirmationOption: ConfirmationHandler.Option): GooglePayConfirmationOption? {
        return confirmationOption as? GooglePayConfirmationOption
    }

    override suspend fun action(
        confirmationOption: GooglePayConfirmationOption,
        intent: StripeIntent
    ): ConfirmationDefinition.Action<Unit> {
        if (
            confirmationOption.config.merchantCurrencyCode == null &&
            !confirmationOption.initializationMode.isProcessingPayment
        ) {
            val message = "GooglePayConfig.currencyCode is required in order to use " +
                "Google Pay when processing a Setup Intent"

            userFacingLogger?.logWarningWithoutPii(message)

            return ConfirmationDefinition.Action.Fail(
                cause = IllegalStateException(message),
                message = R.string.stripe_something_went_wrong.resolvableString,
                errorType = ConfirmationHandler.Result.Failed.ErrorType.MerchantIntegration,
            )
        }

        return ConfirmationDefinition.Action.Launch(
            launcherArguments = Unit,
            receivesResultInProcess = true,
            deferredIntentConfirmationType = null,
        )
    }

    override fun createLauncher(
        activityResultCaller: ActivityResultCaller,
        onResult: (GooglePayPaymentMethodLauncher.Result) -> Unit
    ): ActivityResultLauncher<GooglePayPaymentMethodLauncherContractV2.Args> {
        return activityResultCaller.registerForActivityResult(
            GooglePayPaymentMethodLauncherContractV2(),
            onResult,
        )
    }

    override fun launch(
        launcher: ActivityResultLauncher<GooglePayPaymentMethodLauncherContractV2.Args>,
        arguments: Unit,
        confirmationOption: GooglePayConfirmationOption,
        intent: StripeIntent,
    ) {
        val config = confirmationOption.config
        val googlePayLauncher = createGooglePayLauncher(
            factory = googlePayPaymentMethodLauncherFactory,
            activityLauncher = launcher,
            config = confirmationOption.config,
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
        confirmationOption: GooglePayConfirmationOption,
        deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        intent: StripeIntent,
        result: GooglePayPaymentMethodLauncher.Result,
    ): ConfirmationDefinition.Result {
        return when (result) {
            is GooglePayPaymentMethodLauncher.Result.Completed -> {
                val nextConfirmationOption = PaymentMethodConfirmationOption.Saved(
                    paymentMethod = result.paymentMethod,
                    initializationMode = confirmationOption.initializationMode,
                    shippingDetails = confirmationOption.shippingDetails,
                    optionsParams = null,
                )

                ConfirmationDefinition.Result.NextStep(
                    confirmationOption = nextConfirmationOption,
                    intent = intent,
                )
            }
            is GooglePayPaymentMethodLauncher.Result.Failed -> {
                ConfirmationDefinition.Result.Failed(
                    cause = result.error,
                    message = when (result.errorCode) {
                        GooglePayPaymentMethodLauncher.NETWORK_ERROR ->
                            PaymentsCoreR.string.stripe_failure_connection_error.resolvableString
                        else -> PaymentsCoreR.string.stripe_internal_error.resolvableString
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
        config: GooglePayConfirmationOption.Config,
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

    private val PaymentElementLoader.InitializationMode.isProcessingPayment: Boolean
        get() = when (this) {
            is PaymentElementLoader.InitializationMode.PaymentIntent -> true
            is PaymentElementLoader.InitializationMode.SetupIntent -> false
            is PaymentElementLoader.InitializationMode.DeferredIntent -> {
                intentConfiguration.mode is PaymentSheet.IntentConfiguration.Mode.Payment
            }
        }
}
