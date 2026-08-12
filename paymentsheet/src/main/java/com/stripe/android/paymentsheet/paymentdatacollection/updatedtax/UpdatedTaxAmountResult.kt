package com.stripe.android.paymentsheet.paymentdatacollection.updatedtax

import android.content.Intent
import android.os.Parcelable
import androidx.core.os.BundleCompat
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal sealed interface UpdatedTaxAmountResult : Parcelable {

    @Parcelize
    data object Confirmed : UpdatedTaxAmountResult

    @Parcelize
    data object Cancelled : UpdatedTaxAmountResult

    companion object {
        internal const val EXTRA_RESULT = ActivityStarter.Result.EXTRA

        fun toIntent(intent: Intent, result: UpdatedTaxAmountResult): Intent {
            return intent.putExtra(EXTRA_RESULT, result)
        }

        fun fromIntent(intent: Intent?): UpdatedTaxAmountResult {
            return intent?.extras?.let { bundle ->
                BundleCompat.getParcelable(bundle, EXTRA_RESULT, UpdatedTaxAmountResult::class.java)
            } ?: Cancelled
        }
    }
}
