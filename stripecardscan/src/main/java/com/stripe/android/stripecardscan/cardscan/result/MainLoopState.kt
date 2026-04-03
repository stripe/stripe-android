package com.stripe.android.stripecardscan.cardscan.result

import com.stripe.android.stripecardscan.framework.MachineState
import com.stripe.android.stripecardscan.framework.util.ItemCounter
import com.stripe.android.stripecardscan.payment.ml.CardOcr
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

internal sealed class MainLoopState(
    timeSource: TimeSource,
    protected val enableExpiryWait: Boolean = false,
) : MachineState(timeSource) {

    companion object {
        /**
         * The duration after which the scan will reset if no card is visible.
         */
        val NO_CARD_VISIBLE_DURATION = 5.seconds

        /**
         * The maximum duration for which to search for a card number.
         */
        val OCR_SEARCH_DURATION = 10.seconds

        /**
         * Once this number of frames with matching card numbers are found, stop looking for card
         * numbers.
         */
        const val DESIRED_OCR_AGREEMENT = 3

        /**
         * After PAN agreement is reached, wait up to this duration for an expiration date
         * when expiry detection is enabled.
         */
        val EXPIRY_WAIT_DURATION = 1.seconds
    }

    internal abstract suspend fun consumeTransition(
        transition: CardOcr.Prediction
    ): MainLoopState

    class Initial(
        timeSource: TimeSource,
        enableExpiryWait: Boolean = false,
    ) : MainLoopState(timeSource, enableExpiryWait) {
        override suspend fun consumeTransition(
            transition: CardOcr.Prediction
        ): MainLoopState = if (transition.pan.isNullOrEmpty()) {
            this
        } else {
            OcrFound(
                timeSource = timeSource,
                enableExpiryWait = enableExpiryWait,
                pan = transition.pan,
                expiryMonth = transition.expiryMonth,
                expiryYear = transition.expiryYear,
            )
        }
    }

    class OcrFound(
        timeSource: TimeSource,
        enableExpiryWait: Boolean = false,
        pan: String,
        expiryMonth: Int? = null,
        expiryYear: Int? = null,
    ) : MainLoopState(timeSource, enableExpiryWait) {

        private val panCounter = ItemCounter(pan)
        private val expiryCounter = ItemCounter(
            if (expiryMonth != null && expiryYear != null) {
                CardOcr.Expiry(month = expiryMonth, year = expiryYear)
            } else {
                null
            }
        )

        private val mostLikelyPan: String
            get() = panCounter.getHighestCountItem().item

        private val mostLikelyExpiry: CardOcr.Expiry?
            get() = expiryCounter.getHighestCountItemOrNull()?.item

        private var lastCardVisible = TimeSource.Monotonic.markNow()

        private fun highestOcrCount() = panCounter.getHighestCountItem().count
        private fun isOcrSatisfied() = highestOcrCount() >= DESIRED_OCR_AGREEMENT
        private fun isTimedOut() = reachedStateAt.elapsedNow() > OCR_SEARCH_DURATION
        private fun isNoCardVisible() = lastCardVisible.elapsedNow() > NO_CARD_VISIBLE_DURATION

        override suspend fun consumeTransition(
            transition: CardOcr.Prediction
        ): MainLoopState {
            val transitionPan = transition.pan
            if (!transitionPan.isNullOrEmpty()) {
                panCounter.countItem(transitionPan)
                lastCardVisible = TimeSource.Monotonic.markNow()
            }

            if (transition.expiryMonth != null && transition.expiryYear != null) {
                expiryCounter.countItem(
                    CardOcr.Expiry(month = transition.expiryMonth, year = transition.expiryYear)
                )
            }

            return when {
                isOcrSatisfied() ->
                    if (enableExpiryWait && mostLikelyExpiry == null) {
                        ExpiryWait(
                            timeSource = timeSource,
                            enableExpiryWait = enableExpiryWait,
                            pan = mostLikelyPan,
                        )
                    } else {
                        Finished(
                            timeSource = timeSource,
                            pan = mostLikelyPan,
                            expiryMonth = mostLikelyExpiry?.month,
                            expiryYear = mostLikelyExpiry?.year,
                        )
                    }
                isTimedOut() ->
                    Finished(
                        timeSource = timeSource,
                        pan = mostLikelyPan,
                        expiryMonth = mostLikelyExpiry?.month,
                        expiryYear = mostLikelyExpiry?.year,
                    )
                isNoCardVisible() ->
                    Initial(timeSource, enableExpiryWait)
                else -> this
            }
        }
    }

    class ExpiryWait(
        timeSource: TimeSource,
        enableExpiryWait: Boolean = false,
        private val pan: String,
    ) : MainLoopState(timeSource, enableExpiryWait) {

        private val expiryCounter = ItemCounter<CardOcr.Expiry>(null)

        private val mostLikelyExpiry: CardOcr.Expiry?
            get() = expiryCounter.getHighestCountItemOrNull()?.item

        private fun isTimedOut() = reachedStateAt.elapsedNow() > EXPIRY_WAIT_DURATION

        override suspend fun consumeTransition(
            transition: CardOcr.Prediction
        ): MainLoopState {
            if (transition.expiryMonth != null && transition.expiryYear != null) {
                expiryCounter.countItem(
                    CardOcr.Expiry(month = transition.expiryMonth, year = transition.expiryYear)
                )
            }

            val expiry = mostLikelyExpiry
            val timedOut = isTimedOut()
            return if (expiry != null || timedOut) {
                Finished(
                    timeSource = timeSource,
                    pan = pan,
                    expiryMonth = expiry?.month,
                    expiryYear = expiry?.year,
                )
            } else {
                this
            }
        }
    }

    class Finished(
        timeSource: TimeSource,
        val pan: String,
        val expiryMonth: Int? = null,
        val expiryYear: Int? = null,
    ) : MainLoopState(timeSource) {
        override suspend fun consumeTransition(
            transition: CardOcr.Prediction
        ): MainLoopState = this
    }
}
