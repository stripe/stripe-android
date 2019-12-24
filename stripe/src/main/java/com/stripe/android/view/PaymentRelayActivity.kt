package com.stripe.android.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ults.listeners.SdkChallengeInterface.UL_HANDLE_CHALLENGE_ACTION

/**
 * An `Activity` that relays the intent extras that it received as a result and finishes.
 */
internal class PaymentRelayActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocalBroadcastManager.getInstance(this)
            .sendBroadcast(Intent().setAction(UL_HANDLE_CHALLENGE_ACTION))
        setResult(
            Activity.RESULT_OK,
            Intent()
                .apply {
                    intent.extras?.let {
                        putExtras(it)
                    }
                }
        )
        finish()
    }
}
