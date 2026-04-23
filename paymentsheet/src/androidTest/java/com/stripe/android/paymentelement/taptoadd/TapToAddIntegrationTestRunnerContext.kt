package com.stripe.android.paymentelement.taptoadd

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.EmbeddedContentPage
import com.stripe.android.paymentelement.EmbeddedFormPage
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.EmbeddedPaymentElementTestRunnerContext
import com.stripe.android.paymentelement.embedded.form.EMBEDDED_FORM_ACTIVITY_PRIMARY_BUTTON
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet as StripePaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetPage
import com.stripe.android.paymentsheet.utils.FlowControllerTestRunnerContext
import com.stripe.android.paymentsheet.utils.PaymentSheetTestRunnerContext
import com.stripe.paymentelementtestpages.DEFAULT_PE_PAGE_UI_TIMEOUT

internal sealed class TapToAddIntegrationTestRunnerContext(
    protected val composeTestRule: ComposeTestRule,
) {
    protected val intentConfiguration = StripePaymentSheet.IntentConfiguration(
        mode = StripePaymentSheet.IntentConfiguration.Mode.Payment(
            amount = 5099,
            currency = "usd"
        )
    )

    protected val customerConfig = StripePaymentSheet.CustomerConfiguration.createWithCustomerSession(
        id = "cus_123",
        clientSecret = "cuss_123",
    )

    abstract suspend fun launchFlow()

    abstract fun openCardForm()

    abstract suspend fun confirm()

    abstract fun clickPrimaryButton()

    abstract fun markTestSucceeded()

    sealed class Sheet(
        composeTestRule: ComposeTestRule,
    ) : TapToAddIntegrationTestRunnerContext(composeTestRule) {
        protected val configuration = StripePaymentSheet.Configuration.Builder(
            merchantDisplayName = "Merchant, Inc.",
        )
            .paymentMethodLayout(StripePaymentSheet.PaymentMethodLayout.Vertical)
            .customer(customerConfig)
            .build()

        protected val paymentSheetPage = PaymentSheetPage(composeTestRule)

        final override fun openCardForm() {
            paymentSheetPage.clickOnLpm(code = "card", forVerticalMode = true)
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

            override suspend fun confirm() {
                throw IllegalStateException("Should confirm from Tap to Add flow!")
            }

            override fun markTestSucceeded() {
                context.markTestSucceeded()
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

            override suspend fun confirm() {
                context.consumePaymentOptionEventForFlowController(
                    paymentMethodType = "card",
                    label = "···· 4242"
                )

                context.flowController.confirm()
            }

            override fun markTestSucceeded() {
                context.markTestSucceeded()
            }
        }
    }

    sealed class Embedded(
        composeTestRule: ComposeTestRule,
        protected val context: EmbeddedPaymentElementTestRunnerContext
    ) : TapToAddIntegrationTestRunnerContext(composeTestRule) {
        private val embeddedContentPage = EmbeddedContentPage(composeTestRule)
        protected val embeddedFormPage = EmbeddedFormPage(composeTestRule)

        abstract val formSheetAction: EmbeddedPaymentElement.FormSheetAction

        final override suspend fun launchFlow() {
            context.configure(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 5099,
                        currency = "usd",
                    )
                ),
                configurationMutator = {
                    customer(customerConfig)
                    formSheetAction(formSheetAction)
                }
            )
        }

        override fun clickPrimaryButton() {
            composeTestRule.onNodeWithTag(EMBEDDED_FORM_ACTIVITY_PRIMARY_BUTTON)
                .performScrollTo()
                .performClick()

            composeTestRule.waitUntil(DEFAULT_PE_PAGE_UI_TIMEOUT) {
                !hasPrimaryButton()
            }

            composeTestRule.waitForIdle()
        }

        final override fun openCardForm() {
            embeddedContentPage.clickOnLpm("card")
            embeddedFormPage.waitUntilVisible()
        }

        override suspend fun confirm() {
            val paymentOption = context.paymentOptionTurbine.awaitItem()

            assertThat(paymentOption?.label).isEqualTo("···· 4242")
            assertThat(paymentOption?.paymentMethodType).isEqualTo("card")

            context.confirm()
        }

        protected fun hasPrimaryButton(): Boolean {
            return composeTestRule.onAllNodesWithTag(EMBEDDED_FORM_ACTIVITY_PRIMARY_BUTTON)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }

        class Continue(
            composeTestRule: ComposeTestRule,
            context: EmbeddedPaymentElementTestRunnerContext
        ) : Embedded(composeTestRule, context) {
            override val formSheetAction = EmbeddedPaymentElement.FormSheetAction.Continue

            override fun markTestSucceeded() {
                context.markTestSucceeded()
            }
        }

        class Confirm(
            composeTestRule: ComposeTestRule,
            context: EmbeddedPaymentElementTestRunnerContext
        ) : Embedded(composeTestRule, context)  {
            override val formSheetAction = EmbeddedPaymentElement.FormSheetAction.Confirm

            override fun markTestSucceeded() {
                context.markTestSucceeded()
            }
        }
    }
}
