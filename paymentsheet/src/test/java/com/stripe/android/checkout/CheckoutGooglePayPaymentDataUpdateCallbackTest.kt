package com.stripe.android.checkout

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.common.model.CommonConfigurationFactory
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataError
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdate
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdateResponse
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.testing.FakeErrorReporter
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class CheckoutGooglePayPaymentDataUpdateCallbackTest {
    @Test
    fun `onPaymentDataChanged returns error when state is not loaded`() = runScenario(
        state = null,
    ) {
        val response = callback.onPaymentDataChanged(
            GooglePayPaymentDataUpdate(
                callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.Initialize,
                shippingAddress = null,
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
    fun `onPaymentDataChanged returns error and reports when google pay is not configured`() = runScenario(
        state = CheckoutControllerStateFactory.create(
            commonConfiguration = CommonConfigurationFactory.create(
                googlePay = null,
            ),
        ),
    ) {
        val response = callback.onPaymentDataChanged(
            GooglePayPaymentDataUpdate(
                callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.Initialize,
                shippingAddress = null,
            )
        )

        assertThat(response.newTransactionInfo).isNull()

        val error = response.error
        assertThat(error).isNotNull()
        assertThat(error?.reason).isEqualTo(GooglePayPaymentDataError.Reason.OtherError)
        assertThat(error?.message).isEqualTo("Something went wrong")
        assertThat(error?.intent).isEqualTo(GooglePayPaymentDataError.Intent.ShippingAddress)

        val call = errorReporter.awaitCall()
        assertThat(call.errorEvent).isEqualTo(
            ErrorReporter.UnexpectedErrorEvent.CHECKOUT_SESSION_GOOGLE_PAY_UNEXPECTED_CALLBACK_TRIGGER
        )
        assertThat(call.additionalNonPiiParams)
            .isEqualTo(mapOf("unexpected_trigger_type" to "without_config"))
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
    fun `onPaymentDataChanged updates tax region & responds successfully for initialization`() = successTest(
        callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.Initialize,
    )

    @Test
    fun `onPaymentDataChanged updates tax region & responds successfully for shipping address change`() = successTest(
        callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.ShippingAddress,
    )

    @Test
    fun `onPaymentDataChanged returns error when updating tax region fails`() = runScenario(
        updateResult = Result.failure(IllegalStateException("Unable to compute tax")),
    ) {
        val paymentDataUpdate = GooglePayPaymentDataUpdate(
            callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.ShippingAddress,
            shippingAddress = SHIPPING_ADDRESS,
        )

        val response = callback.onPaymentDataChanged(paymentDataUpdate)

        assertThat(response.newTransactionInfo).isNull()

        val error = response.error
        assertThat(error).isNotNull()
        assertThat(error?.reason).isEqualTo(GooglePayPaymentDataError.Reason.ShippingAddressInvalid)
        assertThat(error?.message).isEqualTo("Something went wrong")
        assertThat(error?.intent).isEqualTo(GooglePayPaymentDataError.Intent.ShippingAddress)

        assertThat(addressBuilder.buildCalls.awaitItem()).isNotNull()
        assertThat(taxRegionUpdater.updateServerStateIfNeededCalls.awaitItem()).isNotNull()
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

    private fun unsupportedCallbackTest(
        callbackTrigger: GooglePayPaymentDataUpdate.CallbackTrigger,
        expectedTriggerParam: String,
    ) = runScenario(
        mapperResponse = GooglePayPaymentDataUpdateResponse(newTransactionInfo = null, error = null),
    ) {
        val paymentDataUpdate = GooglePayPaymentDataUpdate(
            callbackTrigger = callbackTrigger,
            shippingAddress = null,
        )

        val response = callback.onPaymentDataChanged(paymentDataUpdate)

        assertThat(response.newTransactionInfo).isNull()
        assertThat(response.error).isNull()

        val toResponseCall = mapper.toResponseCalls.awaitItem()

        assertThat(toResponseCall.configuration).isEqualTo(providedState?.commonConfiguration?.googlePay)
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
            )

            val response = callback.onPaymentDataChanged(paymentDataUpdate)

            val buildCall = addressBuilder.buildCalls.awaitItem()

            assertThat(buildCall.shippingAddress).isNull()
            assertThat(buildCall.callbackTrigger).isEqualTo(callbackTrigger)

            assertThat(response).isEqualTo(providedMapperResponse)

            val toResponseCall = mapper.toResponseCalls.awaitItem()
            assertThat(toResponseCall.configuration).isEqualTo(providedState?.commonConfiguration?.googlePay)
            assertThat(toResponseCall.response).isEqualTo(providedState?.checkoutSessionResponse)
            assertThat(toResponseCall.paymentDataUpdate).isEqualTo(paymentDataUpdate)
        }
    }

    private fun successTest(
        callbackTrigger: GooglePayPaymentDataUpdate.CallbackTrigger
    ) {
        runScenario(
            addressBuilderResult = CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result.Available(ADDRESS),
            state = CheckoutControllerStateFactory.create(
                checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                    id = UUID.randomUUID().toString(),
                )
            ),
            mapperResponse = GooglePayPaymentDataUpdateResponse(
                newTransactionInfo = TRANSACTION_INFO,
                error = null,
            )
        ) {
            val paymentDataUpdate = GooglePayPaymentDataUpdate(
                callbackTrigger = callbackTrigger,
                shippingAddress = SHIPPING_ADDRESS,
            )

            val response = callback.onPaymentDataChanged(paymentDataUpdate)

            assertThat(response).isEqualTo(providedMapperResponse)

            val buildCall = addressBuilder.buildCalls.awaitItem()
            assertThat(buildCall.callbackTrigger).isEqualTo(callbackTrigger)
            assertThat(buildCall.shippingAddress).isEqualTo(SHIPPING_ADDRESS)

            val updateCall = taxRegionUpdater.updateServerStateIfNeededCalls.awaitItem()
            assertThat(updateCall.addressSource).isEqualTo(CheckoutSessionResponse.TaxAddressSource.SHIPPING)
            assertThat(updateCall.address).isEqualTo(ADDRESS)
            assertThat(updateCall.checkoutSessionResponse).isEqualTo(providedState?.checkoutSessionResponse)

            val toResponseCall = mapper.toResponseCalls.awaitItem()
            assertThat(toResponseCall.configuration).isEqualTo(providedState?.commonConfiguration?.googlePay)
            assertThat(toResponseCall.response).isEqualTo(providedUpdateResult.getOrNull())
            assertThat(toResponseCall.paymentDataUpdate.callbackTrigger).isEqualTo(callbackTrigger)
            assertThat(toResponseCall.paymentDataUpdate.shippingAddress).isEqualTo(SHIPPING_ADDRESS)
        }
    }

    private fun runScenario(
        state: CheckoutControllerState? = CheckoutControllerStateFactory.create(),
        addressBuilderResult: CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result =
            CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result.Available(ADDRESS),
        mapperResponse: GooglePayPaymentDataUpdateResponse =
            GooglePayPaymentDataUpdateResponse(newTransactionInfo = null, error = null),
        updateResult: Result<CheckoutSessionResponse> = Result.success(UPDATED_CHECKOUT_SESSION_RESPONSE),
        block: suspend Scenario.() -> Unit
    ) = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val addressBuilder = FakeCheckoutGooglePayPaymentDataUpdateAddressBuilder(addressBuilderResult)
        val mapper = FakeCheckoutGooglePayPaymentDataUpdateMapper(mapperResponse)
        val taxRegionUpdater = FakeCheckoutSessionTaxRegionUpdater(updateResult)
        val errorReporter = FakeErrorReporter()

        val callback = CheckoutGooglePayPaymentDataUpdateCallback(
            context = context,
            taxRegionUpdater = taxRegionUpdater.mocked,
            stateHolder = CheckoutControllerStateFactory.createStateHolder(
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
                taxRegionUpdater = taxRegionUpdater,
                errorReporter = errorReporter,
                providedMapperResponse = mapperResponse,
                providedState = state,
                providedUpdateResult = updateResult,
            )
        )

        addressBuilder.ensureAllEventsConsumed()
        mapper.ensureAllEventsConsumed()
        taxRegionUpdater.ensureAllEventsConsumed()
        errorReporter.ensureAllEventsConsumed()
    }

    private class Scenario(
        val callback: CheckoutGooglePayPaymentDataUpdateCallback,
        val addressBuilder: FakeCheckoutGooglePayPaymentDataUpdateAddressBuilder,
        val mapper: FakeCheckoutGooglePayPaymentDataUpdateMapper,
        val taxRegionUpdater: FakeCheckoutSessionTaxRegionUpdater,
        val errorReporter: FakeErrorReporter,
        val providedMapperResponse: GooglePayPaymentDataUpdateResponse,
        val providedState: CheckoutControllerState?,
        val providedUpdateResult: Result<CheckoutSessionResponse>,
    )

    private class FakeCheckoutSessionTaxRegionUpdater(
        updateResult: Result<CheckoutSessionResponse>
    ) {
        val mocked = mock<CheckoutSessionTaxRegionUpdater>()
        val updateServerStateIfNeededCalls = Turbine<UpdateServerStateIfNeededCall>()

        init {
            whenever {
                mocked.updateServerStateIfNeeded(
                    checkoutSessionResponse = any(),
                    addressSource = any(),
                    address = any()
                )
            } doAnswer { invocation ->
                updateServerStateIfNeededCalls.add(
                    UpdateServerStateIfNeededCall(
                        checkoutSessionResponse = invocation.component1(),
                        addressSource = invocation.component2(),
                        address = invocation.component3(),
                    )
                )

                updateResult
            }
        }

        fun ensureAllEventsConsumed() {
            updateServerStateIfNeededCalls.ensureAllEventsConsumed()
        }

        data class UpdateServerStateIfNeededCall(
            val checkoutSessionResponse: CheckoutSessionResponse,
            val addressSource: CheckoutSessionResponse.TaxAddressSource,
            val address: CheckoutController.Address.State,
        )
    }

    private companion object {
        val SHIPPING_ADDRESS = GooglePayPaymentDataUpdate.ShippingAddress(
            administrativeArea = "CA",
            countryCode = "US",
            locality = "South San Francisco",
            postalCode = "94080",
            iso3166AdministrativeArea = null,
        )

        val ADDRESS = CheckoutController.Address()
            .country("US")
            .city("South San Francisco")
            .postalCode("94080")
            .state("CA")
            .build()

        val UPDATED_CHECKOUT_SESSION_RESPONSE = CheckoutSessionResponseFactory.create(merchantCountry = "US")

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
