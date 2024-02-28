package com.stripe.android.identity.networking

import android.os.Parcelable
import com.stripe.android.core.model.StripeFile
import com.stripe.android.identity.networking.models.DocumentUploadParam
import kotlinx.parcelize.Parcelize

/**
 * Result class for file upload.
 */
@Parcelize
internal data class UploadedResult(
    val uploadedStripeFile: StripeFile,
    val scores: List<Float>? = null,
    val uploadMethod: DocumentUploadParam.UploadMethod? = null
) : Parcelable
