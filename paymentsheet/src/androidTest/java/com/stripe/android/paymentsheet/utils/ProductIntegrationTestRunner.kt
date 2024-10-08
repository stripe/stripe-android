package com.stripe.android.paymentsheet.utils

import com.google.common.truth.Truth.assertThat
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResultCallback

internal fun runProductIntegrationTest(
    networkRule: NetworkRule,
    integrationType: ProductIntegrationType,
    createIntentCallback: CreateIntentCallback? = null,
    resultCallback: PaymentSheetResultCallback,
    block: (ProductIntegrationTestRunnerContext) -> Unit,
) {
    when (integrationType) {
        ProductIntegrationType.PaymentSheet -> {
            runPaymentSheetTest(
                networkRule = networkRule,
                integrationType = IntegrationType.Compose,
                resultCallback = resultCallback,
                block = { context ->
                    block(ProductIntegrationTestRunnerContext.WithPaymentSheet(context))
                },
            )
        }
        ProductIntegrationType.FlowController -> {
            runFlowControllerTest(
                networkRule = networkRule,
                integrationType = IntegrationType.Compose,
                createIntentCallback = createIntentCallback,
                resultCallback = resultCallback,
                block = { context ->
                    block(ProductIntegrationTestRunnerContext.WithFlowController(context))
                }
            )
        }
    }
}

internal sealed interface ProductIntegrationTestRunnerContext {

    fun launch(
        configuration: PaymentSheet.Configuration = PaymentSheet.Configuration(
            merchantDisplayName = "Merchant, Inc."
        )
    )

    fun markTestSucceeded()

    class WithPaymentSheet(
        private val context: PaymentSheetTestRunnerContext
    ) : ProductIntegrationTestRunnerContext {
        override fun launch(configuration: PaymentSheet.Configuration) {
            context.presentPaymentSheet {
                presentWithPaymentIntent(
                    paymentIntentClientSecret = "pi_example_secret_example",
                    configuration = configuration,
                )
            }
        }

        override fun markTestSucceeded() {
            context.markTestSucceeded()
        }
    }

    class WithFlowController(
        val context: FlowControllerTestRunnerContext
    ) : ProductIntegrationTestRunnerContext {
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

        override fun markTestSucceeded() {
            context.markTestSucceeded()
        }

        fun confirm() {
            context.flowController.confirm()
        }
    }
}
