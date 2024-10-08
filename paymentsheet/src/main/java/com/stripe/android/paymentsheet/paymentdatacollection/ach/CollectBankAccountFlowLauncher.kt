package com.stripe.android.paymentsheet.paymentdatacollection.ach

import androidx.activity.result.ActivityResultCaller
import com.stripe.android.PaymentConfiguration
import com.stripe.android.financialconnections.FinancialConnectionsSheet.ElementsSessionContext
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.bankaccount.CollectBankAccountConfiguration
import com.stripe.android.payments.bankaccount.CollectBankAccountForInstantDebitsLauncher
import com.stripe.android.payments.bankaccount.CollectBankAccountLauncher
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountForInstantDebitsResult
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResultInternal
import com.stripe.android.paymentsheet.PaymentConfirmationOption
import com.stripe.android.paymentsheet.PaymentSheet
import javax.inject.Inject
import javax.inject.Provider

internal class CollectBankAccountFlowLauncherFactory @Inject constructor(
    private val lazyPaymentConfig: Provider<PaymentConfiguration>,
) {

    fun create(
        activityResultCaller: ActivityResultCaller,
        onUSBankAccountResult: (CollectBankAccountResultInternal) -> Unit,
        onLinkBankPaymentResult: (CollectBankAccountForInstantDebitsResult) -> Unit,
    ): CollectBankAccountFlowLauncher {
        return CollectBankAccountFlowLauncher(
            lazyPaymentConfig = lazyPaymentConfig,
            activityResultCaller = activityResultCaller,
            onUSBankAccountResult = onUSBankAccountResult,
            onLinkBankPaymentResult = onLinkBankPaymentResult,
        )
    }
}

internal class CollectBankAccountFlowLauncher(
    private val lazyPaymentConfig: Provider<PaymentConfiguration>,
    activityResultCaller: ActivityResultCaller,
    private val onUSBankAccountResult: (CollectBankAccountResultInternal) -> Unit,
    private val onLinkBankPaymentResult: (CollectBankAccountForInstantDebitsResult) -> Unit,
) {

    private val collectBankAccountLauncher =  CollectBankAccountLauncher.createForPaymentSheet(
        hostedSurface = "payment_element",
        activityResultCaller = activityResultCaller,
        callback = ::handleCollectBankAccountResult,
    )

    private val collectBankAccountForInstantDebitsLauncher = CollectBankAccountForInstantDebitsLauncher.createForPaymentSheet(
        hostedSurface = "payment_element",
        activityResultCaller = activityResultCaller,
        callback = ::handleCollectBankAccountForInstantDebitsResult,
    )

    fun launch(
        confirmationOption: PaymentConfirmationOption.BankAccount,
        intent: StripeIntent,
        linkMode: LinkMode?,
    ) {
        val createParams = confirmationOption.createParams
        val initializationMode = confirmationOption.initializationMode
        val isInstantDebits = confirmationOption.isInstantDebits

        launch(
            intent = intent,
            initializationMode = initializationMode,
            isInstantDebits = isInstantDebits,
            createParams = createParams,
            linkMode = linkMode,
        )
    }

    fun launch(
        intent: StripeIntent,
        initializationMode: PaymentSheet.InitializationMode,
        isInstantDebits: Boolean,
        createParams: PaymentMethodCreateParams,
        linkMode: LinkMode?,
    ) {
        val clientSecret = intent.clientSecret

        if (clientSecret != null) {
            collectBankAccountForIntent(
                clientSecret = clientSecret,
                intent = intent,
                createParams = createParams,
                isInstantDebits = isInstantDebits,
                linkMode = linkMode,
            )
        } else {
            collectBankAccountForDeferredIntent(
                intent = intent,
                createParams = createParams,
                initializationMode = initializationMode,
            )
        }
    }

    private fun collectBankAccountForIntent(
        clientSecret: String,
        intent: StripeIntent,
        createParams: PaymentMethodCreateParams,
        isInstantDebits: Boolean,
        linkMode: LinkMode?,
    ) {
        val name = PaymentMethodCreateParams.getNameFromParams(createParams) ?: return
        val email = PaymentMethodCreateParams.getEmailFromParams(createParams)

        val configuration = if (isInstantDebits) {
            CollectBankAccountConfiguration.InstantDebits(
                email = email,
                elementsSessionContext = ElementsSessionContext(
                    linkMode = linkMode,
                ),
            )
        } else {
            CollectBankAccountConfiguration.USBankAccount(
                name = name,
                email = email,
            )
        }

        val launcher = if (isInstantDebits) {
            collectBankAccountForInstantDebitsLauncher
        } else {
            collectBankAccountLauncher
        }

        when (intent) {
            is PaymentIntent -> {
                launcher.presentWithPaymentIntent(
                    publishableKey = lazyPaymentConfig.get().publishableKey,
                    stripeAccountId = lazyPaymentConfig.get().stripeAccountId,
                    clientSecret = clientSecret,
                    configuration = configuration,
                )
            }
            is SetupIntent -> {
                launcher.presentWithSetupIntent(
                    publishableKey = lazyPaymentConfig.get().publishableKey,
                    stripeAccountId = lazyPaymentConfig.get().stripeAccountId,
                    clientSecret = clientSecret,
                    configuration = configuration,
                )
            }
        }
    }

    private fun collectBankAccountForDeferredIntent(
        initializationMode: PaymentSheet.InitializationMode,
        intent: StripeIntent,
        createParams: PaymentMethodCreateParams,
    ) {
        val elementsSessionId = intent.id ?: return
        val name = PaymentMethodCreateParams.getNameFromParams(createParams) ?: return
        val email = PaymentMethodCreateParams.getEmailFromParams(createParams)

        val amount = intent.asPaymentIntent()?.amount
        val currency = intent.asPaymentIntent()?.currency

        val onBehalfOf = (initializationMode as? PaymentSheet.InitializationMode.DeferredIntent)
            ?.intentConfiguration
            ?.onBehalfOf

        when (intent) {
            is PaymentIntent -> {
                collectBankAccountLauncher.presentWithDeferredPayment(
                    publishableKey = lazyPaymentConfig.get().publishableKey,
                    stripeAccountId = lazyPaymentConfig.get().stripeAccountId,
                    configuration = CollectBankAccountConfiguration.USBankAccount(name, email),
                    elementsSessionId = elementsSessionId,
                    customerId = null,
                    onBehalfOf = onBehalfOf,
                    amount = amount?.toInt(),
                    currency = currency
                )
            }
            is SetupIntent -> {
                collectBankAccountLauncher.presentWithDeferredSetup(
                    publishableKey = lazyPaymentConfig.get().publishableKey,
                    stripeAccountId = lazyPaymentConfig.get().stripeAccountId,
                    configuration = CollectBankAccountConfiguration.USBankAccount(name, email),
                    elementsSessionId = elementsSessionId,
                    customerId = null,
                    onBehalfOf = onBehalfOf,
                )
            }
        }
    }

    private fun handleCollectBankAccountResult(
        result: CollectBankAccountResultInternal,
    ) {
        onUSBankAccountResult(result)
    }

    private fun handleCollectBankAccountForInstantDebitsResult(
        result: CollectBankAccountForInstantDebitsResult,
    ) {
        onLinkBankPaymentResult(result)
    }
}

private fun StripeIntent.asPaymentIntent(): PaymentIntent? {
    return this as? PaymentIntent
}
