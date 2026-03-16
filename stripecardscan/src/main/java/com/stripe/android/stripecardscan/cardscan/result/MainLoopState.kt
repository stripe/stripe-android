package com.stripe.android.stripecardscan.cardscan.result

import com.stripe.android.stripecardscan.framework.MachineState
import com.stripe.android.stripecardscan.framework.util.ItemCounter
import com.stripe.android.stripecardscan.payment.ml.SSDOcr
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

        /**
         * The minimum number of identical expiry detections required before accepting an expiry
         * date. This filters out OCR noise where a misread expiry is seen only once.
         */
        const val DESIRED_EXPIRY_AGREEMENT = 2
    }

    internal abstract suspend fun consumeTransition(
        transition: SSDOcr.Prediction
    ): MainLoopState

    class Initial(timeSource: TimeSource) : MainLoopState(timeSource) {
        override suspend fun consumeTransition(
            transition: SSDOcr.Prediction
        ): MainLoopState = if (transition.pan.isNullOrEmpty()) {
            this
        } else {
            OcrFound(
                timeSource = timeSource,
                pan = transition.pan,
                initialExpiry = transition.expiry
            )
        }
    }

    class OcrFound private constructor(
        timeSource: TimeSource,
        private val panCounter: ItemCounter<String>,
        private val expiryCounts: MutableMap<SSDOcr.ExpiryDate, Int>
    ) : MainLoopState(timeSource) {

        internal constructor(
            timeSource: TimeSource,
            pan: String,
            initialExpiry: SSDOcr.ExpiryDate? = null
        ) : this(
            timeSource,
            ItemCounter(pan),
            mutableMapOf<SSDOcr.ExpiryDate, Int>().also { map ->
                if (initialExpiry != null) {
                    map[initialExpiry] = 1
                }
            }
        )

        private val mostLikelyPan: String
            get() = panCounter.getHighestCountItem().second

        internal val mostLikelyExpiry: SSDOcr.ExpiryDate?
            get() = expiryCounts.entries
                .filter { it.value >= DESIRED_EXPIRY_AGREEMENT }
                .maxByOrNull { it.value }
                ?.key

        private var lastCardVisible = TimeSource.Monotonic.markNow()

        private fun highestOcrCount() = panCounter.getHighestCountItem().first
        private fun isOcrSatisfied() = highestOcrCount() >= DESIRED_OCR_AGREEMENT
        private fun isTimedOut() = reachedStateAt.elapsedNow() > OCR_SEARCH_DURATION
        private fun isNoCardVisible() = lastCardVisible.elapsedNow() > NO_CARD_VISIBLE_DURATION

        override suspend fun consumeTransition(
            transition: SSDOcr.Prediction
        ): MainLoopState {
            val transitionPan = transition.pan
            if (!transitionPan.isNullOrEmpty()) {
                panCounter.countItem(transitionPan)
                lastCardVisible = TimeSource.Monotonic.markNow()
            }

            val transitionExpiry = transition.expiry
            if (transitionExpiry != null) {
                expiryCounts[transitionExpiry] = (expiryCounts[transitionExpiry] ?: 0) + 1
            }

            val pan = mostLikelyPan

            return when {
                isOcrSatisfied() || isTimedOut() ->
                    Finished(
                        timeSource = timeSource,
                        pan = pan,
                        expiry = mostLikelyExpiry
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
        val expiry: SSDOcr.ExpiryDate? = null
    ) : MainLoopState(timeSource) {
        override suspend fun consumeTransition(
            transition: SSDOcr.Prediction
        ): MainLoopState = this
    }
}
