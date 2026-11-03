package com.stripe.android.stripecardscan.payment.ml

import android.content.Context
import com.stripe.android.stripecardscan.framework.Fetcher
import com.stripe.android.stripecardscan.framework.ResourceFetcher
import com.stripe.android.stripecardscan.payment.ModelManager

private const val OCR_ASSET_FULL = "embossed_fine_w8.tflite"

internal object SSDOcrModelManager : ModelManager() {
    override fun getModelFetcher(context: Context): Fetcher = object : ResourceFetcher() {
        override val assetFileName: String = OCR_ASSET_FULL
        override val modelVersion: String = "embossed-fine-w8-2026-11-02"
        override val hash: String =
            "a87452ad7ce36c7e65b65c248927476e6fbc8261e886544f0c11cff443282fa9"
        override val hashAlgorithm: String = "SHA-256"
        override val modelClass: String = "ocr"
        override val modelFrameworkVersion: Int = 1
    }
}
