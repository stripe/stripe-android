package com.stripe.android.paymentelement.embedded.form

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult

internal object FormContract : ActivityResultContract<EmbeddedActivityArgs, EmbeddedActivityResult>() {
    override fun createIntent(context: Context, input: EmbeddedActivityArgs): Intent {
        return Intent(context, FormActivity::class.java)
            .putExtra(EmbeddedActivityArgs.EXTRA_ARGS, input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): EmbeddedActivityResult {
        return EmbeddedActivityResult.fromIntent(intent)
    }
}
