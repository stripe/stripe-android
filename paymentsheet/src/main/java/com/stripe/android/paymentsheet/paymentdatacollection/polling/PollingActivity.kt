package com.stripe.android.paymentsheet.paymentdatacollection.polling

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentResultListener
import com.stripe.android.link.R as LinkR

internal class PollingActivity : AppCompatActivity() {

    private val args: PollingContract.Args by lazy {
        requireNotNull(PollingContract.Args.fromIntent(intent))
    }

    private val listener = FragmentResultListener { _, result ->
        handleResult(result)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportFragmentManager.setFragmentResultListener(
            PollingFragment.KEY_FRAGMENT_RESULT,
            this,
            listener
        )

        args.statusBarColor?.let { color ->
            window.statusBarColor = color
        }

        if (savedInstanceState == null) {
            val fragment = PollingFragment.newInstance(args)
            fragment.isCancelable = false
            fragment.show(supportFragmentManager, fragment.tag)
        }
    }

    private fun handleResult(result: Bundle) {
        // Prevent a colored status bar from moving across the screen
        window.statusBarColor = Color.TRANSPARENT

        setResult(
            Activity.RESULT_OK,
            Intent().putExtras(result)
        )
        finish()
        overridePendingTransition(0, LinkR.anim.stripe_slide_down)
    }
}
