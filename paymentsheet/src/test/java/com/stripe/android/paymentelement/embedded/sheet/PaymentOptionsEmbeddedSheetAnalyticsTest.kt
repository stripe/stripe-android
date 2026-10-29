package com.stripe.android.paymentelement.embedded.sheet

import android.app.Application
import android.os.Bundle
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.isInstanceOf
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.networktesting.AdvancedFraudSignalsTestRule
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers.analyticsPayloadField
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.flowcontroller.asEmbeddedConfiguration
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.ui.PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.testing.waitUntilWithIdle
import com.stripe.paymentelementtestpages.FormPage
import com.stripe.paymentelementtestpages.VerticalModePage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
internal class PaymentOptionsEmbeddedSheetAnalyticsTest {
    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()
    private val composeTestRule = createEmptyComposeRule()
    private val formPage = FormPage(composeTestRule)
    private val verticalModePage = VerticalModePage(composeTestRule)
    private val networkRule = NetworkRule(
        hostsToTrack = listOf(ApiRequest.API_HOST, AnalyticsRequest.HOST),
        validationTimeout = 5.seconds,
    )

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(composeTestRule)
        .around(createComposeCleanupRule())
        .around(networkRule)
        .around(AdvancedFraudSignalsTestRule())
        .around(PaymentConfigurationTestRule(applicationContext))

    @Before
    fun setup() {
        GooglePayPaymentMethodLauncher.setHasSentInitAnalyticEvent(hasSent = true)
    }

    @Test
    fun `cancel reports FlowController show and dismiss once`() {
        expectAnalytics("mc_custom_sheet_newpm_show")
        val visibilitySnapshotRequest = expectAnalytics("mc_initial_displayed_payment_methods")
        val dismissRequest = expectAnalytics("mc_dismiss")

        launch { scenario ->
            verticalModePage.waitUntilVisible()
            awaitAnalytics(visibilitySnapshotRequest)

            Espresso.pressBack()

            assertThat(scenario.awaitResult()).isInstanceOf<EmbeddedActivityResult.Cancelled>()
            awaitAnalytics(dismissRequest)
        }
    }

    @Test
    fun `continuing with a selection reports selection without dismiss`() {
        expectAnalytics("mc_form_shown", analyticsPayloadField("selected_lpm", "cashapp"))
        expectAnalytics("mc_custom_sheet_newpm_show")
        expectAnalytics("mc_confirm_button_tapped")
        val selectRequest = expectAnalytics("mc_custom_paymentoption_newpm_select")

        launch(
            paymentMethodMetadata = paymentMethodMetadata(paymentMethodTypes = listOf("cashapp")),
        ) { scenario ->
            formPage.waitUntilVisible()

            composeTestRule.onNodeWithTag(PRIMARY_BUTTON_TEST_TAG)
                .performScrollTo()
                .assertIsEnabled()
                .performClick()

            val result = scenario.awaitResult() as EmbeddedActivityResult.Complete
            assertThat(result.selection).isInstanceOf<PaymentSelection.New.GenericPaymentMethod>()
            awaitAnalytics(selectRequest)
        }
    }

    @Test
    fun `initial form reports FlowController sheet shown`() {
        expectAnalytics("mc_form_shown", analyticsPayloadField("selected_lpm", "cashapp"))
        val showRequest = expectAnalytics("mc_custom_sheet_newpm_show")
        val dismissRequest = expectAnalytics("mc_dismiss")

        launch(
            paymentMethodMetadata = paymentMethodMetadata(paymentMethodTypes = listOf("cashapp")),
        ) { scenario ->
            formPage.waitUntilVisible()
            awaitAnalytics(showRequest)

            Espresso.pressBack()

            assertThat(scenario.awaitResult()).isInstanceOf<EmbeddedActivityResult.Cancelled>()
            awaitAnalytics(dismissRequest)
        }
    }

    private fun expectAnalytics(
        eventName: String,
        vararg requestMatchers: RequestMatcher,
    ): CountDownLatch {
        val requestReceived = CountDownLatch(1)
        networkRule.enqueue(
            host("q.stripe.com"),
            method("GET"),
            analyticsPayloadField("event", eventName),
            *requestMatchers,
        ) { response ->
            requestReceived.countDown()
            response.setResponseCode(200)
            response.setBody("{}")
        }
        return requestReceived
    }

    private fun awaitAnalytics(requestReceived: CountDownLatch) {
        composeTestRule.waitUntilWithIdle(conditionDescription = "analytics request") {
            requestReceived.count == 0L
        }
    }

    private fun paymentMethodMetadata(paymentMethodTypes: List<String>): PaymentMethodMetadata {
        return PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = paymentMethodTypes,
            ),
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
        )
    }

    private fun ActivityScenario<EmbeddedSheetActivity>.awaitResult(): EmbeddedActivityResult {
        lateinit var activity: EmbeddedSheetActivity
        onActivity { activity = it }
        composeTestRule.waitUntilWithIdle(
            conditionDescription = "embedded sheet activity to finish",
        ) {
            activity.isFinishing
        }
        return EmbeddedSheetContract.parseResult(result.resultCode, result.resultData)
    }

    private fun launch(
        selection: PaymentSelection? = null,
        paymentMethodMetadata: PaymentMethodMetadata = paymentMethodMetadata(
            paymentMethodTypes = listOf("cashapp", "alipay"),
        ),
        customerState: CustomerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE,
        block: (ActivityScenario<EmbeddedSheetActivity>) -> Unit,
    ) {
        val args = EmbeddedActivityArgs(
            paymentMethodMetadata = paymentMethodMetadata,
            configuration = PaymentSheet.Configuration("Example, Inc.").asEmbeddedConfiguration(),
            productUsage = setOf("PaymentSheet.FlowController"),
            statusBarColor = null,
            paymentElementCallbackIdentifier = "PaymentOptionsAnalyticsTestIdentifier",
            selection = selection,
            previousNewSelections = Bundle(),
            customerState = customerState,
            linkAccountInfo = LinkAccountUpdate.Value(
                account = null,
                lastUpdateReason = LinkAccountUpdate.Value.UpdateReason.LoggedOut,
            ),
            promotions = emptyList(),
            launchMode = EmbeddedLaunchMode.PaymentOptions(
                visibleWallets = WalletType.entries,
                isFlowController = true,
            ),
            presentationState = EmbeddedActivityArgs.PresentationState.Ready,
        )
        ActivityScenario.launchActivityForResult<EmbeddedSheetActivity>(
            EmbeddedSheetContract.createIntent(context = applicationContext, input = args)
        ).use(block)
    }
}
