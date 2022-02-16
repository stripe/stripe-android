package com.stripe.android.stripecardscan.cardscan

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.IdRes
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.stripecardscan.cardscan.exception.UnknownScanException
import com.stripe.android.stripecardscan.payment.card.ScannedCard
import com.stripe.android.stripecardscan.scanui.CancellationReason
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class CardScanSheetParams(
    val stripePublishableKey: String
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

private const val CARD_SCAN_FRAGMENT_TAG = "CardScanFragmentTag"

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
         */
        @JvmStatic
        fun create(from: ComponentActivity, stripePublishableKey: String) =
            CardScanSheet(stripePublishableKey).apply {
                launcher = from.registerForActivityResult(activityResultContract, ::onResult)
            }

        /**
         * Create a [CardScanSheet] instance with [Fragment].
         *
         * This API registers an [ActivityResultLauncher] into the [Fragment], it must be called
         * before the [Fragment] is created (in the onCreate method).
         */
        @JvmStatic
        fun create(from: Fragment, stripePublishableKey: String) =
            CardScanSheet(stripePublishableKey).apply {
                launcher = from.registerForActivityResult(activityResultContract, ::onResult)
            }

        private fun createIntent(context: Context, input: CardScanSheetParams) =
            Intent(context, CardScanActivity::class.java)
                .putExtra(INTENT_PARAM_REQUEST, input)

        private fun parseResult(intent: Intent): CardScanSheetResult =
            intent.getParcelableExtra(INTENT_PARAM_RESULT)
                ?: CardScanSheetResult.Failed(
                    UnknownScanException("No data in the result intent")
                )

        fun removeCardScanFragment(
            supportFragmentManager: FragmentManager
        ) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                val fragment = supportFragmentManager.findFragmentByTag(CARD_SCAN_FRAGMENT_TAG)
                if (fragment != null) {
                    remove(fragment)
                }
            }
        }

        private val activityResultContract = object : ActivityResultContract<
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
        }
    }

    /**
     * Present the scan flow using the provided ID and Secret.
     * Results will be returned in the callback function.
     *
     * The ID and Secret are created from this server-server request:
     * https://paper.dropbox.com/doc/Bouncer-Web-API-Review--BTOclListnApWjHdpv4DoaOuAg-Wy0HGlL0XfwAOz9hHuzS1#:h2=Creating-a-CardImageVerificati
     */
    fun present(
        onFinished: (cardScanSheetResult: CardScanSheetResult) -> Unit,
    ) {
        this.onFinished = onFinished
        launcher.launch(
            CardScanSheetParams(
                stripePublishableKey = stripePublishableKey
            )
        )
    }

    /**
     * When a result is available from the activity, call [onFinished] if it's available.
     */
    private fun onResult(cardScanSheetResult: CardScanSheetResult) {
        onFinished?.let { it(cardScanSheetResult) }
    }

    /**
     * Attach the cardscan fragment to the specified container.
     * Results will be returned in the callback function.
     */
    fun attachCardScanFragment(
        lifecycleOwner: LifecycleOwner,
        supportFragmentManager: FragmentManager,
        @IdRes fragmentContainer: Int,
        onFinished: (cardScanSheetResult: CardScanSheetResult) -> Unit
    ) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            add<CardScanFragment>(
                fragmentContainer,
                args = bundleOf(
                    CARD_SCAN_FRAGMENT_PARAMS_KEY to CardScanSheetParams(stripePublishableKey)
                ),
                tag = CARD_SCAN_FRAGMENT_TAG
            )
        }

        supportFragmentManager
            .setFragmentResultListener(
                CARD_SCAN_FRAGMENT_REQUEST_KEY, lifecycleOwner
            ) { _, bundle ->
                val result: CardScanSheetResult = bundle.getParcelable(
                    CARD_SCAN_FRAGMENT_BUNDLE_KEY
                ) ?: CardScanSheetResult.Failed(Throwable("Card scan params not provided"))
                onFinished(result)
            }
    }
}
