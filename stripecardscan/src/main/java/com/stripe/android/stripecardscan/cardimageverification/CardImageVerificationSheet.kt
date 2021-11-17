package com.stripe.android.stripecardscan.cardimageverification

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import com.stripe.android.stripecardscan.cardimageverification.exception.UnknownScanException
import com.stripe.android.stripecardscan.payment.card.ScannedCard
import com.stripe.android.stripecardscan.scanui.CancellationReason
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class CardImageVerificationSheetParams(
    val stripePublishableKey: String,
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

class CardImageVerificationSheet private constructor(private val stripePublishableKey: String) {

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
         *
         * see https://github.com/stripe/stripe-android/blob/3e92b79190834dc3aab1c2d9ac2dfb7bc343afd2/payments-core/src/main/java/com/stripe/android/payments/paymentlauncher/PaymentLauncher.kt#L52
         */
        @JvmStatic
        fun create(from: ComponentActivity, stripePublishableKey: String) =
            CardImageVerificationSheet(stripePublishableKey).apply {
                launcher = from.registerForActivityResult(
                    object : ActivityResultContract<
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
                        ) = intent?.let { this@Companion.parseResult(it) }
                    },
                    ::onResult,
                )
            }

        private fun createIntent(context: Context, input: CardImageVerificationSheetParams) =
            Intent(context, CardVerifyActivity::class.java).putExtra(INTENT_PARAM_REQUEST, input)

        private fun parseResult(intent: Intent): CardImageVerificationSheetResult =
            intent.getParcelableExtra(INTENT_PARAM_RESULT)
                ?: CardImageVerificationSheetResult.Failed(
                    UnknownScanException("No data in the result intent")
                )
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
                stripePublishableKey = stripePublishableKey,
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
