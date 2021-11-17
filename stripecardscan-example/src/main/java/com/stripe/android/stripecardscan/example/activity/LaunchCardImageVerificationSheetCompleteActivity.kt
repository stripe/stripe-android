package com.stripe.android.stripecardscan.example.activity

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.stripecardscan.cardimageverification.CardImageVerificationSheet
import com.stripe.android.stripecardscan.cardimageverification.CardImageVerificationSheetResult
import com.stripe.android.stripecardscan.example.databinding.ActivityLaunchCardImageVerificationSheetCompleteBinding

internal const val PARAM_PUBLISHABLE_KEY = "stripePublishableKey"

internal class LaunchCardImageVerificationSheetCompleteActivity : AppCompatActivity() {
    private val viewBinding by lazy {
        ActivityLaunchCardImageVerificationSheetCompleteBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        val stripePublishableKey = intent.getStringExtra(PARAM_PUBLISHABLE_KEY) ?: run {
            Log.e("Stripe Example", "Unable to read publishable key")
            finish()
            return@onCreate
        }

        val cardVerificationSheet = CardImageVerificationSheet.create(this, stripePublishableKey)

        viewBinding.launchScanButton.setOnClickListener {
            cardVerificationSheet.present(
                viewBinding.civId.text.toString(),
                viewBinding.civSecret.text.toString(),
                this::onScanFinished,
            )
        }
    }

    private fun onScanFinished(result: CardImageVerificationSheetResult) {
        Toast.makeText(this, result.toString(), Toast.LENGTH_LONG).show()
        finish()
    }
}
