package com.stripe.android.googlepaylauncher

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.bundleOf
import kotlinx.parcelize.Parcelize

internal class GooglePayLauncherContract :
    ActivityResultContract<GooglePayLauncherContract.Args, GooglePayLauncher.Result>() {

    override fun createIntent(context: Context, input: Args): Intent {
        val statusBarColor = when (context) {
            is Activity -> context.window?.statusBarColor
            else -> null
        }

        val extras = input.toBundle().apply {
            if (statusBarColor != null) {
                putInt(EXTRA_STATUS_BAR_COLOR, statusBarColor)
            }
        }

        // TODO(mshafrir-stripe): update target activity
        return Intent(context, StripeGooglePayActivity::class.java)
            .putExtras(extras)
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): GooglePayLauncher.Result {
        return intent?.getParcelableExtra(EXTRA_RESULT)
            ?: GooglePayLauncher.Result.Failed(
                IllegalArgumentException("Could not parse a valid result.")
            )
    }

    @Parcelize
    data class Args(
        internal val clientSecret: String,
        internal val config: GooglePayLauncher.Config,
    ) : Parcelable {
        fun toBundle() = bundleOf("" to this)

        internal companion object {
            private const val EXTRA_ARGS = "extra_args"
            fun fromIntent(intent: Intent): Args? {
                return intent.getParcelableExtra<Args>(EXTRA_ARGS)
            }
        }
    }

    internal companion object {
        internal const val EXTRA_RESULT = "extra_result"
        internal const val EXTRA_STATUS_BAR_COLOR = "extra_status_bar_color"
    }
}
