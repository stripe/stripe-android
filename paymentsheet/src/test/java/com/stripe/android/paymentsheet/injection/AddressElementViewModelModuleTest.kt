package com.stripe.android.paymentsheet.injection

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutSessionTaxRegionUpdater
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.addresselement.AddressElementActivityContract
import com.stripe.android.paymentsheet.addresselement.AddressElementNavigator
import com.stripe.android.paymentsheet.addresselement.AddressElementResultStateHolder
import com.stripe.android.paymentsheet.addresselement.AddressLauncher
import com.stripe.android.paymentsheet.addresselement.FakeStripeAutocompleteRepository
import com.stripe.android.paymentsheet.addresselement.InputAddressViewModel
import com.stripe.android.paymentsheet.addresselement.StripeHostedPlacesClientProxy
import com.stripe.android.paymentsheet.addresselement.analytics.FakeAddressLauncherEventReporter
import com.stripe.android.paymentsheet.addresselement.analytics.NoOpShippingAddressElementEventReporter
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
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

    @get:Rule
    val networkRule = NetworkRule()

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
                taxRegionUpdater = createTaxRegionUpdater(),
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
        runCheckoutShippingScenario(automaticTaxEnabled = false) {
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
    fun `providePrimaryButtonAction updates checkout shipping tax from submitted address`() =
        runCheckoutShippingScenario {
            resultStateHolder.result.test {
                assertThat(awaitItem()).isNull()

                networkRule.checkoutUpdate(
                    bodyPart("tax_region[country]", "US"),
                    bodyPart("tax_region[line1]", "510 Townsend St"),
                    bodyPart("tax_region[line2]", "Floor 2"),
                    bodyPart("tax_region[city]", "San Francisco"),
                    bodyPart("tax_region[state]", "CA"),
                    bodyPart("tax_region[postal_code]", "94103"),
                ) { response ->
                    response.testBodyFromFile("checkout-session-init.json") { json ->
                        json.getJSONArray("checkout_items").getJSONObject(0)
                            .getJSONObject("one_time_price").getJSONArray("items").getJSONObject(0)
                            .put("total", 5099)
                    }
                }
                viewModel.clickPrimaryButton(
                    completedFormValues = COMPLETED_FORM_VALUES,
                    checkboxChecked = true,
                )

                val result = awaitItem() as AddressElementActivityContract.Result.CheckoutShippingSucceeded
                assertThat(result.address).isEqualTo(EXPECTED_ADDRESS)
                assertThat(result.checkoutSessionResponse.id).isEqualTo(checkoutSessionResponse.id)
                assertThat(result.checkoutSessionResponse.amount).isEqualTo(5099L)
                assertThat(result.checkoutSessionResponse).isNotEqualTo(checkoutSessionResponse)
            }
        }

    @Test
    fun `providePrimaryButtonAction returns failure and can retry after checkout shipping tax update fails`() =
        runCheckoutShippingScenario {
            viewModel.formEnabled.test {
                assertThat(awaitItem()).isTrue()

                networkRule.checkoutUpdate { response ->
                    response.setResponseCode(400)
                    response.setBody("""{"error":{"message":"Invalid tax region"}}""")
                }
                viewModel.clickPrimaryButton(
                    completedFormValues = COMPLETED_FORM_VALUES,
                    checkboxChecked = true,
                )

                assertThat(awaitItem()).isFalse()
                assertThat(awaitItem()).isTrue()
            }
            assertThat(resultStateHolder.result.value).isNull()

            resultStateHolder.result.test {
                assertThat(awaitItem()).isNull()

                networkRule.checkoutUpdate { response ->
                    response.testBodyFromFile("checkout-session-init.json") { json ->
                        json.getJSONArray("checkout_items").getJSONObject(0)
                            .getJSONObject("one_time_price").getJSONArray("items").getJSONObject(0)
                            .put("total", 5099)
                    }
                }
                viewModel.clickPrimaryButton(
                    completedFormValues = COMPLETED_FORM_VALUES,
                    checkboxChecked = true,
                )

                val result = awaitItem() as AddressElementActivityContract.Result.CheckoutShippingSucceeded
                assertThat(result.address).isEqualTo(EXPECTED_ADDRESS)
                assertThat(result.checkoutSessionResponse.amount).isEqualTo(5099L)
            }
            assertThat(viewModel.formEnabled.value).isFalse()
        }

    @Test
    fun `providePrimaryButtonAction returns original response when automatic tax targets billing`() =
        runCheckoutShippingScenario(
            taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
        ) {
            resultStateHolder.result.test {
                assertThat(awaitItem()).isNull()

                viewModel.clickPrimaryButton(
                    completedFormValues = COMPLETED_FORM_VALUES,
                    checkboxChecked = true,
                )

                val result = awaitItem() as AddressElementActivityContract.Result.CheckoutShippingSucceeded
                assertThat(result.checkoutSessionResponse).isSameInstanceAs(checkoutSessionResponse)
            }
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

    private fun runCheckoutShippingScenario(
        automaticTaxEnabled: Boolean = true,
        taxAddressSource: CheckoutSessionResponse.TaxAddressSource =
            CheckoutSessionResponse.TaxAddressSource.SHIPPING,
        block: suspend CheckoutShippingScenario.() -> Unit,
    ) = runTest(UnconfinedTestDispatcher()) {
        val checkoutSessionResponse = CheckoutSessionResponseFactory.create(
            automaticTaxEnabled = automaticTaxEnabled,
            taxAddressSource = taxAddressSource,
        )
        val resultStateHolder = AddressElementResultStateHolder()
        val args = AddressElementActivityContract.Args.CheckoutShipping(
            publishableKey = "pk_123",
            config = AddressLauncher.Configuration(),
            checkoutSessionResponse = checkoutSessionResponse,
        )
        val viewModel = createViewModel(
            args = args,
            resultStateHolder = resultStateHolder,
            taxRegionUpdater = createTaxRegionUpdater(),
        )

        CheckoutShippingScenario(
            checkoutSessionResponse = checkoutSessionResponse,
            resultStateHolder = resultStateHolder,
            viewModel = viewModel,
        ).block()
    }

    private fun createViewModel(
        args: AddressElementActivityContract.Args,
        resultStateHolder: AddressElementResultStateHolder,
        taxRegionUpdater: CheckoutSessionTaxRegionUpdater,
    ): InputAddressViewModel = InputAddressViewModel(
        args = args,
        navigator = mock<AddressElementNavigator>(),
        resultStateHolder = resultStateHolder,
        eventReporter = mock(),
        shippingAddressElementEventReporter = NoOpShippingAddressElementEventReporter,
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

    private data class CheckoutShippingScenario(
        val checkoutSessionResponse: CheckoutSessionResponse,
        val resultStateHolder: AddressElementResultStateHolder,
        val viewModel: InputAddressViewModel,
    )

    private companion object {
        val EXPECTED_ADDRESS = AddressDetails(
            name = "Jenny Rosen",
            address = PaymentSheet.Address(
                city = "San Francisco",
                country = "US",
                line1 = "510 Townsend St",
                line2 = "Floor 2",
                postalCode = "94103",
                state = "CA",
            ),
            phoneNumber = "+14155551212",
            isCheckboxSelected = true,
        )
        val COMPLETED_FORM_VALUES = mapOf(
            FormFieldId.Name to FormFieldEntry("Jenny Rosen", true),
            FormFieldId.City to FormFieldEntry("San Francisco", true),
            FormFieldId.Country to FormFieldEntry("US", true),
            FormFieldId.Line1 to FormFieldEntry("510 Townsend St", true),
            FormFieldId.Line2 to FormFieldEntry("Floor 2", true),
            FormFieldId.Phone to FormFieldEntry("+14155551212", true),
            FormFieldId.PostalCode to FormFieldEntry("94103", true),
            FormFieldId.State to FormFieldEntry("CA", true),
        )
    }
}
