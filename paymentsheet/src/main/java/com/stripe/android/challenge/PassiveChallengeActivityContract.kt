package com.stripe.android.challenge

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.BundleCompat
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import javax.inject.Inject

internal class PassiveChallengeActivityContract @Inject constructor()
    : ActivityResultContract<PassiveChallengeActivityContract.Args, PassiveChallengeActivityResult>() {

    override fun createIntent(context: Context, input: Args): Intent {
        return PassiveChallengeActivity.createIntent(
            context = context,
            args = PassiveChallengeArgs(input.newPaymentMethodConfirmationOption)
        )
    }

    override fun parseResult(resultCode: Int, intent: Intent?): PassiveChallengeActivityResult {
        val result = intent?.extras?.let {
            BundleCompat.getParcelable(it, EXTRA_RESULT, PassiveChallengeActivityResult::class.java)
        }
        return result ?: PassiveChallengeActivityResult.Failed(Throwable("No result"))
    }

    data class Args(
        val newPaymentMethodConfirmationOption: PaymentMethodConfirmationOption.New
    )

    companion object {
        internal const val EXTRA_RESULT = "com.stripe.android.challenge.PassiveChallengeActivityContract.extra_result"
    }
}
