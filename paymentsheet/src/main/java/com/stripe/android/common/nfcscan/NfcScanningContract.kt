package com.stripe.android.common.nfcscan

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.bundleOf
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal object NfcScanningContract : ActivityResultContract<Unit, NfcScanningContract.Result>() {
    override fun createIntent(context: Context, input: Unit): Intent {
        return Intent(context, NfcScanningActivity::class.java)
    }

    @Suppress("DEPRECATION")
    override fun parseResult(resultCode: Int, intent: Intent?): Result =
        intent?.getParcelableExtra(EXTRA_RESULT) ?: throw IllegalStateException(
            "Unknown NFC scanning result!"
        )

    internal sealed interface Result : ActivityStarter.Result {
        @Parcelize
        data class Complete(
            val cardNumber: String,
            val expirationMonth: Int,
            val expirationYear: Int,
            val shouldSave: Boolean,
        ) : Result

        @Parcelize
        data object Canceled : Result

        override fun toBundle(): Bundle {
            return bundleOf(EXTRA_RESULT to this)
        }
    }

    private const val EXTRA_RESULT = "com.stripe.android.common.nfcscan.NfcScanningContract.extra_result"
}
