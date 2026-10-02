package com.stripe.android.paymentsheet.addresselement

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.common.ui.PRIMARY_BUTTON_LOADING_INDICATOR_TEST_TAG
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.DEFAULT_API_CONFIG
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.analyticsPayloadField
import com.stripe.android.networktesting.RequestMatchers.hasQueryParam
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.not
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.ui.SHEET_NAVIGATION_BUTTON_TAG
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.testing.waitUntilWithIdle
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds
import androidx.compose.ui.R as ComposeUiR

@RunWith(RobolectricTestRunner::class)
internal class AddressElementActivityTest {
    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()
    private val composeTestRule = createEmptyComposeRule()
    private val networkRule = NetworkRule(
        hostsToTrack = listOf(ApiRequest.API_HOST, AnalyticsRequest.HOST),
        validationTimeout = 5.seconds,
    )

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(composeTestRule)
        .around(createComposeCleanupRule())
        .around(networkRule)

    @Test
    fun `when launched without args should finish with canceled result`() {
        ActivityScenario.launchActivityForResult(
            AddressElementActivity::class.java,
            Bundle.EMPTY
        ).use { activityScenario ->
            assertThat(activityScenario.state).isEqualTo(Lifecycle.State.DESTROYED)
            val result = AddressElementActivityContract.Standalone.parseResult(
                activityScenario.result.resultCode,
                activityScenario.result.resultData,
            )
            assertThat(result).isEqualTo(AddressLauncherResult.Canceled())
        }
    }

    @Test
    fun `standalone contract creates intent with standalone args`() {
        val args = AddressElementActivityContract.Args.Standalone(
            apiConfiguration = DEFAULT_API_CONFIG,
            config = null,
        )

        val intent = AddressElementActivityContract.Standalone.createIntent(
            ApplicationProvider.getApplicationContext(),
            args,
        )

        assertThat(intent.component?.className).isEqualTo(AddressElementActivity::class.java.name)
        assertThat(AddressElementActivityContract.Args.fromIntent(intent)).isEqualTo(args)
    }

    @Test
    fun `checkout shipping contract creates intent with checkout shipping args`() {
        val args = AddressElementActivityContract.Args.CheckoutShipping(
            apiConfiguration = DEFAULT_API_CONFIG,
            config = null,
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
        )

        val intent = AddressElementActivityContract.CheckoutShipping.createIntent(
            ApplicationProvider.getApplicationContext(),
            args,
        )

        assertThat(intent.component?.className).isEqualTo(AddressElementActivity::class.java.name)
        assertThat(AddressElementActivityContract.Args.fromIntent(intent)).isEqualTo(args)
    }

    @Test
    fun `standalone contract maps standalone success to public success`() {
        val result = AddressElementActivityContract.Result.StandaloneSucceeded(AddressDetails())

        val parsed = AddressElementActivityContract.Standalone.parseResult(
            resultCode = result.resultCode,
            intent = Intent().putExtras(result.toBundle()),
        )

        assertThat(parsed).isEqualTo(AddressLauncherResult.Succeeded(result.address))
    }

    @Test
    fun `standalone contract maps missing result to canceled`() {
        val parsed = AddressElementActivityContract.Standalone.parseResult(
            resultCode = Activity.RESULT_CANCELED,
            intent = Intent(),
        )

        assertThat(parsed).isEqualTo(AddressLauncherResult.Canceled())
    }

    @Test
    fun `standalone contract maps canceled result to public canceled`() {
        val result = AddressElementActivityContract.Result.Canceled

        val parsed = AddressElementActivityContract.Standalone.parseResult(
            resultCode = result.resultCode,
            intent = Intent().putExtras(result.toBundle()),
        )

        assertThat(parsed).isEqualTo(AddressLauncherResult.Canceled())
    }

    @Test
    fun `standalone contract maps checkout shipping success to canceled`() {
        val result = AddressElementActivityContract.Result.CheckoutShippingSucceeded(
            address = AddressDetails(),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
        )

        val parsed = AddressElementActivityContract.Standalone.parseResult(
            resultCode = result.resultCode,
            intent = Intent().putExtras(result.toBundle()),
        )

        assertThat(parsed).isEqualTo(AddressLauncherResult.Canceled())
    }

