package com.stripe.android.stripecardscan.cardscan.result

import com.stripe.android.stripecardscan.framework.MachineState
import com.stripe.android.stripecardscan.framework.util.ItemCounter
import com.stripe.android.stripecardscan.payment.ml.CardOcr
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

internal sealed class MainLoopState(timeSource: TimeSource) : MachineState(timeSource) {

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
    }

    internal abstract suspend fun consumeTransition(
        transition: CardOcr.Prediction
    ): MainLoopState

    class Initial(timeSource: TimeSource) : MainLoopState(timeSource) {
        override suspend fun consumeTransition(
            transition: CardOcr.Prediction
        ): MainLoopState = if (transition.pan.isNullOrEmpty()) {
            this
        } else {
            OcrFound(
                timeSource = timeSource,
                pan = transition.pan,
                expiryMonth = transition.expiryMonth,
                expiryYear = transition.expiryYear,
            )
        }
    }

    class OcrFound private constructor(
        timeSource: TimeSource,
        private val panCounter: ItemCounter<String>,
        private var expiryCounter: ItemCounter<CardOcr.Expiry>?,
    ) : MainLoopState(timeSource) {

        internal constructor(
            timeSource: TimeSource,
            pan: String,
            expiryMonth: Int? = null,
            expiryYear: Int? = null,
        ) : this(
            timeSource,
            ItemCounter(pan),
            if (expiryMonth != null && expiryYear != null) {
                ItemCounter(CardOcr.Expiry(month = expiryMonth, year = expiryYear))
            } else {
                null
            }
        )

        private val mostLikelyPan: String
            get() = panCounter.getHighestCountItem().second

        private val mostLikelyExpiry: CardOcr.Expiry?
            get() = expiryCounter?.getHighestCountItem()?.second

        private var lastCardVisible = TimeSource.Monotonic.markNow()

        private fun highestOcrCount() = panCounter.getHighestCountItem().first
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
                val expiry = CardOcr.Expiry(month = transition.expiryMonth, year = transition.expiryYear)
                val counter = expiryCounter
                if (counter != null) {
                    counter.countItem(expiry)
                } else {
                    expiryCounter = ItemCounter(expiry)
                }
            }

            return when {
                isOcrSatisfied() || isTimedOut() ->
                    Finished(
                        timeSource = timeSource,
                        pan = mostLikelyPan,
                        expiryMonth = mostLikelyExpiry?.month,
                        expiryYear = mostLikelyExpiry?.year,
                    )
                isNoCardVisible() ->
                    Initial(timeSource)
                else -> this
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
