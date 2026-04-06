package com.stripe.android.stripecardscan.cardscan

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RestrictTo
import androidx.fragment.app.Fragment
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.stripe.android.stripecardscan.cardscan.exception.UnknownScanException
import com.stripe.android.stripecardscan.payment.card.ScannedCard
import com.stripe.android.stripecardscan.scanui.CancellationReason
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class CardScanSheetParams(
    val cardScanConfiguration: CardScanConfiguration
) : Parcelable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface CardScanSheetResult : Parcelable {

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Completed(
        val scannedCard: ScannedCard
    ) : CardScanSheetResult

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Canceled(
        val reason: CancellationReason
    ) : CardScanSheetResult

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Failed(val error: Throwable) : CardScanSheetResult
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CardScanSheet private constructor() {

    private lateinit var launcher: ActivityResultLauncher<CardScanSheetParams>

    /**
     * Callback to notify when scanning finishes and a result is available.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun interface CardScanResultCallback {
        fun onCardScanSheetResult(cardScanSheetResult: CardScanSheetResult)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {

        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun isSupported(context: Context): Boolean {
            return GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
        }

        /**
         * Create a [CardScanSheet] instance with [ComponentActivity].
         *
         * This API registers an [ActivityResultLauncher] into the
         * [ComponentActivity] and notifies its result to [cardScanSheetResultCallback], it must be
         * called before the [ComponentActivity] is created (in the onCreate method).
         */
        @JvmStatic
        fun create(
            from: ComponentActivity,
            cardScanSheetResultCallback: CardScanResultCallback,
            registry: ActivityResultRegistry = from.activityResultRegistry
        ) =
            CardScanSheet().apply {
                launcher = from.registerForActivityResult(
                    activityResultContract,
                    registry,
                    cardScanSheetResultCallback::onCardScanSheetResult
                )
            }

        /**
         * Create a [CardScanSheet] instance with [Fragment].
         *
         * This API registers an [ActivityResultLauncher] into the [Fragment] and notifies its
         * result to [cardScanSheetResultCallback], it must be called before the [Fragment] is
         * created (in the onCreate method).
         */
        @JvmStatic
        fun create(
            from: Fragment,
            cardScanSheetResultCallback: CardScanResultCallback,
            registry: ActivityResultRegistry? = null
        ) =
            CardScanSheet().apply {
                launcher = if (registry != null) {
                    from.registerForActivityResult(
                        activityResultContract,
                        registry,
                        cardScanSheetResultCallback::onCardScanSheetResult
                    )
                } else {
                    from.registerForActivityResult(
                        activityResultContract,
                        cardScanSheetResultCallback::onCardScanSheetResult
                    )
                }
            }

        private fun createIntent(context: Context, input: CardScanSheetParams) =
            Intent(context, CardScanActivity::class.java)
                .putExtra(INTENT_PARAM_REQUEST, input)

        private fun parseResult(intent: Intent?): CardScanSheetResult =
            intent?.getParcelableExtra(INTENT_PARAM_RESULT)
                ?: CardScanSheetResult.Failed(
                    UnknownScanException("No data in the result intent")
                )

        private val activityResultContract = object : ActivityResultContract<
            CardScanSheetParams,
            CardScanSheetResult
            >() {
            override fun createIntent(
                context: Context,
                input: CardScanSheetParams
            ) = this@Companion.createIntent(context, input)

            override fun parseResult(
                resultCode: Int,
                intent: Intent?
            ) = this@Companion.parseResult(intent)
        }
    }

    /**
     * Present the scan flow using the provided ID and Secret.
     * Results will be returned in the callback function.
     *
     * The ID and Secret are created from this server-server request:
     * https://paper.dropbox.com/doc/Bouncer-Web-API-Review--BTOclListnApWjHdpv4DoaOuAg-Wy0HGlL0XfwAOz9hHuzS1#:h2=Creating-a-CardImageVerificati
     */
    fun present() {
        present(CardScanConfiguration(elementsSessionId = null))
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun present(configuration: CardScanConfiguration) {
        launcher.launch(CardScanSheetParams(configuration))
    }
}
