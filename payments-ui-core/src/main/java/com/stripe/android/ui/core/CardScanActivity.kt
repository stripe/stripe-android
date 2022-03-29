package com.stripe.android.ui.core

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.stripecardscan.cardscan.CardScanSheetResult
import com.stripe.android.ui.core.databinding.ActivityCardScanBinding

const val CARD_SCAN_PARCELABLE_NAME = "CardScanActivityResult"

class CardScanActivity : AppCompatActivity() {
    private val viewBinding by lazy {
        ActivityCardScanBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        val stripeCardScanProxy = StripeCardScanProxy.create(this, "test")
        stripeCardScanProxy.attachCardScanFragment(
            this, supportFragmentManager, R.id.fragment_container, this::onScanFinished
        )
    }

    private fun onScanFinished(result: CardScanSheetResult) {
        val intent = Intent()
            .putExtra(
                CARD_SCAN_PARCELABLE_NAME,
                result
            )
        setResult(RESULT_OK, intent)
        finish()
    }
}
