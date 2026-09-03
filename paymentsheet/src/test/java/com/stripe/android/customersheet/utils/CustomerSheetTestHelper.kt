package com.stripe.android.customersheet.utils

import android.app.Application
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.PaymentConfiguration
import com.stripe.android.common.model.PaymentMethodRemovePermission
import com.stripe.android.common.nfcscan.NoOpIsNfcScanningAvailable
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.customersheet.CustomerPermissions
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.CustomerSheetIntegration
import com.stripe.android.customersheet.CustomerSheetLoader
import com.stripe.android.customersheet.CustomerSheetViewModel
import com.stripe.android.customersheet.analytics.CustomerSheetEventReporter
import com.stripe.android.customersheet.data.CustomerSheetDataResult
import com.stripe.android.customersheet.data.CustomerSheetPaymentMethodDataSource
import com.stripe.android.customersheet.data.CustomerSheetSavedSelectionDataSource
import com.stripe.android.customersheet.data.FakeCustomerSheetPaymentMethodDataSource
import com.stripe.android.customersheet.data.FakeCustomerSheetSavedSelectionDataSource
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdateCallback
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContractV2
import com.stripe.android.googlepaylauncher.InternalGooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.injection.InternalGooglePayPaymentMethodLauncherFactory
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethodFixtures
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures.CARD_PAYMENT_METHOD
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.createTestConfirmationHandlerFactory
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationInterceptor
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.paymentlauncher.PaymentLauncherContract
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncher
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncherAssistedFactory
import com.stripe.android.paymentsheet.cvcrecollection.RecordingCvcRecollectionLauncherFactory
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.FakeBacsMandateConfirmationLauncher
import com.stripe.android.paymentsheet.utils.FakeUserFacingLogger
import com.stripe.android.paymentsheet.utils.ViewModelStoreTestRule
import com.stripe.android.testing.DummyActivityResultCaller
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.utils.CompletableSingle
import com.stripe.android.utils.FakeIntentConfirmationInterceptor
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.RecordingLinkPaymentLauncher
import org.mockito.kotlin.mock
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

internal interface CustomerSheetTestHelper {
    val application: Application
        get() = ApplicationProvider.getApplicationContext()

    val viewModelStoreTestRule: ViewModelStoreTestRule

