package com.stripe.android.paymentelement.confirmation

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
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
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentelement.CreateIntentWithConfirmationTokenCallback
import com.stripe.android.paymentelement.PreparePaymentMethodHandler
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.confirmation.bacs.BacsConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.cvc.CvcRecollectionConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.epms.ExternalPaymentMethodConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.intent.ConfirmationTokenConfirmationInterceptor
import com.stripe.android.paymentelement.confirmation.intent.DefaultIntentConfirmationInterceptorFactory
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentCallbackRetriever
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
internal suspend fun createIntentConfirmationInterceptor(
    initializationMode: InitializationMode,
    customerId: String? = null,
    ephemeralKeySecret: String? = null,
    stripeRepository: StripeRepository = object : AbsFakeStripeRepository() {},
    publishableKeyProvider: () -> String = { "pk" },
    errorReporter: ErrorReporter = FakeErrorReporter(),
    intentCreationCallbackProvider: Provider<CreateIntentCallback?> = Provider { null },
    intentCreationConfirmationTokenCallbackProvider: Provider<CreateIntentWithConfirmationTokenCallback?> = Provider {
        null
    },
    preparePaymentMethodHandlerProvider: Provider<PreparePaymentMethodHandler?> = Provider { null }
): IntentConfirmationInterceptor {
    val requestOptions = ApiRequest.Options(
        apiKey = publishableKeyProvider(),
        stripeAccount = null,
    )
    val deferredIntentCallbackRetriever = DeferredIntentCallbackRetriever(
        intentCreationCallbackProvider = intentCreationCallbackProvider,
        intentCreateIntentWithConfirmationTokenCallback = intentCreationConfirmationTokenCallbackProvider,
        preparePaymentMethodHandlerProvider = preparePaymentMethodHandlerProvider,
        errorReporter = errorReporter,
        requestOptionsProvider = { requestOptions },
    )
    return DefaultIntentConfirmationInterceptorFactory(
        deferredIntentCallbackRetriever = deferredIntentCallbackRetriever,
        intentFirstConfirmationInterceptorFactory = object : IntentFirstConfirmationInterceptor.Factory {
            override fun create(
                clientSecret: String,
                clientAttributionMetadata: ClientAttributionMetadata?
            ): IntentFirstConfirmationInterceptor {
                return IntentFirstConfirmationInterceptor(
                    clientSecret = clientSecret,
                    requestOptions = requestOptions,
                    clientAttributionMetadata = clientAttributionMetadata,
                )
            }
        },
        deferredIntentConfirmationInterceptorFactory = object : DeferredIntentConfirmationInterceptor.Factory {
            override fun create(
                intentConfiguration: PaymentSheet.IntentConfiguration,
                createIntentCallback: CreateIntentCallback,
                clientAttributionMetadata: ClientAttributionMetadata?
            ): DeferredIntentConfirmationInterceptor {
                return DeferredIntentConfirmationInterceptor(
                    intentConfiguration = intentConfiguration,
                    createIntentCallback = createIntentCallback,
                    stripeRepository = stripeRepository,
                    allowsManualConfirmation = false,
                    requestOptions = requestOptions,
                    clientAttributionMetadata = clientAttributionMetadata,
                )
            }
        },
        confirmationTokenConfirmationInterceptorFactory = object : ConfirmationTokenConfirmationInterceptor.Factory {
            override fun create(
                intentConfiguration: PaymentSheet.IntentConfiguration,
                createIntentCallback: CreateIntentWithConfirmationTokenCallback,
                customerId: String?,
                ephemeralKeySecret: String?,
            ): ConfirmationTokenConfirmationInterceptor {
                return ConfirmationTokenConfirmationInterceptor(
                    intentConfiguration = intentConfiguration,
                    customerId = customerId,
                    createIntentCallback = createIntentCallback,
                    ephemeralKeySecret = ephemeralKeySecret,
                    context = ApplicationProvider.getApplicationContext(),
                    stripeRepository = stripeRepository,
                    requestOptions = requestOptions,
                    userFacingLogger = FakeUserFacingLogger(),
                )
            }
        },
        sharedPaymentTokenConfirmationInterceptorFactory = object : SharedPaymentTokenConfirmationInterceptor.Factory {
            override fun create(
                intentConfiguration: PaymentSheet.IntentConfiguration,
                handler: PreparePaymentMethodHandler
            ): SharedPaymentTokenConfirmationInterceptor {
                return SharedPaymentTokenConfirmationInterceptor(
                    intentConfiguration = intentConfiguration,
                    handler = handler,
                    stripeRepository = stripeRepository,
                    errorReporter = errorReporter,
                    requestOptions = requestOptions,
                )
            }
        },
    ).create(
        initializationMode = initializationMode,
        customerId = customerId,
        ephemeralKeySecret = ephemeralKeySecret,
        clientAttributionMetadata = null,
    )
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
