package com.stripe.android.identity.networking

import android.os.Parcelable
import com.stripe.android.identity.states.FaceDetectorTransitioner
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * Indicates the update states of images for selfie.
 */
@Parcelize
internal data class SelfieUploadState(
    val firstHighResResult: Resource<UploadedResult> = Resource.idle(),
    val firstLowResResult: Resource<UploadedResult> = Resource.idle(),
    val lastHighResResult: Resource<UploadedResult> = Resource.idle(),
    val lastLowResResult: Resource<UploadedResult> = Resource.idle(),
    val bestHighResResult: Resource<UploadedResult> = Resource.idle(),
    val bestLowResResult: Resource<UploadedResult> = Resource.idle()
) : Parcelable {

    @IgnoredOnParcel
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

    fun updateLoading(
        isHighRes: Boolean,
        selfie: FaceDetectorTransitioner.Selfie
    ) = when (selfie) {
        FaceDetectorTransitioner.Selfie.FIRST -> {
            if (isHighRes) {
                this.copy(
                    firstHighResResult = Resource.loading()
                )
            } else {
                this.copy(
                    firstLowResResult = Resource.loading()
                )
            }
        }
        FaceDetectorTransitioner.Selfie.BEST -> {
            if (isHighRes) {
                this.copy(
                    bestHighResResult = Resource.loading()
                )
            } else {
                this.copy(
                    bestLowResResult = Resource.loading()
                )
            }
        }
        FaceDetectorTransitioner.Selfie.LAST -> {
            if (isHighRes) {
                this.copy(
                    lastHighResResult = Resource.loading()
                )
            } else {
                this.copy(
                    lastLowResResult = Resource.loading()
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

    fun isIdle() = allResults.all { it.status == Status.IDLE }
}
