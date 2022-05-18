package com.stripe.android.identity.networking

/**
 * Indicates the update states of 4 images for documents.
 */
internal data class DocumentUploadState(
    val frontHighResResult: Resource<UploadedResult> = Resource.loading(),
    val frontLowResResult: Resource<UploadedResult> = Resource.loading(),
    val backHighResResult: Resource<UploadedResult> = Resource.loading(),
    val backLowResResult: Resource<UploadedResult> = Resource.loading()
) {
    fun update(
        isHighRes: Boolean,
        isFront: Boolean,
        newResult: UploadedResult
    ) = if (isHighRes) {
        if (isFront) {
            this.copy(
                frontHighResResult = Resource.success(newResult)
            )
        } else {
            this.copy(
                backHighResResult = Resource.success(newResult)
            )
        }
    } else {
        if (isFront) {
            this.copy(
                frontLowResResult = Resource.success(newResult)
            )
        } else {
            this.copy(
                backLowResResult = Resource.success(newResult)
            )
        }
    }

    fun updateError(
        isHighRes: Boolean,
        isFront: Boolean,
        message: String,
        throwable: Throwable
    ) = if (isHighRes) {
        if (isFront) {
            this.copy(
                frontHighResResult = Resource.error(msg = message, throwable = throwable)
            )
        } else {
            this.copy(
                backHighResResult = Resource.error(msg = message, throwable = throwable)
            )
        }
    } else {
        if (isFront) {
            this.copy(
                frontLowResResult = Resource.error(msg = message, throwable = throwable)
            )
        } else {
            this.copy(
                backLowResResult = Resource.error(msg = message, throwable = throwable)
            )
        }
    }

    fun isAnyLoading() =
        frontHighResResult.status == Status.LOADING ||
            backHighResResult.status == Status.LOADING ||
            frontLowResResult.status == Status.LOADING ||
            backLowResResult.status == Status.LOADING

    fun isFrontLoading() =
        frontHighResResult.status == Status.LOADING ||
            frontLowResResult.status == Status.LOADING

    fun hasError() =
        frontHighResResult.status == Status.ERROR ||
            backHighResResult.status == Status.ERROR ||
            frontLowResResult.status == Status.ERROR ||
            backHighResResult.status == Status.ERROR

    fun getError(): Throwable {
        StringBuilder().let { errorMessageBuilder ->
            if (frontHighResResult.status == Status.ERROR) {
                errorMessageBuilder.appendLine(frontHighResResult.message)
            }
            if (frontLowResResult.status == Status.ERROR) {
                errorMessageBuilder.appendLine(frontLowResResult.message)
            }
            if (backHighResResult.status == Status.ERROR) {
                errorMessageBuilder.appendLine(backHighResResult.message)
            }
            if (backLowResResult.status == Status.ERROR) {
                errorMessageBuilder.appendLine(backLowResResult.message)
            }
            return IllegalStateException(errorMessageBuilder.toString())
        }
    }

    fun isFrontUploaded() =
        frontHighResResult.status == Status.SUCCESS &&
            frontLowResResult.status == Status.SUCCESS

    fun isBothUploaded() =
        frontHighResResult.status == Status.SUCCESS &&
            backHighResResult.status == Status.SUCCESS &&
            frontLowResResult.status == Status.SUCCESS &&
            backLowResResult.status == Status.SUCCESS

    fun isFrontHighResUploaded() =
        frontHighResResult.status == Status.SUCCESS

    fun isBackHighResUploaded() =
        backHighResResult.status == Status.SUCCESS

    fun isHighResUploaded() = isFrontHighResUploaded() && isBackHighResUploaded()
}