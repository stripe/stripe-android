package com.stripe.android.paymentelement.nfcscan

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import com.stripe.android.common.nfcscan.NfcScanningActivity
import com.stripe.android.common.nfcscan.NfcScanningContract
import com.stripe.android.testing.waitUntilWithIdle

internal object NfcScanningIntentsHelper {
    fun intendingNfcScanningToComplete(
        result: NfcScanningContract.Result.Complete,
    ) {
        intending(hasComponent(NfcScanningActivity::class.java.name)).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK,
                Intent().putExtras(result.toBundle()),
            )
        )
    }

    fun intendedNfcScanningToBeLaunched(
        composeTestRule: ComposeTestRule,
    ) {
        composeTestRule.waitUntilWithIdle {
            try {
                intended(hasComponent(NfcScanningActivity::class.java.name))
                true
            } catch (_: AssertionError) {
                false
            }
        }
    }
}
