package com.stripe.android.stripecardscan.cardimageverification.result

import com.stripe.android.stripecardscan.cardimageverification.analyzer.MainLoopAnalyzer
import com.stripe.android.stripecardscan.framework.MachineState
import com.stripe.android.camera.framework.time.Clock
import com.stripe.android.camera.framework.time.seconds
import com.stripe.android.stripecardscan.framework.util.ItemCounter
import com.stripe.android.stripecardscan.payment.card.CardIssuer
import com.stripe.android.stripecardscan.payment.card.CardMatchResult
import com.stripe.android.stripecardscan.payment.card.RequiresMatchingCard

internal sealed class MainLoopState(
    val runOcr: Boolean,
    val runCardDetect: Boolean,
) : MachineState() {

    companion object {

        /**
         * The duration after which the scan will reset if no card is visible.
         */
        val NO_CARD_VISIBLE_DURATION = 5.seconds

        /**
         * The maximum duration for which to search for both a card number and good verification images.
         */
        val OCR_AND_CARD_SEARCH_DURATION = 10.seconds

        /**
         * The maximum duration for which to search for a card number after the verification images
         * have been satisfied.
         */
        val OCR_ONLY_SEARCH_DURATION = 10.seconds

        /**
         * The maximum duration for which to search for good verification images after the card number
         * has been found.
         */
        val CARD_ONLY_SEARCH_DURATION = 5.seconds

        /**
         * Display the wrong card notification to the user for this duration.
         */
        val WRONG_CARD_DURATION = 2.seconds

        /**
         * Once this number of frames with matching card numbers are found, stop looking for card
         * numbers.
         */
        const val DESIRED_OCR_AGREEMENT = 3

        /**
         * Once this number of frames with a clearly centered card are found, stop looking for images
         * with clearly centered cards.
         */
        const val DESIRED_CARD_COUNT = 5
    }

    internal abstract suspend fun consumeTransition(
        transition: MainLoopAnalyzer.Prediction,
    ): MainLoopState

    class Initial(
        override val requiredCardIssuer: CardIssuer?,
        override val requiredLastFour: String?,
    ) : MainLoopState(runOcr = true, runCardDetect = false),
        RequiresMatchingCard {
        override suspend fun consumeTransition(
            transition: MainLoopAnalyzer.Prediction,
        ): MainLoopState = when (compareToRequiredCard(transition.ocr?.pan)) {
            is CardMatchResult.NoPan -> this
            is CardMatchResult.Mismatch ->
                WrongCard(
                    pan = transition.ocr?.pan ?: "",
                    requiredCardIssuer = requiredCardIssuer,
                    requiredLastFour = requiredLastFour,
                )
            is CardMatchResult.Match ->
                OcrFound(
                    pan = transition.ocr?.pan ?: "",
                    isCardVisible = transition.isCardVisible,
                    requiredCardIssuer = requiredCardIssuer,
                    requiredLastFour = requiredLastFour,
                )
            is CardMatchResult.NoRequiredCard ->
                OcrFound(
                    pan = transition.ocr?.pan ?: "",
                    isCardVisible = transition.isCardVisible,
                    requiredCardIssuer = requiredCardIssuer,
                    requiredLastFour = requiredLastFour,
                )
        }
    }

    class OcrFound private constructor(
        private val panCounter: ItemCounter<String>,
        private var visibleCardCount: Int,
        private val requiredCardIssuer: CardIssuer?,
        private val requiredLastFour: String?,
    ) : MainLoopState(runOcr = true, runCardDetect = true) {

        internal constructor(
            pan: String,
            isCardVisible: Boolean?,
            requiredCardIssuer: CardIssuer?,
            requiredLastFour: String?,
        ) : this(
            ItemCounter(pan),
            if (isCardVisible == true) 1 else 0,
            requiredCardIssuer,
            requiredLastFour,
        )

        val mostLikelyPan: String
            get() = panCounter.getHighestCountItem().second

        private var lastCardVisible = Clock.markNow()

        private fun highestOcrCount() = panCounter.getHighestCountItem().first
        private fun isCardSatisfied() = visibleCardCount >= DESIRED_CARD_COUNT
        private fun isOcrSatisfied() = highestOcrCount() >= DESIRED_OCR_AGREEMENT
        private fun isTimedOut() = reachedStateAt.elapsedSince() > OCR_AND_CARD_SEARCH_DURATION
        private fun isNoCardVisible() = lastCardVisible.elapsedSince() > NO_CARD_VISIBLE_DURATION

        override suspend fun consumeTransition(
            transition: MainLoopAnalyzer.Prediction,
        ): MainLoopState {
            val transitionPan = transition.ocr?.pan
            if (!transitionPan.isNullOrEmpty()) {
                panCounter.countItem(transitionPan)
                lastCardVisible = Clock.markNow()
            }

            if (transition.isCardVisible == true) {
                visibleCardCount++
                lastCardVisible = Clock.markNow()
            }

            val pan = mostLikelyPan

            return when {
                isCardSatisfied() && isOcrSatisfied() ->
                    Finished(
                        pan = pan,
                    )
                isCardSatisfied() ->
                    CardSatisfied(
                        panCounter = panCounter,
                        requiredCardIssuer = requiredCardIssuer,
                        requiredLastFour = requiredLastFour,
                    )
                isOcrSatisfied() ->
                    OcrSatisfied(
                        pan = pan,
                        visibleCardCount = visibleCardCount,
                    )
                isTimedOut() ->
                    Finished(
                        pan = pan,
                    )
                isNoCardVisible() ->
                    Initial(
                        requiredCardIssuer = requiredCardIssuer,
                        requiredLastFour = requiredLastFour,
                    )
                else -> this
            }
        }
    }

    class OcrSatisfied(
        val pan: String,
        private var visibleCardCount: Int,
    ) : MainLoopState(runOcr = false, runCardDetect = true) {
        private fun isCardSatisfied() = visibleCardCount >= DESIRED_CARD_COUNT
        private fun isTimedOut() = reachedStateAt.elapsedSince() > CARD_ONLY_SEARCH_DURATION

        override suspend fun consumeTransition(
            transition: MainLoopAnalyzer.Prediction,
        ): MainLoopState {
            if (transition.isCardVisible == true) {
                visibleCardCount++
            }

            return when {
                isCardSatisfied() || isTimedOut() -> Finished(pan)
                else -> this
            }
        }
    }

    class CardSatisfied(
        private val panCounter: ItemCounter<String>,
        private val requiredCardIssuer: CardIssuer?,
        private val requiredLastFour: String?,
    ) : MainLoopState(runOcr = true, runCardDetect = false) {

        private var lastCardVisible = Clock.markNow()

        val mostLikelyPan: String
            get() = panCounter.getHighestCountItem().second

        private fun highestOcrCount() = panCounter.getHighestCountItem().first
        private fun isOcrSatisfied() = highestOcrCount() >= DESIRED_OCR_AGREEMENT
        private fun isTimedOut() = reachedStateAt.elapsedSince() > OCR_ONLY_SEARCH_DURATION
        private fun isNoCardVisible() = lastCardVisible.elapsedSince() > NO_CARD_VISIBLE_DURATION

        override suspend fun consumeTransition(
            transition: MainLoopAnalyzer.Prediction,
        ): MainLoopState {
            if (!transition.ocr?.pan.isNullOrEmpty()) {
                panCounter.countItem(transition.ocr?.pan ?: "")
                lastCardVisible = Clock.markNow()
            }

            val pan = mostLikelyPan

            return when {
                isOcrSatisfied() || isTimedOut() ->
                    Finished(
                        pan = pan,
                    )
                isNoCardVisible() ->
                    Initial(
                        requiredCardIssuer = requiredCardIssuer,
                        requiredLastFour = requiredLastFour,
                    )
                else -> this
            }
        }
    }

    class WrongCard(
        val pan: String,
        override val requiredCardIssuer: CardIssuer?,
        override val requiredLastFour: String?,
    ) : MainLoopState(runOcr = true, runCardDetect = false), RequiresMatchingCard {
        private fun isTimedOut() = reachedStateAt.elapsedSince() > WRONG_CARD_DURATION

        override suspend fun consumeTransition(
            transition: MainLoopAnalyzer.Prediction,
        ): MainLoopState {
            val pan = transition.ocr?.pan
            val cardMatch = compareToRequiredCard(pan)

            return when {
                cardMatch is CardMatchResult.Mismatch ->
                    WrongCard(
                        pan = transition.ocr?.pan ?: "",
                        requiredCardIssuer = requiredCardIssuer,
                        requiredLastFour = requiredLastFour,
                    )
                cardMatch is CardMatchResult.Match ->
                    OcrFound(
                        pan = transition.ocr?.pan ?: "",
                        isCardVisible = transition.isCardVisible,
                        requiredCardIssuer = requiredCardIssuer,
                        requiredLastFour = requiredLastFour,
                    )
                cardMatch is CardMatchResult.NoRequiredCard ->
                    OcrFound(
                        pan = transition.ocr?.pan ?: "",
                        isCardVisible = transition.isCardVisible,
                        requiredCardIssuer = requiredCardIssuer,
                        requiredLastFour = requiredLastFour,
                    )
                isTimedOut() ->
                    Initial(
                        requiredCardIssuer = requiredCardIssuer,
                        requiredLastFour = requiredLastFour,
                    )
                else -> this
            }
        }
    }

    class Finished(val pan: String) : MainLoopState(runOcr = false, runCardDetect = false) {
        override suspend fun consumeTransition(
            transition: MainLoopAnalyzer.Prediction,
        ): MainLoopState = this
    }
}
