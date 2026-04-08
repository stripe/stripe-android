package com.stripe.android.tta.testing

import android.app.Activity.RESULT_OK
import android.app.Instrumentation
import android.content.Intent
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.core.os.bundleOf
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import com.stripe.android.payments.paymentlauncher.InternalPaymentResult
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf

class TapToAddConfirmationTestHelper(
    private val composeTestRule: ComposeTestRule,
) {
    fun intendingPaymentConfirmationToBeLaunched(result: InternalPaymentResult) {
        intending(hasComponent(PAYMENT_CONFIRMATION_LAUNCHER_ACTIVITY_NAME)).respondWith(
            Instrumentation.ActivityResult(
                RESULT_OK,
                Intent().putExtras(bundleOf("extra_args" to result))
            )
        )
    }

    fun intendedPaymentConfirmationToBeLaunched(vararg matchers: Matcher<Intent>) {
        composeTestRule.waitUntil(DEFAULT_UI_TIMEOUT) {
            runCatching {
                intended(
                    allOf(
                        hasComponent(PAYMENT_CONFIRMATION_LAUNCHER_ACTIVITY_NAME),
                        *matchers,
                    )
                )
            }.fold(
                onSuccess = { true },
                onFailure = { false }
            )
        }
    }

    private companion object {
        const val PAYMENT_CONFIRMATION_LAUNCHER_ACTIVITY_NAME =
            "com.stripe.android.payments.paymentlauncher.PaymentLauncherConfirmationActivity"
    }
}
