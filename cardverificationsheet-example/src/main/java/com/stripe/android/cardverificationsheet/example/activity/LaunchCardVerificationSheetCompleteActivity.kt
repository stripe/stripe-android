package com.stripe.android.cardverificationsheet.example.activity

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.cardverificationsheet.cardverifyui.CardVerificationSheet
import com.stripe.android.cardverificationsheet.cardverifyui.CardVerificationSheetResult
import com.stripe.android.cardverificationsheet.example.databinding.ActivityLaunchCardVerificationSheetCompleteBinding

internal const val PARAM_PUBLISHABLE_KEY = "stripePublishableKey"

internal class LaunchCardVerificationSheetCompleteActivity : AppCompatActivity() {
    private val viewBinding by lazy {
        ActivityLaunchCardVerificationSheetCompleteBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        val stripePublishableKey = intent.getStringExtra(PARAM_PUBLISHABLE_KEY) ?: run {
            Log.e("Stripe Example", "Unable to read publishable key")
            finish()
            return@onCreate
        }

        val cardVerificationSheet = CardVerificationSheet.create(this, stripePublishableKey)

        viewBinding.launchScanButton.setOnClickListener {
            cardVerificationSheet.present(
                viewBinding.civId.text.toString(),
                viewBinding.civSecret.text.toString(),
                this::onScanFinished,
            )
        }
    }

    private fun onScanFinished(result: CardVerificationSheetResult) {
        Toast.makeText(this, result.toString(), Toast.LENGTH_LONG).show()
        finish()
    }
}
