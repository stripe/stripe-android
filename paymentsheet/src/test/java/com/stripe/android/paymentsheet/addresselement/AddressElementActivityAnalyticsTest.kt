package com.stripe.android.paymentsheet.addresselement

import android.app.Application
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.DEFAULT_API_CONFIG
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.analyticsPayloadField
import com.stripe.android.networktesting.RequestMatchers.hasQueryParam
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.not
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.testing.waitUntilWithIdle
import com.stripe.paymentelementtestpages.AddressElementPage
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
internal class AddressElementActivityAnalyticsTest {
    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()
    private val composeTestRule = createEmptyComposeRule()
    private val networkRule = NetworkRule(
        hostsToTrack = listOf(ApiRequest.API_HOST, AnalyticsRequest.HOST),
        validationTimeout = 5.seconds,
    )
    private val addressPage = AddressElementPage(
        composeTestRule = composeTestRule,
        context = applicationContext,
    )
    private val activityTestRunner = AddressElementActivityTestRunner(
        composeTestRule = composeTestRule,
        networkRule = networkRule,
        addressPage = addressPage,
    )

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(composeTestRule)
        .around(createComposeCleanupRule())
        .around(networkRule)

    @Test
    fun `failed save reports failure and close reports cancellation`() = runScenario {
        val taxUpdate = enqueueTaxUpdate(fails = true)

        try {
            startTaxUpdateWithAnalytics(taxUpdate)
            assertDismissalBlocked()
            val failedRequest = expectShippingAnalytics("elements.shipping_address.save_failed")
            taxUpdate.releaseResponse.countDown()

            addressPage.assertErrorDisplayed(applicationContext.getString(R.string.stripe_something_went_wrong))
            awaitAnalytics(failedRequest)
            addressPage.assertReadyToSave()

            val canceledRequest = expectShippingAnalytics("elements.shipping_address.canceled")
            addressPage.clickClose()

            assertThat(awaitResult()).isEqualTo(AddressElementActivityContract.Result.Canceled)
            awaitAnalytics(canceledRequest)
        } finally {
            taxUpdate.releaseResponse.countDown()
        }
    }

    @Test
    fun `root back reports cancellation`() = runScenario {
        addressPage.assertReadyToSave()

        val canceledRequest = expectShippingAnalytics("elements.shipping_address.canceled")
        activityScenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }

