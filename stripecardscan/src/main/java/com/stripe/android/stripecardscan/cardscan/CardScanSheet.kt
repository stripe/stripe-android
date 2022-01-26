package com.stripe.android.stripecardscan.cardscan

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import com.stripe.android.stripecardscan.cardscan.exception.UnknownScanException
import com.stripe.android.stripecardscan.payment.card.ScannedCard
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class CardScanSheetParams(
    val stripePublishableKey: String,
//    val cardScanIntentId: String,
//    val cardScanIntentSecret: String,
) : Parcelable

sealed interface CardScanSheetResult : Parcelable {

    @Parcelize
    data class Completed(
        val scannedCard: ScannedCard,
    ) : CardScanSheetResult

    @Parcelize
    data class Canceled(
        val reason: CancellationReason,
    ) : CardScanSheetResult

    @Parcelize
    data class Failed(val error: Throwable) : CardScanSheetResult
}

class CardScanSheet private constructor(private val stripePublishableKey: String) {

    private var onFinished:
        ((cardImageVerificationSheetResult: CardScanSheetResult) -> Unit)? = null
    private lateinit var launcher: ActivityResultLauncher<CardScanSheetParams>

    companion object {
        /**
         * Create a [CardScanSheet] instance with [ComponentActivity].
         *
         * This API registers an [ActivityResultLauncher] into the
         * [ComponentActivity], it must be called before the [ComponentActivity]
         * is created (in the onCreate method).
         *
         * see https://github.com/stripe/stripe-android/blob/3e92b79190834dc3aab1c2d9ac2dfb7bc343afd2/payments-core/src/main/java/com/stripe/android/payments/paymentlauncher/PaymentLauncher.kt#L52
         */
        @JvmStatic
        fun create(from: ComponentActivity, stripePublishableKey: String) =
            CardScanSheet(stripePublishableKey).apply {
                launcher = from.registerForActivityResult(
                    object : ActivityResultContract<
                        CardScanSheetParams,
                        CardScanSheetResult
                        >() {
                        override fun createIntent(
                            context: Context,
                            input: CardScanSheetParams,
                        ) = this@Companion.createIntent(context, input)

                        override fun parseResult(
                            resultCode: Int,
                            intent: Intent?,
                        ) = this@Companion.parseResult(requireNotNull(intent))
                    },
                    ::onResult,
                )
            }

        private fun createIntent(context: Context, input: CardScanSheetParams) =
            Intent(context, CardScanActivity::class.java)
                .putExtra(INTENT_PARAM_REQUEST, input)

        private fun parseResult(intent: Intent): CardScanSheetResult =
            intent.getParcelableExtra(INTENT_PARAM_RESULT)
                ?: CardScanSheetResult.Failed(
                    UnknownScanException("No data in the result intent")
                )
    }

    /**
     * Present the scan flow using the provided ID and Secret.
     * Results will be returned in the callback function.
     *
     * The ID and Secret are created from this server-server request:
     * https://paper.dropbox.com/doc/Bouncer-Web-API-Review--BTOclListnApWjHdpv4DoaOuAg-Wy0HGlL0XfwAOz9hHuzS1#:h2=Creating-a-CardImageVerificati
     */
    fun present(
//        cardScanIntentId: String,
//        cardScanIntentSecret: String,
        onFinished: (cardScanSheetResult: CardScanSheetResult) -> Unit,
    ) {
        this.onFinished = onFinished
        launcher.launch(
            CardScanSheetParams(
                stripePublishableKey = stripePublishableKey,
//                cardScanIntentId = cardScanIntentId,
//                cardScanIntentSecret = cardScanIntentSecret,
            )
        )
    }

    /**
     * When a result is available from the activity, call [onFinished] if it's available.
     */
    private fun onResult(cardScanSheetResult: CardScanSheetResult) {
        onFinished?.let { it(cardScanSheetResult) }
    }
}
