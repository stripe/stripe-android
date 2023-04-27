package com.stripe.android.ui.core.cardscan

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.PaymentConfiguration
import com.stripe.android.stripecardscan.cardscan.CardScanSheetResult
import com.stripe.android.ui.core.StripeCardScanProxy
import com.stripe.android.ui.core.databinding.StripeActivityCardScanBinding

internal class CardScanActivity : AppCompatActivity() {
    private val viewBinding by lazy {
        StripeActivityCardScanBinding.inflate(layoutInflater)
    }

    private lateinit var stripeCardScanProxy: StripeCardScanProxy

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        stripeCardScanProxy = StripeCardScanProxy.create(
            this,
            PaymentConfiguration.getInstance(this).publishableKey,
            this::onScanFinished
        )
        stripeCardScanProxy.present()
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

    companion object {
        const val CARD_SCAN_PARCELABLE_NAME = "CardScanActivityResult"
    }
}
