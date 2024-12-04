package com.stripe.android.paymentsheet.example.playground.embedded

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

internal class EmbeddedPlaygroundContract : ActivityResultContract<PlaygroundState.Payment, Boolean>() {
    override fun createIntent(context: Context, input: PlaygroundState.Payment): Intent {
        return EmbeddedPlaygroundActivity.create(context, input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?) = resultCode == Activity.RESULT_OK
}
