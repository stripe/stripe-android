package com.stripe.android.view

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

@Suppress("ktlint:max-line-length")
internal class AddPaymentMethodContract :
    ActivityResultContract<AddPaymentMethodActivityStarter.Args, AddPaymentMethodActivityStarter.Result>() {

    override fun createIntent(
        context: Context,
        input: AddPaymentMethodActivityStarter.Args
    ): Intent {
        return Intent(context, AddPaymentMethodActivity::class.java)
            .putExtra(ActivityStarter.Args.EXTRA, input)
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): AddPaymentMethodActivityStarter.Result {
        return AddPaymentMethodActivityStarter.Result.fromIntent(intent)
    }
}
