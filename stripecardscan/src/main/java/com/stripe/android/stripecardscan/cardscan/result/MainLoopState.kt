package com.stripe.android.stripecardscan.cardscan.result

import com.stripe.android.stripecardscan.cardscan.CardScanConfig
import com.stripe.android.stripecardscan.framework.MachineState
import com.stripe.android.camera.framework.time.Clock
import com.stripe.android.camera.framework.time.milliseconds
import com.stripe.android.stripecardscan.framework.util.ItemCounter
import com.stripe.android.stripecardscan.payment.ml.SSDOcr

internal sealed class MainLoopState : MachineState() {

    internal abstract suspend fun consumeTransition(
        transition: SSDOcr.Prediction,
    ): MainLoopState

    class Initial : MainLoopState() {
        override suspend fun consumeTransition(
            transition: SSDOcr.Prediction,
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
        private fun isOcrSatisfied() =
            highestOcrCount() >= CardScanConfig.DESIRED_OCR_AGREEMENT
        private fun isTimedOut() =
            reachedStateAt.elapsedSince() >
                CardScanConfig.OCR_SEARCH_DURATION_MILLIS.milliseconds
        private fun isNoCardVisible() =
            lastCardVisible.elapsedSince() >
                CardScanConfig.NO_CARD_VISIBLE_DURATION_MILLIS.milliseconds

        override suspend fun consumeTransition(
            transition: SSDOcr.Prediction,
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
                        pan = pan,
                    )
                isNoCardVisible() ->
                    Initial()
                else -> this
            }
        }
    }

    class Finished(val pan: String) : MainLoopState() {
        override suspend fun consumeTransition(
            transition: SSDOcr.Prediction,
        ): MainLoopState = this
    }
}
