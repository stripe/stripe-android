package com.stripe.android.identity.networking.models

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.model.StripeFile
import com.stripe.android.identity.networking.UploadedResult
import org.junit.Test

internal class CollectedDataParamTest {

    @Test
    fun `createForSelfie maps uploaded file ids and scores`() {
        val face = CollectedDataParam.createForSelfie(
            firstHighResResult = uploadedResult("file_first_high"),
            firstLowResResult = uploadedResult("file_first_low"),
            lastHighResResult = uploadedResult("file_last_high"),
            lastLowResResult = uploadedResult("file_last_low"),
            bestHighResResult = uploadedResult("file_best_high"),
            bestLowResResult = uploadedResult("file_best_low"),
            trainingConsent = true,
            bestFaceScore = BEST_FACE_SCORE,
            faceScoreVariance = FACE_SCORE_VARIANCE,
            numFrames = NUM_FRAMES
        ).face

        assertThat(face).isNotNull()
        requireNotNull(face)
        assertThat(face.firstHighResImage).isEqualTo("file_first_high")
        assertThat(face.firstLowResImage).isEqualTo("file_first_low")
        assertThat(face.lastHighResImage).isEqualTo("file_last_high")
        assertThat(face.lastLowResImage).isEqualTo("file_last_low")
        assertThat(face.bestHighResImage).isEqualTo("file_best_high")
        assertThat(face.bestLowResImage).isEqualTo("file_best_low")
        assertThat(face.bestFaceScore).isEqualTo(BEST_FACE_SCORE)
        assertThat(face.faceScoreVariance).isEqualTo(FACE_SCORE_VARIANCE)
        assertThat(face.numFrames).isEqualTo(NUM_FRAMES)
        assertThat(face.trainingConsent).isTrue()
        assertThat(face.leftHighResImage).isNull()
        assertThat(face.rightHighResImage).isNull()
    }

    @Test
    fun `createForSelfie includes side images when side results are provided`() {
        val face = CollectedDataParam.createForSelfie(
            firstHighResResult = uploadedResult("file_first_high"),
            firstLowResResult = uploadedResult("file_first_low"),
            lastHighResResult = uploadedResult("file_last_high"),
            lastLowResResult = uploadedResult("file_last_low"),
            bestHighResResult = uploadedResult("file_best_high"),
            bestLowResResult = uploadedResult("file_best_low"),
            trainingConsent = false,
            bestFaceScore = BEST_FACE_SCORE,
            faceScoreVariance = FACE_SCORE_VARIANCE,
            numFrames = NUM_FRAMES,
            leftHighResResult = uploadedResult("file_left"),
            rightHighResResult = uploadedResult("file_right")
        ).face

        requireNotNull(face)
        assertThat(face.leftHighResImage).isEqualTo("file_left")
        assertThat(face.rightHighResImage).isEqualTo("file_right")
        assertThat(face.trainingConsent).isFalse()
    }

    @Test
    fun `createForSelfie converts camera metadata and passes through frame data`() {
        val bestFrameData = FaceFrameDataParam(faceScore = 0.9f)
        val firstFrameData = FaceFrameDataParam(faceScore = 0.8f)
        val lastFrameData = FaceFrameDataParam(faceScore = 0.7f)
        val leftFrameData = FaceFrameDataParam(faceScore = 0.6f)
        val rightFrameData = FaceFrameDataParam(faceScore = 0.5f)

        val face = CollectedDataParam.createForSelfie(
            firstHighResResult = uploadedResult("file_first_high"),
            firstLowResResult = uploadedResult("file_first_low"),
            lastHighResResult = uploadedResult("file_last_high"),
            lastLowResResult = uploadedResult("file_last_low"),
            bestHighResResult = uploadedResult("file_best_high"),
            bestLowResResult = uploadedResult("file_best_low"),
            trainingConsent = false,
            bestFaceScore = BEST_FACE_SCORE,
            faceScoreVariance = FACE_SCORE_VARIANCE,
            numFrames = NUM_FRAMES,
            bestCameraLensModel = "wide_angle",
            bestExposureIso = 100f,
            bestFocalLength = 2.5f,
            bestExposureDuration = EXPOSURE_DURATION_MS,
            bestIsVirtualCamera = false,
            bestFrameData = bestFrameData,
            firstFrameData = firstFrameData,
            lastFrameData = lastFrameData,
            leftFrameData = leftFrameData,
            rightFrameData = rightFrameData
        ).face

        requireNotNull(face)
        assertThat(face.bestCameraLensModel).isEqualTo("wide_angle")
        assertThat(face.bestExposureIso).isEqualTo(100f)
        assertThat(face.bestFocalLength).isEqualTo(2.5f)
        assertThat(face.bestExposureDuration).isEqualTo(EXPOSURE_DURATION_MS.toInt())
        assertThat(face.bestIsVirtualCamera).isFalse()
        assertThat(face.bestFrameData).isEqualTo(bestFrameData)
        assertThat(face.firstFrameData).isEqualTo(firstFrameData)
        assertThat(face.lastFrameData).isEqualTo(lastFrameData)
        assertThat(face.leftFrameData).isEqualTo(leftFrameData)
        assertThat(face.rightFrameData).isEqualTo(rightFrameData)
    }

    private fun uploadedResult(fileId: String) = UploadedResult(
        uploadedStripeFile = StripeFile(id = fileId)
    )

    private companion object {
        const val BEST_FACE_SCORE = 0.91f
        const val FACE_SCORE_VARIANCE = 0.04f
        const val NUM_FRAMES = 8
        const val EXPOSURE_DURATION_MS = 33L
    }
}
