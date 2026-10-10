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
import com.stripe.android.networktesting.RequestMatchers.not
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.paymentelementtestpages.AddressElementPage
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
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
    fun `failed save reports sanitized failure and re-enables saving`() = runTaxScenario(fails = true) { taxUpdate ->
        val failedRequest = expectShippingAnalytics(
            "elements.shipping_address.save_failed",
            analyticsPayloadField("analytics_value", "apiError"),
            analyticsPayloadField("status_code", "400"),
            not(hasQueryParam("error_message")),
        )
        taxUpdate.releaseResponse.countDown()

        addressPage.assertErrorDisplayed(applicationContext.getString(R.string.stripe_something_went_wrong))
        awaitAnalytics(failedRequest)
        addressPage.assertReadyToSave()
    }

    @Test
    fun `recreation during saving reports shown and save completion`() = runTaxScenario { taxUpdate ->
        val shownRequest = expectShippingAnalytics(
            eventName = "elements.shipping_address.shown",
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
    }

    private fun runTaxScenario(
        fails: Boolean = false,
        block: AddressElementActivityTestRunner.Scenario.(AddressElementActivityTestRunner.TaxUpdate) -> Unit,
    ) = runScenario {
        val taxUpdate = enqueueTaxUpdate(fails = fails)
        try {
            startTaxUpdateWithAnalytics(taxUpdate)
            assertDismissalBlocked()
            block(taxUpdate)
        } finally {
            taxUpdate.releaseResponse.countDown()
        }
    }

    private fun runScenario(block: AddressElementActivityTestRunner.Scenario.() -> Unit) {
        val checkoutSessionResponse = CheckoutSessionResponseFactory.create(
            automaticTaxEnabled = true,
            taxAddressSource = CheckoutSessionResponse.TaxAddressSource.SHIPPING,
        )
        val shownRequest = activityTestRunner.expectShippingAnalytics(
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