        assertThat(awaitResult()).isEqualTo(AddressElementActivityContract.Result.Canceled)
        awaitAnalytics(canceledRequest)
    }

    @Test
    fun `close reports cancellation`() = runScenario {
        addressPage.assertReadyToSave()

        val canceledRequest = expectShippingAnalytics("elements.shipping_address.canceled")
        addressPage.clickClose()

        assertThat(awaitResult()).isEqualTo(AddressElementActivityContract.Result.Canceled)
        awaitAnalytics(canceledRequest)
    }

    @Test
    fun `scrim accessibility action reports cancellation`() = runScenario {
        addressPage.assertReadyToSave()

        val canceledRequest = expectShippingAnalytics("elements.shipping_address.canceled")
        addressPage.dismissViaScrimAccessibilityAction()

        assertThat(awaitResult()).isEqualTo(AddressElementActivityContract.Result.Canceled)
        awaitAnalytics(canceledRequest)
    }

    @Test
    fun `back reports the current country once even when pressed twice`() = runScenario {
        addressPage.assertReadyToSave()
        addressPage.selectCountry("Canada")

        val canceledRequest = expectShippingAnalytics("elements.shipping_address.canceled", country = "CA")
        activityScenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
            activity.onBackPressedDispatcher.onBackPressed()
        }

        assertThat(awaitResult()).isEqualTo(AddressElementActivityContract.Result.Canceled)
        awaitAnalytics(canceledRequest)
    }

    @Test
    fun `recreation during saving reports shown and save completion`() = runScenario {
        val taxUpdate = enqueueTaxUpdate()

        try {
            startTaxUpdateWithAnalytics(taxUpdate)
            assertDismissalBlocked()

            val shownRequest = expectShippingAnalytics(
                eventName = "elements.shipping_address.shown",
                checkoutSessionId = checkoutSessionResponse.id,
                country = "US",
                autocompleteResultSelected = null,
            )
            activityScenario.recreate()
            activityScenario.onActivity { activity = it }
            composeTestRule.waitForIdle()
            awaitAnalytics(shownRequest)

            addressPage.assertVisible()
            assertDismissalBlocked()

            val completedRequest = expectShippingAnalytics("elements.shipping_address.save_completed")
            taxUpdate.releaseResponse.countDown()

            val result = awaitResult() as AddressElementActivityContract.Result.CheckoutShippingSucceeded
            assertThat(result.checkoutSessionResponse.id).isEqualTo(checkoutSessionResponse.id)
            awaitAnalytics(completedRequest)
        } finally {
            taxUpdate.releaseResponse.countDown()
        }
    }

    @Test
    fun `saving blocks cancellation analytics from close back and scrim dismissal`() = runScenario {
        val taxUpdate = enqueueTaxUpdate()

        try {
            startTaxUpdateWithAnalytics(taxUpdate)
            assertDismissalBlocked()

            addressPage.clickClose()
            assertDismissalBlocked()

            activityScenario.onActivity { activity ->
                activity.onBackPressedDispatcher.onBackPressed()
            }
            assertDismissalBlocked()

            addressPage.dismissViaScrimAccessibilityAction()
            assertDismissalBlocked()

            val completedRequest = expectShippingAnalytics("elements.shipping_address.save_completed")
            taxUpdate.releaseResponse.countDown()

            val result = awaitResult() as AddressElementActivityContract.Result.CheckoutShippingSucceeded
            assertThat(result.checkoutSessionResponse.id).isEqualTo(checkoutSessionResponse.id)
            awaitAnalytics(completedRequest)
        } finally {
            taxUpdate.releaseResponse.countDown()
        }
    }

    private fun AddressElementActivityTestRunner.Scenario.startTaxUpdateWithAnalytics(
        taxUpdate: AddressElementActivityTestRunner.TaxUpdate,
    ) {
        val startedRequest = expectShippingAnalytics("elements.shipping_address.save_started")
        startTaxUpdate(taxUpdate)
        awaitAnalytics(startedRequest)
    }

    private fun AddressElementActivityTestRunner.Scenario.assertDismissalBlocked() {
        assertSaving()
        addressPage.assertCloseDisabled()
    }

    private fun AddressElementActivityTestRunner.Scenario.expectShippingAnalytics(
        eventName: String,
        country: String = "US",
    ): CountDownLatch = expectShippingAnalytics(
        eventName = eventName,
        checkoutSessionId = checkoutSessionResponse.id,
        country = country,
        autocompleteResultSelected = false,
    )

    private fun expectShippingAnalytics(
        eventName: String,
        checkoutSessionId: String,
        country: String,
        autocompleteResultSelected: Boolean?,
    ): CountDownLatch {
        val requestReceived = CountDownLatch(1)
        val selectionMatcher = autocompleteResultSelected?.let {
            analyticsPayloadField("address_data_blob[auto_complete_result_selected]", it.toString())
        } ?: not(hasQueryParam("address_data_blob[auto_complete_result_selected]"))
        networkRule.enqueue(
            host("q.stripe.com"),
            method("GET"),
            analyticsPayloadField("event", eventName),
            analyticsPayloadField("product_usage", "PaymentSheet.AddressController"),
            analyticsPayloadField("checkout_session_id", checkoutSessionId),
            analyticsPayloadField("address_data_blob[address_country_code]", country),
            selectionMatcher,
            not(hasQueryParam("address_data_blob[edit_distance]")),
        ) { response ->
            response.status = "HTTP/1.1 200 OK"
            requestReceived.countDown()
        }
        return requestReceived
    }

    private fun awaitAnalytics(requestReceived: CountDownLatch) {
        composeTestRule.waitUntilWithIdle {
            requestReceived.count == 0L
        }
    }

    private fun runScenario(block: AddressElementActivityTestRunner.Scenario.() -> Unit) {
        val checkoutSessionResponse = CheckoutSessionResponseFactory.create(
            automaticTaxEnabled = true,
            taxAddressSource = CheckoutSessionResponse.TaxAddressSource.SHIPPING,
        )
        val shownRequest = expectShippingAnalytics(
            eventName = "elements.shipping_address.shown",
            checkoutSessionId = checkoutSessionResponse.id,
            country = "US",
            autocompleteResultSelected = null,
        )
        activityTestRunner.run(
            args = AddressElementActivityContract.Args.CheckoutShipping(
                apiConfiguration = DEFAULT_API_CONFIG,
                config = AddressLauncher.Configuration.Builder()
                    .address(SHIPPING_ADDRESS)
                    .build(),
                checkoutSessionResponse = checkoutSessionResponse,
            ),
        ) {
            awaitAnalytics(shownRequest)
            block()
        }
    }

    private companion object {
        val SHIPPING_ADDRESS = AddressDetails(
            name = "Jenny Rosen",
            address = PaymentSheet.Address(
                city = "San Francisco",
                country = "US",
                line1 = "510 Townsend St",
                postalCode = "94103",
                state = "CA",
            ),
        )
    }
}
