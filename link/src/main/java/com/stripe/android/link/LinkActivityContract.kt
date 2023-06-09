package com.stripe.android.link

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf
import com.stripe.android.link.LinkActivityResult.Canceled.Reason.BackPressed
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class LinkActivityContract :
    ActivityResultContract<LinkActivityContract.Args, LinkActivityResult>() {

    override fun createIntent(context: Context, input: Args) = Intent()

    override fun parseResult(resultCode: Int, intent: Intent?): LinkActivityResult {
        val linkResult = intent?.getParcelableExtra<Result>(EXTRA_RESULT)?.linkResult
        return linkResult ?: LinkActivityResult.Canceled(reason = BackPressed)
    }

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Args internal constructor(
        internal val configuration: LinkConfiguration,
        internal val prefilledCardParams: PaymentMethodCreateParams? = null,
    ) : ActivityStarter.Args {
        internal companion object {
            internal fun fromIntent(intent: Intent): Args? {
                return intent.getParcelableExtra(EXTRA_ARGS)
            }
        }
    }

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Result(
        val linkResult: LinkActivityResult
    ) : ActivityStarter.Result {
        override fun toBundle() = bundleOf(EXTRA_RESULT to this)
    }

    internal companion object {
        const val EXTRA_ARGS =
            "com.stripe.android.link.LinkActivityContract.extra_args"
        const val EXTRA_RESULT =
            "com.stripe.android.link.LinkActivityContract.extra_result"
    }
}
