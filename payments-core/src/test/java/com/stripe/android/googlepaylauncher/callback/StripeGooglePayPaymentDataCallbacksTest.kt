package com.stripe.android.googlepaylauncher.callback

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.wallet.callback.IntermediatePaymentData
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdate
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdateCallbackRegistry
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdateResponse
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StripeGooglePayPaymentDataCallbacksTest {
    @After
    fun tearDown() {
        GooglePayPaymentDataUpdateCallbackRegistry.deselect()
        GooglePayPaymentDataUpdateCallbackRegistry.deregister(CALLBACK_KEY)
        PaymentConfiguration.clearInstance()
    }

    @Test
    fun `callback uses selected configuration without global initialization`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        PaymentConfiguration.clearInstance()
        context.getSharedPreferences(
            PaymentConfiguration::class.java.canonicalName,
            Context.MODE_PRIVATE,
        ).edit().clear().commit()
        assertThat(runCatching { PaymentConfiguration.getInstance(context) }.isFailure).isTrue()

        val callback = FakeGooglePayPaymentDataUpdateCallback {
            GooglePayPaymentDataUpdateResponse(
                newTransactionInfo = GooglePayJsonFactory.TransactionInfo(
                    currencyCode = "USD",
                    totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Final,
                    totalPrice = 1099,
                ),
                error = null,
            )
        }
        val listener = FakeOnCompleteListener()
        GooglePayPaymentDataUpdateCallbackRegistry.register(CALLBACK_KEY, callback)
        GooglePayPaymentDataUpdateCallbackRegistry.select(
            key = CALLBACK_KEY,
            workScope = this,
            apiConfiguration = ApiConfiguration.State(
                publishableKey = ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY,
                stripeAccountId = ApiKeyFixtures.FAKE_STRIPE_ACCOUNT,
            ),
        )
        val request = mock<IntermediatePaymentData>()
        whenever(request.toJson()).thenReturn("""{"callbackTrigger":"INITIALIZE"}""")

        StripeGooglePayPaymentDataCallbacks(context).onPaymentDataChanged(request, listener)
        advanceUntilIdle()

        assertThat(callback.updates.awaitItem().callbackTrigger)
            .isEqualTo(GooglePayPaymentDataUpdate.CallbackTrigger.Initialize)
        val transactionInfo = JSONObject(listener.completions.awaitItem().toJson())
            .getJSONObject("newTransactionInfo")
        assertThat(transactionInfo.getString("totalPrice")).isEqualTo("10.99")
        assertThat(runCatching { PaymentConfiguration.getInstance(context) }.isFailure).isTrue()
        callback.ensureAllEventsConsumed()
        listener.ensureAllEventsConsumed()
    }

    private companion object {
        const val CALLBACK_KEY = "callback-key"
    }
}
