package com.stripe.android.stripecardscan.cardscan.result

import com.stripe.android.camera.framework.time.Clock
import com.stripe.android.camera.framework.time.seconds
import com.stripe.android.stripecardscan.framework.MachineState
import com.stripe.android.stripecardscan.framework.util.ItemCounter
import com.stripe.android.stripecardscan.payment.ml.SSDOcr

internal sealed class MainLoopState : MachineState() {

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
        transition: SSDOcr.Prediction
    ): MainLoopState

    class Initial : MainLoopState() {
        override suspend fun consumeTransition(
            transition: SSDOcr.Prediction
        ): MainLoopState = if (transition.pan.isNullOrEmpty()) {
            this
        } else {
            OcrFound(
                pan = transition.pan
            )
        }
    }

    class OcrFound private constructor(
        private val panCounter: ItemCounter<String>
    ) : MainLoopState() {

        internal constructor(
            pan: String
        ) : this(
            ItemCounter(pan)
        )

        private val mostLikelyPan: String
            get() = panCounter.getHighestCountItem().second

        private var lastCardVisible = Clock.markNow()

        private fun highestOcrCount() = panCounter.getHighestCountItem().first
        private fun isOcrSatisfied() = highestOcrCount() >= DESIRED_OCR_AGREEMENT
        private fun isTimedOut() = reachedStateAt.elapsedSince() > OCR_SEARCH_DURATION
        private fun isNoCardVisible() = lastCardVisible.elapsedSince() > NO_CARD_VISIBLE_DURATION

        override suspend fun consumeTransition(
            transition: SSDOcr.Prediction
        ): MainLoopState {
            val transitionPan = transition.pan
            if (!transitionPan.isNullOrEmpty()) {
                panCounter.countItem(transitionPan)
                lastCardVisible = Clock.markNow()
            }

            val pan = mostLikelyPan

            return when {
                isOcrSatisfied() || isTimedOut() ->
                    Finished(
                        pan = pan
                    )
                isNoCardVisible() ->
                    Initial()
                else -> this
            }
        }
    }

    class Finished(val pan: String) : MainLoopState() {
        override suspend fun consumeTransition(
            transition: SSDOcr.Prediction
        ): MainLoopState = this
    }
}