    @Test
    fun `checkout shipping contract preserves checkout shipping success`() {
        val result = AddressElementActivityContract.Result.CheckoutShippingSucceeded(
            address = AddressDetails(),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(amount = 2000L),
        )

        val parsed = AddressElementActivityContract.CheckoutShipping.parseResult(
            resultCode = result.resultCode,
            intent = Intent().putExtras(result.toBundle()),
        )

        assertThat(parsed).isEqualTo(result)
    }

    @Test
    fun `checkout shipping contract maps missing result to canceled`() {
        val parsed = AddressElementActivityContract.CheckoutShipping.parseResult(
            resultCode = Activity.RESULT_CANCELED,
            intent = Intent(),
        )

        assertThat(parsed).isEqualTo(AddressElementActivityContract.Result.Canceled)
    }

    @Test
    fun `checkout shipping contract preserves canceled result`() {
        val result = AddressElementActivityContract.Result.Canceled

        val parsed = AddressElementActivityContract.CheckoutShipping.parseResult(
            resultCode = result.resultCode,
            intent = Intent().putExtras(result.toBundle()),
        )

        assertThat(parsed).isEqualTo(result)
    }

    @Test
    fun `checkout shipping contract maps standalone success to canceled`() {
        val result = AddressElementActivityContract.Result.StandaloneSucceeded(AddressDetails())

        val parsed = AddressElementActivityContract.CheckoutShipping.parseResult(
            resultCode = result.resultCode,
            intent = Intent().putExtras(result.toBundle()),
        )

        assertThat(parsed).isEqualTo(AddressElementActivityContract.Result.Canceled)
    }

    @Test
    fun `checkout shipping blocks dismissal while tax update is in flight and returns success`() = runScenario {
        val taxUpdate = enqueueTaxUpdate()

        try {
            startTaxUpdate(taxUpdate)
            assertSaving()

            closeButton.performClick()
            assertSaving()

            activityScenario.onActivity { activity ->
                activity.onBackPressedDispatcher.onBackPressed()
            }
            assertSaving()

            scrim.performClick()
            assertSaving()

            val completedRequest = expectShippingAnalytics("elements.shipping_address.save_completed")
            taxUpdate.releaseResponse.countDown()

            val result = awaitResult() as AddressElementActivityContract.Result.CheckoutShippingSucceeded
            awaitAnalytics(completedRequest)
            assertThat(result.address.name).isEqualTo(SHIPPING_ADDRESS.name)
            assertThat(result.address.address?.country).isEqualTo(SHIPPING_ADDRESS.address?.country)
            assertThat(result.address.address?.line1).isEqualTo(SHIPPING_ADDRESS.address?.line1)
            assertThat(result.address.address?.postalCode).isEqualTo(SHIPPING_ADDRESS.address?.postalCode)
            assertThat(result.checkoutSessionResponse.id).isEqualTo(checkoutSessionResponse.id)
            assertThat(result.checkoutSessionResponse.amount).isEqualTo(5099L)
        } finally {
            taxUpdate.releaseResponse.countDown()
        }
    }

    @Test
    fun `checkout shipping restores close after tax update fails`() = runScenario {
        val taxUpdate = enqueueTaxUpdate(fails = true)

        try {
            startTaxUpdate(taxUpdate)
            assertSaving()
            val failedRequest = expectShippingAnalytics("elements.shipping_address.save_failed")
            taxUpdate.releaseResponse.countDown()

            val expectedError = applicationContext.getString(R.string.stripe_something_went_wrong)
            composeTestRule.waitUntilWithIdle {
                composeTestRule.onAllNodesWithText(expectedError)
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty()
            }
            awaitAnalytics(failedRequest)
            composeTestRule.onNodeWithText(expectedError).performScrollTo().assertIsDisplayed()
            primaryButton.assertIsEnabled()
            val canceledRequest = expectShippingAnalytics("elements.shipping_address.canceled")
            closeButton.assertIsEnabled().performClick()

            assertThat(awaitResult()).isEqualTo(AddressElementActivityContract.Result.Canceled)
            awaitAnalytics(canceledRequest)
        } finally {
            taxUpdate.releaseResponse.countDown()
        }
    }

    @Test
    fun `checkout shipping back reports the current country once even when pressed twice`() = runScenario {
        primaryButton.assertIsEnabled()
        closeButton.assertIsEnabled()
        composeTestRule.onNodeWithText("Country or region").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Canada", substring = true).performScrollTo().performClick()

        val canceledRequest = expectShippingAnalytics("elements.shipping_address.canceled", country = "CA")
        activityScenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
            activity.onBackPressedDispatcher.onBackPressed()
        }

