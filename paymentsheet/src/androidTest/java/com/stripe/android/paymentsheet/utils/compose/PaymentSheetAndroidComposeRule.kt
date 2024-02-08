package com.stripe.android.paymentsheet.utils.compose

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.stripe.android.ui.core.analytics.PaymentsUiEventReporter
import com.stripe.android.ui.core.analytics.ReportablePaymentsUi
import org.junit.rules.TestRule

inline fun <reified A : ComponentActivity> createPaymentSheetAndroidComposeRule() =
    PaymentSheetAndroidComposeRule(createAndroidComposeRule<A>())

class PaymentSheetAndroidComposeContext(
    val eventReporter: PaymentsUiEventReporter = ComposePaymentsUiEventReporter
)

class PaymentSheetAndroidComposeRule<R : TestRule, A : ComponentActivity>(
    private val testRule: AndroidComposeTestRule<R, A>
) : ComposeContentTestRule by testRule {
    val activity: A
        get() = testRule.activity

    val activityRule: R
        get() = testRule.activityRule

    override fun setContent(composable: @Composable () -> Unit) {
        setContent(PaymentSheetAndroidComposeContext(), composable)
    }

    fun setContent(
        context: PaymentSheetAndroidComposeContext,
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
