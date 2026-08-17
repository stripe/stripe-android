package com.stripe.android.checkout

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataError
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdate
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdateResponse
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(CheckoutSessionPreview::class)
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
        assertThat(response.error).isEqualTo(
            GooglePayPaymentDataError(
                reason = GooglePayPaymentDataError.Reason.OtherError,
                message = "Checkout sessions not configured",
                intent = GooglePayPaymentDataError.Intent.ShippingAddress,
            )
        )
        taxRegionUpdater.updateServerStateIfNeededCalls.ensureAllEventsConsumed()
        addressBuilder.buildCalls.ensureAllEventsConsumed()
        mapper.toResponseCalls.ensureAllEventsConsumed()
    }

    @Test
    fun `onPaymentDataChanged returns addressBuilder failure for Initialize trigger`() = runScenario(
        addressBuilderResult = CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result.Failed(
            ADDRESS_BUILDER_FAILURE_RESPONSE,
        ),
    ) {
        val paymentDataUpdate = GooglePayPaymentDataUpdate(
            callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.Initialize,
            shippingAddress = null,
        )

        val response = callback.onPaymentDataChanged(paymentDataUpdate)

        assertThat(response).isEqualTo(ADDRESS_BUILDER_FAILURE_RESPONSE)
        assertThat(addressBuilder.buildCalls.awaitItem()).isEqualTo(paymentDataUpdate)
        taxRegionUpdater.updateServerStateIfNeededCalls.ensureAllEventsConsumed()
        mapper.toResponseCalls.ensureAllEventsConsumed()
    }

    @Test
    fun `onPaymentDataChanged returns addressBuilder failure for ShippingAddress trigger`() = runScenario(
        addressBuilderResult = CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result.Failed(
            ADDRESS_BUILDER_FAILURE_RESPONSE,
        ),
    ) {
        val paymentDataUpdate = GooglePayPaymentDataUpdate(
            callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.ShippingAddress,
            shippingAddress = null,
        )

        val response = callback.onPaymentDataChanged(paymentDataUpdate)

        assertThat(response).isEqualTo(ADDRESS_BUILDER_FAILURE_RESPONSE)
        assertThat(addressBuilder.buildCalls.awaitItem()).isEqualTo(paymentDataUpdate)
        taxRegionUpdater.updateServerStateIfNeededCalls.ensureAllEventsConsumed()
        mapper.toResponseCalls.ensureAllEventsConsumed()
    }

    @Test
    fun `onPaymentDataChanged updates tax region and returns mapper response on success`() = runScenario {
        val paymentDataUpdate = GooglePayPaymentDataUpdate(
            callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.ShippingAddress,
            shippingAddress = SHIPPING_ADDRESS,
        )

        val response = callback.onPaymentDataChanged(paymentDataUpdate)

        assertThat(response).isEqualTo(providedMapperResponse)
        assertThat(addressBuilder.buildCalls.awaitItem()).isEqualTo(paymentDataUpdate)
        assertThat(taxRegionUpdater.updateServerStateIfNeededCalls.awaitItem()).isEqualTo(
            FakeCheckoutSessionTaxRegionUpdater.UpdateServerStateIfNeededCall(
                checkoutSessionResponse = requireState.checkoutSessionResponse,
                addressSource = CheckoutSessionResponse.TaxAddressSource.SHIPPING,
                address = ADDRESS,
            )
        )
        assertThat(mapper.toResponseCalls.awaitItem()).isEqualTo(
            FakeCheckoutGooglePayPaymentDataUpdateMapper.ToResponseCall(
                configuration = requireState.commonConfiguration,
                response = requireUpdatedCheckoutSessionResponse,
                paymentDataUpdate = paymentDataUpdate,
            )
        )
    }

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
        assertThat(response.error).isEqualTo(
            GooglePayPaymentDataError(
                reason = GooglePayPaymentDataError.Reason.ShippingAddressInvalid,
                message = "Unable to compute tax",
                intent = GooglePayPaymentDataError.Intent.ShippingAddress,
            )
        )
        addressBuilder.buildCalls.awaitItem()
        taxRegionUpdater.updateServerStateIfNeededCalls.awaitItem()
        mapper.toResponseCalls.ensureAllEventsConsumed()
    }

    @Test
    fun `onPaymentDataChanged for shipping option maps state without building an address`() = runScenario {
        val paymentDataUpdate = GooglePayPaymentDataUpdate(
            callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.ShippingOption,
            shippingAddress = null,
        )

        val response = callback.onPaymentDataChanged(paymentDataUpdate)

        assertThat(response).isEqualTo(providedMapperResponse)
        assertThat(mapper.toResponseCalls.awaitItem()).isEqualTo(
            FakeCheckoutGooglePayPaymentDataUpdateMapper.ToResponseCall(
                configuration = requireState.commonConfiguration,
                response = requireState.checkoutSessionResponse,
                paymentDataUpdate = paymentDataUpdate,
            )
        )
        taxRegionUpdater.updateServerStateIfNeededCalls.ensureAllEventsConsumed()
        addressBuilder.buildCalls.ensureAllEventsConsumed()
    }

    @Test
    fun `onPaymentDataChanged for offer maps state without building an address`() = runScenario {
        val paymentDataUpdate = GooglePayPaymentDataUpdate(
            callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.Offer,
            shippingAddress = null,
        )

        val response = callback.onPaymentDataChanged(paymentDataUpdate)

        assertThat(response).isEqualTo(providedMapperResponse)
        assertThat(mapper.toResponseCalls.awaitItem()).isEqualTo(
            FakeCheckoutGooglePayPaymentDataUpdateMapper.ToResponseCall(
                configuration = requireState.commonConfiguration,
                response = requireState.checkoutSessionResponse,
                paymentDataUpdate = paymentDataUpdate,
            )
        )
        taxRegionUpdater.updateServerStateIfNeededCalls.ensureAllEventsConsumed()
        addressBuilder.buildCalls.ensureAllEventsConsumed()
    }

    private fun runScenario(
        state: CheckoutControllerState? = CheckoutControllerStateFactory.create(),
        addressBuilderResult: CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result =
            CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result.Success(ADDRESS),
        mapperResponse: GooglePayPaymentDataUpdateResponse =
            GooglePayPaymentDataUpdateResponse(newTransactionInfo = null, error = null),
        updateResult: Result<CheckoutSessionResponse> = Result.success(UPDATED_CHECKOUT_SESSION_RESPONSE),
        block: suspend Scenario.() -> Unit
    ) = runTest {
        val addressBuilder = FakeCheckoutGooglePayPaymentDataUpdateAddressBuilder(addressBuilderResult)
        val mapper = FakeCheckoutGooglePayPaymentDataUpdateMapper(mapperResponse)
        val taxRegionUpdater = FakeCheckoutSessionTaxRegionUpdater(updateResult)
        val callback = CheckoutGooglePayPaymentDataUpdateCallback(
            taxRegionUpdater = taxRegionUpdater.mocked,
            stateHolder = CheckoutControllerStateFactory.createStateHolder(
                savedStateHandle = SavedStateHandle(),
            ).apply {
                this.state = state
            },
            addressBuilder = addressBuilder,
            mapper = mapper,
        )

        block(
            Scenario(
                callback = callback,
                addressBuilder = addressBuilder,
                mapper = mapper,
                taxRegionUpdater = taxRegionUpdater,
                providedMapperResponse = mapperResponse,
                providedState = state,
                updatedCheckoutSessionResponse = updateResult.getOrNull(),
            )
        )

        addressBuilder.ensureAllEventsConsumed()
        mapper.ensureAllEventsConsumed()
        taxRegionUpdater.ensureAllEventsConsumed()
    }

    private class Scenario(
        val callback: CheckoutGooglePayPaymentDataUpdateCallback,
        val addressBuilder: FakeCheckoutGooglePayPaymentDataUpdateAddressBuilder,
        val mapper: FakeCheckoutGooglePayPaymentDataUpdateMapper,
        val taxRegionUpdater: FakeCheckoutSessionTaxRegionUpdater,
        val providedMapperResponse: GooglePayPaymentDataUpdateResponse,
        val providedState: CheckoutControllerState?,
        val updatedCheckoutSessionResponse: CheckoutSessionResponse?,
    ) {
        val requireState: CheckoutControllerState get() = requireNotNull(providedState)
        val requireUpdatedCheckoutSessionResponse: CheckoutSessionResponse
            get() = requireNotNull(updatedCheckoutSessionResponse)
    }

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

        val ADDRESS_BUILDER_FAILURE_RESPONSE = GooglePayPaymentDataUpdateResponse(
            newTransactionInfo = null,
            error = GooglePayPaymentDataError(
                reason = GooglePayPaymentDataError.Reason.ShippingAddressInvalid,
                message = "Shipping address is required.",
                intent = GooglePayPaymentDataError.Intent.ShippingAddress,
            ),
        )
    }
}
