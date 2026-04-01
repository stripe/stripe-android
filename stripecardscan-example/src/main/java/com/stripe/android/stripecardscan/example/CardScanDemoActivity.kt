package com.stripe.android.stripecardscan.example

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.stripecardscan.cardscan.CardScanConfiguration
import com.stripe.android.stripecardscan.cardscan.CardScanSheet
import com.stripe.android.stripecardscan.cardscan.CardScanSheetResult
import com.stripe.android.stripecardscan.example.databinding.ActivityCardScanDemoBinding

private const val YEAR_MOD = 100

class CardScanDemoActivity : AppCompatActivity() {
    private val viewBinding by lazy {
        ActivityCardScanDemoBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        val cardScanSheet = CardScanSheet.create(this, ::onScanFinished)

        viewBinding.launchScanButton.setOnClickListener {
            cardScanSheet.present(
                CardScanConfiguration(
                    elementsSessionId = null,
                    enableMlKitTextRecognition = viewBinding.enableMlKitCheckbox.isChecked,
                )
            )
        }
    }

    private fun onScanFinished(result: CardScanSheetResult) {
        when (result) {
            is CardScanSheetResult.Completed -> {
                val card = result.scannedCard
                val expiry = if (card.expiryMonth != null && card.expiryYear != null) {
                    "\nExpires: %02d/%02d".format(card.expiryMonth, card.expiryYear!! % YEAR_MOD)
                } else {
                    ""
                }
                viewBinding.scanResultText.text = "${card.pan}$expiry"
            }
            is CardScanSheetResult.Canceled ->
                viewBinding.scanResultText.text = result.reason.toString()
            is CardScanSheetResult.Failed ->
                viewBinding.scanResultText.text = result.error.message
        }
    }
}
