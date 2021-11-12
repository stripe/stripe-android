package com.stripe.android.cardverificationsheet.cardverifyui.result

import com.stripe.android.cardverificationsheet.cardverifyui.VerifyConfig
import com.stripe.android.cardverificationsheet.cardverifyui.analyzer.MainLoopAnalyzer
import com.stripe.android.cardverificationsheet.framework.MachineState
import com.stripe.android.cardverificationsheet.framework.time.Clock
import com.stripe.android.cardverificationsheet.framework.util.ItemCounter
import com.stripe.android.cardverificationsheet.payment.card.CardIssuer
import com.stripe.android.cardverificationsheet.payment.card.CardMatch
import com.stripe.android.cardverificationsheet.payment.card.RequiresMatchingCard

internal sealed class MainLoopState(
    val runOcr: Boolean,
    val runCardDetect: Boolean,
) : MachineState() {

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
            is CardMatch.NoPan -> this
            is CardMatch.Mismatch ->
                WrongCard(
                    pan = transition.ocr?.pan ?: "",
                    requiredCardIssuer = requiredCardIssuer,
                    requiredLastFour = requiredLastFour,
                )
            is CardMatch.Match ->
                OcrFound(
                    pan = transition.ocr?.pan ?: "",
                    isCardVisible = transition.isCardVisible,
                    requiredCardIssuer = requiredCardIssuer,
                    requiredLastFour = requiredLastFour,
                )
            is CardMatch.NoRequiredCard ->
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
        override val requiredCardIssuer: CardIssuer?,
        override val requiredLastFour: String?,
    ) : MainLoopState(runOcr = true, runCardDetect = true), RequiresMatchingCard {

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
        private fun isCardSatisfied() = visibleCardCount >= VerifyConfig.DESIRED_CARD_COUNT
        private fun isOcrSatisfied() = highestOcrCount() >= VerifyConfig.DESIRED_OCR_AGREEMENT
        private fun isTimedOut() =
            reachedStateAt.elapsedSince() > VerifyConfig.OCR_AND_CARD_SEARCH_DURATION
        private fun isNoCardVisible() =
            lastCardVisible.elapsedSince() > VerifyConfig.NO_CARD_VISIBLE_DURATION

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
            val cardMatch = compareToRequiredCard(pan)

            return when {
                cardMatch is CardMatch.Mismatch ->
                    WrongCard(
                        pan = pan,
                        requiredCardIssuer = requiredCardIssuer,
                        requiredLastFour = requiredLastFour,
                    )
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
        private fun isCardSatisfied() = visibleCardCount >= VerifyConfig.DESIRED_CARD_COUNT
        private fun isTimedOut() =
            reachedStateAt.elapsedSince() > VerifyConfig.CARD_ONLY_SEARCH_DURATION

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
        override val requiredCardIssuer: CardIssuer?,
        override val requiredLastFour: String?,
    ) : MainLoopState(runOcr = true, runCardDetect = false), RequiresMatchingCard {

        private var lastCardVisible = Clock.markNow()

        val mostLikelyPan: String
            get() = panCounter.getHighestCountItem().second

        private fun highestOcrCount() = panCounter.getHighestCountItem().first
        private fun isOcrSatisfied() = highestOcrCount() >= VerifyConfig.DESIRED_OCR_AGREEMENT
        private fun isTimedOut() =
            reachedStateAt.elapsedSince() > VerifyConfig.OCR_ONLY_SEARCH_DURATION
        private fun isNoCardVisible() =
            lastCardVisible.elapsedSince() > VerifyConfig.NO_CARD_VISIBLE_DURATION

        override suspend fun consumeTransition(
            transition: MainLoopAnalyzer.Prediction,
        ): MainLoopState {
            if (!transition.ocr?.pan.isNullOrEmpty()) {
                panCounter.countItem(transition.ocr?.pan ?: "")
                lastCardVisible = Clock.markNow()
            }

            val pan = mostLikelyPan
            val cardMatch = compareToRequiredCard(pan)

            return when {
                cardMatch is CardMatch.Mismatch ->
                    WrongCard(
                        pan = pan,
                        requiredCardIssuer = requiredCardIssuer,
                        requiredLastFour = requiredLastFour,
                    )
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
        private fun isTimedOut() =
            reachedStateAt.elapsedSince() > VerifyConfig.WRONG_CARD_DURATION

        override suspend fun consumeTransition(
            transition: MainLoopAnalyzer.Prediction,
        ): MainLoopState {
            val pan = transition.ocr?.pan
            val cardMatch = compareToRequiredCard(pan)

            return when {
                cardMatch is CardMatch.Mismatch ->
                    WrongCard(
                        pan = transition.ocr?.pan ?: "",
                        requiredCardIssuer = requiredCardIssuer,
                        requiredLastFour = requiredLastFour,
                    )
                cardMatch is CardMatch.Match ->
                    OcrFound(
                        pan = transition.ocr?.pan ?: "",
                        isCardVisible = transition.isCardVisible,
                        requiredCardIssuer = requiredCardIssuer,
                        requiredLastFour = requiredLastFour,
                    )
                cardMatch is CardMatch.NoRequiredCard ->
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
