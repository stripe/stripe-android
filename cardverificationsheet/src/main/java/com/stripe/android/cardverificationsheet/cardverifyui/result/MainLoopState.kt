package com.stripe.android.cardverificationsheet.cardverifyui.result

import com.stripe.android.cardverificationsheet.cardverifyui.VerifyConfig
import com.stripe.android.cardverificationsheet.cardverifyui.analyzer.MainLoopAnalyzer
import com.stripe.android.cardverificationsheet.framework.MachineState
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
        ): MainLoopState = when (transition.ocr?.outcome) {
            is SSDOcr.OcrOutcome.Mismatch ->
                WrongPanFound(
                    requiredCardIssuer = requiredCardIssuer,
                    requiredLastFour = requiredLastFour,
                )
            is SSDOcr.OcrOutcome.Match ->
                PanFound(
                    matchingPanCount = 1,
                )
            else -> this
        }
    }

    class PanFound(
        private var matchingPanCount: Int,
    ) : MainLoopState(runOcr = true, runCardDetect = true) {
        private var visibleCardCount = 0

        private fun isCardSatisfied() = visibleCardCount >= VerifyConfig.DESIRED_SIDE_COUNT
        private fun isPanSatisfied() =
            matchingPanCount >= VerifyConfig.DESIRED_OCR_AGREEMENT ||
                (
                    matchingPanCount >= VerifyConfig.MINIMUM_PAN_AGREEMENT &&
                        reachedStateAt.elapsedSince() > VerifyConfig.PAN_SEARCH_DURATION
                    )

        override suspend fun consumeTransition(
            transition: MainLoopAnalyzer.Prediction,
        ): MainLoopState {
            if (transition.ocr?.outcome is SSDOcr.OcrOutcome.Match) {
                matchingPanCount++
            }

            if (transition.isCardVisible == true) {
                visibleCardCount++
            }

            return when {
                reachedStateAt.elapsedSince() > VerifyConfig.PAN_AND_CARD_SEARCH_DURATION ->
                    Finished
                isCardSatisfied() && isPanSatisfied() ->
                    Finished
                isCardSatisfied() ->
                    CardSatisfied(
                        matchingPanCount = matchingPanCount
                    )
                isPanSatisfied() ->
                    PanSatisfied(
                        visibleCardCount = visibleCardCount,
                    )
                else -> this
            }
        }
    }

    class PanSatisfied(
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
                reachedStateAt.elapsedSince() > VerifyConfig.PAN_SEARCH_DURATION -> Finished
                isCardSatisfied() -> Finished
                else -> this
            }
        }
    }

    class CardSatisfied(
        private var matchingPanCount: Int,
    ) : MainLoopState(runOcr = true, runCardDetect = false) {

        private fun isPanSatisfied() = matchingPanCount >= VerifyConfig.DESIRED_OCR_AGREEMENT

        override suspend fun consumeTransition(
            transition: MainLoopAnalyzer.Prediction,
        ): MainLoopState {
            if (transition.ocr?.outcome is SSDOcr.OcrOutcome.Match) {
                matchingPanCount++
            }

            return when {
                isPanSatisfied() -> Finished
                reachedStateAt.elapsedSince() >= VerifyConfig.PAN_SEARCH_DURATION -> Finished
                else -> this
            }
        }
    }

    class WrongPanFound(
        override val requiredCardIssuer: CardIssuer?,
        override val requiredLastFour: String,
    ) : MainLoopState(runOcr = true, runCardDetect = false), RequiresMatchingCard {
        override suspend fun consumeTransition(
            transition: MainLoopAnalyzer.Prediction,
        ): MainLoopState = when {
            transition.ocr?.outcome is SSDOcr.OcrOutcome.Mismatch ->
                WrongPanFound(
                    requiredCardIssuer = requiredCardIssuer,
                    requiredLastFour = requiredLastFour,
                )
            transition.ocr?.outcome is SSDOcr.OcrOutcome.Match ->
                PanFound(
                    matchingPanCount = 1,
                )
            reachedStateAt.elapsedSince() >= VerifyConfig.WRONG_CARD_DURATION ->
                Initial(
                    requiredCardIssuer = requiredCardIssuer,
                    requiredLastFour = requiredLastFour,
                )
            else -> this
        }
    }

    object Finished : MainLoopState(runOcr = false, runCardDetect = false) {
        override suspend fun consumeTransition(
            transition: MainLoopAnalyzer.Prediction,
        ): MainLoopState = this
    }
}
