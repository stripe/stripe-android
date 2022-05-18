package com.stripe.android.identity.networking

import com.stripe.android.core.model.StripeFile
import com.stripe.android.identity.networking.models.DocumentUploadParam

/**
 * Result class for file upload.
 */
internal data class UploadedResult(
    val uploadedStripeFile: StripeFile,
    val scores: List<Float>? = null,
    val uploadMethod: DocumentUploadParam.UploadMethod? = null
)