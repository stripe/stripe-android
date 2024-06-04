package com.stripe.android.identity.networking

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Indicates the update states of high and low resolution image of a document.
 */
@Parcelize
internal data class SingleSideDocumentUploadState(
    val highResResult: Resource<UploadedResult> = Resource.idle(),
    val lowResResult: Resource<UploadedResult> = Resource.idle()
) : Parcelable {

    fun updateLoading(
        isHighRes: Boolean
    ) = if (isHighRes) {
        this.copy(
            highResResult = Resource.loading()
        )
    } else {
        this.copy(
            lowResResult = Resource.loading()
        )
    }

    fun update(
        isHighRes: Boolean,
        newResult: UploadedResult
    ) = if (isHighRes) {
        this.copy(
            highResResult = Resource.success(newResult)
        )
    } else {
        this.copy(
            lowResResult = Resource.success(newResult)
        )
    }

    fun updateError(
        isHighRes: Boolean,
        message: String,
        throwable: Throwable
    ) = if (isHighRes) {
        this.copy(
            highResResult = Resource.error(msg = message, throwable = throwable)
        )
    } else {
        this.copy(
            lowResResult = Resource.error(msg = message, throwable = throwable)
        )
    }

    fun isLoading() =
        highResResult.status == Status.LOADING ||
            lowResResult.status == Status.LOADING

    fun hasError() =
        highResResult.status == Status.ERROR ||
            lowResResult.status == Status.ERROR

    fun isUploaded() =
        highResResult.status == Status.SUCCESS &&
            lowResResult.status == Status.SUCCESS

    fun isHighResUploaded() = highResResult.status == Status.SUCCESS

    fun getError(): Throwable {
        StringBuilder().let { errorMessageBuilder ->
            if (highResResult.status == Status.ERROR) {
                errorMessageBuilder.appendLine(highResResult.message)
            }
            if (lowResResult.status == Status.ERROR) {
                errorMessageBuilder.appendLine(lowResResult.message)
            }
            return IllegalStateException(errorMessageBuilder.toString())
        }
    }
}
