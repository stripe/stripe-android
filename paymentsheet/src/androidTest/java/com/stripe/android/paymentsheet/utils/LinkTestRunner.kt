package com.stripe.android.paymentsheet.utils

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.MainActivity
import com.stripe.android.paymentsheet.PaymentOptionCallback
import com.stripe.android.paymentsheet.PaymentSheetResultCallback

internal fun ActivityScenarioRule<MainActivity>.runLinkTest(
    integrationType: LinkIntegrationType,
    createIntentCallback: CreateIntentCallback? = null,
    paymentOptionCallback: PaymentOptionCallback,
    resultCallback: PaymentSheetResultCallback,
    block: (LinkTestRunnerContext) -> Unit,
) {
    when (integrationType) {
        LinkIntegrationType.PaymentSheet -> {
            runPaymentSheetTest(
                integrationType = IntegrationType.Compose,
                resultCallback = resultCallback,
                block = { context ->
                    block(LinkTestRunnerContext.PaymentSheet(scenario, context))
                },
            )
        }
        LinkIntegrationType.FlowController -> {
            runFlowControllerTest(
                integrationType = IntegrationType.Compose,
                createIntentCallback = createIntentCallback,
                paymentOptionCallback = paymentOptionCallback,
                resultCallback = resultCallback,
                block = { context ->
                    block(LinkTestRunnerContext.FlowController(scenario, context))
                }
            )
        }
    }
}

internal sealed interface LinkTestRunnerContext {
    val scenario: ActivityScenario<MainActivity>

    class PaymentSheet(
        override val scenario: ActivityScenario<MainActivity>,
        val context: PaymentSheetTestRunnerContext
    ) : LinkTestRunnerContext

    class FlowController(
        override val scenario: ActivityScenario<MainActivity>,
        val context: FlowControllerTestRunnerContext
    ) : LinkTestRunnerContext
}
