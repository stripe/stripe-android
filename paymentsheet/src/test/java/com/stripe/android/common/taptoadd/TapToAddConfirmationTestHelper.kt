package com.stripe.android.common.taptoadd

import android.app.Activity.RESULT_OK
import android.app.Instrumentation
import android.content.Intent
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.core.os.bundleOf
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import com.stripe.android.paymentelement.TapToAddPreview
import com.stripe.android.payments.paymentlauncher.InternalPaymentResult

@OptIn(TapToAddPreview::class)
class TapToAddConfirmationTestHelper(
    private val composeTestRule: ComposeTestRule,
) {
    val paymentElementCallbackIdentifier = PAYMENT_ELEMENT_CALLBACK_IDENTIFIER

    fun intendingPaymentConfirmationToBeLaunched(result: InternalPaymentResult) {
        intending(hasComponent(PAYMENT_CONFIRMATION_LAUNCHER_ACTIVITY_NAME)).respondWith(
            Instrumentation.ActivityResult(
                RESULT_OK,
                Intent().putExtras(bundleOf("extra_args" to result))
            )
        )
    }

    fun intendedPaymentConfirmationToBeLaunched() {
        composeTestRule.waitUntil(DEFAULT_UI_TIMEOUT) {
            runCatching {
                intended(hasComponent(PAYMENT_CONFIRMATION_LAUNCHER_ACTIVITY_NAME))
            }.fold(
                onSuccess = { true },
                onFailure = { false }
            )
        }
    }

    private companion object {
        const val DEFAULT_UI_TIMEOUT = 5000L

        const val PAYMENT_CONFIRMATION_LAUNCHER_ACTIVITY_NAME =
            "com.stripe.android.payments.paymentlauncher.PaymentLauncherConfirmationActivity"

        const val PAYMENT_ELEMENT_CALLBACK_IDENTIFIER = "mpe1"
    }
}
