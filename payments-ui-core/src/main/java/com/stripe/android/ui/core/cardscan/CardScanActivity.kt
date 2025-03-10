package com.stripe.android.ui.core.cardscan

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.BundleCompat
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.stripecardscan.cardscan.CardScanConfiguration
import com.stripe.android.stripecardscan.cardscan.CardScanSheetResult
import com.stripe.android.ui.core.StripeCardScanProxy
import com.stripe.android.ui.core.databinding.StripeActivityCardScanBinding

internal class CardScanActivity : AppCompatActivity() {
    private val viewBinding by lazy {
        StripeActivityCardScanBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        val config = intent.extras?.let {
            BundleCompat.getParcelable(it, ARGS, CardScanConfiguration::class.java)
        }

        StripeCardScanProxy.create(
            this,
            this::onScanFinished,
            ErrorReporter.createFallbackInstance(applicationContext, setOf("CardScan"))
        ).present(config!!)
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
        const val ARGS = "args"
        const val CARD_SCAN_PARCELABLE_NAME = "CardScanActivityResult"
    }
}
