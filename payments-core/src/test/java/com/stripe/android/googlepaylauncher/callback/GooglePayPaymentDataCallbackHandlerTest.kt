package com.stripe.android.googlepaylauncher.callback

import app.cash.turbine.Turbine
import com.google.android.gms.wallet.callback.IntermediatePaymentData
import com.google.android.gms.wallet.callback.OnCompleteListener
import com.google.android.gms.wallet.callback.PaymentDataRequestUpdate
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.GooglePayConfig
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdate
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdateCallback
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdateCallbackRegistry
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdateResponse
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.testing.FakeErrorReporter
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GooglePayPaymentDataCallbackHandlerTest {
    @After
    fun tearDown() {
        GooglePayPaymentDataUpdateCallbackRegistry.deselect()
        GooglePayPaymentDataUpdateCallbackRegistry.deregister(CALLBACK_KEY)
    }

    @Test
    fun `missing request reports error and completes with empty update`() = runScenario(
        callbackResult = { error("Should not be called!") }
    ) {
        onPaymentDataChanged(null)

        assertThat(errorReporter.awaitCall().errorEvent)
            .isEqualTo(ErrorReporter.UnexpectedErrorEvent.GOOGLE_PAY_DYNAMIC_CALLBACK_MISSING_REQUEST)
        assertThat(listener.completions.awaitItem().toJson()).isEqualTo("{}")
    }

    @Test
    fun `missing callback reports error and completes with empty update`() = runScenario(
        registerCallback = false,
        callbackResult = { error("Should not be called!") }
    ) {
        onPaymentDataChanged(request())

        assertThat(errorReporter.awaitCall().errorEvent)
            .isEqualTo(ErrorReporter.UnexpectedErrorEvent.GOOGLE_PAY_DYNAMIC_CALLBACK_MISSING_CALLBACK)
        assertThat(listener.completions.awaitItem().toJson()).isEqualTo("{}")
    }

    @Test
    fun `callback response completes payment data update`() = runScenario(
        callbackResult = {
            GooglePayPaymentDataUpdateResponse(
                newTransactionInfo = GooglePayJsonFactory.TransactionInfo(
                    currencyCode = "USD",
                    totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Final,
                    totalPrice = 1099,
                ),
                error = null,
            )
        }
    ) {
        onPaymentDataChanged(shippingAddressRequest())

        val update = callback.updates.awaitItem()

        assertThat(update.callbackTrigger)
            .isEqualTo(GooglePayPaymentDataUpdate.CallbackTrigger.ShippingAddress)
        assertThat(update.shippingAddress?.administrativeArea).isEqualTo("California")
        assertThat(update.shippingAddress?.countryCode).isEqualTo("US")
        assertThat(update.shippingAddress?.locality).isEqualTo("San Francisco")
        assertThat(update.shippingAddress?.postalCode).isEqualTo("94103")
        assertThat(update.shippingAddress?.iso3166AdministrativeArea).isEqualTo("US-CA")

        val transactionInfo = JSONObject(listener.completions.awaitItem().toJson())
            .getJSONObject("newTransactionInfo")

        assertThat(transactionInfo.getString("currencyCode")).isEqualTo("USD")
        assertThat(transactionInfo.getString("totalPriceStatus")).isEqualTo("FINAL")
        assertThat(transactionInfo.getString("totalPrice")).isEqualTo("10.99")
    }

    @Test
    fun `callback failure for missing trigger returns shipping address error`() = runFailureScenario(
        callbackTrigger = null,
        expectedParsedCallbackTrigger = null,
        expectedIntent = "SHIPPING_ADDRESS",
    )

    @Test
    fun `callback failure for initialize trigger returns shipping address error`() = runFailureScenario(
        callbackTrigger = "INITIALIZE",
        expectedParsedCallbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.Initialize,
        expectedIntent = "SHIPPING_ADDRESS",
    )

    @Test
    fun `callback failure for shipping address trigger returns shipping address error`() = runFailureScenario(
        callbackTrigger = "SHIPPING_ADDRESS",
        expectedParsedCallbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.ShippingAddress,
        expectedIntent = "SHIPPING_ADDRESS",
    )

    @Test
    fun `callback failure for shipping option trigger returns shipping option error`() = runFailureScenario(
        callbackTrigger = "SHIPPING_OPTION",
        expectedParsedCallbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.ShippingOption,
        expectedIntent = "SHIPPING_OPTION",
    )

    @Test
    fun `callback failure for offer trigger returns offer error`() = runFailureScenario(
        callbackTrigger = "OFFER",
        expectedParsedCallbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.Offer,
        expectedIntent = "OFFER",
    )

    @Test
    fun `callback failure with no message returns empty error message`() = runScenario(
        callbackResult = { error("") }
    ) {
        onPaymentDataChanged(request())

        assertThat(callback.updates.awaitItem()).isNotNull()

        val error = JSONObject(listener.completions.awaitItem().toJson()).getJSONObject("error")
        assertThat(error.getString("message")).isEmpty()
    }

    private fun runFailureScenario(
        callbackTrigger: String?,
        expectedParsedCallbackTrigger: GooglePayPaymentDataUpdate.CallbackTrigger?,
        expectedIntent: String,
    ) = runScenario(
        callbackResult = { throw IllegalStateException("Callback failed") }
    ) {
        onPaymentDataChanged(request(callbackTrigger))

        val update = callback.updates.awaitItem()

        assertThat(update.callbackTrigger)
            .isEqualTo(expectedParsedCallbackTrigger)

        val error = JSONObject(listener.completions.awaitItem().toJson()).getJSONObject("error")
        assertThat(error.getString("reason")).isEqualTo("OTHER_ERROR")
        assertThat(error.getString("message")).isEqualTo("Callback failed")
        assertThat(error.getString("intent")).isEqualTo(expectedIntent)
    }

    private fun runScenario(
        registerCallback: Boolean = true,
        callbackResult: (GooglePayPaymentDataUpdate) -> GooglePayPaymentDataUpdateResponse,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val callback = FakeGooglePayPaymentDataUpdateCallback(callbackResult)
        val listener = FakeOnCompleteListener()
        val errorReporter = FakeErrorReporter()

        if (registerCallback) {
            GooglePayPaymentDataUpdateCallbackRegistry.register(CALLBACK_KEY, callback)
            GooglePayPaymentDataUpdateCallbackRegistry.select(CALLBACK_KEY, this)
        }

        Scenario(
            testScope = this,
            callback = callback,
            listener = listener,
            errorReporter = errorReporter,
        ).apply { block() }

        callback.ensureAllEventsConsumed()
        listener.ensureAllEventsConsumed()
        errorReporter.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val testScope: TestScope,
        val callback: FakeGooglePayPaymentDataUpdateCallback,
        val listener: FakeOnCompleteListener,
        val errorReporter: FakeErrorReporter,
    ) {
        fun onPaymentDataChanged(request: IntermediatePaymentData?) {
            GooglePayPaymentDataCallbackHandler.onPaymentDataChanged(
                request = request,
                onCompleteListener = listener,
                googlePayJsonFactory = JSON_FACTORY,
                errorReporter = errorReporter,
            )
            testScope.advanceUntilIdle()
        }
    }

    private class FakeGooglePayPaymentDataUpdateCallback(
        private val result: (GooglePayPaymentDataUpdate) -> GooglePayPaymentDataUpdateResponse,
    ) : GooglePayPaymentDataUpdateCallback {
        val updates = Turbine<GooglePayPaymentDataUpdate>()

        override suspend fun onPaymentDataChanged(
            update: GooglePayPaymentDataUpdate,
        ): GooglePayPaymentDataUpdateResponse {
            updates.add(update)
            return result(update)
        }

        fun ensureAllEventsConsumed() {
            updates.ensureAllEventsConsumed()
        }
    }

    private class FakeOnCompleteListener : OnCompleteListener<PaymentDataRequestUpdate> {
        val completions = Turbine<PaymentDataRequestUpdate>()

        override fun complete(result: PaymentDataRequestUpdate) {
            completions.add(result)
        }

        fun ensureAllEventsConsumed() {
            completions.ensureAllEventsConsumed()
        }
    }

    private companion object {
        const val CALLBACK_KEY = "callback-key"
        val JSON_FACTORY = GooglePayJsonFactory(
            GooglePayConfig(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
        )

        fun request(callbackTrigger: String? = null): IntermediatePaymentData {
            return mock<IntermediatePaymentData>().also {
                val json = callbackTrigger?.let { trigger ->
                    """{"callbackTrigger":"$trigger"}"""
                } ?: "{}"
                whenever(it.toJson()).thenReturn(json)
            }
        }

        fun shippingAddressRequest(): IntermediatePaymentData {
            return mock<IntermediatePaymentData>().also {
                whenever(it.toJson()).thenReturn(
                    """
                    {
                      "callbackTrigger": "SHIPPING_ADDRESS",
                      "shippingAddress": {
                        "administrativeArea": "California",
                        "countryCode": "US",
                        "locality": "San Francisco",
                        "postalCode": "94103",
                        "iso3166AdministrativeArea": "US-CA"
                      }
                    }
                    """.trimIndent()
                )
            }
        }
    }
}
