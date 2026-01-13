package com.stripe.android.paymentsheet

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.espresso.intent.rule.IntentsRule
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.core.networking.AnalyticsRequestV2
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.ElementsSession
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.ui.TEST_TAG_LIST
import com.stripe.android.paymentsheet.utils.IntegrationType
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.assertCompleted
import com.stripe.android.paymentsheet.utils.runPaymentSheetTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.seconds

@RunWith(TestParameterInjector::class)
internal class HorizontalModeExperimentsTest {
    private val networkRule = NetworkRule(
        hostsToTrack = listOf(ApiRequest.API_HOST,
            AnalyticsRequestV2.ANALYTICS_HOST
        ),
        validationTimeout = 5.seconds,
    )

    @get:Rule
    val testRules: TestRules = TestRules.create(
        networkRule = networkRule,
    ) {
        around(IntentsRule())
    }

    private val composeTestRule = testRules.compose

    private val page: PaymentSheetPage = PaymentSheetPage(composeTestRule)

    @Test
    fun testHorizontalModeExperiment() = runPaymentSheetTest(
        networkRule = networkRule,
        integrationType = IntegrationType.Compose,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method_with_horizontal_mode_experiment.json")
        }

        // TODO: add dimensions
        networkRule.enqueue(
            host("r.stripe.com"),
            method("POST"),
            bodyPart("event_name", "elements.experiment_exposure"),
            bodyPart("experiment_retrieved", "ocs_mobile_horizontal_mode")
        ) { }

        // There are 3 exposure events logged for Link experiments.
        (1..3).forEach { _ ->
            networkRule.enqueue(
                host("r.stripe.com"),
                method("POST"),
                bodyPart("event_name", "elements.experiment_exposure"),
            ) { }
        }



        testContext.presentPaymentSheet {
            presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = PaymentSheet.Configuration(
                    merchantDisplayName = "Example, Inc.",
                    paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Automatic,
                ),
            )
        }

        page.waitForCardForm()

        // Assert that horizontal mode is being used
        composeTestRule.waitUntil {
            composeTestRule
                .onAllNodes(hasTestTag(TEST_TAG_LIST))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule.onNodeWithTag(TEST_TAG_LIST, useUnmergedTree = true)
            .assertExists()

        testContext.markTestSucceeded()
    }
}
