package com.stripe.android.paymentsheet.paymentdatacollection.bacs

import android.content.Intent
import android.os.Build
import android.os.Parcelable
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
        private const val EXTRA_RESULT = ActivityStarter.Result.EXTRA

        fun toIntent(intent: Intent, result: BacsMandateConfirmationResult): Intent {
            return intent.putExtra(EXTRA_RESULT, result)
        }

        fun fromIntent(intent: Intent?): BacsMandateConfirmationResult {
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent?.getParcelableExtra(EXTRA_RESULT, BacsMandateConfirmationResult::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent?.getParcelableExtra(EXTRA_RESULT)
            }

            return result ?: Cancelled
        }
    }
}
