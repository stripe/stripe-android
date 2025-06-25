package com.stripe.android.paymentsheet.example.playground.spt

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

internal class SharedPaymentTokenPlaygroundContract :
    ActivityResultContract<PlaygroundState.SharedPaymentToken, Boolean>() {
    override fun createIntent(context: Context, input: PlaygroundState.SharedPaymentToken): Intent {
        return SharedPaymentTokenPlaygroundActivity.create(context, input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?) = resultCode == Activity.RESULT_OK
}
