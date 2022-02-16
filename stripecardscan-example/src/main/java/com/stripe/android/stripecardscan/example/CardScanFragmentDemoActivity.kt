package com.stripe.android.stripecardscan.example

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.stripe.android.stripecardscan.cardscan.CardScanSheet
import com.stripe.android.stripecardscan.cardscan.CardScanSheetResult
import com.stripe.android.stripecardscan.example.databinding.ActivityCardScanFragmentDemoBinding

class CardScanFragmentDemoActivity : AppCompatActivity() {
    private val viewBinding by lazy {
        ActivityCardScanFragmentDemoBinding.inflate(layoutInflater)
    }

    private lateinit var cardScanSheet: CardScanSheet

    private val settings by lazy { Settings(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        cardScanSheet = CardScanSheet.create(this, settings.publishableKey)

        viewBinding.launchScanButton.setOnClickListener {
            attachCardScanFragment()
        }
    }

    private fun attachCardScanFragment() {
        viewBinding.launchScanButton.isEnabled = false
        viewBinding.fragmentContainer.visibility = View.VISIBLE
        cardScanSheet.attachCardScanFragment(
            this, supportFragmentManager, R.id.fragment_container, this::onScanFinished
        )
    }

    private fun onScanFinished(result: CardScanSheetResult) {

        CardScanSheet.removeCardScanFragment(supportFragmentManager)
        viewBinding.fragmentContainer.visibility = View.GONE
        viewBinding.launchScanButton.isEnabled = true

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