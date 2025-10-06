package com.stripe.android.paymentelement.confirmation

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.PaymentConfiguration
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.googlepaylauncher.injection.GooglePayPaymentMethodLauncherFactory
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.analytics.FakeLinkAnalyticsHelper
import com.stripe.android.link.analytics.FakeLinkEventsReporter
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentelement.PreparePaymentMethodHandler
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.confirmation.bacs.BacsConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.cvc.CvcRecollectionConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.epms.ExternalPaymentMethodConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.intent.DefaultIntentConfirmationInterceptorFactory
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationInterceptor
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationInterceptor
import com.stripe.android.paymentelement.confirmation.intent.IntentFirstConfirmationInterceptor
import com.stripe.android.paymentelement.confirmation.intent.SharedPaymentTokenConfirmationInterceptor
import com.stripe.android.paymentelement.confirmation.link.LinkConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.linkinline.LinkInlineSignupConfirmationDefinition
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncherAssistedFactory
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.cvcrecollection.CvcRecollectionHandlerImpl
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationLauncherFactory
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionLauncherFactory
import com.stripe.android.paymentsheet.state.PaymentElementLoader.InitializationMode
import com.stripe.android.paymentsheet.utils.FakeUserFacingLogger
import com.stripe.android.testing.AbsFakeStripeRepository
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.utils.RecordingLinkStore
import kotlinx.coroutines.Dispatchers
import javax.inject.Provider

@OptIn(SharedPaymentTokenSessionPreview::class)
internal fun createIntentConfirmationInterceptor(
    initializationMode: InitializationMode,
    stripeRepository: StripeRepository = object : AbsFakeStripeRepository() {},
    publishableKeyProvider: () -> String = { "pk" },
    errorReporter: ErrorReporter = FakeErrorReporter(),
    intentCreationCallbackProvider: Provider<CreateIntentCallback?> = Provider { null },
    preparePaymentMethodHandlerProvider: Provider<PreparePaymentMethodHandler?> = Provider { null }
): IntentConfirmationInterceptor {
    val requestOptions = ApiRequest.Options(
        apiKey = publishableKeyProvider(),
        stripeAccount = null,
    )
    return DefaultIntentConfirmationInterceptorFactory(
        intentFirstConfirmationInterceptorFactory = object : IntentFirstConfirmationInterceptor.Factory {
            override fun create(clientSecret: String): IntentFirstConfirmationInterceptor {
                return IntentFirstConfirmationInterceptor(
                    clientSecret = clientSecret,
                    requestOptions = requestOptions,
                )
            }
        },
        deferredIntentConfirmationInterceptorFactory = object : DeferredIntentConfirmationInterceptor.Factory {
            override fun create(
                intentConfiguration: PaymentSheet.IntentConfiguration,
                createIntentCallback: CreateIntentCallback
            ): DeferredIntentConfirmationInterceptor {
                return DeferredIntentConfirmationInterceptor(
                    intentConfiguration = intentConfiguration,
                    stripeRepository = stripeRepository,
                    errorReporter = errorReporter,
                    intentCreationCallbackProvider = intentCreationCallbackProvider,
                    allowsManualConfirmation = false,
                    requestOptions = requestOptions,
                )
            }
        },
        sharedPaymentTokenConfirmationInterceptorFactory = object : SharedPaymentTokenConfirmationInterceptor.Factory {
            override fun create(
                initializationMode: InitializationMode.DeferredIntent,
                handler: PreparePaymentMethodHandler
            ): SharedPaymentTokenConfirmationInterceptor {
                return SharedPaymentTokenConfirmationInterceptor(
                    initializationMode = initializationMode,
                    stripeRepository = stripeRepository,
                    errorReporter = errorReporter,
                    preparePaymentMethodHandlerProvider = preparePaymentMethodHandlerProvider,
                    requestOptions = requestOptions,
                )
            }
        },
    ).create(initializationMode)
}

internal fun createTestConfirmationHandlerFactory(
    paymentElementCallbackIdentifier: String,
    intentConfirmationInterceptorFactory: IntentConfirmationInterceptor.Factory,
    savedStateHandle: SavedStateHandle,
    bacsMandateConfirmationLauncherFactory: BacsMandateConfirmationLauncherFactory,
    stripePaymentLauncherAssistedFactory: StripePaymentLauncherAssistedFactory,
    googlePayPaymentMethodLauncherFactory: GooglePayPaymentMethodLauncherFactory,
    cvcRecollectionLauncherFactory: CvcRecollectionLauncherFactory,
    linkConfigurationCoordinator: LinkConfigurationCoordinator,
    linkLauncher: LinkPaymentLauncher,
    paymentConfiguration: PaymentConfiguration,
    statusBarColor: Int?,
    errorReporter: ErrorReporter
): ConfirmationHandler.Factory {
    return DefaultConfirmationHandler.Factory(
        registry = ConfirmationRegistry(
            confirmationDefinitions = listOf(
                IntentConfirmationDefinition(
                    intentConfirmationInterceptorFactory = intentConfirmationInterceptorFactory,
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
                    paymentElementCallbackIdentifier = paymentElementCallbackIdentifier,
                    externalPaymentMethodConfirmHandlerProvider = {
                        PaymentElementCallbackReferences[paymentElementCallbackIdentifier]
                            ?.externalPaymentMethodConfirmHandler
                    },
                    errorReporter = errorReporter,
                ),
                LinkConfirmationDefinition(
                    linkPaymentLauncher = linkLauncher,
                    linkStore = RecordingLinkStore.noOp(),
                    linkAccountHolder = LinkAccountHolder(SavedStateHandle())
                ),
                LinkInlineSignupConfirmationDefinition(
                    linkConfigurationCoordinator = linkConfigurationCoordinator,
                    linkStore = RecordingLinkStore.noOp(),
                    linkAnalyticsHelper = FakeLinkAnalyticsHelper(),
                ),
                CvcRecollectionConfirmationDefinition(
                    factory = cvcRecollectionLauncherFactory,
                    handler = CvcRecollectionHandlerImpl(),
                ),
            )
        ),
        savedStateHandle = savedStateHandle,
        errorReporter = FakeErrorReporter(),
        ioContext = Dispatchers.Unconfined
    )
}

internal class ConfirmationTestScenario(
    val confirmationHandler: ConfirmationHandler,
)

internal class FakeLinkEventsReporterForConfirmation(
    reporter: FakeLinkEventsReporter
) : LinkEventsReporter by reporter {
    override fun onPopupShow() {
        // No-op
    }

    override fun onPopupSuccess() {
        // No-op
    }
}