    fun createViewModel(
        isLiveMode: Boolean = false,
        workContext: CoroutineContext = EmptyCoroutineContext,
        integrationType: CustomerSheetIntegration.Type = CustomerSheetIntegration.Type.CustomerAdapter,
        isGooglePayAvailable: Boolean = true,
        customerPaymentMethods: List<PaymentMethod> = listOf(CARD_PAYMENT_METHOD),
        customerPermissions: CustomerPermissions = CustomerPermissions(
            removePaymentMethod = PaymentMethodRemovePermission.Full,
            canRemoveLastPaymentMethod = true,
            canUpdateCardExpiryAndBillingDetails = true,
        ),
        cbcEligibility: CardBrandChoiceEligibility = CardBrandChoiceEligibility.Ineligible,
        supportedPaymentMethods: List<SupportedPaymentMethod> = listOf(
            SupportedPaymentMethodFixtures.card,
            SupportedPaymentMethodFixtures.usBankAccount,
        ),
        savedPaymentSelection: PaymentSelection? = null,
        paymentConfiguration: PaymentConfiguration = PaymentConfiguration(
            publishableKey = "pk_test_123",
            stripeAccountId = null,
        ),
        apiConfiguration: ApiConfiguration.State = ApiConfiguration.State(
            publishableKey = paymentConfiguration.publishableKey,
            stripeAccountId = paymentConfiguration.stripeAccountId,
        ),
        configuration: CustomerSheet.Configuration = CustomerSheet.Configuration(
            merchantDisplayName = "Example",
            googlePayEnabled = isGooglePayAvailable,
        ),
        eventReporter: CustomerSheetEventReporter = mock(),
        intentConfirmationInterceptorFactory: IntentConfirmationInterceptor.Factory =
            object : IntentConfirmationInterceptor.Factory {
                override suspend fun create(
                    integrationMetadata: IntegrationMetadata,
                    customerMetadata: CustomerMetadata?,
                    clientAttributionMetadata: ClientAttributionMetadata,
                ): IntentConfirmationInterceptor {
                    return FakeIntentConfirmationInterceptor().apply {
                        enqueueCompleteStep(true)
                    }
                }
            },
        paymentMethodDataSource: CustomerSheetPaymentMethodDataSource = FakeCustomerSheetPaymentMethodDataSource(
            paymentMethods = CustomerSheetDataResult.success(customerPaymentMethods)
        ),
        attachmentStyle: IntegrationMetadata.CustomerSheet.AttachmentStyle =
            IntegrationMetadata.CustomerSheet.AttachmentStyle.SetupIntent,
        savedSelectionDataSource: CustomerSheetSavedSelectionDataSource = FakeCustomerSheetSavedSelectionDataSource(),
        customerSheetLoader: CustomerSheetLoader = FakeCustomerSheetLoader(
            customerPaymentMethods = customerPaymentMethods,
            supportedPaymentMethods = supportedPaymentMethods,
            paymentSelection = savedPaymentSelection,
            isGooglePayAvailable = isGooglePayAvailable,
            cbcEligibility = cbcEligibility,
            permissions = customerPermissions,
            attachmentStyle = attachmentStyle,
        ),
        errorReporter: ErrorReporter = FakeErrorReporter(),
        confirmationHandler: ConfirmationHandler? = null,
    ): CustomerSheetViewModel {
        val savedStateHandle = SavedStateHandle()
        return CustomerSheetViewModel(
            application = application,
            workContext = workContext,
            originalPaymentSelection = savedPaymentSelection,
            paymentMethodDataSourceProvider = CompletableSingle(paymentMethodDataSource),
            savedSelectionDataSourceProvider = CompletableSingle(savedSelectionDataSource),
            configuration = configuration,
            integrationType = integrationType,
            statusBarColor = null,
            apiConfigurationProvider = {
                if (isLiveMode) {
                    ApiConfiguration.State(publishableKey = "pk_live", stripeAccountId = null)
                } else {
                    apiConfiguration
                }
            },
            logger = Logger.noop(),
            productUsage = emptySet(),
            confirmationHandlerFactory = confirmationHandler?.let { ConfirmationHandler.Factory { _ -> it } }
                ?: createTestConfirmationHandlerFactory(
                    paymentElementCallbackIdentifier = "CustomerSheetTestIdentifier",
                    intentConfirmationInterceptorFactory = intentConfirmationInterceptorFactory,
                    paymentConfiguration = paymentConfiguration,
                    bacsMandateConfirmationLauncherFactory = {
                        FakeBacsMandateConfirmationLauncher()
                    },
                    stripePaymentLauncherAssistedFactory = object : StripePaymentLauncherAssistedFactory {
                        override fun create(
                            apiConfigurationProvider: Provider<ApiConfiguration.State>,
                            statusBarColor: Int?,
                            includePaymentSheetNextHandlers: Boolean,
                            hostActivityLauncher: ActivityResultLauncher<PaymentLauncherContract.Args>
                        ): StripePaymentLauncher {
                            return mock()
                        }
                    },
                    googlePayPaymentMethodLauncherFactory = object : InternalGooglePayPaymentMethodLauncherFactory {
                        override fun create(
                            instanceId: String,
                            lifecycleOwner: LifecycleOwner,
                            activityResultLauncher:
                            ActivityResultLauncher<GooglePayPaymentMethodLauncherContractV2.Args>,
                            onPaymentDataChangedCallback: GooglePayPaymentDataUpdateCallback?,
                        ): InternalGooglePayPaymentMethodLauncher = mock()
                    },
                    statusBarColor = null,
                    savedStateHandle = savedStateHandle,
                    errorReporter = FakeErrorReporter(),
                    linkLauncher = RecordingLinkPaymentLauncher.noOp(),
                    linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
                    cvcRecollectionLauncherFactory = RecordingCvcRecollectionLauncherFactory.noOp()
                ),
            eventReporter = eventReporter,
            customerSheetLoader = customerSheetLoader,
            isNfcScanningAvailable = NoOpIsNfcScanningAvailable(),
            errorReporter = errorReporter,
            savedStateHandle = savedStateHandle,
            userFacingLogger = FakeUserFacingLogger(),
        ).apply {
            registerFromActivity(DummyActivityResultCaller.noOp(), TestLifecycleOwner())
        }.also { viewModelStoreTestRule.track(it) }
    }
}
