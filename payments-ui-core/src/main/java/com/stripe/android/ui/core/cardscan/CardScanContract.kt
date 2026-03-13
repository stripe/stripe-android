package com.stripe.android.ui.core.cardscan

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.BundleCompat
import com.stripe.android.stripecardscan.cardscan.CardScanConfiguration
import com.stripe.android.stripecardscan.cardscan.CardScanSheetResult
import com.stripe.android.stripecardscan.cardscan.exception.UnknownScanException
import kotlinx.parcelize.Parcelize

internal class CardScanContract : ActivityResultContract<CardScanContract.Args, CardScanSheetResult>() {
    override fun createIntent(context: Context, input: Args): Intent {
        return Intent(context, CardScanActivity::class.java)
            .apply {
                putExtra(CardScanActivity.ARGS, input.configuration)
            }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): CardScanSheetResult {
        val unknownScanError = CardScanSheetResult.Failed(
            error = UnknownScanException("No data in the result intent")
        )
        val bundle = intent?.extras ?: return unknownScanError
        return BundleCompat.getParcelable(
            bundle,
            CardScanActivity.CARD_SCAN_PARCELABLE_NAME,
            CardScanSheetResult::class.java
        ) ?: unknownScanError
    }

    @Parcelize
    data class Args(
        val configuration: CardScanConfiguration
    ) : Parcelable
}
