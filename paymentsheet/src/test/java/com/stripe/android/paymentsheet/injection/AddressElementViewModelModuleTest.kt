package com.stripe.android.paymentsheet.injection

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutSessionTaxRegionUpdater
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.addresselement.AddressElementActivityContract
import com.stripe.android.paymentsheet.addresselement.AddressElementNavigator
import com.stripe.android.paymentsheet.addresselement.AddressElementResultStateHolder
import com.stripe.android.paymentsheet.addresselement.AddressLauncher
import com.stripe.android.paymentsheet.addresselement.FakeStripeAutocompleteRepository
import com.stripe.android.paymentsheet.addresselement.InputAddressViewModel
import com.stripe.android.paymentsheet.addresselement.StripeHostedPlacesClientProxy
import com.stripe.android.paymentsheet.addresselement.analytics.AddressLauncherEventReporter
import com.stripe.android.paymentsheet.addresselement.analytics.FakeAddressLauncherEventReporter
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.utils.ViewModelStoreTestRule
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import com.stripe.android.uicore.elements.FormFieldId
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import javax.inject.Provider

@RunWith(RobolectricTestRunner::class)
class AddressElementViewModelModuleTest {
    private val module = AddressElementViewModelModule()

    @get:Rule
    val viewModelStoreRule = ViewModelStoreTestRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `providePrimaryButtonAction completes standalone through the view model`() =
        runTest(UnconfinedTestDispatcher()) {
            val args = AddressElementActivityContract.Args.Standalone(
                publishableKey = "pk_123",
                config = AddressLauncher.Configuration(),
            )
            val resultStateHolder = AddressElementResultStateHolder()
            val viewModel = createViewModel(
                args = args,
                resultStateHolder = resultStateHolder,
            )

            viewModel.clickPrimaryButton(
                completedFormValues = mapOf(
                    FormFieldId.Country to FormFieldEntry("US", true),
                ),
                checkboxChecked = true,
            )

            assertThat(resultStateHolder.result.value).isEqualTo(
                AddressElementActivityContract.Result.StandaloneSucceeded(
                    AddressDetails(
                        address = PaymentSheet.Address(country = "US"),
                        isCheckboxSelected = true,
                    )
                )
            )
        }

    @Test
    fun `providePrimaryButtonAction completes checkout shipping through the view model`() =
        runTest(UnconfinedTestDispatcher()) {
            val checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                automaticTaxEnabled = false,
            )
            val args = AddressElementActivityContract.Args.CheckoutShipping(
                publishableKey = "pk_123",
                config = AddressLauncher.Configuration(),
                checkoutSessionResponse = checkoutSessionResponse,
            )
            val resultStateHolder = AddressElementResultStateHolder()
            val viewModel = createViewModel(
                args = args,
                resultStateHolder = resultStateHolder,
                taxRegionUpdater = Provider { createTaxRegionUpdater() },
            )

            viewModel.clickPrimaryButton(
                completedFormValues = mapOf(
                    FormFieldId.Country to FormFieldEntry("US", true),
                ),
                checkboxChecked = true,
            )

            assertThat(resultStateHolder.result.value).isEqualTo(
                AddressElementActivityContract.Result.CheckoutShippingSucceeded(
                    address = AddressDetails(
                        address = PaymentSheet.Address(country = "US"),
                        isCheckboxSelected = true,
                    ),
                    checkoutSessionResponse = checkoutSessionResponse,
                )
            )
        }

    @Test
    fun `provideInlinePlacesClient returns hosted client by default when google client is available`() {
        val googlePlacesClient = mock<PlacesClientProxy>()
        val placesClient = module.provideInlinePlacesClient(
            args = AddressElementActivityContract.Args.Standalone(
                publishableKey = "pk_123",
                config = AddressLauncher.Configuration(),
            ),
            stripeAutocompleteRepository = FakeStripeAutocompleteRepository(),
            googlePlacesClient = googlePlacesClient,
            addressLauncherEventReporter = FakeAddressLauncherEventReporter(),
        )

        assertThat(placesClient).isInstanceOf(StripeHostedPlacesClientProxy::class.java)
        assertThat(placesClient).isNotSameInstanceAs(googlePlacesClient)
    }

    @Test
    fun `provideGooglePlacesClient returns null without google api key`() {
        val placesClient = module.provideGooglePlacesClient(
            context = mock<Context>(),
            args = AddressElementActivityContract.Args.Standalone(
                publishableKey = "pk_123",
                config = AddressLauncher.Configuration(billingAddress = null),
            ),
        )

        assertThat(placesClient).isNull()
    }

    private fun createViewModel(
        args: AddressElementActivityContract.Args,
        resultStateHolder: AddressElementResultStateHolder,
        taxRegionUpdater: Provider<CheckoutSessionTaxRegionUpdater> = Provider {
            error("Tax region updater should not be requested for standalone")
        },
    ): InputAddressViewModel = InputAddressViewModel(
        args = args,
        navigator = mock<AddressElementNavigator>(),
        resultStateHolder = resultStateHolder,
        eventReporter = mock<AddressLauncherEventReporter>(),
        placesClient = null,
        primaryButtonAction = module.providePrimaryButtonAction(
            args = args,
            taxRegionUpdater = taxRegionUpdater,
        ),
    ).also(viewModelStoreRule::track)

    private fun createTaxRegionUpdater(): CheckoutSessionTaxRegionUpdater {
        return CheckoutSessionTaxRegionUpdater(
            CheckoutSessionRepository(
                stripeNetworkClient = DefaultStripeNetworkClient(),
                analyticsRequestExecutor = FakeAnalyticsRequestExecutor(),
                paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
                    context = ApplicationProvider.getApplicationContext(),
                    publishableKey = "pk_test_123",
                ),
                apiRequestOptionsProvider = Provider {
                    ApiRequest.Options(
                        apiKey = "pk_test_123",
                        stripeAccount = "acct_123",
                    )
                },
            ),
        )
    }
}
