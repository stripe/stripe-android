package com.stripe.android.payments.core.authentication.stripejs

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.BundleCompat
import com.stripe.android.model.StripeIntent
import kotlinx.parcelize.Parcelize

internal class StripeJsNextActionContract : ActivityResultContract<StripeJsNextActionContract.Args, StripeJsNextActionActivityResult>() {

    override fun createIntent(context: Context, input: Args): Intent {
        return StripeJsNextActionActivity.createIntent(
            context,
            StripeJsNextActionArgs(
                publishableKey = input.publishableKey,
                intent = input.intent
            )
        )
    }

    override fun parseResult(resultCode: Int, intent: Intent?): StripeJsNextActionActivityResult {
        return when (resultCode) {
            StripeJsNextActionActivity.RESULT_COMPLETE -> {
                intent?.extras?.let { bundle ->
                    BundleCompat.getParcelable(bundle, StripeJsNextActionActivity.EXTRA_RESULT, StripeJsNextActionActivityResult::class.java)
                } ?: StripeJsNextActionActivityResult.Failed(IllegalArgumentException("Missing result"))
            }
            else -> StripeJsNextActionActivityResult.Canceled
        }
    }

    @Parcelize
    data class Args(
        val publishableKey: String,
        val intent: StripeIntent,
    ) : Parcelable

    companion object {
        internal const val EXTRA_RESULT = "stripe_js_next_action_result"
    }
}
