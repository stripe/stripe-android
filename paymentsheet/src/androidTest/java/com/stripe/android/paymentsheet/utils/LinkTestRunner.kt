package com.stripe.android.paymentsheet.utils

import com.google.common.truth.Truth.assertThat
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.PaymentOptionCallback
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResultCallback

internal fun runLinkTest(
    networkRule: NetworkRule,
    integrationType: LinkIntegrationType,
    createIntentCallback: CreateIntentCallback? = null,
    paymentOptionCallback: PaymentOptionCallback,
    resultCallback: PaymentSheetResultCallback,
    block: (LinkTestRunnerContext) -> Unit,
) {
    when (integrationType) {
        LinkIntegrationType.PaymentSheet -> {
            runPaymentSheetTest(
                networkRule = networkRule,
                integrationType = IntegrationType.Compose,
                resultCallback = resultCallback,
                block = { context ->
                    block(LinkTestRunnerContext.WithPaymentSheet(context))
                },
            )
        }
        LinkIntegrationType.FlowController -> {
            runFlowControllerTest(
                networkRule = networkRule,
                integrationType = IntegrationType.Compose,
                createIntentCallback = createIntentCallback,
                paymentOptionCallback = paymentOptionCallback,
                resultCallback = resultCallback,
                block = { context ->
                    block(LinkTestRunnerContext.WithFlowController(context))
                }
            )
        }
    }
}

internal sealed interface LinkTestRunnerContext {

    fun launch(
        configuration: PaymentSheet.Configuration = PaymentSheet.Configuration(
            merchantDisplayName = "Merchant, Inc."
        )
    )

    class WithPaymentSheet(
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
