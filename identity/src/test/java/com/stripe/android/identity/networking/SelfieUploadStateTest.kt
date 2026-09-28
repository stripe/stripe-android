package com.stripe.android.identity.networking

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.model.StripeFile
import com.stripe.android.identity.states.FaceDetectorTransitioner
import org.junit.Test

internal class SelfieUploadStateTest {

    @Test
    fun `update sets high res results for front selfies`() {
        val state = SelfieUploadState()

        assertThat(
            state.update(isHighRes = true, newResult = UPLOADED_RESULT, selfie = FaceDetectorTransitioner.Selfie.FIRST)
                .firstHighResResult
        ).isEqualTo(Resource.success(UPLOADED_RESULT))
        assertThat(
            state.update(isHighRes = true, newResult = UPLOADED_RESULT, selfie = FaceDetectorTransitioner.Selfie.BEST)
                .bestHighResResult
        ).isEqualTo(Resource.success(UPLOADED_RESULT))
        assertThat(
            state.update(isHighRes = true, newResult = UPLOADED_RESULT, selfie = FaceDetectorTransitioner.Selfie.LAST)
                .lastHighResResult
        ).isEqualTo(Resource.success(UPLOADED_RESULT))
    }

    @Test
    fun `update sets low res results for front selfies`() {
        val state = SelfieUploadState()

        assertThat(
            state.update(isHighRes = false, newResult = UPLOADED_RESULT, selfie = FaceDetectorTransitioner.Selfie.FIRST)
                .firstLowResResult
        ).isEqualTo(Resource.success(UPLOADED_RESULT))
        assertThat(
            state.update(isHighRes = false, newResult = UPLOADED_RESULT, selfie = FaceDetectorTransitioner.Selfie.BEST)
                .bestLowResResult
        ).isEqualTo(Resource.success(UPLOADED_RESULT))
        assertThat(
            state.update(isHighRes = false, newResult = UPLOADED_RESULT, selfie = FaceDetectorTransitioner.Selfie.LAST)
                .lastLowResResult
        ).isEqualTo(Resource.success(UPLOADED_RESULT))
    }

    @Test
    fun `update sets low res results for side selfies regardless of resolution`() {
        val state = SelfieUploadState()

        val leftUpdated = state.update(
            isHighRes = true,
            newResult = UPLOADED_RESULT,
            selfie = FaceDetectorTransitioner.Selfie.LEFT
        )
        assertThat(leftUpdated.leftFullFrameResult).isEqualTo(Resource.success(UPLOADED_RESULT))
        assertThat(leftUpdated.expectedFrontResultsAreAllIdle()).isTrue()

        val rightUpdated = state.update(
            isHighRes = false,
            newResult = UPLOADED_RESULT,
            selfie = FaceDetectorTransitioner.Selfie.RIGHT
        )
        assertThat(rightUpdated.rightFullFrameResult).isEqualTo(Resource.success(UPLOADED_RESULT))
        assertThat(rightUpdated.expectedFrontResultsAreAllIdle()).isTrue()
    }

    @Test
    fun `update returns a new state without modifying the original`() {
        val state = SelfieUploadState()

        state.update(
            isHighRes = true,
            newResult = UPLOADED_RESULT,
            selfie = FaceDetectorTransitioner.Selfie.FIRST
        )

        assertThat(state.isIdle()).isTrue()
    }

    @Test
    fun `updateError sets error on the matching result`() {
        val state = SelfieUploadState()
        val expectedError = Resource.error<UploadedResult>(msg = ERROR_MESSAGE, throwable = ERROR_THROWABLE)

        assertThat(
            state.updateError(
                isHighRes = true,
                selfie = FaceDetectorTransitioner.Selfie.FIRST,
                message = ERROR_MESSAGE,
                throwable = ERROR_THROWABLE
            ).firstHighResResult
        ).isEqualTo(expectedError)
        assertThat(
            state.updateError(
                isHighRes = false,
                selfie = FaceDetectorTransitioner.Selfie.BEST,
                message = ERROR_MESSAGE,
                throwable = ERROR_THROWABLE
            ).bestLowResResult
        ).isEqualTo(expectedError)
        assertThat(
            state.updateError(
                isHighRes = true,
                selfie = FaceDetectorTransitioner.Selfie.LEFT,
                message = ERROR_MESSAGE,
                throwable = ERROR_THROWABLE
            ).leftFullFrameResult
        ).isEqualTo(expectedError)
    }

