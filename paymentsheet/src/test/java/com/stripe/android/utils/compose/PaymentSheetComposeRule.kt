package com.stripe.android.utils.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import com.stripe.android.ui.core.analytics.PaymentsUiEventReporter
import com.stripe.android.ui.core.analytics.ReportablePaymentsUi

fun createPaymentSheetComposeRule() = PaymentSheetComposeRule(createComposeRule())

class PaymentSheetComposeContext(
    val eventReporter: PaymentsUiEventReporter = ComposePaymentsUiEventReporter
)

class PaymentSheetComposeRule(
    private val testRule: ComposeContentTestRule
) : ComposeContentTestRule by testRule {
    override fun setContent(composable: @Composable () -> Unit) {
        setContent(PaymentSheetComposeContext(), composable)
    }

    fun setContent(
        context: PaymentSheetComposeContext = PaymentSheetComposeContext(),
        composable: @Composable () -> Unit
    ) {
        testRule.setContent {
            ReportablePaymentsUi(context.eventReporter) {
                composable()
            }
        }
    }
}

private object ComposePaymentsUiEventReporter : PaymentsUiEventReporter {
    override fun onCardNumberCompleted() {
        // No-op
    }
    override fun onAutofillEvent(type: String) {
        // No-op
    }
    override fun onFieldInteracted() {
        // No-op
    }
}
