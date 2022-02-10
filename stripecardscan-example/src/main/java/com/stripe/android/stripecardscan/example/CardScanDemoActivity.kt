package com.stripe.android.stripecardscan.example

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.stripe.android.stripecardscan.cardscan.CardScanSheet
import com.stripe.android.stripecardscan.cardscan.CardScanSheetResult
import com.stripe.android.stripecardscan.example.databinding.ActivityCardScanDemoBinding

class CardScanDemoActivity : AppCompatActivity() {
    private val viewBinding by lazy {
        ActivityCardScanDemoBinding.inflate(layoutInflater)
    }

    private val settings by lazy { Settings(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        val cardScanSheet = CardScanSheet.create(this, settings.publishableKey)

        viewBinding.launchScanButton.setOnClickListener {
            cardScanSheet.present(this::onScanFinished)
        }
    }

    private fun onScanFinished(result: CardScanSheetResult) {
        when (result) {
            is CardScanSheetResult.Completed ->
                viewBinding.scanResultText.text = result.scannedCard.pan
            is CardScanSheetResult.Canceled ->
                viewBinding.scanResultText.text = result.reason.toString()
            is CardScanSheetResult.Failed ->
                viewBinding.scanResultText.text = result.error.message
        }
    }
}