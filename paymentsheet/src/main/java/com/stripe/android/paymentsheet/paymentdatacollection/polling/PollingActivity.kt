package com.stripe.android.paymentsheet.paymentdatacollection.polling

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.postDelayed
import androidx.fragment.app.FragmentResultListener
import androidx.lifecycle.lifecycleScope
import com.stripe.android.utils.AnimationConstants

internal class PollingActivity : AppCompatActivity() {

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

        if (savedInstanceState == null) {
            val fragment = PollingFragment.newInstance()
            fragment.isCancelable = false
            fragment.show(supportFragmentManager, fragment.tag)
        }
    }

    private fun handleResult(result: Bundle) {
        Handler(Looper.getMainLooper()).postDelayed(400L) {
            setResult(
                Activity.RESULT_OK,
                Intent().putExtras(result)
            )
            finish()
            setFadeAnimations()
        }
    }

    private fun setFadeAnimations() {
        overridePendingTransition(AnimationConstants.FADE_IN, AnimationConstants.FADE_OUT)
    }
}
