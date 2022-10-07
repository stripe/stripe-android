package com.stripe.android.stripecardscan.cardimageverification

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.Fragment
import com.stripe.android.stripecardscan.cardimageverification.exception.UnknownScanException
import com.stripe.android.stripecardscan.payment.card.ScannedCard
import com.stripe.android.stripecardscan.scanui.CancellationReason
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class CardImageVerificationSheetParams(
    val stripePublishableKey: String,
    val configuration: CardImageVerificationSheet.Configuration,
    val cardImageVerificationIntentId: String,
    val cardImageVerificationIntentSecret: String
) : Parcelable

sealed interface CardImageVerificationSheetResult : Parcelable {

    @Parcelize
    data class Completed(
        val cardImageVerificationIntentId: String,
        val scannedCard: ScannedCard
    ) : CardImageVerificationSheetResult

    @Parcelize
    data class Canceled(
        val reason: CancellationReason
    ) : CardImageVerificationSheetResult

    @Parcelize
    data class Failed(val error: Throwable) : CardImageVerificationSheetResult
}

class CardImageVerificationSheet private constructor(
    private val stripePublishableKey: String,
    private val configuration: Configuration
) {

    @Parcelize
    data class Configuration(
        /**
         * The amount of frames that must have a centered, focused card before the scan
         * is allowed to terminate. This is an experimental feature that should only be
         * used with guidance from Stripe support.
         */
        val strictModeFrames: StrictModeFrameCount = StrictModeFrameCount.None,
        /**
         * Determine if the "I can't scan this card" button should be included in the scan window.
         * This is an experimental feature that should only be used with guidance from Stripe
         * support.
         */
        val enableCannotScanButton: Boolean = true
    ) : Parcelable {
        sealed class StrictModeFrameCount(val count: (maxCompletionLoopFrames: Int) -> Int) : Parcelable {
            @Parcelize object None : StrictModeFrameCount({ 0 })

            @Parcelize object Low : StrictModeFrameCount({ 1 })

            @Parcelize object Medium : StrictModeFrameCount({ maxCompletionLoopFrames -> maxCompletionLoopFrames / 2 })

            @Parcelize object High : StrictModeFrameCount({ maxCompletionLoopFrames -> maxCompletionLoopFrames })
        }
    }

    /**
     * Callback to notify when scanning finishes and a result is available.
     */
    fun interface CardImageVerificationResultCallback {
        fun onCardImageVerificationSheetResult(
            cardImageVerificationSheetResult: CardImageVerificationSheetResult
        )
    }

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
        @JvmOverloads
        fun create(
            from: ComponentActivity,
            stripePublishableKey: String,
            config: Configuration = Configuration(),
            cardImageVerificationResultCallback: CardImageVerificationResultCallback,
            registry: ActivityResultRegistry = from.activityResultRegistry
        ) =
            CardImageVerificationSheet(stripePublishableKey, config).apply {
                launcher = from.registerForActivityResult(
                    activityResultContract,
                    registry,
                    cardImageVerificationResultCallback::onCardImageVerificationSheetResult
                )
            }

        /**
         * Create a [CardImageVerificationSheet] instance with [Fragment].
         *
         * This API registers an [ActivityResultLauncher] into the [Fragment], it must be called
         * before the [Fragment] is created (in the onCreate method).
         */
        @JvmStatic
        @JvmOverloads
        fun create(
            from: Fragment,
            stripePublishableKey: String,
            config: Configuration = Configuration(),
            cardImageVerificationResultCallback: CardImageVerificationResultCallback,
            registry: ActivityResultRegistry? = null
        ) =
            CardImageVerificationSheet(stripePublishableKey, config).apply {
                launcher = if (registry != null) {
                    from.registerForActivityResult(
                        activityResultContract,
                        registry,
                        cardImageVerificationResultCallback::onCardImageVerificationSheetResult
                    )
                } else {
                    from.registerForActivityResult(
                        activityResultContract,
                        cardImageVerificationResultCallback::onCardImageVerificationSheetResult
                    )
                }
            }

        private fun createIntent(context: Context, input: CardImageVerificationSheetParams) =
            Intent(context, CardImageVerificationActivity::class.java)
                .putExtra(INTENT_PARAM_REQUEST, input)

        private fun parseResult(intent: Intent?): CardImageVerificationSheetResult =
            intent?.getParcelableExtra(INTENT_PARAM_RESULT)
                ?: CardImageVerificationSheetResult.Failed(
                    UnknownScanException("No data in the result intent")
                )

        private val activityResultContract = object : ActivityResultContract<
            CardImageVerificationSheetParams,
            CardImageVerificationSheetResult
            >() {
            override fun createIntent(
                context: Context,
                input: CardImageVerificationSheetParams
            ) = this@Companion.createIntent(context, input)

            override fun parseResult(
                resultCode: Int,
                intent: Intent?
            ) = this@Companion.parseResult(intent)
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
        cardImageVerificationIntentSecret: String
    ) {
        launcher.launch(
            CardImageVerificationSheetParams(
                stripePublishableKey = stripePublishableKey,
                configuration = configuration,
                cardImageVerificationIntentId = cardImageVerificationIntentId,
                cardImageVerificationIntentSecret = cardImageVerificationIntentSecret
            )
        )
    }
}
