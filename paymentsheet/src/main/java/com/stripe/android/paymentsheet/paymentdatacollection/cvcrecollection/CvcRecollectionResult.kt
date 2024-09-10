package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import android.content.Intent
import android.os.Parcelable
import androidx.core.os.BundleCompat
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal sealed interface CvcRecollectionResult : Parcelable {

    @Parcelize
    data class Confirmed(val cvc: String) : CvcRecollectionResult

    @Parcelize
    object Cancelled : CvcRecollectionResult

    companion object {
        internal const val EXTRA_RESULT = ActivityStarter.Result.EXTRA

        fun toIntent(intent: Intent, result: CvcRecollectionResult): Intent {
            return intent.putExtra(EXTRA_RESULT, result)
        }

        fun fromIntent(intent: Intent?): CvcRecollectionResult {
            val result = intent?.extras?.let { bundle ->
                BundleCompat.getParcelable(bundle, EXTRA_RESULT, CvcRecollectionResult::class.java)
            }
            return result ?: Cancelled
        }
    }
}
