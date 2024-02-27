package com.stripe.android.paymentsheet.utils

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.MainActivity
import com.stripe.android.paymentsheet.PaymentOptionCallback
import com.stripe.android.paymentsheet.PaymentSheet
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
                    block(LinkTestRunnerContext.WithPaymentSheet(scenario, context))
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
                    block(LinkTestRunnerContext.WithFlowController(scenario, context))
                }
            )
        }
    }
}

internal sealed interface LinkTestRunnerContext {
    val scenario: ActivityScenario<MainActivity>

    fun launch(
        configuration: PaymentSheet.Configuration = PaymentSheet.Configuration(
            merchantDisplayName = "Merchant, Inc."
        )
    )

    class WithPaymentSheet(
        override val scenario: ActivityScenario<MainActivity>,
        private val context: PaymentSheetTestRunnerContext
    ) : LinkTestRunnerContext {
        override fun launch(configuration: PaymentSheet.Configuration) {
            context.presentPaymentSheet {
                presentWithPaymentIntent(
                    paymentIntentClientSecret = "pi_example_secret_example",
                    configuration = configuration,
                )
            }
        }
    }

    class WithFlowController(
        override val scenario: ActivityScenario<MainActivity>,
        private val context: FlowControllerTestRunnerContext
    ) : LinkTestRunnerContext {
        override fun launch(configuration: PaymentSheet.Configuration) {
            context.configureFlowController {
                configureWithPaymentIntent(
                    paymentIntentClientSecret = "pi_example_secret_example",
                    configuration = configuration,
                    callback = { success, error ->
                        assertThat(success).isTrue()
                        assertThat(error).isNull()
                        presentPaymentOptions()
                    },
                )
            }
        }
    }
}
