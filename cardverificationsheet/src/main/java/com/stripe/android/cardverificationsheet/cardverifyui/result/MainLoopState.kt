package com.stripe.android.cardverificationsheet.cardverifyui.result

import com.stripe.android.cardverificationsheet.cardverifyui.VerifyConfig
import com.stripe.android.cardverificationsheet.cardverifyui.analyzer.MainLoopAnalyzer
import com.stripe.android.cardverificationsheet.framework.MachineState
import com.stripe.android.cardverificationsheet.framework.util.ItemTotalCounter
import com.stripe.android.cardverificationsheet.payment.card.CardIssuer
import com.stripe.android.cardverificationsheet.payment.card.RequiresMatchingCard
import com.stripe.android.cardverificationsheet.payment.ml.SSDOcr

internal sealed class MainLoopState(
    val runOcr: Boolean,
    val runCardDetect: Boolean,
) : MachineState() {

    internal abstract suspend fun consumeTransition(
        transition: MainLoopAnalyzer.Prediction,
    ): MainLoopState

    class Initial(
        override val requiredCardIssuer: CardIssuer?,
        override val requiredLastFour: String,
    ) : MainLoopState(runOcr = true, runCardDetect = false),
        RequiresMatchingCard {
        override suspend fun consumeTransition(
            transition: MainLoopAnalyzer.Prediction,
        ): MainLoopState = when {
            doesNotMatchRequiredCard(transition.ocr?.cardIssuer, transition.ocr?.lastFour) ->
                WrongPanFound(
                    cardIssuer = transition.ocr?.cardIssuer,
                    // doesNotMatchRequiredCard guarantees this is non-null
                    lastFour = transition.ocr?.lastFour!!,
                    requiredCardIssuer = requiredCardIssuer,
                    requiredLastFour = requiredLastFour,
                )
            transition.ocr?.lastFour != null ->
                PanFound(
                    resultCounter = ItemTotalCounter(transition.ocr),
                )
            else -> this
        }
    }

    class PanFound(
        private val resultCounter: ItemTotalCounter<SSDOcr.Prediction>,
    ) : MainLoopState(runOcr = true, runCardDetect = true) {
        private var visibleCardCount = 0

        private fun getMostLikelyResult() = resultCounter.getHighestCountItem()?.second
        fun getMostLikelyCardIssuer() = getMostLikelyResult()?.cardIssuer

        // guaranteed to be non-null since the item total counter is primed
        fun getMostLikelyLastFour() = getMostLikelyResult()?.lastFour!!

        private fun isCardSatisfied() = visibleCardCount >= VerifyConfig.DESIRED_SIDE_COUNT
        private fun isPanSatisfied() =
            resultCounter.getHighestCountItem()?.first ?: 0 >= VerifyConfig.DESIRED_OCR_AGREEMENT ||
                (
                    resultCounter.getHighestCountItem()?.first ?: 0 >=
                        VerifyConfig.MINIMUM_PAN_AGREEMENT &&
                        reachedStateAt.elapsedSince() > VerifyConfig.PAN_SEARCH_DURATION
                    )

        override suspend fun consumeTransition(
            transition: MainLoopAnalyzer.Prediction,
        ): MainLoopState {
            if (transition.ocr?.lastFour != null) {
                resultCounter.countItem(transition.ocr)
            }

            if (transition.isCardVisible == true) {
                visibleCardCount++
            }

            return when {
                reachedStateAt.elapsedSince() > VerifyConfig.PAN_AND_CARD_SEARCH_DURATION ->
                    Finished(
                        cardIssuer = getMostLikelyCardIssuer(),
                        lastFour = getMostLikelyLastFour(),
                    )
                isCardSatisfied() && isPanSatisfied() ->
                    Finished(
                        cardIssuer = getMostLikelyCardIssuer(),
                        lastFour = getMostLikelyLastFour(),
                    )
                isCardSatisfied() ->
                    CardSatisfied(
                        resultCounter = resultCounter
                    )
                isPanSatisfied() ->
                    PanSatisfied(
                        cardIssuer = getMostLikelyCardIssuer(),
                        lastFour = getMostLikelyLastFour(),
                        visibleCardCount = visibleCardCount,
                    )
                else -> this
            }
        }
    }

    class PanSatisfied(
        val cardIssuer: CardIssuer?,
        val lastFour: String,
        private var visibleCardCount: Int,
    ) : MainLoopState(runOcr = false, runCardDetect = true) {
        private fun isCardSatisfied() =
            visibleCardCount >= VerifyConfig.DESIRED_SIDE_COUNT

        override suspend fun consumeTransition(
            transition: MainLoopAnalyzer.Prediction,
        ): MainLoopState {
            if (transition.isCardVisible == true) {
                visibleCardCount++
            }

            return when {
                reachedStateAt.elapsedSince() > VerifyConfig.PAN_SEARCH_DURATION ->
                    Finished(cardIssuer, lastFour)
                isCardSatisfied() ->
                    Finished(cardIssuer, lastFour)
                else ->
                    this
            }
        }
    }

    class CardSatisfied(
        private val resultCounter: ItemTotalCounter<SSDOcr.Prediction>,
    ) : MainLoopState(runOcr = true, runCardDetect = false) {
        private fun getMostLikelyResult() = resultCounter.getHighestCountItem()?.second
        fun getMostLikelyCardIssuer() = getMostLikelyResult()?.cardIssuer

        // guaranteed to be non-null since the item total counter is primed
        fun getMostLikelyLastFour() = getMostLikelyResult()?.lastFour!!

        private fun isPanSatisfied() =
            resultCounter.getHighestCountItem()?.first ?: 0 >= VerifyConfig.DESIRED_OCR_AGREEMENT

        override suspend fun consumeTransition(
            transition: MainLoopAnalyzer.Prediction,
        ): MainLoopState {
            if (transition.ocr?.lastFour != null) {
                resultCounter.countItem(transition.ocr)
            }

            val mostLikelyResult = getMostLikelyResult()
            val mostLikelyCardIssuer = mostLikelyResult?.cardIssuer
            // guaranteed to be non-null since the item total counter is primed
            val mostLikelyLastFour = mostLikelyResult?.lastFour!!
            return when {
                isPanSatisfied() -> Finished(mostLikelyCardIssuer, mostLikelyLastFour)
                reachedStateAt.elapsedSince() >= VerifyConfig.PAN_SEARCH_DURATION ->
                    Finished(mostLikelyCardIssuer, mostLikelyLastFour)
                else -> this
            }
        }
    }

    class WrongPanFound(
        val cardIssuer: CardIssuer?,
        val lastFour: String,
        override val requiredCardIssuer: CardIssuer?,
        override val requiredLastFour: String,
    ) : MainLoopState(runOcr = true, runCardDetect = false), RequiresMatchingCard {
        override suspend fun consumeTransition(
            transition: MainLoopAnalyzer.Prediction,
        ): MainLoopState = when {
            doesNotMatchRequiredCard(transition.ocr?.cardIssuer, transition.ocr?.lastFour) ->
                WrongPanFound(
                    cardIssuer = transition.ocr?.cardIssuer,
                    // doesNotMatchRequiredCard guarantees this is non-null
                    lastFour = transition.ocr?.lastFour!!,
                    requiredCardIssuer = requiredCardIssuer,
                    requiredLastFour = requiredLastFour,
                )
            transition.ocr?.lastFour != null ->
                PanFound(
                    resultCounter = ItemTotalCounter(transition.ocr),
                )
            reachedStateAt.elapsedSince() >= VerifyConfig.WRONG_CARD_DURATION ->
                Initial(
                    requiredCardIssuer = requiredCardIssuer,
                    requiredLastFour = requiredLastFour,
                )
            else -> this
        }
    }

    class Finished(
        val cardIssuer: CardIssuer?,
        val lastFour: String,
    ) : MainLoopState(runOcr = false, runCardDetect = false) {
        override suspend fun consumeTransition(
            transition: MainLoopAnalyzer.Prediction,
        ): MainLoopState = this
    }
}
