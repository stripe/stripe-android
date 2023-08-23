package com.stripe.android.customersheet

import android.app.Application
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures.CARD_PAYMENT_METHOD
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.paymentlauncher.PaymentLauncherContract
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncher
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncherAssistedFactory
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.injection.FormViewModelSubcomponent
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.uicore.address.AddressRepository
import com.stripe.android.utils.DummyActivityResultCaller
import com.stripe.android.utils.FakeIntentConfirmationInterceptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import javax.inject.Provider

@OptIn(ExperimentalCustomerSheetApi::class)
object CustomerSheetTestHelper {
    internal val application = ApplicationProvider.getApplicationContext<Application>()
    internal val lpmRepository = LpmRepository(
        LpmRepository.LpmRepositoryArguments(
            resources = application.resources,
            isFinancialConnectionsAvailable = { true },
        )
    ).apply {
        update(
            PaymentIntentFactory.create(paymentMethodTypes = this.supportedPaymentMethodTypes),
            null
        )
    }

    internal fun createViewModel(
        lpmRepository: LpmRepository = this.lpmRepository,
        isLiveMode: Boolean = false,
        initialBackStack: List<CustomerSheetViewState> = listOf(CustomerSheetViewState.Loading(isLiveMode)),
        savedPaymentSelection: PaymentSelection? = null,
        customerAdapter: CustomerAdapter = FakeCustomerAdapter(
            paymentMethods = CustomerAdapter.Result.success(listOf(CARD_PAYMENT_METHOD))
        ),
        stripeRepository: StripeRepository = FakeStripeRepository(),
        paymentConfiguration: PaymentConfiguration = PaymentConfiguration(
            publishableKey = "pk_test_123",
            stripeAccountId = null,
        ),
        configuration: CustomerSheet.Configuration = CustomerSheet.Configuration(
            googlePayEnabled = true
        ),
        isGooglePayAvailable: Boolean = true,
    ): CustomerSheetViewModel {
        val formViewModel = FormViewModel(
            context = application,
            formArguments = FormArguments(
                PaymentMethod.Type.Card.code,
                showCheckbox = false,
                showCheckboxControlledFields = false,
                initialPaymentMethodCreateParams = null,
                merchantName = configuration.merchantDisplayName
                    ?: application.applicationInfo.loadLabel(application.packageManager).toString(),
                billingDetails = configuration.defaultBillingDetails,
                billingDetailsCollectionConfiguration = configuration.billingDetailsCollectionConfiguration,
                isEligibleForCardBrandChoice = false,
            ),
            lpmRepository = lpmRepository,
            addressRepository = AddressRepository(
                resources = ApplicationProvider.getApplicationContext<Application>().resources,
                workContext = Dispatchers.Unconfined,
            ),
            showCheckboxFlow = mock()
        )
        val mockFormBuilder = mock<FormViewModelSubcomponent.Builder>()
        val mockFormSubcomponent = mock<FormViewModelSubcomponent>()
        val mockFormSubComponentBuilderProvider =
            mock<Provider<FormViewModelSubcomponent.Builder>>()
        whenever(mockFormBuilder.build()).thenReturn(mockFormSubcomponent)
        whenever(mockFormBuilder.formArguments(any())).thenReturn(mockFormBuilder)
        whenever(mockFormBuilder.showCheckboxFlow(any())).thenReturn(mockFormBuilder)
        whenever(mockFormSubcomponent.viewModel).thenReturn(formViewModel)
        whenever(mockFormSubComponentBuilderProvider.get()).thenReturn(mockFormBuilder)

        return CustomerSheetViewModel(
            application = application,
            initialBackStack = initialBackStack,
            savedPaymentSelection = savedPaymentSelection,
            paymentConfigurationProvider = { paymentConfiguration },
            formViewModelSubcomponentBuilderProvider = mockFormSubComponentBuilderProvider,
            resources = application.resources,
            stripeRepository = stripeRepository,
            customerAdapter = customerAdapter,
            lpmRepository = lpmRepository,
            configuration = configuration,
            isLiveModeProvider = { isLiveMode },
            logger = Logger.noop(),
            intentConfirmationInterceptor = FakeIntentConfirmationInterceptor().apply {
                enqueueCompleteStep(true)
            },
            paymentLauncherFactory = object : StripePaymentLauncherAssistedFactory {
                override fun create(
                    publishableKey: () -> String,
                    stripeAccountId: () -> String?,
                    statusBarColor: Int?,
                    hostActivityLauncher: ActivityResultLauncher<PaymentLauncherContract.Args>
                ): StripePaymentLauncher {
                    return mock()
                }
            },
            googlePayRepositoryFactory = {
                GooglePayRepository {
                    flowOf(isGooglePayAvailable)
                }
            },
            statusBarColor = { null },
        ).apply {
            registerFromActivity(DummyActivityResultCaller(), TestLifecycleOwner())
        }
    }
}
