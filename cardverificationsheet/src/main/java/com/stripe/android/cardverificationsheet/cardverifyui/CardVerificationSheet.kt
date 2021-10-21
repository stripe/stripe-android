package com.stripe.android.cardverificationsheet.cardverifyui

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import com.stripe.android.cardverificationsheet.cardverifyui.exception.UnknownScanException
import com.stripe.android.cardverificationsheet.scanui.CardVerificationSheetCancelationReason
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class CardVerificationSheetParams(
    val stripePublishableKey: String,
    val cardImageVerificationIntentId: String,
    val cardImageVerificationIntentSecret: String,
) : Parcelable

sealed interface CardVerificationSheetResult : Parcelable {

    @Parcelize
    object Completed : CardVerificationSheetResult

    @Parcelize
    class Canceled(val reason: CardVerificationSheetCancelationReason) : CardVerificationSheetResult

    @Parcelize
    class Failed(val error: Throwable) : CardVerificationSheetResult
}

class CardVerificationSheet private constructor(private val stripePublishableKey: String) {

    private var onFinished:
        ((cardVerificationSheetResult: CardVerificationSheetResult) -> Unit)? = null
    private lateinit var launcher: ActivityResultLauncher<CardVerificationSheetParams>

    companion object {
        /**
         * Create a [CardVerificationSheet] instance with [ComponentActivity].
         *
         * This API registers an [ActivityResultLauncher] into the
         * [ComponentActivity], it must be called before the [ComponentActivity]
         * is created (in the onCreate method).
         *
         * see https://github.com/stripe/stripe-android/blob/3e92b79190834dc3aab1c2d9ac2dfb7bc343afd2/payments-core/src/main/java/com/stripe/android/payments/paymentlauncher/PaymentLauncher.kt#L52
         */
        @JvmStatic
        fun create(from: ComponentActivity, stripePublishableKey: String): CardVerificationSheet {
            val sheet = CardVerificationSheet(stripePublishableKey)

            sheet.launcher = from.registerForActivityResult(
                object : ActivityResultContract<
                    CardVerificationSheetParams,
                    CardVerificationSheetResult
                    >() {
                    override fun createIntent(
                        context: Context,
                        input: CardVerificationSheetParams,
                    ) = this@Companion.createIntent(context, input)

                    override fun parseResult(
                        resultCode: Int,
                        intent: Intent?,
                    ) = intent?.let { this@Companion.parseResult(it) }
                },
                sheet::onResult,
            )
            return sheet
        }

        private fun createIntent(context: Context, input: CardVerificationSheetParams): Intent =
            Intent(context, CardVerifyActivity::class.java).putExtra(INTENT_PARAM_REQUEST, input)

        private fun parseResult(intent: Intent): CardVerificationSheetResult =
            intent.getParcelableExtra(INTENT_PARAM_RESULT)
                ?: CardVerificationSheetResult.Failed(
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
        onFinished: (cardVerificationSheetResult: CardVerificationSheetResult) -> Unit,
    ) {
        this.onFinished = onFinished
        launcher.launch(
            CardVerificationSheetParams(
                stripePublishableKey = stripePublishableKey,
                cardImageVerificationIntentId = cardImageVerificationIntentId,
                cardImageVerificationIntentSecret = cardImageVerificationIntentSecret,
            )
        )
    }

    /**
     * When a result is available from the activity, call [onFinished] if it's available.
     */
    private fun onResult(cardVerificationSheetResult: CardVerificationSheetResult) {
        onFinished?.let { it(cardVerificationSheetResult) }
    }
}
