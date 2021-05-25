package com.stripe.android.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.stripe.android.payments.PaymentFlowResult
import com.ults.listeners.SdkChallengeInterface.UL_HANDLE_CHALLENGE_ACTION

/**
 * An `Activity` that relays the intent extras that it received as a result and finishes.
 */
internal class PaymentRelayActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocalBroadcastManager.getInstance(this)
            .sendBroadcast(Intent().setAction(UL_HANDLE_CHALLENGE_ACTION))

        val paymentFlowResult = PaymentFlowResult.Unvalidated.fromIntent(intent)

        setResult(
            Activity.RESULT_OK,
            Intent()
                .putExtras(paymentFlowResult.toBundle())
        )
    }

    override fun onResume() {
        super.onResume()
        finish()
    }
}
