package com.stripe.android.stripecardscan.cardimageverification

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.Fragment
import com.stripe.android.stripecardscan.cardimageverification.CardImageVerificationFlow.Companion.MAX_COMPLETION_LOOP_FRAMES
import com.stripe.android.stripecardscan.cardimageverification.exception.UnknownScanException
import com.stripe.android.stripecardscan.payment.card.ScannedCard
import com.stripe.android.stripecardscan.scanui.CancellationReason
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class CardImageVerificationSheetParams(
    val configuration: CardImageVerificationSheet.Configuration,
    val cardImageVerificationIntentId: String,
    val cardImageVerificationIntentSecret: String,
) : Parcelable

sealed interface CardImageVerificationSheetResult : Parcelable {

    @Parcelize
    data class Completed(
        val scannedCard: ScannedCard,
    ) : CardImageVerificationSheetResult

    @Parcelize
    data class Canceled(
        val reason: CancellationReason,
    ) : CardImageVerificationSheetResult

    @Parcelize
    data class Failed(val error: Throwable) : CardImageVerificationSheetResult
}

class CardImageVerificationSheet private constructor(private val configuration: Configuration) {

    @Parcelize
    data class Configuration(
        val stripePublishableKey: String,
        val verificationConfiguration: VerificationConfiguration = VerificationConfiguration(),
    ) : Parcelable

    @Parcelize
    data class VerificationConfiguration(
        // The amount of frames that must have a centered, focused card before the scan
        // is allowed to terminate
        val strictModeFrames: StrictModeFrameCount = StrictModeFrameCount.None,
    ) : Parcelable {
        sealed class StrictModeFrameCount(val count: Int) : Parcelable {
            @Parcelize object None : StrictModeFrameCount(0)
            @Parcelize object Low : StrictModeFrameCount(1)
            @Parcelize object Medium : StrictModeFrameCount(MAX_COMPLETION_LOOP_FRAMES / 2)
            @Parcelize object High : StrictModeFrameCount(MAX_COMPLETION_LOOP_FRAMES)
        }
    }

    private var onFinished:
        ((cardImageVerificationSheetResult: CardImageVerificationSheetResult) -> Unit)? = null
    private lateinit var launcher: ActivityResultLauncher<CardImageVerificationSheetParams>

    companion object {
        /**
         * Create a [CardImageVerificationSheet] instance with [ComponentActivity].
         *
         * This API registers an [ActivityResultLauncher] into the
         * [ComponentActivity], it must be called before the [ComponentActivity]
         * is created (in the onCreate method).
         */
        @JvmStatic
        fun create(from: ComponentActivity, config: Configuration) =
            CardImageVerificationSheet(config).apply {
                launcher = from.registerForActivityResult(activityResultContract, ::onResult)
            }

        /**
         * Create a [CardImageVerificationSheet] instance with [Fragment].
         *
         * This API registers an [ActivityResultLauncher] into the [Fragment], it must be called
         * before the [Fragment] is created (in the onCreate method).
         */
        @JvmStatic
        fun create(from: Fragment, config: Configuration) =
            CardImageVerificationSheet(config).apply {
                launcher = from.registerForActivityResult(activityResultContract, ::onResult)
            }

        private fun createIntent(context: Context, input: CardImageVerificationSheetParams) =
            Intent(context, CardImageVerificationActivity::class.java)
                .putExtra(INTENT_PARAM_REQUEST, input)

        private fun parseResult(intent: Intent): CardImageVerificationSheetResult =
            intent.getParcelableExtra(INTENT_PARAM_RESULT)
                ?: CardImageVerificationSheetResult.Failed(
                    UnknownScanException("No data in the result intent")
                )

        private val activityResultContract = object : ActivityResultContract<
            CardImageVerificationSheetParams,
            CardImageVerificationSheetResult
            >() {
            override fun createIntent(
                context: Context,
                input: CardImageVerificationSheetParams,
            ) = this@Companion.createIntent(context, input)

            override fun parseResult(
                resultCode: Int,
                intent: Intent?,
            ) = this@Companion.parseResult(requireNotNull(intent))
        }
    }

    /**
     * Present the CardVerification scan flow using the provided ID and Secret.
     * Results will be returned in the callback function.
     *
     * The ID and Secret are created from this server-server request:
     * https://paper.dropbox.com/doc/Bouncer-Web-API-Review--BTOclListnApWjHdpv4DoaOuAg-Wy0HGlL0XfwAOz9hHuzS1#:h2=Creating-a-CardImageVerificati
     */
    fun present(
        cardImageVerificationIntentId: String,
        cardImageVerificationIntentSecret: String,
        onFinished: (cardImageVerificationSheetResult: CardImageVerificationSheetResult) -> Unit,
    ) {
        this.onFinished = onFinished
        launcher.launch(
            CardImageVerificationSheetParams(
                configuration = configuration,
                cardImageVerificationIntentId = cardImageVerificationIntentId,
                cardImageVerificationIntentSecret = cardImageVerificationIntentSecret,
            )
        )
    }

    /**
     * When a result is available from the activity, call [onFinished] if it's available.
     */
    private fun onResult(cardImageVerificationSheetResult: CardImageVerificationSheetResult) {
        onFinished?.let { it(cardImageVerificationSheetResult) }
    }
}
