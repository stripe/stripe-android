package com.stripe.android.stripecardscan.payment.ml

import android.content.Context
import com.stripe.android.stripecardscan.framework.Fetcher
import com.stripe.android.stripecardscan.framework.ResourceFetcher
import com.stripe.android.stripecardscan.payment.ModelManager

// Each scan owns its manager so the cached fetcher cannot retain a previous assignment.
internal class SSDOcrModelManager(private val model: SSDOcrModel) : ModelManager() {
    override fun getModelFetcher(context: Context): Fetcher = object : ResourceFetcher() {
        override val assetFileName: String = model.assetFileName
        override val modelVersion: String = model.version
        override val hash: String = model.hash
        override val hashAlgorithm: String = "SHA-256"
        override val modelClass: String = "ocr"
        override val modelFrameworkVersion: Int = 1
    }
}
