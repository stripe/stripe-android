package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import android.content.Intent
import android.os.Build
import android.os.Parcelable
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal sealed interface CvcRecollectionResult : Parcelable {

    @Parcelize
    data class Confirmed(val cvc: String) : CvcRecollectionResult

    @Parcelize
    object Cancelled : CvcRecollectionResult

    companion object {
        private const val EXTRA_RESULT = ActivityStarter.Result.EXTRA

        fun toIntent(intent: Intent, result: CvcRecollectionResult): Intent {
            return intent.putExtra(EXTRA_RESULT, result)
        }

        fun fromIntent(intent: Intent?): CvcRecollectionResult {
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent?.getParcelableExtra(EXTRA_RESULT, CvcRecollectionResult::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent?.getParcelableExtra(EXTRA_RESULT)
            }

            return result ?: Cancelled
        }
    }
}
