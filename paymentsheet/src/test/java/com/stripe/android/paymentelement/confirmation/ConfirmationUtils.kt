package com.stripe.android.paymentelement.confirmation

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.PaymentConfiguration
import com.stripe.android.googlepaylauncher.injection.GooglePayPaymentMethodLauncherFactory
import com.stripe.android.paymentelement.confirmation.bacs.BacsConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.epms.ExternalPaymentMethodConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationInterceptor
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncherAssistedFactory
import com.stripe.android.paymentsheet.ExternalPaymentMethodInterceptor
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationLauncherFactory
import com.stripe.android.paymentsheet.utils.FakeUserFacingLogger
import com.stripe.android.testing.FakeErrorReporter

internal fun createTestConfirmationHandlerFactory(
    intentConfirmationInterceptor: IntentConfirmationInterceptor,
    savedStateHandle: SavedStateHandle,
    bacsMandateConfirmationLauncherFactory: BacsMandateConfirmationLauncherFactory,
    stripePaymentLauncherAssistedFactory: StripePaymentLauncherAssistedFactory,
    googlePayPaymentMethodLauncherFactory: GooglePayPaymentMethodLauncherFactory,
    paymentConfiguration: PaymentConfiguration,
    statusBarColor: Int?,
    errorReporter: ErrorReporter,
): ConfirmationHandler.Factory {
    return DefaultConfirmationHandler.Factory(
        registry = ConfirmationRegistry(
            confirmationDefinitions = listOf(
                IntentConfirmationDefinition(
                    intentConfirmationInterceptor = intentConfirmationInterceptor,
                    paymentLauncherFactory = { launcher ->
                        stripePaymentLauncherAssistedFactory.create(
                            publishableKey = { paymentConfiguration.publishableKey },
                            stripeAccountId = { paymentConfiguration.stripeAccountId },
                            hostActivityLauncher = launcher,
                            statusBarColor = statusBarColor,
                            includePaymentSheetNextHandlers = true,
                        )
                    }
                ),
                BacsConfirmationDefinition(
                    bacsMandateConfirmationLauncherFactory = bacsMandateConfirmationLauncherFactory,
                ),
                GooglePayConfirmationDefinition(
                    googlePayPaymentMethodLauncherFactory = googlePayPaymentMethodLauncherFactory,
                    userFacingLogger = FakeUserFacingLogger(),
                ),
                ExternalPaymentMethodConfirmationDefinition(
                    externalPaymentMethodConfirmHandlerProvider = {
                        ExternalPaymentMethodInterceptor.externalPaymentMethodConfirmHandler
                    },
                    errorReporter = errorReporter,
                ),
            )
        ),
        savedStateHandle = savedStateHandle,
        errorReporter = FakeErrorReporter(),
    )
}
