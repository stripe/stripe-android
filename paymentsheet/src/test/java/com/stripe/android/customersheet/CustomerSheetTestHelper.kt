package com.stripe.android.customersheet

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.injection.FormViewModelSubcomponent
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.uicore.address.AddressRepository
import kotlinx.coroutines.Dispatchers
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Stack
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
        backstack: Stack<CustomerSheetViewState> = Stack<CustomerSheetViewState>().apply {
            push(CustomerSheetViewState.Loading(isLiveMode))
        },
        customerAdapter: CustomerAdapter = FakeCustomerAdapter(),
        stripeRepository: StripeRepository = FakeStripeRepository(),
        paymentConfiguration: PaymentConfiguration = PaymentConfiguration(
            publishableKey = "pk_test_123",
            stripeAccountId = null,
        ),
        configuration: CustomerSheet.Configuration = CustomerSheet.Configuration(),
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
                billingDetailsCollectionConfiguration = configuration.billingDetailsCollectionConfiguration
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
            backstack = backstack,
            paymentConfiguration = paymentConfiguration,
            formViewModelSubcomponentBuilderProvider = mockFormSubComponentBuilderProvider,
            resources = application.resources,
            stripeRepository = stripeRepository,
            customerAdapter = customerAdapter,
            lpmRepository = lpmRepository,
            configuration = configuration,
            isLiveMode = isLiveMode,
            logger = Logger.noop(),
        )
    }
}
