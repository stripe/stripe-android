package com.stripe.android.paymentsheet.utils

import com.google.common.truth.Truth.assertThat
import com.stripe.android.elements.payment.IntentConfiguration
import com.stripe.android.elements.payment.PaymentMethodLayout
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.elements.payment.ConfirmCustomPaymentMethodCallback
import com.stripe.android.paymentelement.ExperimentalCustomPaymentMethodsApi
import com.stripe.android.elements.payment.CreateIntentCallback
import com.stripe.android.elements.payment.FlowController
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResultCallback

internal fun runProductIntegrationTest(
    networkRule: NetworkRule,
    integrationType: ProductIntegrationType,
    builder: ProductIntegrationBuilder.() -> Unit = {},
    resultCallback: PaymentSheetResultCallback,
    block: suspend (ProductIntegrationTestRunnerContext) -> Unit,
) {
    val integrationBuilder = ProductIntegrationBuilder().apply {
        builder()
    }

    when (integrationType) {
        ProductIntegrationType.PaymentSheet -> {
            runPaymentSheetTest(
                networkRule = networkRule,
                integrationType = IntegrationType.Compose,
                builder = {
                    integrationBuilder.applyToPaymentSheetBuilder(this)
                },
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
                builder = {
                    integrationBuilder.applyToFlowControllerBuilder(this)
                },
                resultCallback = resultCallback,
                block = { context ->
                    block(ProductIntegrationTestRunnerContext.WithFlowController(context))
                }
            )
        }
    }
}

@OptIn(ExperimentalCustomPaymentMethodsApi::class)
internal class ProductIntegrationBuilder {
    private var createIntentCallback: CreateIntentCallback? = null

    private var confirmCustomPaymentMethodCallback: ConfirmCustomPaymentMethodCallback? = null

    fun confirmCustomPaymentMethodCallback(
        confirmCustomPaymentMethodCallback: ConfirmCustomPaymentMethodCallback?
    ) = apply {
        this.confirmCustomPaymentMethodCallback = confirmCustomPaymentMethodCallback
    }

    fun createIntentCallback(createIntentCallback: CreateIntentCallback?) = apply {
        this.createIntentCallback = createIntentCallback
    }

    fun applyToPaymentSheetBuilder(builder: PaymentSheet.Builder) {
        createIntentCallback?.let {
            builder.createIntentCallback(it)
        }

        confirmCustomPaymentMethodCallback?.let {
            builder.confirmCustomPaymentMethodCallback(it)
        }
    }

    fun applyToFlowControllerBuilder(builder: FlowController.Builder) {
        createIntentCallback?.let {
            builder.createIntentCallback(it)
        }

        confirmCustomPaymentMethodCallback?.let {
            builder.confirmCustomPaymentMethodCallback(it)
        }
    }
}

internal sealed interface ProductIntegrationTestRunnerContext {

    fun launch(
        configuration: PaymentSheet.Configuration = PaymentSheet.Configuration(
            merchantDisplayName = "Merchant, Inc.",
            paymentMethodLayout = PaymentMethodLayout.Horizontal,
        ),
        isDeferredIntent: Boolean = false
    )

    fun markTestSucceeded()

    suspend fun consumePaymentOptionEventForFlowController(paymentMethodType: String, label: String)

    class WithPaymentSheet(
        private val context: PaymentSheetTestRunnerContext
    ) : ProductIntegrationTestRunnerContext {
        override fun launch(configuration: PaymentSheet.Configuration, isDeferredIntent: Boolean) {
            context.presentPaymentSheet {
                if (isDeferredIntent) {
                    presentWithIntentConfiguration(
                        intentConfiguration = IntentConfiguration(
                            mode = IntentConfiguration.Mode.Payment(
                                amount = 5099,
                                currency = "usd"
                            )
                        ),
                        configuration = configuration,
                    )
                } else {
                    presentWithPaymentIntent(
                        paymentIntentClientSecret = "pi_example_secret_example",
                        configuration = configuration,
                    )
                }
            }
        }

        override fun markTestSucceeded() {
            context.markTestSucceeded()
        }

        override suspend fun consumePaymentOptionEventForFlowController(paymentMethodType: String, label: String) {
        }
    }

    class WithFlowController(
        val context: FlowControllerTestRunnerContext
    ) : ProductIntegrationTestRunnerContext {
        override fun launch(configuration: PaymentSheet.Configuration, isDeferredIntent: Boolean) {
            context.configureFlowController {
                if (isDeferredIntent) {
                    configureWithIntentConfiguration(
                        IntentConfiguration(
                            mode = IntentConfiguration.Mode.Payment(
                                amount = 5099,
                                currency = "usd",
                            )
                        ),
                        configuration = configuration,
                        callback = { success, error ->
                            assertThat(success).isTrue()
                            assertThat(error).isNull()
                            presentPaymentOptions()
                        },
                    )
                } else {
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

        override fun markTestSucceeded() {
            context.markTestSucceeded()
        }

        override suspend fun consumePaymentOptionEventForFlowController(paymentMethodType: String, label: String) {
            val paymentOption = context.configureCallbackTurbine.awaitItem()
            assertThat(paymentOption?.label).endsWith(label)
            assertThat(paymentOption?.paymentMethodType).isEqualTo(paymentMethodType)
        }

        fun confirm() {
            context.flowController.confirm()
        }
    }
}
