package com.stripe.android.paymentelement.embedded.sheet

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedActivityState
import com.stripe.android.paymentelement.embedded.toArgs

internal object EmbeddedSheetContract : ActivityResultContract<EmbeddedActivityState, EmbeddedActivityResult>() {
    override fun createIntent(context: Context, input: EmbeddedActivityState): Intent {
        return Intent(context, EmbeddedSheetActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EmbeddedActivityArgs.EXTRA_ARGS, input.toArgs())
    }

    override fun parseResult(resultCode: Int, intent: Intent?): EmbeddedActivityResult {
        return EmbeddedActivityResult.fromIntent(intent)
    }
}
