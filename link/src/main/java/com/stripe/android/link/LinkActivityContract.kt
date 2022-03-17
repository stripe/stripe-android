package com.stripe.android.link

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class LinkActivityContract :
    ActivityResultContract<LinkActivityContract.Args, LinkActivityResult>() {

    override fun createIntent(context: Context, input: Args) =
        Intent(context, LinkActivity::class.java)
            .putExtra(EXTRA_ARGS, input)

    override fun parseResult(resultCode: Int, intent: Intent?) =
        intent?.getParcelableExtra<Result>(EXTRA_RESULT)?.linkResult ?: LinkActivityResult.Failed(
            IllegalStateException("Failed to retrieve LinkActivityResult.")
        )

    /**
     * Arguments for launching [LinkActivity] to confirm a payment with Link.
     *
     * @param merchantName The customer-facing business name.
     * @param customerEmail Email of the customer used to pre-fill the form.
     * @param injectionParams Parameters needed to perform dependency injection.
     *                        If null, a new dependency graph will be created.
     */
    @Parcelize
    data class Args internal constructor(
        internal val merchantName: String,
        internal val customerEmail: String? = null,
        internal val injectionParams: InjectionParams? = null
    ) : ActivityStarter.Args {

        companion object {
            internal fun fromIntent(intent: Intent): Args? {
                return intent.getParcelableExtra(EXTRA_ARGS)
            }
        }

        @Parcelize
        internal data class InjectionParams(
            @InjectorKey val injectorKey: String,
            val productUsage: Set<String>,
            val enableLogging: Boolean,
            val publishableKey: String,
            val stripeAccountId: String?
        ) : Parcelable
    }

    @Parcelize
    data class Result(
        val linkResult: LinkActivityResult
    ) : ActivityStarter.Result {
        override fun toBundle() = bundleOf(EXTRA_RESULT to this)
    }

    companion object {
        const val EXTRA_ARGS =
            "com.stripe.android.link.LinkActivityContract.extra_args"
        const val EXTRA_RESULT =
            "com.stripe.android.link.LinkActivityContract.extra_result"
    }
}
