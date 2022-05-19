package com.stripe.android.identity.networking

import com.stripe.android.identity.states.FaceDetectorTransitioner

/**
 * Indicates the update states of images for selfie.
 */
internal data class SelfieUploadState(
    val firstHighResResult: Resource<UploadedResult> = Resource.loading(),
    val firstLowResResult: Resource<UploadedResult> = Resource.loading(),
    val lastHighResResult: Resource<UploadedResult> = Resource.loading(),
    val lastLowResResult: Resource<UploadedResult> = Resource.loading(),
    val bestHighResResult: Resource<UploadedResult> = Resource.loading(),
    val bestLowResResult: Resource<UploadedResult> = Resource.loading()
) {

    private val allResults = listOf(
        firstHighResResult,
        firstLowResResult,
        lastHighResResult,
        lastLowResResult,
        bestHighResResult,
        bestLowResResult
    )

    fun update(
        isHighRes: Boolean,
        newResult: UploadedResult,
        selfie: FaceDetectorTransitioner.Selfie
    ) = if (isHighRes) {
        when (selfie) {
            FaceDetectorTransitioner.Selfie.FIRST -> {
                this.copy(
                    firstHighResResult = Resource.success(newResult)
                )
            }
            FaceDetectorTransitioner.Selfie.LAST -> {
                this.copy(
                    lastHighResResult = Resource.success(newResult)
                )
            }
            FaceDetectorTransitioner.Selfie.BEST -> {
                this.copy(
                    bestHighResResult = Resource.success(newResult)
                )
            }
        }
    } else {
        when (selfie) {
            FaceDetectorTransitioner.Selfie.FIRST -> {
                this.copy(
                    firstLowResResult = Resource.success(newResult)
                )
            }
            FaceDetectorTransitioner.Selfie.LAST -> {
                this.copy(
                    lastLowResResult = Resource.success(newResult)
                )
            }
            FaceDetectorTransitioner.Selfie.BEST -> {
                this.copy(
                    bestLowResResult = Resource.success(newResult)
                )
            }
        }
    }

    fun updateError(
        isHighRes: Boolean,
        selfie: FaceDetectorTransitioner.Selfie,
        message: String,
        throwable: Throwable
    ) = if (isHighRes) {
        when (selfie) {
            FaceDetectorTransitioner.Selfie.FIRST -> {
                this.copy(
                    firstHighResResult = Resource.error(msg = message, throwable = throwable)
                )
            }
            FaceDetectorTransitioner.Selfie.LAST -> {
                this.copy(
                    lastHighResResult = Resource.error(msg = message, throwable = throwable)
                )
            }
            FaceDetectorTransitioner.Selfie.BEST -> {
                this.copy(
                    bestHighResResult = Resource.error(msg = message, throwable = throwable)
                )
            }
        }
    } else {
        when (selfie) {
            FaceDetectorTransitioner.Selfie.FIRST -> {
                this.copy(
                    firstLowResResult = Resource.error(msg = message, throwable = throwable)
                )
            }
            FaceDetectorTransitioner.Selfie.LAST -> {
                this.copy(
                    lastLowResResult = Resource.error(msg = message, throwable = throwable)
                )
            }
            FaceDetectorTransitioner.Selfie.BEST -> {
                this.copy(
                    bestLowResResult = Resource.error(msg = message, throwable = throwable)
                )
            }
        }
    }

    fun hasError(): Boolean {
        allResults.forEach { result ->
            if (result.status == Status.ERROR) {
                return true
            }
        }
        return false
    }

    fun getError(): Throwable {
        StringBuilder().let { errorMessageBuilder ->
            allResults.forEach { result ->
                if (result.status == Status.ERROR) {
                    errorMessageBuilder.appendLine(result.message)
                }
            }
            return IllegalStateException(errorMessageBuilder.toString())
        }
    }

    fun isAnyLoading(): Boolean {
        allResults.forEach { result ->
            if (result.status == Status.LOADING) {
                return true
            }
        }
        return false
    }

    fun isAllUploaded(): Boolean {
        allResults.forEach { result ->
            if (result.status != Status.SUCCESS) {
                return false
            }
        }
        return true
    }
}
