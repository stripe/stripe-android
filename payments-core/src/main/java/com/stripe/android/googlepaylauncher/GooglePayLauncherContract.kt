package com.stripe.android.googlepaylauncher

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.bundleOf
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import kotlinx.parcelize.Parcelize

internal class GooglePayLauncherContract :
    ActivityResultContract<GooglePayLauncherContract.Args, GooglePayLauncher.Result>() {

    override fun createIntent(context: Context, input: Args): Intent {
        return Intent(context, GooglePayLauncherActivity::class.java).putExtras(input.toBundle())
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): GooglePayLauncher.Result {
        return intent?.getParcelableExtra(EXTRA_RESULT) ?: GooglePayLauncher.Result.Failed(
            IllegalStateException(
                "Error while processing result from Google Pay."
            )
        )
    }

    /**
     * Args for launching [GooglePayLauncherContract] to confirm a [PaymentIntent].
     */
    @Parcelize
    data class PaymentIntentArgs @JvmOverloads constructor(
        override val clientSecret: String,
        override val config: GooglePayLauncher.Config,
        internal val label: String? = null,
    ) : Args()

    /**
     * Args for launching [GooglePayLauncherContract] to confirm a [SetupIntent].
     */
    @Parcelize
    data class SetupIntentArgs @JvmOverloads constructor(
        override val clientSecret: String,
        override val config: GooglePayLauncher.Config,
        internal val currencyCode: String,
        internal val amount: Long? = null,
        internal val label: String? = null,
    ) : Args()

    sealed class Args : Parcelable {
        internal abstract val clientSecret: String
        internal abstract val config: GooglePayLauncher.Config

        internal fun toBundle() = bundleOf(EXTRA_ARGS to this)

        internal companion object {
            private const val EXTRA_ARGS = "extra_args"

            internal fun fromIntent(intent: Intent): Args? {
                return intent.getParcelableExtra(EXTRA_ARGS)
            }
        }
    }

    internal companion object {
        internal const val EXTRA_RESULT = "extra_result"
    }
}
