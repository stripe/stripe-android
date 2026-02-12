package com.stripe.android.common.taptoadd

import android.content.Intent
import android.os.Parcelable
import androidx.core.os.BundleCompat
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal sealed interface TapToAddResult : Parcelable {
    @Parcelize
    data object Complete : TapToAddResult

    @Parcelize
    data class Continue(val paymentSelection: PaymentSelection) : TapToAddResult

    @Parcelize
    data class Canceled(
        val paymentSelection: PaymentSelection?
    ) : TapToAddResult

    companion object {
        internal const val EXTRA_RESULT = ActivityStarter.Result.EXTRA

        fun toIntent(intent: Intent, result: TapToAddResult): Intent {
            return intent.putExtra(EXTRA_RESULT, result)
        }

        fun fromIntent(intent: Intent?): TapToAddResult {
            val result = intent?.extras?.let { bundle ->
                BundleCompat.getParcelable(bundle, EXTRA_RESULT, TapToAddResult::class.java)
            }

            return result ?: Canceled(paymentSelection = null)
        }
    }
}
