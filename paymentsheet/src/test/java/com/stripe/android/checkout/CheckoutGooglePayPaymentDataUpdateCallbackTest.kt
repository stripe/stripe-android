package com.stripe.android.checkout

import android.content.Context
import android.os.Looper
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataError
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdate
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdateResponse
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.DEFAULT_API_CONFIG
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.repositories.CheckoutStripeRepositoryFactory
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.utils.FakeFraudDetectionDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class CheckoutGooglePayPaymentDataUpdateCallbackTest {
    @get:Rule
    val networkRule = NetworkRule()

    @Test
    fun `onPaymentDataChanged returns error when state is not loaded`() = runScenario(
        state = null,
    ) {
        val response = onPaymentDataChanged(
            GooglePayPaymentDataUpdate(
                callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.Initialize,
                shippingAddress = null,
                billingAddress = null,
            )
        )

        assertThat(response.newTransactionInfo).isNull()

        val error = response.error
        assertThat(error).isNotNull()
        assertThat(error?.reason).isEqualTo(GooglePayPaymentDataError.Reason.OtherError)
        assertThat(error?.message).isEqualTo("Something went wrong")
        assertThat(error?.intent).isEqualTo(GooglePayPaymentDataError.Intent.ShippingAddress)
    }

    @Test
    fun `onPaymentDataChanged maps state when merchant country is unavailable`() = runScenario(
        state = CheckoutElementsControllerStateFactory.create(
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(merchantCountry = null),
        ),
        addressBuilderResult = CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result.Unavailable,
        mapperResponse = GooglePayPaymentDataUpdateResponse(newTransactionInfo = null, error = null),
    ) {
        val paymentDataUpdate = GooglePayPaymentDataUpdate(
            callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.Initialize,
            shippingAddress = null,
            billingAddress = null,
        )

        val response = onPaymentDataChanged(paymentDataUpdate)

        assertThat(response).isEqualTo(providedMapperResponse)
        val shippingBuildCall = addressBuilder.buildCalls.awaitItem()
        assertThat(shippingBuildCall.paymentDataUpdate).isEqualTo(paymentDataUpdate)
        assertThat(shippingBuildCall.addressSource)
            .isEqualTo(CheckoutSessionResponse.TaxAddressSource.SHIPPING)
        val billingBuildCall = addressBuilder.buildCalls.awaitItem()
        assertThat(billingBuildCall.paymentDataUpdate).isEqualTo(paymentDataUpdate)
        assertThat(billingBuildCall.addressSource)
            .isEqualTo(CheckoutSessionResponse.TaxAddressSource.BILLING)

        val toResponseCall = mapper.toResponseCalls.awaitItem()
        assertThat(toResponseCall.countryCode).isNull()
        assertThat(toResponseCall.response).isEqualTo(providedState?.checkoutSessionResponse)
        assertThat(toResponseCall.paymentDataUpdate).isEqualTo(paymentDataUpdate)
    }

    @Test
    fun `onPaymentDataChanged maps state without building an address for initialization when unavailable`() =
        addressUnavailableTest(
            callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.Initialize
        )

    @Test
    fun `onPaymentDataChanged maps state without building an address for shipping address changes when unavailable`() =
        addressUnavailableTest(
            callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.ShippingAddress
        )

    @Test
    fun `onPaymentDataChanged maps state for payment method changes when billing address is unavailable`() =
        addressUnavailableTest(
            callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.PaymentMethod
        )

    @Test
    fun `onPaymentDataChanged updates tax region & responds successfully for initialization`() = successTest(
        callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.Initialize,
    )

    @Test
    fun `initialization retains shipping tax response through local billing update`() = runScenario(
        addressBuilderResultProvider = { addressSource ->
            CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result.Available(
                if (addressSource == CheckoutSessionResponse.TaxAddressSource.SHIPPING) ADDRESS else BILLING_ADDRESS
            )
        },
    ) {
        enqueueTaxUpdate()
        val paymentDataUpdate = GooglePayPaymentDataUpdate(
            callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.Initialize,
            shippingAddress = SHIPPING_ADDRESS,
            billingAddress = BILLING_GOOGLE_PAY_ADDRESS,
        )

        assertThat(onPaymentDataChanged(paymentDataUpdate)).isEqualTo(providedMapperResponse)
        assertBuildCalls(GooglePayPaymentDataUpdate.CallbackTrigger.Initialize, paymentDataUpdate)
        val toResponseCall = mapper.toResponseCalls.awaitItem()
        assertThat(toResponseCall.response).isNotSameInstanceAs(providedState?.checkoutSessionResponse)
        assertThat(toResponseCall.response.customerEmail).isEqualTo(UPDATED_EMAIL)
        assertThat(toResponseCall.response.amount).isEqualTo(UPDATED_AMOUNT)
        assertThat(toResponseCall.response.taxAddressSource).isEqualTo(CheckoutSessionResponse.TaxAddressSource.SHIPPING)
        assertThat(toResponseCall.paymentDataUpdate).isEqualTo(paymentDataUpdate)
    }

    @Test
    fun `initialization updates billing when shipping address is unavailable`() = runScenario(
        state = taxState(CheckoutSessionResponse.TaxAddressSource.BILLING),
        addressBuilderResultProvider = { addressSource ->
            when (addressSource) {
                CheckoutSessionResponse.TaxAddressSource.SHIPPING ->
                    CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result.Unavailable
                CheckoutSessionResponse.TaxAddressSource.BILLING ->
                    CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result.Available(BILLING_ADDRESS)
            }
        },
    ) {
        enqueueTaxUpdate(BILLING_ADDRESS)
        val paymentDataUpdate = GooglePayPaymentDataUpdate(
            callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.Initialize,
            shippingAddress = null,
            billingAddress = BILLING_GOOGLE_PAY_ADDRESS,
        )

        assertThat(onPaymentDataChanged(paymentDataUpdate)).isEqualTo(providedMapperResponse)
        assertBuildCalls(GooglePayPaymentDataUpdate.CallbackTrigger.Initialize, paymentDataUpdate)
        val response = mapper.toResponseCalls.awaitItem().response
        assertThat(response.customerEmail).isEqualTo(UPDATED_EMAIL)
        assertThat(response.amount).isEqualTo(UPDATED_AMOUNT)
    }

    @Test
    fun `initialization stops before billing when shipping tax update fails`() = runScenario {
        enqueueTaxUpdateFailure()
        val paymentDataUpdate = GooglePayPaymentDataUpdate(
            callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.Initialize,
            shippingAddress = SHIPPING_ADDRESS,
            billingAddress = BILLING_GOOGLE_PAY_ADDRESS,
        )

        val response = onPaymentDataChanged(paymentDataUpdate)

        assertThat(response.newTransactionInfo).isNull()
        assertThat(response.error?.intent).isEqualTo(GooglePayPaymentDataError.Intent.ShippingAddress)
        assertThat(response.error?.reason).isEqualTo(GooglePayPaymentDataError.Reason.ShippingAddressInvalid)
        assertThat(addressBuilder.buildCalls.awaitItem().addressSource)
            .isEqualTo(CheckoutSessionResponse.TaxAddressSource.SHIPPING)
        mapper.toResponseCalls.expectNoEvents()
    }

    @Test
    fun `initialization returns payment method error when billing tax update fails after local shipping succeeds`() =
        runScenario(
            state = taxState(CheckoutSessionResponse.TaxAddressSource.BILLING),
            addressBuilderResultProvider = { addressSource ->
                CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result.Available(
                    if (addressSource == CheckoutSessionResponse.TaxAddressSource.SHIPPING) ADDRESS else BILLING_ADDRESS
                )
            },
        ) {
            enqueueTaxUpdateFailure(BILLING_ADDRESS)
            val paymentDataUpdate = GooglePayPaymentDataUpdate(
                callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.Initialize,
                shippingAddress = SHIPPING_ADDRESS,
                billingAddress = BILLING_GOOGLE_PAY_ADDRESS,
            )

            val response = onPaymentDataChanged(paymentDataUpdate)

            assertThat(response.newTransactionInfo).isNull()
            assertThat(response.error?.intent).isEqualTo(GooglePayPaymentDataError.Intent.PaymentMethod)
            assertThat(response.error?.reason).isEqualTo(GooglePayPaymentDataError.Reason.OtherError)
            assertBuildCalls(GooglePayPaymentDataUpdate.CallbackTrigger.Initialize, paymentDataUpdate)
            mapper.toResponseCalls.expectNoEvents()
        }

    @Test
    fun `onPaymentDataChanged updates tax region & responds successfully for shipping address change`() = successTest(
        callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.ShippingAddress,
    )

    @Test
    fun `onPaymentDataChanged updates billing tax region for payment method change`() = successTest(
        callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.PaymentMethod,
    )

    @Test
    fun `onPaymentDataChanged returns error when updating tax region fails`() = runScenario {
        enqueueTaxUpdateFailure()
        val paymentDataUpdate = GooglePayPaymentDataUpdate(
            callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.ShippingAddress,
            shippingAddress = SHIPPING_ADDRESS,
            billingAddress = null,
        )

        val response = onPaymentDataChanged(paymentDataUpdate)

        assertThat(response.newTransactionInfo).isNull()

        val error = response.error
        assertThat(error).isNotNull()
        assertThat(error?.reason).isEqualTo(GooglePayPaymentDataError.Reason.ShippingAddressInvalid)
        assertThat(error?.message).isEqualTo("Something went wrong")
        assertThat(error?.intent).isEqualTo(GooglePayPaymentDataError.Intent.ShippingAddress)

        assertThat(addressBuilder.buildCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `onPaymentDataChanged returns payment method error when updating billing tax region fails`() = runScenario(
        state = taxState(CheckoutSessionResponse.TaxAddressSource.BILLING),
    ) {
        enqueueTaxUpdateFailure()
        val paymentDataUpdate = GooglePayPaymentDataUpdate(
            callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.PaymentMethod,
            shippingAddress = null,
            billingAddress = SHIPPING_ADDRESS,
        )

        val response = onPaymentDataChanged(paymentDataUpdate)

        assertThat(response.newTransactionInfo).isNull()

        val error = response.error
        assertThat(error).isNotNull()
        assertThat(error?.reason).isEqualTo(GooglePayPaymentDataError.Reason.OtherError)
        assertThat(error?.message).isEqualTo("Something went wrong")
        assertThat(error?.intent).isEqualTo(GooglePayPaymentDataError.Intent.PaymentMethod)

        assertThat(addressBuilder.buildCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `onPaymentDataChanged for shipping option maps state without updating tax region`() = unsupportedCallbackTest(
        callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.ShippingOption,
        expectedTriggerParam = "shipping_option",
    )

    @Test
    fun `onPaymentDataChanged for offer maps state without updating tax region`() = unsupportedCallbackTest(
        callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.Offer,
        expectedTriggerParam = "offer",
    )

    @Test
    fun `shipping address change returns original response when automatic tax is disabled`() = runScenario(
        state = CheckoutElementsControllerStateFactory.create(),
    ) {
        assertThat(onPaymentDataChanged(SHIPPING_ADDRESS_UPDATE)).isEqualTo(providedMapperResponse)
        assertThat(addressBuilder.buildCalls.awaitItem().addressSource)
            .isEqualTo(CheckoutSessionResponse.TaxAddressSource.SHIPPING)
        assertThat(mapper.toResponseCalls.awaitItem().response)
            .isSameInstanceAs(providedState?.checkoutSessionResponse)
    }

    private fun unsupportedCallbackTest(
        callbackTrigger: GooglePayPaymentDataUpdate.CallbackTrigger,
        expectedTriggerParam: String,
    ) = runScenario(
        mapperResponse = GooglePayPaymentDataUpdateResponse(newTransactionInfo = null, error = null),
    ) {
        val paymentDataUpdate = GooglePayPaymentDataUpdate(
            callbackTrigger = callbackTrigger,
            shippingAddress = null,
            billingAddress = null,
        )

        val response = onPaymentDataChanged(paymentDataUpdate)

        assertThat(response.newTransactionInfo).isNull()
        assertThat(response.error).isNull()

        val toResponseCall = mapper.toResponseCalls.awaitItem()

        assertThat(toResponseCall.countryCode).isEqualTo(providedState?.checkoutSessionResponse?.merchantCountry)
        assertThat(toResponseCall.response).isEqualTo(providedState?.checkoutSessionResponse)
        assertThat(toResponseCall.paymentDataUpdate).isEqualTo(paymentDataUpdate)

        val call = errorReporter.awaitCall()
        assertThat(call.errorEvent).isEqualTo(
            ErrorReporter.UnexpectedErrorEvent.CHECKOUT_SESSION_GOOGLE_PAY_UNEXPECTED_CALLBACK_TRIGGER
        )
        assertThat(call.additionalNonPiiParams)
            .isEqualTo(mapOf("unexpected_trigger_type" to expectedTriggerParam))
    }

    private fun addressUnavailableTest(
        callbackTrigger: GooglePayPaymentDataUpdate.CallbackTrigger
    ) {
        runScenario(
            addressBuilderResult = CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result.Unavailable,
            mapperResponse = GooglePayPaymentDataUpdateResponse(newTransactionInfo = null, error = null),
        ) {
            val paymentDataUpdate = GooglePayPaymentDataUpdate(
                callbackTrigger = callbackTrigger,
                shippingAddress = null,
                billingAddress = null,
            )

            val response = onPaymentDataChanged(paymentDataUpdate)

            val expectedSources = when (callbackTrigger) {
                GooglePayPaymentDataUpdate.CallbackTrigger.PaymentMethod ->
                    listOf(CheckoutSessionResponse.TaxAddressSource.BILLING)
                GooglePayPaymentDataUpdate.CallbackTrigger.Initialize -> listOf(
                    CheckoutSessionResponse.TaxAddressSource.SHIPPING,
                    CheckoutSessionResponse.TaxAddressSource.BILLING,
                )
                else -> listOf(CheckoutSessionResponse.TaxAddressSource.SHIPPING)
            }
            expectedSources.forEach { expectedSource ->
                val buildCall = addressBuilder.buildCalls.awaitItem()
                assertThat(buildCall.paymentDataUpdate.shippingAddress).isNull()
                assertThat(buildCall.paymentDataUpdate.callbackTrigger).isEqualTo(callbackTrigger)
                assertThat(buildCall.addressSource).isEqualTo(expectedSource)
            }

            assertThat(response).isEqualTo(providedMapperResponse)

            val toResponseCall = mapper.toResponseCalls.awaitItem()
            assertThat(toResponseCall.countryCode).isEqualTo(providedState?.checkoutSessionResponse?.merchantCountry)
            assertThat(toResponseCall.response).isEqualTo(providedState?.checkoutSessionResponse)
            assertThat(toResponseCall.paymentDataUpdate).isEqualTo(paymentDataUpdate)
        }
    }

    private fun successTest(
        callbackTrigger: GooglePayPaymentDataUpdate.CallbackTrigger,
    ) {
        val addressSource = if (callbackTrigger == GooglePayPaymentDataUpdate.CallbackTrigger.PaymentMethod) {
            CheckoutSessionResponse.TaxAddressSource.BILLING
        } else {
            CheckoutSessionResponse.TaxAddressSource.SHIPPING
        }
        runScenario(
            addressBuilderResultProvider = { source ->
                if (callbackTrigger == GooglePayPaymentDataUpdate.CallbackTrigger.Initialize && source != addressSource) {
                    CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result.Unavailable
                } else {
                    CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result.Available(ADDRESS)
                }
            },
            state = taxState(addressSource),
            mapperResponse = GooglePayPaymentDataUpdateResponse(newTransactionInfo = TRANSACTION_INFO, error = null),
        ) {
            enqueueTaxUpdate()
            val paymentDataUpdate = GooglePayPaymentDataUpdate(
                callbackTrigger = callbackTrigger,
                shippingAddress = SHIPPING_ADDRESS.takeUnless {
                    callbackTrigger == GooglePayPaymentDataUpdate.CallbackTrigger.PaymentMethod
                },
                billingAddress = SHIPPING_ADDRESS.takeIf {
                    callbackTrigger == GooglePayPaymentDataUpdate.CallbackTrigger.PaymentMethod
                },
            )

            assertThat(onPaymentDataChanged(paymentDataUpdate)).isEqualTo(providedMapperResponse)
            assertBuildCalls(callbackTrigger, paymentDataUpdate)
            val toResponseCall = mapper.toResponseCalls.awaitItem()
            assertThat(toResponseCall.countryCode).isEqualTo(providedState?.checkoutSessionResponse?.merchantCountry)
            assertThat(toResponseCall.response.customerEmail).isEqualTo(UPDATED_EMAIL)
            assertThat(toResponseCall.response.amount).isEqualTo(UPDATED_AMOUNT)
            assertThat(toResponseCall.response.taxAddressSource).isEqualTo(addressSource)
            assertThat(toResponseCall.paymentDataUpdate).isEqualTo(paymentDataUpdate)
        }
    }

    private suspend fun Scenario.assertBuildCalls(
        callbackTrigger: GooglePayPaymentDataUpdate.CallbackTrigger,
        paymentDataUpdate: GooglePayPaymentDataUpdate,
    ) {
        val expectedBuildSources = when (callbackTrigger) {
            GooglePayPaymentDataUpdate.CallbackTrigger.Initialize -> listOf(
                CheckoutSessionResponse.TaxAddressSource.SHIPPING,
                CheckoutSessionResponse.TaxAddressSource.BILLING,
            )
            GooglePayPaymentDataUpdate.CallbackTrigger.PaymentMethod ->
                listOf(CheckoutSessionResponse.TaxAddressSource.BILLING)
            else -> listOf(CheckoutSessionResponse.TaxAddressSource.SHIPPING)
        }
        expectedBuildSources.forEach { expectedSource ->
            val buildCall = addressBuilder.buildCalls.awaitItem()
            assertThat(buildCall.paymentDataUpdate.callbackTrigger).isEqualTo(callbackTrigger)
            assertThat(buildCall.paymentDataUpdate.shippingAddress).isEqualTo(paymentDataUpdate.shippingAddress)
            assertThat(buildCall.paymentDataUpdate.billingAddress).isEqualTo(paymentDataUpdate.billingAddress)
            assertThat(buildCall.addressSource).isEqualTo(expectedSource)
        }
    }

    private fun checkoutSessionRepository(
        context: Context,
    ) = CheckoutSessionRepository(
        stripeRepository = CheckoutStripeRepositoryFactory.create(),
        elementsSessionClientParams = CheckoutStripeRepositoryFactory.clientParams(),
        fraudDetectionDataRepository = FakeFraudDetectionDataRepository.create(),
        stripeNetworkClient = DefaultStripeNetworkClient(),
        analyticsRequestExecutor = AnalyticsRequestExecutor {},
        paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
            context = context,
            publishableKey = DEFAULT_API_CONFIG.publishableKey,
        ),
        apiRequestOptionsProvider = {
            ApiRequest.Options(
                apiKey = DEFAULT_API_CONFIG.publishableKey,
                stripeAccount = DEFAULT_API_CONFIG.stripeAccountId,
            )
        },
    )

    private fun runScenario(
        state: CheckoutElementsControllerState? = taxState(CheckoutSessionResponse.TaxAddressSource.SHIPPING),
        addressBuilderResult: CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result =
            CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result.Available(ADDRESS),
        addressBuilderResultProvider: (
            (CheckoutSessionResponse.TaxAddressSource) ->
                CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result
        )? = null,
        mapperResponse: GooglePayPaymentDataUpdateResponse =
            GooglePayPaymentDataUpdateResponse(newTransactionInfo = null, error = null),
        block: suspend Scenario.() -> Unit
    ) = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val addressBuilder = FakeCheckoutGooglePayPaymentDataUpdateAddressBuilder(
            addressBuilderResultProvider ?: { addressBuilderResult }
        )
        val mapper = FakeCheckoutGooglePayPaymentDataUpdateMapper(mapperResponse)
        val taxRegionUpdater = CheckoutSessionTaxRegionUpdater(checkoutSessionRepository(context))
        val errorReporter = FakeErrorReporter()
        val callback = CheckoutGooglePayPaymentDataUpdateCallback(
            context = context,
            taxRegionUpdater = taxRegionUpdater,
            stateHolder = CheckoutElementsControllerStateFactory.createStateHolder(
                savedStateHandle = SavedStateHandle(),
            ).apply {
                this.state = state
            },
            addressBuilder = addressBuilder,
            mapper = mapper,
            errorReporter = errorReporter,
        )

        block(
            Scenario(
                callback = callback,
                addressBuilder = addressBuilder,
                mapper = mapper,
                errorReporter = errorReporter,
                providedMapperResponse = mapperResponse,
                providedState = state,
            )
        )

        addressBuilder.ensureAllEventsConsumed()
        mapper.ensureAllEventsConsumed()
        errorReporter.ensureAllEventsConsumed()
    }

    private class Scenario(
        private val callback: CheckoutGooglePayPaymentDataUpdateCallback,
        val addressBuilder: FakeCheckoutGooglePayPaymentDataUpdateAddressBuilder,
        val mapper: FakeCheckoutGooglePayPaymentDataUpdateMapper,
        val errorReporter: FakeErrorReporter,
        val providedMapperResponse: GooglePayPaymentDataUpdateResponse,
        val providedState: CheckoutElementsControllerState?,
    ) {
        // Match Google Pay's work scope: state reads must be safe on a real background thread.
        suspend fun onPaymentDataChanged(
            update: GooglePayPaymentDataUpdate,
        ): GooglePayPaymentDataUpdateResponse = withContext(Dispatchers.IO) {
            assertThat(Thread.currentThread()).isNotSameInstanceAs(Looper.getMainLooper().thread)
            callback.onPaymentDataChanged(update)
        }
    }

    private fun taxState(addressSource: CheckoutSessionResponse.TaxAddressSource) =
        CheckoutElementsControllerStateFactory.create(
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                automaticTaxEnabled = true,
                taxAddressSource = addressSource,
            ),
        )

    private fun Scenario.enqueueTaxUpdate(address: CheckoutElementsController.Address.State = ADDRESS) {
        val sessionResponse = requireNotNull(providedState).checkoutSessionResponse
        networkRule.checkoutUpdate(
            bodyPart("tax_region[country]", address.country),
            bodyPart("tax_region[city]", requireNotNull(address.city)),
            bodyPart("tax_region[state]", requireNotNull(address.state)),
            bodyPart("tax_region[postal_code]", requireNotNull(address.postalCode)),
            bodyPart("client_secret", sessionResponse.clientSecret.value),
        ) { response ->
            response.testBodyFromFile("checkout-session-init.json") { json ->
                json.put("customer_email", UPDATED_EMAIL)
                json.getJSONObject("automatic_tax")
                    .put("enabled", true)
                    .put("status", "ready")
                    .put(
                        "address_source",
                        if (sessionResponse.taxAddressSource ==
                            CheckoutSessionResponse.TaxAddressSource.SHIPPING
                        ) "shipping" else "billing",
                    )
            }
        }
    }

    private fun enqueueTaxUpdateFailure(address: CheckoutElementsController.Address.State = ADDRESS) {
        networkRule.checkoutUpdate(
            bodyPart("tax_region[country]", address.country),
            bodyPart("tax_region[city]", requireNotNull(address.city)),
            bodyPart("tax_region[state]", requireNotNull(address.state)),
            bodyPart("tax_region[postal_code]", requireNotNull(address.postalCode)),
        ) { response ->
            response.setResponseCode(400)
            response.setBody("""{"error":{"message":"Unable to compute tax"}}""")
        }
    }

    private companion object {
        val SHIPPING_ADDRESS = GooglePayPaymentDataUpdate.GooglePayAddress(
            administrativeArea = "CA",
            countryCode = "US",
            locality = "South San Francisco",
            postalCode = "94080",
            iso3166AdministrativeArea = null,
        )

        val SHIPPING_ADDRESS_UPDATE = GooglePayPaymentDataUpdate(
            callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.ShippingAddress,
            shippingAddress = SHIPPING_ADDRESS,
            billingAddress = null,
        )

        val BILLING_GOOGLE_PAY_ADDRESS = GooglePayPaymentDataUpdate.GooglePayAddress(
            administrativeArea = "NY",
            countryCode = "US",
            locality = "New York",
            postalCode = "10011",
            iso3166AdministrativeArea = null,
        )

        val BILLING_ADDRESS = CheckoutElementsController.Address(country = "US")
            .city("New York")
            .postalCode("10011")
            .state("NY")
            .build()

        val ADDRESS = CheckoutElementsController.Address(country = "US")
            .city("South San Francisco")
            .postalCode("94080")
            .state("CA")
            .build()

        const val UPDATED_EMAIL = "updated@example.com"
        const val UPDATED_AMOUNT = 5099L

        val TRANSACTION_INFO = GooglePayJsonFactory.TransactionInfo(
            currencyCode = "USD",
            totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Estimated,
            countryCode = "US",
            transactionId = "transaction_id",
            totalPrice = 1000L,
            totalPriceLabel = "Total",
            checkoutOption = GooglePayJsonFactory.TransactionInfo.CheckoutOption.Default,
        )
    }
}
