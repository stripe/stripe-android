package com.stripe.android.stripecardscan.payment.ml

import com.stripe.android.stripecardscan.payment.ml.ssd.OcrFeatureMapSizes

internal enum class SSDOcrModel(
    val analyticsValue: String,
    val assetFileName: String,
    val version: String,
    val hash: String,
    val featureMapSizes: OcrFeatureMapSizes,
) {
    Original(
        analyticsValue = "original",
        assetFileName = "darknite_1_1_1_16.tflite",
        version = "1.1.1.16",
        hash = "8d8e3f79aa0783ab0cfa5c8d65d663a9da6ba99401efb2298aaaee387c3b00d6",
        featureMapSizes = OcrFeatureMapSizes(
            layerOneWidth = 38,
            layerOneHeight = 24,
            layerTwoWidth = 19,
            layerTwoHeight = 12,
        ),
    ),
    Compact(
        analyticsValue = "compact",
        assetFileName = "embossed_fine_w8.tflite",
        version = "embossed-fine-w8-2026-11-02",
        hash = "a87452ad7ce36c7e65b65c248927476e6fbc8261e886544f0c11cff443282fa9",
        featureMapSizes = OcrFeatureMapSizes(
            layerOneWidth = 38,
            layerOneHeight = 24,
            layerTwoWidth = 0,
            layerTwoHeight = 0,
        ),
    );

    val numberOfPriors: Int
        get() = with(featureMapSizes) {
            (layerOneWidth * layerOneHeight + layerTwoWidth * layerTwoHeight) * PRIORS_PER_ACTIVATION
        }

    companion object {
        const val PRIORS_PER_ACTIVATION = 3
    }
}
