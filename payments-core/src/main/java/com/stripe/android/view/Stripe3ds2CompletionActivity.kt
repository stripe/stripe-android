package com.stripe.android.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.stripe.android.payments.Stripe3ds2CompletionContract
import com.ults.listeners.SdkChallengeInterface.UL_HANDLE_CHALLENGE_ACTION

class Stripe3ds2CompletionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val result = Stripe3ds2CompletionContract().parsePaymentFlowResult(intent)

        LocalBroadcastManager.getInstance(this)
            .sendBroadcast(Intent().setAction(UL_HANDLE_CHALLENGE_ACTION))
        setResult(
            Activity.RESULT_OK,
            Intent()
                .putExtras(result.toBundle())
        )
        finish()
    }
}
