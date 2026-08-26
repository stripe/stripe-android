package com.stripe.android.googlepaylauncher

import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.DefaultCardFundingFilter
import com.stripe.android.FakeActivityResultLauncher
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InternalGooglePayPaymentMethodLauncherTest {
    @Test
    fun `init registers callback with registry when callback is provided`() {
        val callback = GooglePayPaymentDataUpdateCallback { _ ->
            GooglePayPaymentDataUpdateResponse(newTransactionInfo = null, error = null)
        }

        createLauncher(instanceId = "instanceId", onPaymentDataChangedCallback = callback)

        assertThat(GooglePayPaymentDataUpdateCallbackRegistry.get("instanceId")).isSameInstanceAs(callback)
    }

    @Test
    fun `init does not register anything when callback is null`() {
        createLauncher(instanceId = "instanceId", onPaymentDataChangedCallback = null)

        assertThat(GooglePayPaymentDataUpdateCallbackRegistry.get("instanceId")).isNull()
    }

    @Test
    fun `callback is deregistered when lifecycle owner is destroyed`() {
        val lifecycleOwner = TestLifecycleOwner(initialState = Lifecycle.State.CREATED)
        val callback = GooglePayPaymentDataUpdateCallback { _ ->
            GooglePayPaymentDataUpdateResponse(newTransactionInfo = null, error = null)
        }

        createLauncher(
            instanceId = "instanceId",
            lifecycleOwner = lifecycleOwner,
            onPaymentDataChangedCallback = callback,
        )

        assertThat(GooglePayPaymentDataUpdateCallbackRegistry.get("instanceId")).isSameInstanceAs(callback)

        lifecycleOwner.currentState = Lifecycle.State.DESTROYED

        assertThat(GooglePayPaymentDataUpdateCallbackRegistry.get("instanceId")).isNull()
    }

    @Test
    fun `present launches activity result launcher with expected args`() {
        val activityResultLauncher = FakeActivityResultLauncher(GooglePayPaymentMethodLauncherContractV2())
        val config = GooglePayPaymentMethodLauncher.Config(
            environment = GooglePayEnvironment.Test,
            merchantCountryCode = "US",
            merchantName = "Widget Store",
        )
        val launcher = createLauncher(activityResultLauncher = activityResultLauncher)

        launcher.present(
            currencyCode = "usd",
            amount = 1000L,
            config = config,
            cardBrandFilter = DefaultCardBrandFilter,
            cardFundingFilter = DefaultCardFundingFilter,
            clientAttributionMetadata = null,
            transactionId = "pi_12345",
            label = null,
            isElements = true,
            apiConfiguration = ApiConfiguration.State(
                publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                stripeAccountId = null,
            ),
            displayItems = emptyList(),
            billingEmailOverride = null,
            shippingAddressParameters = null,
        )

        assertThat(activityResultLauncher.launchArgs[0]).isEqualTo(
            GooglePayPaymentMethodLauncherContractV2.Args(
                config = config,
                currencyCode = "usd",
                amount = 1000L,
                label = null,
                transactionId = "pi_12345",
                cardBrandFilter = DefaultCardBrandFilter,
                cardFundingFilter = DefaultCardFundingFilter,
                clientAttributionMetadata = null,
                isElements = true,
                publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                stripeAccountId = null,
                displayItems = emptyList(),
                billingEmailOverride = null,
                shippingAddressParameters = null,
            )
        )
    }

    private fun createLauncher(
        instanceId: String = "instanceId",
        lifecycleOwner: TestLifecycleOwner = TestLifecycleOwner(),
        activityResultLauncher: ActivityResultLauncher<GooglePayPaymentMethodLauncherContractV2.Args> = mock(),
        onPaymentDataChangedCallback: GooglePayPaymentDataUpdateCallback? = null,
        analyticsRequestExecutor: AnalyticsRequestExecutor = AnalyticsRequestExecutor { },
    ): InternalGooglePayPaymentMethodLauncher {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        return InternalGooglePayPaymentMethodLauncher(
            instanceId = instanceId,
            lifecycleOwner = lifecycleOwner,
            activityResultLauncher = activityResultLauncher,
            onPaymentDataChangedCallback = onPaymentDataChangedCallback,
            paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
                context = context,
                publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            ),
            analyticsRequestExecutor = analyticsRequestExecutor,
        )
    }
}
