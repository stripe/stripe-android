package com.stripe.android.paymentelement.confirmation.gpay

import android.content.Context
import androidx.activity.result.ActivityResultCaller
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContractV2
import com.stripe.android.googlepaylauncher.InternalGooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.injection.InternalGooglePayPaymentMethodLauncherFactory
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.EmptyConfirmationLauncherArgs
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import javax.inject.Inject
import com.stripe.android.R as PaymentsCoreR

internal class GooglePayConfirmationDefinition @Inject constructor(
    private val context: Context,
    private val googlePayPaymentMethodLauncherFactory: InternalGooglePayPaymentMethodLauncherFactory,
    private val userFacingLogger: UserFacingLogger?,
) : ConfirmationDefinition<
    GooglePayConfirmationOption,
    InternalGooglePayPaymentMethodLauncher,
    EmptyConfirmationLauncherArgs,
    GooglePayPaymentMethodLauncher.Result,
    > {
    override val key: String = "GooglePay"

    override fun option(confirmationOption: ConfirmationHandler.Option): GooglePayConfirmationOption? {
        return confirmationOption as? GooglePayConfirmationOption
    }

    override suspend fun action(
        confirmationOption: GooglePayConfirmationOption,
        confirmationArgs: ConfirmationHandler.Args,
    ): ConfirmationDefinition.Action<EmptyConfirmationLauncherArgs> {
        if (
            confirmationOption.config.merchantCurrencyCode == null &&
            confirmationArgs.intent !is PaymentIntent
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
            launcherArguments = EmptyConfirmationLauncherArgs,
            receivesResultInProcess = true,
        )
    }

    override fun createLauncher(
        activityResultCaller: ActivityResultCaller,
        onResult: (GooglePayPaymentMethodLauncher.Result) -> Unit
    ): InternalGooglePayPaymentMethodLauncher {
        val activityResultLauncher = activityResultCaller.registerForActivityResult(
            GooglePayPaymentMethodLauncherContractV2(),
            onResult,
        )

        return googlePayPaymentMethodLauncherFactory.create(
            activityResultLauncher = activityResultLauncher,
        )
    }

    override fun launch(
        launcher: InternalGooglePayPaymentMethodLauncher,
        arguments: EmptyConfirmationLauncherArgs,
        confirmationOption: GooglePayConfirmationOption,
        confirmationArgs: ConfirmationHandler.Args,
    ) {
        val config = confirmationOption.config
        val intent = confirmationArgs.intent

        launcher.present(
            currencyCode = intent.asPaymentIntent()?.currency
                ?: config.merchantCurrencyCode.orEmpty(),
            amount = when (intent) {
                is PaymentIntent -> intent.amount ?: 0L
                is SetupIntent -> config.customAmount ?: 0L
            },
            config = config.toGooglePayLauncherConfig(confirmationArgs),
            cardBrandFilter = config.cardBrandFilter,
            cardFundingFilter = config.cardFundingFilter,
            clientAttributionMetadata = confirmationArgs.paymentMethodMetadata.clientAttributionMetadata,
            transactionId = intent.id,
            label = config.customLabel,
            isElements = true,
            apiConfiguration = confirmationArgs.paymentMethodMetadata.apiConfiguration,
            displayItems = config.displayItems.map { it.resolve(context) },
            billingEmailOverride = config.billingEmailOverride,
            shippingAddressParameters = config.shippingAddressParameters,
        )
    }

    override fun toResult(
        confirmationOption: GooglePayConfirmationOption,
        confirmationArgs: ConfirmationHandler.Args,
        launcherArgs: EmptyConfirmationLauncherArgs,
        result: GooglePayPaymentMethodLauncher.Result,
    ): ConfirmationDefinition.Result {
        return when (result) {
            is GooglePayPaymentMethodLauncher.Result.Completed -> {
                val nextConfirmationOption = PaymentMethodConfirmationOption.Saved(
                    paymentMethod = result.paymentMethod,
                    optionsParams = null,
                    originatedFromWallet = true,
                )

                ConfirmationDefinition.Result.NextStep(
                    confirmationOption = nextConfirmationOption,
                    arguments = confirmationArgs,
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

    private fun GooglePayConfirmationOption.Config.toGooglePayLauncherConfig(
        confirmationArgs: ConfirmationHandler.Args,
    ): GooglePayPaymentMethodLauncher.Config {
        return GooglePayPaymentMethodLauncher.Config(
            environment = when (environment) {
                PaymentSheet.GooglePayConfiguration.Environment.Production -> GooglePayEnvironment.Production
                else -> GooglePayEnvironment.Test
            },
            merchantCountryCode = merchantCountryCode,
            merchantName = confirmationArgs.paymentMethodMetadata.sellerBusinessName
                ?: merchantName,
            isEmailRequired = isEmailRequired,
            billingAddressConfig = billingDetailsCollectionConfiguration.toBillingAddressConfig(),
            existingPaymentMethodRequired = !FeatureFlags.allowNoExistingPaymentMethodForGooglePay.isEnabled,
            additionalEnabledNetworks = additionalEnabledNetworks
        )
    }

    private fun StripeIntent.asPaymentIntent(): PaymentIntent? {
        return this as? PaymentIntent
    }
}
