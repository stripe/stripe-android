package com.stripe.android.paymentelement.nfcscan

import androidx.compose.ui.test.junit4.ComposeTestRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.EmbeddedContentPage
import com.stripe.android.paymentelement.EmbeddedFormPage
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.EmbeddedPaymentElementTestRunnerContext
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.PaymentMethodLayout
import com.stripe.android.paymentsheet.PaymentSheet as StripePaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetPage
import com.stripe.android.paymentsheet.utils.FlowControllerTestRunnerContext
import com.stripe.android.paymentsheet.utils.PaymentSheetTestRunnerContext

internal sealed class NfcScanningIntegrationTestRunnerContext(
    protected val composeTestRule: ComposeTestRule,
) {
    protected val intentConfiguration = StripePaymentSheet.IntentConfiguration(
        mode = StripePaymentSheet.IntentConfiguration.Mode.Payment(
            amount = 5099,
            currency = "usd",
        )
    )

    protected val customerConfig = StripePaymentSheet.CustomerConfiguration.createWithCustomerSession(
        id = "cus_12345",
        clientSecret = "cuss_654321",
    )

    abstract suspend fun launchFlow()

    abstract fun openCardForm()

    abstract fun clickPrimaryButton()

    abstract suspend fun completeCheckout(cardLastFour: String)

    sealed class Sheet(
        composeTestRule: ComposeTestRule,
    ) : NfcScanningIntegrationTestRunnerContext(composeTestRule) {
        protected val configuration = StripePaymentSheet.Configuration.Builder(
            merchantDisplayName = "Merchant, Inc.",
        )
            .paymentMethodLayout(PaymentMethodLayout.Vertical)
            .customer(customerConfig)
            .build()

        protected val paymentSheetPage = PaymentSheetPage(composeTestRule)

        final override fun openCardForm() {
            paymentSheetPage.waitForCardForm()
        }

        final override fun clickPrimaryButton() {
            paymentSheetPage.clickPrimaryButton()
        }

        class PaymentSheet(
            composeTestRule: ComposeTestRule,
            private val context: PaymentSheetTestRunnerContext,
        ) : Sheet(composeTestRule) {
            override suspend fun launchFlow() {
                context.presentPaymentSheet {
                    presentWithIntentConfiguration(
                        intentConfiguration = intentConfiguration,
                        configuration = configuration,
                    )
                }
            }

            override suspend fun completeCheckout(cardLastFour: String) {
                // Payment Sheet completes after the primary button is clicked.
            }
        }

        class FlowController(
            composeTestRule: ComposeTestRule,
            private val context: FlowControllerTestRunnerContext,
        ) : Sheet(composeTestRule) {
            override suspend fun launchFlow() {
                context.configureFlowController {
                    configureWithIntentConfiguration(
                        intentConfiguration = intentConfiguration,
                        configuration = configuration,
                        callback = { success, error ->
                            assertThat(success).isTrue()
                            assertThat(error).isNull()
                            presentPaymentOptions()
                        },
                    )
                }
            }

            override suspend fun completeCheckout(cardLastFour: String) {
                context.consumePaymentOptionEventForFlowController(
                    paymentMethodType = "card",
                    label = cardLastFour,
                )
                context.consumeNullPaymentOptionEventForFlowController()
            }
        }
    }

    class Embedded(
        composeTestRule: ComposeTestRule,
        private val context: EmbeddedPaymentElementTestRunnerContext,
    ) : NfcScanningIntegrationTestRunnerContext(composeTestRule) {
        private val embeddedContentPage = EmbeddedContentPage(composeTestRule)
        private val embeddedFormPage = EmbeddedFormPage(composeTestRule)

        override suspend fun launchFlow() {
            context.configure(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 5099,
                        currency = "usd",
                    )
                ),
                configurationMutator = {
                    customer(customerConfig)
                    formSheetAction(EmbeddedPaymentElement.FormSheetAction.Continue)
                }
            )
        }

        override fun openCardForm() {
            embeddedContentPage.clickOnLpm("card")
            embeddedFormPage.waitUntilVisible()
        }

        override fun clickPrimaryButton() {
            embeddedFormPage.clickPrimaryButton()
            embeddedFormPage.waitUntilMissing()
        }

        override suspend fun completeCheckout(cardLastFour: String) {
            context.consumePaymentOptionEvent(
                paymentMethodType = "card",
                label = cardLastFour,
            )
            context.confirm()
            assertThat(context.paymentOptionTurbine.awaitItem()).isNull()
        }
    }
}
