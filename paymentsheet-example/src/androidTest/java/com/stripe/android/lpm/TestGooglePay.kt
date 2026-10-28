package com.stripe.android.lpm

import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.DefaultCardFundingFilter
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.ApiConfigurationPreview
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataError
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdate
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdateCallback
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdateResponse
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContractV2
import com.stripe.android.googlepaylauncher.InternalGooglePayPaymentMethodLauncher_Factory
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.paymentsheet.example.MainActivity
import com.stripe.android.paymentsheet.example.playground.network.PlaygroundRequester
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutMode
import com.stripe.android.paymentsheet.example.playground.settings.Merchant
import com.stripe.android.paymentsheet.example.playground.settings.MerchantSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.UseApiConfigurationSettingsDefinition
import com.stripe.android.test.core.DEFAULT_UI_TIMEOUT
import com.stripe.android.test.core.TestParameters
import com.stripe.android.test.core.ui.GooglePayPage
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ApiConfigurationPreview::class)
internal class TestGooglePay : BasePlaygroundTest() {
    @Test
    fun testUnitedStates() {
        testDriver.confirmWithGooglePay(Merchant.US)
    }

    @Test
    fun testFrance() {
        testDriver.confirmWithGooglePay(Merchant.FR)
    }

    @Test
    fun testGreatBritain() {
        testDriver.confirmWithGooglePay(Merchant.GB)
    }

    @Test
    fun testAustralia() {
        testDriver.confirmWithGooglePay(Merchant.AU)
    }

    @Test
    fun testBrazil() {
        testDriver.confirmWithGooglePay(Merchant.BR)
    }

    @Test
    fun testJapan() {
        testDriver.confirmWithGooglePay(Merchant.JP)
    }

    @Test
    fun testMexico() {
        testDriver.confirmWithGooglePay(Merchant.MX)
    }

    @Test
    fun testSetupIntentInEEA() {
        testDriver.confirmWithGooglePay(
            merchant = Merchant.FR,
            checkoutMode = CheckoutMode.SETUP
        )
    }

    @Test
    fun testSetupIntentInUS() {
        testDriver.confirmWithGooglePay(
            merchant = Merchant.US,
            checkoutMode = CheckoutMode.SETUP
        )
    }

    @Test
    fun testPaymentMethodCallbackReceivesBillingAddress() = runCallbackScenario(
        response = GooglePayPaymentDataUpdateResponse(newTransactionInfo = null, error = null),
    ) {
        val update = callback.awaitPaymentMethodUpdate()

        val billingAddress = requireNotNull(update.billingAddress)
        assertThat(billingAddress.countryCode).hasLength(2)
        assertThat(billingAddress.postalCode).isNotEmpty()
        assertThat(update.shippingAddress).isNull()
    }

    @Test
    fun testPaymentDataChangedUpdatesTotal() = runCallbackScenario(
        response = GooglePayPaymentDataUpdateResponse(
            newTransactionInfo = GooglePayJsonFactory.TransactionInfo(
                currencyCode = "USD",
                totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Final,
                countryCode = "US",
                totalPrice = 4242,
            ),
            error = null,
        ),
    ) {
        callback.awaitPaymentMethodUpdate()

        assertThat(
            device.wait(
                Until.hasObject(By.pkg("com.google.android.gms").textContains("42.42")),
                DEFAULT_UI_TIMEOUT.inWholeMilliseconds,
            )
        ).isTrue()
    }

    @Test
    fun testPaymentDataChangedDisplaysError() = runCallbackScenario(
        response = GooglePayPaymentDataUpdateResponse(
            newTransactionInfo = null,
            error = GooglePayPaymentDataError(
                reason = GooglePayPaymentDataError.Reason.OtherError,
                message = "Please choose another payment method",
                intent = GooglePayPaymentDataError.Intent.PaymentMethod,
            ),
        ),
    ) {
        callback.awaitPaymentMethodUpdate()

        assertThat(
            device.wait(
                Until.hasObject(By.pkg("com.google.android.gms").text("Please choose another payment method")),
                DEFAULT_UI_TIMEOUT.inWholeMilliseconds,
            )
        ).isTrue()
    }

    private fun runCallbackScenario(
        response: GooglePayPaymentDataUpdateResponse,
        block: suspend Scenario.() -> Unit,
    ) = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val previousConfiguration = runCatching { PaymentConfiguration.getInstance(context) }.getOrNull()
        val callback = FakeGooglePayPaymentDataUpdateCallback(response)
        val results = Turbine<GooglePayPaymentMethodLauncher.Result>(timeout = DEFAULT_UI_TIMEOUT)