    @Test
    fun `updateLoading sets loading on the matching result`() {
        val state = SelfieUploadState()

        assertThat(
            state.updateLoading(isHighRes = true, selfie = FaceDetectorTransitioner.Selfie.FIRST).firstHighResResult
        ).isEqualTo(Resource.loading<UploadedResult>())
        assertThat(
            state.updateLoading(isHighRes = false, selfie = FaceDetectorTransitioner.Selfie.LAST).lastLowResResult
        ).isEqualTo(Resource.loading<UploadedResult>())
        // Side selfies have one full-frame result, so both resolution flags map to the same field.
        assertThat(
            state.updateLoading(
                isHighRes = true,
                selfie = FaceDetectorTransitioner.Selfie.LEFT
            ).leftFullFrameResult
        ).isEqualTo(Resource.loading<UploadedResult>())
        assertThat(
            state.updateLoading(
                isHighRes = false,
                selfie = FaceDetectorTransitioner.Selfie.RIGHT
            ).rightFullFrameResult
        ).isEqualTo(Resource.loading<UploadedResult>())
    }

    @Test
    fun `isAnyLoading is false when nothing is loading`() {
        assertThat(SelfieUploadState().isAnyLoading()).isFalse()
    }

    @Test
    fun `isAnyLoading is true when any result is loading`() {
        val state = SelfieUploadState().updateLoading(
            isHighRes = false,
            selfie = FaceDetectorTransitioner.Selfie.BEST
        )

        assertThat(state.isAnyLoading()).isTrue()
    }

    @Test
    fun `hasError is false when no result errored`() {
        assertThat(SelfieUploadState().hasError()).isFalse()
    }

    @Test
    fun `hasError is true when a front result errored`() {
        val state = SelfieUploadState().updateError(
            isHighRes = true,
            selfie = FaceDetectorTransitioner.Selfie.FIRST,
            message = ERROR_MESSAGE,
            throwable = ERROR_THROWABLE
        )

        assertThat(state.hasError()).isTrue()
    }

    @Test
    fun `hasError includes side results by default`() {
        val state = SelfieUploadState().updateError(
            isHighRes = false,
            selfie = FaceDetectorTransitioner.Selfie.RIGHT,
            message = ERROR_MESSAGE,
            throwable = ERROR_THROWABLE
        )

        assertThat(state.hasError()).isTrue()
        assertThat(state.hasError(sideSelfies = emptyList())).isFalse()
    }

    @Test
    fun `getError concatenates all error messages`() {
        val state = SelfieUploadState()
            .updateError(
                isHighRes = true,
                selfie = FaceDetectorTransitioner.Selfie.FIRST,
                message = "front failed",
                throwable = ERROR_THROWABLE
            )
            .updateError(
                isHighRes = false,
                selfie = FaceDetectorTransitioner.Selfie.LEFT,
                message = "side failed",
                throwable = ERROR_THROWABLE
            )

        val error = state.getError()

        assertThat(error).isInstanceOf(IllegalStateException::class.java)
        assertThat(error.message).contains("front failed")
        assertThat(error.message).contains("side failed")
    }

    @Test
    fun `getError ignores side errors when sideSelfies is empty`() {
        val state = SelfieUploadState().updateError(
            isHighRes = false,
            selfie = FaceDetectorTransitioner.Selfie.LEFT,
            message = "side failed",
            throwable = ERROR_THROWABLE
        )

        assertThat(state.getError(sideSelfies = emptyList()).message).doesNotContain("side failed")
    }

