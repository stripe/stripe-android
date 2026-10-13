package com.stripe.android.paymentsheet.injection

import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutSessionTaxRegionUpdater
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.DEFAULT_API_CONFIG
import com.stripe.android.model.Address
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.addresselement.AddressElementActivityContract
import com.stripe.android.paymentsheet.addresselement.AddressElementNavigator
import com.stripe.android.paymentsheet.addresselement.AddressElementResultStateHolder
import com.stripe.android.paymentsheet.addresselement.AddressElementResultStateHolder.State
import com.stripe.android.paymentsheet.addresselement.AddressLauncher
import com.stripe.android.paymentsheet.addresselement.FakePlacesClientProxy
import com.stripe.android.paymentsheet.addresselement.FakeStripeAutocompleteRepository
import com.stripe.android.paymentsheet.addresselement.InputAddressViewModel
import com.stripe.android.paymentsheet.addresselement.StripeHostedPlacesClientProxy
import com.stripe.android.paymentsheet.addresselement.TestAddressElementNavigator
import com.stripe.android.paymentsheet.addresselement.analytics.FakeAddressLauncherEventReporter
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.utils.ViewModelStoreTestRule
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import com.stripe.android.ui.core.elements.autocomplete.model.FindAutocompletePredictionsResponse
import com.stripe.android.uicore.elements.FormFieldId
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
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
    fun `providePrimaryButtonAction completes standalone through the view model`() = runScenario {
        viewModel.clickPrimaryButton(
            completedFormValues = mapOf(
                FormFieldId.Country to FormFieldEntry("US", true),
            ),
            checkboxChecked = true,
        )

        assertThat(resultStateHolder.state.value).isEqualTo(
            State.Finished(
                AddressElementActivityContract.Result.StandaloneSucceeded(
                    AddressDetails(
                        address = PaymentSheet.Address(country = "US"),
                        isCheckboxSelected = true,
                    )
                )
            )
        )
        assertThat(addressLauncherEventReporter.completedCalls.awaitItem()).isEqualTo(
            FakeAddressLauncherEventReporter.CompletedCall(
                country = "US",
                autocompleteResultSelected = false,
                editDistance = null,
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

            assertThat(resultStateHolder.state.value).isEqualTo(
                State.Finished(
                    AddressElementActivityContract.Result.CheckoutShippingSucceeded(
                        address = AddressDetails(
                            address = PaymentSheet.Address(country = "US"),
                            isCheckboxSelected = true,
                        ),
                        checkoutSessionResponse = checkoutSessionResponse,
                    )
                )
            )
        }

    @Test
    fun `providePrimaryButtonAction updates checkout shipping tax from submitted address`() =
        runCheckoutShippingScenario {
            resultStateHolder.state.test {
                assertThat(awaitItem()).isEqualTo(State.Idle)

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

                assertThat(awaitItem()).isEqualTo(State.Saving)
                val result = (awaitItem() as State.Finished).result as
                    AddressElementActivityContract.Result.CheckoutShippingSucceeded
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
            assertThat(viewModel.saveError.value)
                .isEqualTo(IllegalStateException("Invalid tax region").stripeErrorMessage())
            assertThat(resultStateHolder.state.value).isEqualTo(State.Idle)

            resultStateHolder.state.test {
                assertThat(awaitItem()).isEqualTo(State.Idle)

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

                assertThat(awaitItem()).isEqualTo(State.Saving)
                val result = (awaitItem() as State.Finished).result as
                    AddressElementActivityContract.Result.CheckoutShippingSucceeded
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
            resultStateHolder.state.test {
                assertThat(awaitItem()).isEqualTo(State.Idle)

                viewModel.clickPrimaryButton(
                    completedFormValues = COMPLETED_FORM_VALUES,
                    checkboxChecked = true,
                )

                assertThat(awaitItem()).isEqualTo(State.Saving)
                val result = (awaitItem() as State.Finished).result as
                    AddressElementActivityContract.Result.CheckoutShippingSucceeded
                assertThat(result.checkoutSessionResponse).isSameInstanceAs(checkoutSessionResponse)
            }
        }

    @Test
    fun `provideAddressElementEventReporter forwards standalone computed values`() = runTest {
        val addressLauncherEventReporter = FakeAddressLauncherEventReporter()
        val analyticsRequestExecutor = FakeAnalyticsRequestExecutor()
        val eventReporter = module.provideAddressElementEventReporter(
            args = AddressElementActivityContract.Args.Standalone(
                apiConfiguration = DEFAULT_API_CONFIG,
                config = AddressLauncher.Configuration(),
            ),
            addressLauncherEventReporter = addressLauncherEventReporter,
            analyticsRequestExecutor = analyticsRequestExecutor,
            analyticsRequestFactory = createAnalyticsRequestFactory(),
        )

        eventReporter.onShown(country = "CA")
        eventReporter.onSaveCompleted(
            addressDetails = EXPECTED_ADDRESS,
            autocompleteAddressDetails = AddressDetails(
                address = PaymentSheet.Address(
                    city = "San Francisco",
                    country = "US",
                    line1 = "511 Townsend St",
                    line2 = "Floor 2",
                    postalCode = "94103",
                    state = "CA",
                )
            ),
        )

        assertThat(addressLauncherEventReporter.showCalls.awaitItem()).isEqualTo("CA")
        assertThat(addressLauncherEventReporter.completedCalls.awaitItem()).isEqualTo(
            FakeAddressLauncherEventReporter.CompletedCall(
                country = "US",
                autocompleteResultSelected = true,
                editDistance = 1,
            )
        )
        assertThat(analyticsRequestExecutor.getExecutedRequests()).isEmpty()
        addressLauncherEventReporter.validate()
    }

    @Test
    fun `provideAddressElementEventReporter reports Checkout shipping events for the Checkout Session`() = runTest {
        val addressLauncherEventReporter = FakeAddressLauncherEventReporter()
        val analyticsRequestExecutor = FakeAnalyticsRequestExecutor()
        val checkoutSessionResponse = CheckoutSessionResponseFactory.create()
        val eventReporter = module.provideAddressElementEventReporter(
            args = AddressElementActivityContract.Args.CheckoutShipping(
                apiConfiguration = DEFAULT_API_CONFIG,
                config = AddressLauncher.Configuration(),
                checkoutSessionResponse = checkoutSessionResponse,
            ),
            addressLauncherEventReporter = addressLauncherEventReporter,
            analyticsRequestExecutor = analyticsRequestExecutor,
            analyticsRequestFactory = createAnalyticsRequestFactory(),
        )

        eventReporter.onShown(country = "CA")

        val params = analyticsRequestExecutor.getExecutedRequests().single().params
        assertThat(params).containsEntry("event", "elements.shipping_address.shown")
        assertThat(params).containsEntry("checkout_session_id", checkoutSessionResponse.id)
        assertThat(params["address_data_blob"]).isEqualTo(
            mapOf("address_country_code" to "CA")
        )
        addressLauncherEventReporter.validate()
    }

    @Test
    fun `provideInlinePlacesClient returns hosted client by default when google client is available`() {
        val googlePlacesClient = FakePlacesClientProxy(
            findPredictionsResult = Result.success(FindAutocompletePredictionsResponse(emptyList())),
            fetchPlaceResult = Result.success(Address()),
        )
        val addressLauncherEventReporter = FakeAddressLauncherEventReporter()
        val stripeAutocompleteRepository = FakeStripeAutocompleteRepository()
        val placesClient = module.provideInlinePlacesClient(
            args = AddressElementActivityContract.Args.Standalone(
                apiConfiguration = DEFAULT_API_CONFIG,
                config = AddressLauncher.Configuration(),
            ),
            stripeAutocompleteRepository = stripeAutocompleteRepository,
            googlePlacesClient = googlePlacesClient,
            addressLauncherEventReporter = addressLauncherEventReporter,
        )

        assertThat(placesClient).isInstanceOf(StripeHostedPlacesClientProxy::class.java)
        assertThat(placesClient).isNotSameInstanceAs(googlePlacesClient)
        googlePlacesClient.ensureAllEventsConsumed()
        stripeAutocompleteRepository.ensureAllEventsConsumed()
        addressLauncherEventReporter.validate()
    }

    @Test
    fun `provideGooglePlacesClient returns null without google api key`() {
        val placesClient = module.provideGooglePlacesClient(
            context = ApplicationProvider.getApplicationContext(),
            args = AddressElementActivityContract.Args.Standalone(
                apiConfiguration = DEFAULT_API_CONFIG,
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
    ) {
        val checkoutSessionResponse = CheckoutSessionResponseFactory.create(
            automaticTaxEnabled = automaticTaxEnabled,
            taxAddressSource = taxAddressSource,
        )
        runScenario(
            args = AddressElementActivityContract.Args.CheckoutShipping(
                apiConfiguration = DEFAULT_API_CONFIG,
                config = AddressLauncher.Configuration(),
                checkoutSessionResponse = checkoutSessionResponse,
            ),
        ) {
            CheckoutShippingScenario(
                checkoutSessionResponse = checkoutSessionResponse,
                resultStateHolder = resultStateHolder,
                viewModel = viewModel,
            ).block()

            addressLauncherEventReporter.completedCalls.expectNoEvents()
        }
    }

    private fun runScenario(
        args: AddressElementActivityContract.Args = AddressElementActivityContract.Args.Standalone(
            apiConfiguration = DEFAULT_API_CONFIG,
            config = AddressLauncher.Configuration(),
        ),
        block: suspend Scenario.() -> Unit,
    ) = runTest(UnconfinedTestDispatcher()) {
        TestAddressElementNavigator.test {
            val resultStateHolder = AddressElementResultStateHolder()
            val addressLauncherEventReporter = FakeAddressLauncherEventReporter()
            val viewModel = InputAddressViewModel(
                args = args,
                navigator = navigator,
                resultStateHolder = resultStateHolder,
                eventReporter = module.provideAddressElementEventReporter(
                    args = args,
                    addressLauncherEventReporter = addressLauncherEventReporter,
                    analyticsRequestExecutor = FakeAnalyticsRequestExecutor(),
                    analyticsRequestFactory = createAnalyticsRequestFactory(),
                ),
                placesClient = null,
                primaryButtonAction = module.providePrimaryButtonAction(
                    args = args,
                    taxRegionUpdater = createTaxRegionUpdater(),
                ),
            ).also(viewModelStoreRule::track)

            assertThat(getResultFlowCalls.awaitItem().key)
                .isEqualTo(AddressElementNavigator.AutocompleteEvent.KEY)

            Scenario(
                resultStateHolder = resultStateHolder,
                viewModel = viewModel,
                addressLauncherEventReporter = addressLauncherEventReporter,
            ).block()

            addressLauncherEventReporter.validate()
        }
    }

    private fun createAnalyticsRequestFactory() = AnalyticsRequestFactory(
        packageManager = null,
        packageInfo = null,
        packageName = "",
        publishableKeyProvider = { "" },
        networkTypeProvider = { "" },
        pluginTypeProvider = { null },
    )

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

    private data class Scenario(
        val resultStateHolder: AddressElementResultStateHolder,
        val viewModel: InputAddressViewModel,
        val addressLauncherEventReporter: FakeAddressLauncherEventReporter,
    )

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
