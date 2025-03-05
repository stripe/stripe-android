package com.stripe.android.identity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.bundleOf
import com.stripe.android.core.injection.InjectorKey
import kotlinx.parcelize.Parcelize

internal class IdentityVerificationSheetContract :
    ActivityResultContract<IdentityVerificationSheetContract.Args, IdentityVerificationSheet.VerificationFlowResult>() {

    @Parcelize
    internal data class Args(
        val verificationSessionId: String,
        val ephemeralKeySecret: String,
        @InjectorKey val injectorKey: String,
        val presentTime: Long
    ) : Parcelable {
        fun toBundle() = bundleOf(EXTRA_ARGS to this)

        companion object {
            private const val EXTRA_ARGS = "extra_args"

            fun fromIntent(intent: Intent): Args? {
                return intent.getParcelableExtra(EXTRA_ARGS)
            }
        }
    }

    override fun createIntent(context: Context, input: Args): Intent {
        return Intent(
            context,
            IdentityActivity::class.java
        ).putExtras(input.toBundle())
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): IdentityVerificationSheet.VerificationFlowResult {
        return IdentityVerificationSheet.VerificationFlowResult.fromIntent(intent)
    }
}
