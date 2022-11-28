package com.stripe.android.stripecardscan.cardimageverification.analyzer

import android.graphics.Bitmap
import android.graphics.Rect
import com.stripe.android.camera.CameraPreviewImage
import com.stripe.android.camera.framework.Analyzer
import com.stripe.android.camera.framework.AnalyzerFactory
import com.stripe.android.stripecardscan.cardimageverification.result.MainLoopState
import com.stripe.android.stripecardscan.payment.ml.CardDetect
import com.stripe.android.stripecardscan.payment.ml.SSDOcr
import kotlinx.coroutines.supervisorScope

internal class MainLoopAnalyzer(
    private val ssdOcr: Analyzer<SSDOcr.Input, Any, SSDOcr.Prediction>?,
    private val cardDetect: Analyzer<CardDetect.Input, Any, CardDetect.Prediction>?
) : Analyzer<MainLoopAnalyzer.Input, MainLoopState, MainLoopAnalyzer.Prediction> {

    data class Input(
        val cameraPreviewImage: CameraPreviewImage<Bitmap>,
        val cardFinder: Rect
    )

    class Prediction(
        val ocr: SSDOcr.Prediction?,
        val card: CardDetect.Prediction?
    ) {
        val isCardVisible = card?.side?.let {
            it == CardDetect.Prediction.Side.NO_PAN || it == CardDetect.Prediction.Side.PAN
        }
    }

    override suspend fun analyze(data: Input, state: MainLoopState): Prediction = supervisorScope {
        val cardResult = if (state.runCardDetect) {
            cardDetect?.analyze(
                CardDetect.cameraPreviewToInput(
                    data.cameraPreviewImage.image,
                    data.cameraPreviewImage.viewBounds,
                    data.cardFinder
                ),
                Unit
            )
        } else {
            null
        }

        val ocrResult = if (state.runOcr) {
            ssdOcr?.analyze(
                SSDOcr.cameraPreviewToInput(
                    data.cameraPreviewImage.image,
                    data.cameraPreviewImage.viewBounds,
                    data.cardFinder
                ),
                Unit
            )
        } else {
            null
        }

        Prediction(
            ocr = ocrResult,
            card = cardResult
        )
    }

    class Factory(
        private val ssdOcrFactory: AnalyzerFactory<
            SSDOcr.Input,
            Any,
            SSDOcr.Prediction,
            out Analyzer<SSDOcr.Input, Any, SSDOcr.Prediction>>,
        private val cardDetectFactory: AnalyzerFactory<
            CardDetect.Input,
            Any,
            CardDetect.Prediction,
            out Analyzer<CardDetect.Input, Any, CardDetect.Prediction>>
    ) : AnalyzerFactory<Input, MainLoopState, Prediction, MainLoopAnalyzer> {
        override suspend fun newInstance() = MainLoopAnalyzer(
            ssdOcr = ssdOcrFactory.newInstance(),
            cardDetect = cardDetectFactory.newInstance()
        )
    }
}