    @Test
    fun `isAllUploaded is false when results are missing`() {
        val state = SelfieUploadState()
            .update(isHighRes = true, newResult = UPLOADED_RESULT, selfie = FaceDetectorTransitioner.Selfie.FIRST)
            .update(isHighRes = false, newResult = UPLOADED_RESULT, selfie = FaceDetectorTransitioner.Selfie.FIRST)
            .update(isHighRes = true, newResult = UPLOADED_RESULT, selfie = FaceDetectorTransitioner.Selfie.BEST)
            .update(isHighRes = false, newResult = UPLOADED_RESULT, selfie = FaceDetectorTransitioner.Selfie.BEST)
            .update(isHighRes = true, newResult = UPLOADED_RESULT, selfie = FaceDetectorTransitioner.Selfie.LAST)
            .update(isHighRes = false, newResult = UPLOADED_RESULT, selfie = FaceDetectorTransitioner.Selfie.LAST)

        assertThat(state.isAllUploaded()).isFalse()
        // Front frames are all uploaded, side frames are excluded from this check.
        assertThat(state.isAllUploaded(sideSelfies = emptyList())).isTrue()
    }

    @Test
    fun `isAllUploaded is true when all expected results succeed`() {
        val state = SelfieUploadState()
            .update(isHighRes = true, newResult = UPLOADED_RESULT, selfie = FaceDetectorTransitioner.Selfie.FIRST)
            .update(isHighRes = false, newResult = UPLOADED_RESULT, selfie = FaceDetectorTransitioner.Selfie.FIRST)
            .update(isHighRes = true, newResult = UPLOADED_RESULT, selfie = FaceDetectorTransitioner.Selfie.BEST)
            .update(isHighRes = false, newResult = UPLOADED_RESULT, selfie = FaceDetectorTransitioner.Selfie.BEST)
            .update(isHighRes = true, newResult = UPLOADED_RESULT, selfie = FaceDetectorTransitioner.Selfie.LAST)
            .update(isHighRes = false, newResult = UPLOADED_RESULT, selfie = FaceDetectorTransitioner.Selfie.LAST)
            .update(isHighRes = false, newResult = UPLOADED_RESULT, selfie = FaceDetectorTransitioner.Selfie.LEFT)
            .update(isHighRes = false, newResult = UPLOADED_RESULT, selfie = FaceDetectorTransitioner.Selfie.RIGHT)

        assertThat(state.isAllUploaded()).isTrue()
    }

    @Test
    fun `isIdle is true for a fresh state and false after an update`() {
        val freshState = SelfieUploadState()
        assertThat(freshState.isIdle()).isTrue()

        val updatedState = freshState.updateLoading(
            isHighRes = true,
            selfie = FaceDetectorTransitioner.Selfie.FIRST
        )
        assertThat(updatedState.isIdle()).isFalse()
    }

    @Test
    fun `isIdle ignores side results when sideSelfies is empty`() {
        val state = SelfieUploadState().updateLoading(
            isHighRes = false,
            selfie = FaceDetectorTransitioner.Selfie.LEFT
        )

        assertThat(state.isIdle()).isFalse()
        assertThat(state.isIdle(sideSelfies = emptyList())).isTrue()
    }

    private fun SelfieUploadState.expectedFrontResultsAreAllIdle() = listOf(
        firstHighResResult,
        firstLowResResult,
        lastHighResResult,
        lastLowResResult,
        bestHighResResult,
        bestLowResResult
    ).all { it.status == Status.IDLE }

    private companion object {
        const val ERROR_MESSAGE = "upload failed"
        val ERROR_THROWABLE = IllegalStateException("upload failed")
        val UPLOADED_RESULT = UploadedResult(uploadedStripeFile = StripeFile())
    }
}