        assertThat(awaitResult()).isEqualTo(AddressElementActivityContract.Result.Canceled)
        awaitAnalytics(canceledRequest)
    }

    @Test
    fun `checkout shipping can be canceled with scrim before saving`() = runScenario {
        primaryButton.assertIsEnabled()

        val canceledRequest = expectShippingAnalytics("elements.shipping_address.canceled")
        scrim.performClick()

        assertThat(awaitResult()).isEqualTo(AddressElementActivityContract.Result.Canceled)
        awaitAnalytics(canceledRequest)
    }

    @Test
    fun `checkout shipping can be canceled with close before saving`() = runScenario {
        primaryButton.assertIsEnabled()

        val canceledRequest = expectShippingAnalytics("elements.shipping_address.canceled")
        closeButton.assertIsEnabled().performClick()

        assertThat(awaitResult()).isEqualTo(AddressElementActivityContract.Result.Canceled)
        awaitAnalytics(canceledRequest)
    }

    private fun enqueueTaxUpdate(fails: Boolean = false): TaxUpdate {
        val requestReceived = CountDownLatch(1)
        val releaseResponse = CountDownLatch(1)
        networkRule.checkoutUpdate { response ->
            if (fails) {
                response.setResponseCode(400)
                response.setBody("""{"error":{"message":"Invalid tax region"}}""")
            } else {
                response.testBodyFromFile("checkout-session-init.json")
            }
            requestReceived.countDown()
            check(releaseResponse.await(10, TimeUnit.SECONDS))
        }

        return TaxUpdate(requestReceived, releaseResponse)
    }

    private fun Scenario.startTaxUpdate(taxUpdate: TaxUpdate) {
        val startedRequest = expectShippingAnalytics("elements.shipping_address.save_started")
        closeButton.assertIsEnabled()
        primaryButton.performScrollTo().assertIsEnabled().performClick()
        composeTestRule.waitUntilWithIdle {
            taxUpdate.requestReceived.count == 0L
        }
        awaitAnalytics(startedRequest)
    }

    private fun Scenario.expectShippingAnalytics(
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

    private fun Scenario.assertSaving() {
        assertThat(activityScenario.state).isEqualTo(Lifecycle.State.RESUMED)
        primaryButton.assertIsNotEnabled()
        closeButton.assertIsNotEnabled()
        composeTestRule.onNodeWithTag(PRIMARY_BUTTON_LOADING_INDICATOR_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    private fun Scenario.awaitResult(): AddressElementActivityContract.Result {
        composeTestRule.waitUntilWithIdle {
            activityScenario.state == Lifecycle.State.DESTROYED
        }
        return AddressElementActivityContract.CheckoutShipping.parseResult(
            activityScenario.result.resultCode,
            activityScenario.result.resultData,
        )
    }

    private fun runScenario(block: Scenario.() -> Unit) {
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
        ActivityScenario.launchActivityForResult<AddressElementActivity>(
            AddressElementActivityContract.CheckoutShipping.createIntent(
                applicationContext,
                AddressElementActivityContract.Args.CheckoutShipping(
                    apiConfiguration = DEFAULT_API_CONFIG,
                    config = AddressLauncher.Configuration.Builder()
                        .address(SHIPPING_ADDRESS)
                        .build(),
                    checkoutSessionResponse = checkoutSessionResponse,
                ),
            )
        ).use { activityScenario ->
            awaitAnalytics(shownRequest)
            Scenario(
                activityScenario = activityScenario,
                checkoutSessionResponse = checkoutSessionResponse,
                primaryButton = composeTestRule.onNodeWithText(
                    applicationContext.getString(R.string.stripe_paymentsheet_address_element_primary_button)
                ),
                closeButton = composeTestRule.onNodeWithTag(SHEET_NAVIGATION_BUTTON_TAG),
                scrim = composeTestRule.onNodeWithContentDescription(
                    applicationContext.getString(ComposeUiR.string.close_sheet)
                ),
            ).block()
        }
    }

    private data class Scenario(
        val activityScenario: ActivityScenario<AddressElementActivity>,
        val checkoutSessionResponse: CheckoutSessionResponse,
        val primaryButton: SemanticsNodeInteraction,
        val closeButton: SemanticsNodeInteraction,
        val scrim: SemanticsNodeInteraction,
    )

    private data class TaxUpdate(
        val requestReceived: CountDownLatch,
        val releaseResponse: CountDownLatch,
    )

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