        try {
            val parameters = TestParameters.create(paymentMethodCode = "card") { settings ->
                settings[MerchantSettingsDefinition] = Merchant.US
                settings[UseApiConfigurationSettingsDefinition] = false
            }
            PlaygroundRequester(parameters.playgroundSettingsSnapshot, context).fetch().getOrThrow()
            val configuration = PaymentConfiguration.getInstance(context)
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    launchGooglePay(
                        activity = activity,
                        callback = callback,
                        results = results,
                        apiConfiguration = ApiConfiguration.State(
                            publishableKey = configuration.publishableKey,
                            stripeAccountId = configuration.stripeAccountId,
                        ),
                    )
                }

                // Force a selection change even when Visa 4242 is already the default card.
                val googlePayPage = GooglePayPage(device)
                googlePayPage.selectTestCard("4444")
                googlePayPage.selectTestCard("4242")
                Scenario(callback).block()

                device.pressBack()
                assertThat(results.awaitItem()).isEqualTo(GooglePayPaymentMethodLauncher.Result.Canceled)
                // Google Pay may send additional initialization/selection updates depending on
                // the wallet's starting selection. Only the payment-method update is relevant here.
                callback.updates.cancelAndIgnoreRemainingEvents()
                callback.updates.ensureAllEventsConsumed()
                results.ensureAllEventsConsumed()
            }
        } finally {
            previousConfiguration?.let {
                PaymentConfiguration.init(context, it.publishableKey, it.stripeAccountId)
            }
        }
    }

    private fun launchGooglePay(
        activity: ComponentActivity,
        callback: GooglePayPaymentDataUpdateCallback,
        results: Turbine<GooglePayPaymentMethodLauncher.Result>,
        apiConfiguration: ApiConfiguration.State,
    ) {
        val activityResultLauncher = activity.activityResultRegistry.register(
            "google_pay_callback_test",
            GooglePayPaymentMethodLauncherContractV2(),
            results::add,
        )
        // Use the generated factory to exercise the internal launcher without exposing
        // a callback hook in the public PaymentSheet API just for these tests.
        val launcher = InternalGooglePayPaymentMethodLauncher_Factory.newInstance(
            "google_pay_callback_test",
            activity,
            activityResultLauncher,
            callback,
            PaymentAnalyticsRequestFactory(activity, apiConfiguration.publishableKey),
            DefaultAnalyticsRequestExecutor(),
        )
        launcher.present(
            currencyCode = "USD",
            amount = 1000,
            config = GooglePayPaymentMethodLauncher.Config(
                environment = GooglePayEnvironment.Test,
                merchantCountryCode = "US",
                merchantName = "Callback test",
                billingAddressConfig = GooglePayPaymentMethodLauncher.BillingAddressConfig(
                    isRequired = true,
                    format = GooglePayPaymentMethodLauncher.BillingAddressConfig.Format.Full,
                ),
                existingPaymentMethodRequired = false,
            ),
            cardBrandFilter = DefaultCardBrandFilter,
            cardFundingFilter = DefaultCardFundingFilter,
            clientAttributionMetadata = null,
            transactionId = null,
            label = null,
            isElements = false,
            apiConfiguration = apiConfiguration,
            displayItems = emptyList(),
            billingEmailOverride = null,
            shippingAddressParameters = null,
        )
    }

    private data class Scenario(val callback: FakeGooglePayPaymentDataUpdateCallback)
}

internal class FakeGooglePayPaymentDataUpdateCallback(
    private val response: GooglePayPaymentDataUpdateResponse,
) : GooglePayPaymentDataUpdateCallback {
    val updates = Turbine<GooglePayPaymentDataUpdate>(timeout = DEFAULT_UI_TIMEOUT)

    override suspend fun onPaymentDataChanged(
        paymentDataUpdate: GooglePayPaymentDataUpdate,
    ): GooglePayPaymentDataUpdateResponse {
        updates.add(paymentDataUpdate)
        return if (paymentDataUpdate.callbackTrigger == GooglePayPaymentDataUpdate.CallbackTrigger.PaymentMethod) {
            response
        } else {
            GooglePayPaymentDataUpdateResponse(newTransactionInfo = null, error = null)
        }
    }

    suspend fun awaitPaymentMethodUpdate(): GooglePayPaymentDataUpdate {
        var update = updates.awaitItem()
        while (update.callbackTrigger != GooglePayPaymentDataUpdate.CallbackTrigger.PaymentMethod) {
            assertThat(update.callbackTrigger).isEqualTo(GooglePayPaymentDataUpdate.CallbackTrigger.Initialize)
            update = updates.awaitItem()
        }
        return update
    }
}
