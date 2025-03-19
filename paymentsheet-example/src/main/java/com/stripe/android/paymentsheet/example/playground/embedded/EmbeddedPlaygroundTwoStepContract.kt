package com.stripe.android.paymentsheet.example.playground.embedded

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import kotlinx.parcelize.Parcelize

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
internal class EmbeddedPlaygroundTwoStepContract :
    ActivityResultContract<EmbeddedPlaygroundTwoStepContract.Args, EmbeddedPlaygroundTwoStepContract.Result>() {
    override fun createIntent(context: Context, input: Args): Intent {
        return EmbeddedPlaygroundActivity.create(context, input.playgroundState, input.embeddedPaymentElementState)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Result {
        return if (resultCode == Activity.RESULT_OK) {
            Result.Complete
        } else {
            val embeddedPlaygroundState = intent?.getParcelableExtra<EmbeddedPaymentElement.State?>(
                EmbeddedPlaygroundActivity.EMBEDDED_PAYMENT_ELEMENT_STATE_KEY
            )
            if (embeddedPlaygroundState != null) {
                Result.Updated(embeddedPlaygroundState)
            } else {
                Result.Cancelled
            }
        }
    }

    @Parcelize
    class Args(
        val playgroundState: PlaygroundState.Payment,
        val embeddedPaymentElementState: EmbeddedPaymentElement.State,
    ) : Parcelable

    sealed interface Result {
        object Complete : Result

        class Updated(val embeddedPaymentElementState: EmbeddedPaymentElement.State) : Result

        object Cancelled : Result
    }
}
