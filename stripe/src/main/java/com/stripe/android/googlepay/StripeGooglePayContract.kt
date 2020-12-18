package com.stripe.android.googlepay

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.stripe.android.view.ActivityStarter

internal class StripeGooglePayContract :
    ActivityResultContract<StripeGooglePayLauncher.Args, StripeGooglePayLauncher.Result>() {

    override fun createIntent(
        context: Context,
        input: StripeGooglePayLauncher.Args?
    ): Intent {
        return Intent(context, StripeGooglePayActivity::class.java)
            .putExtra(ActivityStarter.Args.EXTRA, input)
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): StripeGooglePayLauncher.Result {
        return StripeGooglePayLauncher.Result.fromIntent(intent)
    }
}
