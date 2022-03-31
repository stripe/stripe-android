package com.stripe.android.ui.core

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.PaymentConfiguration
import com.stripe.android.stripecardscan.cardscan.CardScanSheetResult
import com.stripe.android.ui.core.databinding.ActivityCardScanBinding

const val CARD_SCAN_PARCELABLE_NAME = "CardScanActivityResult"

class CardScanActivity : AppCompatActivity() {
    private val viewBinding by lazy {
        ActivityCardScanBinding.inflate(layoutInflater)
    }

    private lateinit var stripeCardScanProxy: StripeCardScanProxy

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        stripeCardScanProxy = StripeCardScanProxy.create(
            this, PaymentConfiguration.getInstance(this).publishableKey, this::onScanFinished
        )
    }

    override fun onStart() {
        super.onStart()
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

    override fun onStop() {
        StripeCardScanProxy.removeCardScanFragment(supportFragmentManager)
        super.onStop()
    }
}
