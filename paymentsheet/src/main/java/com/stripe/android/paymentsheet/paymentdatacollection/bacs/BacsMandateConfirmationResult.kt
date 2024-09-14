package com.stripe.android.paymentsheet.paymentdatacollection.bacs

import android.content.Intent
import android.os.Parcelable
import androidx.core.os.BundleCompat
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal sealed interface BacsMandateConfirmationResult : Parcelable {
    @Parcelize
    object Confirmed : BacsMandateConfirmationResult

    @Parcelize
    object ModifyDetails : BacsMandateConfirmationResult

    @Parcelize
    object Cancelled : BacsMandateConfirmationResult

    companion object {
        internal const val EXTRA_RESULT = ActivityStarter.Result.EXTRA

        fun toIntent(intent: Intent, result: BacsMandateConfirmationResult): Intent {
            return intent.putExtra(EXTRA_RESULT, result)
        }

        fun fromIntent(intent: Intent?): BacsMandateConfirmationResult {
            val result = intent?.extras?.let { bundle ->
                BundleCompat.getParcelable(bundle, EXTRA_RESULT, BacsMandateConfirmationResult::class.java)
            }
            return result ?: Cancelled
        }
    }
}
