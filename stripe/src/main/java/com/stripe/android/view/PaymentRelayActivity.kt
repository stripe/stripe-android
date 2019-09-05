package com.stripe.android.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ults.listeners.SdkChallengeInterface.UL_HANDLE_CHALLENGE_ACTION

/**
 * An `Activity` that relays the intent extras that it received as a result and finishes.
 */
internal class PaymentRelayActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this)
            .sendBroadcast(Intent().setAction(UL_HANDLE_CHALLENGE_ACTION))
        setResult(Activity.RESULT_OK, Intent().putExtras(intent.extras!!))
        finish()
    }
}
