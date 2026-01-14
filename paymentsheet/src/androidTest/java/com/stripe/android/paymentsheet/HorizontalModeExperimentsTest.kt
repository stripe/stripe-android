package com.stripe.android.paymentsheet

import androidx.test.espresso.intent.rule.IntentsRule
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.core.networking.AnalyticsRequestV2
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.utils.urlEncode
import com.stripe.android.model.ElementsSession
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.ResponseReplacement
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.utils.ProductIntegrationType
import com.stripe.android.paymentsheet.utils.ProductIntegrationTypeProvider
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.assertCompleted
import com.stripe.android.paymentsheet.utils.runProductIntegrationTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
internal class HorizontalModeExperimentsTest {
    private val networkRule = NetworkRule(
        hostsToTrack = listOf(
            ApiRequest.API_HOST,
            AnalyticsRequestV2.ANALYTICS_HOST
        ),
    )

    @get:Rule
    val testRules: TestRules = TestRules.create(
        networkRule = networkRule,
    ) {
        around(IntentsRule())
    }

    @TestParameter(valuesProvider = ProductIntegrationTypeProvider::class)
    lateinit var integrationType: ProductIntegrationType

    private val composeTestRule = testRules.compose
    private val page: PaymentSheetPage = PaymentSheetPage(composeTestRule)

    @Test
    fun testHorizontalModeAAExperiment_control() = testHorizontalModeExperiment(
        experimentAssignment = ElementsSession.ExperimentAssignment.OCS_MOBILE_HORIZONTAL_MODE_AA,
        experimentVariant = "control",
        showsVerticalMode = true,
    )

    @Test
    fun testHorizontalModeAAExperiment_controlTest() = testHorizontalModeExperiment(
        experimentAssignment = ElementsSession.ExperimentAssignment.OCS_MOBILE_HORIZONTAL_MODE_AA,
        experimentVariant = "control_test",
        showsVerticalMode = true,
    )

    @Test
    fun testHorizontalModeExperiment_control_showsVerticalMode() = testHorizontalModeExperiment(
        experimentAssignment = ElementsSession.ExperimentAssignment.OCS_MOBILE_HORIZONTAL_MODE,
        experimentVariant = "control",
        showsVerticalMode = true,
    )

    @Test
    fun testHorizontalModeExperiment_treatment_showsHorizontalMode() = testHorizontalModeExperiment(
        experimentAssignment = ElementsSession.ExperimentAssignment.OCS_MOBILE_HORIZONTAL_MODE,
        experimentVariant = "treatment",
        showsVerticalMode = false,
    )

    fun testHorizontalModeExperiment(
        experimentAssignment: ElementsSession.ExperimentAssignment,
        experimentVariant: String,
        showsVerticalMode: Boolean,
    ) = runProductIntegrationTest(
        networkRule = networkRule,
        integrationType = integrationType,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile(
                filename = "elements-sessions-requires_payment_method_with_horizontal_mode_experiment.json",
                replacements = listOf(
                    ResponseReplacement(
                        "[EXPERIMENT_ASSIGNMENTS_HERE]",
                        "{ ${experimentAssignment.experimentValue} : \"${experimentVariant}\" }"
                    ),
                )
            )
        }

        val expectedIntegrationType = when (integrationType) {
            ProductIntegrationType.FlowController -> "custom"
            ProductIntegrationType.PaymentSheet -> "complete"
        }

        networkRule.enqueue(
            host("r.stripe.com"),
            method("POST"),
            bodyPart("event_name", "elements.experiment_exposure"),
            bodyPart("experiment_retrieved", experimentAssignment.experimentValue),
            bodyPart("dimensions-in_app_elements_integration_type", expectedIntegrationType),
            bodyPart("dimensions-has_saved_payment_method", "false"),
            bodyPart("dimensions-displayed_payment_method_types", urlEncode("card,afterpay_clearpay,klarna")),
            bodyPart("dimensions-displayed_payment_method_types_including_wallets", urlEncode("card,afterpay_clearpay,klarna,link")),
        ) { }

        enqueueLogLinkHoldbackExperiments()

        testContext.launch(
            configuration = PaymentSheet.Configuration(
                merchantDisplayName = "Example, Inc.",
                paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Automatic,
            ),
        )

        page.assertLayout(isVerticalMode = showsVerticalMode)

        testContext.markTestSucceeded()
    }

    private fun enqueueLogLinkHoldbackExperiments() {
        networkRule.enqueue(
            host("r.stripe.com"),
            method("POST"),
            bodyPart("event_name", "elements.experiment_exposure"),
            bodyPart("experiment_retrieved", "link_global_holdback")
        ) { }

        networkRule.enqueue(
            host("r.stripe.com"),
            method("POST"),
            bodyPart("event_name", "elements.experiment_exposure"),
            bodyPart("experiment_retrieved", "link_global_holdback_aa")
        ) { }

        networkRule.enqueue(
            host("r.stripe.com"),
            method("POST"),
            bodyPart("event_name", "elements.experiment_exposure"),
            bodyPart("experiment_retrieved", "link_ab_test")
        ) { }
    }
}
