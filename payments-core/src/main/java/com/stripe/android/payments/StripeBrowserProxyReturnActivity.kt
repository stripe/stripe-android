package com.stripe.android.payments

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * An externally exposed activity that re-launches the existing StripeBrowserLauncherActivity
 * and finishes.  This is meant so no processing is done on external data.
 */
internal class StripeBrowserProxyReturnActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startActivity(Intent(this, StripeBrowserLauncherActivity::class.java))
        finish()
    }
}
