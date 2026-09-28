package com.stripe.android.identity.ml

import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.utils.roundToMaxDecimals
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

internal class MediaPipeFaceDetectorAnalyzerTest {
    @Test
    fun `rotation matrix pose is returned in degrees`() {
        val pose = requireNotNull(
            MediaPipeFaceDetectorAnalyzer.rotationMatrixToFacePose(
                yawRotationMatrix(Math.toRadians(15.0))
            )
        )

        assertThat(pose.yaw).isWithin(0.001f).of(15f)
        assertThat(pose.pitch).isWithin(0.001f).of(0f)
        assertThat(pose.roll).isWithin(0.001f).of(0f)
    }

    @Test
    fun `rotation matrix with fewer than 16 elements returns null`() {
        assertThat(
            MediaPipeFaceDetectorAnalyzer.rotationMatrixToFacePose(FloatArray(15))
        ).isNull()
        assertThat(
            MediaPipeFaceDetectorAnalyzer.rotationMatrixToFacePose(floatArrayOf())
        ).isNull()
    }

    @Test
    fun `negative yaw is returned in degrees`() {
        val pose = requireNotNull(
            MediaPipeFaceDetectorAnalyzer.rotationMatrixToFacePose(
                yawRotationMatrix(Math.toRadians(-15.0))
            )
        )

        assertThat(pose.yaw).isWithin(0.001f).of(-15f)
        assertThat(pose.pitch).isWithin(0.001f).of(0f)
        assertThat(pose.roll).isWithin(0.001f).of(0f)
    }

    @Test
    fun `rotation about x axis is returned as pitch in degrees`() {
        val pitchRadians = Math.toRadians(15.0)
        val cosine = cos(pitchRadians).toFloat()
        val sine = sin(pitchRadians).toFloat()
        val columnMajorRotationMatrix = floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, cosine, sine, 0f,
            0f, -sine, cosine, 0f,
            0f, 0f, 0f, 1f
        )

        val pose = requireNotNull(
            MediaPipeFaceDetectorAnalyzer.rotationMatrixToFacePose(columnMajorRotationMatrix)
        )

        assertThat(pose.pitch).isWithin(0.001f).of(15f)
        assertThat(pose.yaw).isWithin(0.001f).of(0f)
        assertThat(pose.roll).isWithin(0.001f).of(0f)
    }

    @Test
    fun `rotation about z axis is returned as roll in degrees`() {
        val rollRadians = Math.toRadians(15.0)
        val cosine = cos(rollRadians).toFloat()
        val sine = sin(rollRadians).toFloat()
        val columnMajorRotationMatrix = floatArrayOf(
            cosine, sine, 0f, 0f,
            -sine, cosine, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f
        )

        val pose = requireNotNull(
            MediaPipeFaceDetectorAnalyzer.rotationMatrixToFacePose(columnMajorRotationMatrix)
        )

        assertThat(pose.roll).isWithin(0.001f).of(15f)
        assertThat(pose.yaw).isWithin(0.001f).of(0f)
        assertThat(pose.pitch).isWithin(0.001f).of(0f)
    }

    @Test
    fun `pose values are rounded to four decimals`() {
        val yawRadians = 0.1234567
        val pose = requireNotNull(
            MediaPipeFaceDetectorAnalyzer.rotationMatrixToFacePose(
                yawRotationMatrix(yawRadians)
            )
        )

        val unroundedYaw = Math.toDegrees(yawRadians).toFloat()
        assertThat(pose.yaw).isNotEqualTo(unroundedYaw)
        assertThat(pose.yaw).isEqualTo(unroundedYaw.roundToMaxDecimals(4))
    }

    @Test
    fun `bounding box maps to full frame for landscape input`() {
        val fullFrame = toFullFrame(
            BoundingBox(0.1f, 0.2f, 0.5f, 0.6f),
            inputWidth = 200,
            inputHeight = 100
        )

        assertThat(fullFrame.left).isWithin(0.0001f).of(0.3f)
        assertThat(fullFrame.top).isWithin(0.0001f).of(0.2f)
        assertThat(fullFrame.width).isWithin(0.0001f).of(0.25f)
        assertThat(fullFrame.height).isWithin(0.0001f).of(0.6f)
    }

    @Test
    fun `bounding box maps to full frame for portrait input`() {
        val fullFrame = toFullFrame(
            BoundingBox(0.1f, 0.2f, 0.5f, 0.6f),
            inputWidth = 100,
            inputHeight = 200
        )

        assertThat(fullFrame.left).isWithin(0.0001f).of(0.1f)
        assertThat(fullFrame.top).isWithin(0.0001f).of(0.35f)
        assertThat(fullFrame.width).isWithin(0.0001f).of(0.5f)
        assertThat(fullFrame.height).isWithin(0.0001f).of(0.3f)
    }

    @Test
    fun `bounding box mapping is identity for square input`() {
        val fullFrame = toFullFrame(
            BoundingBox(0.1f, 0.2f, 0.5f, 0.6f),
            inputWidth = 100,
            inputHeight = 100
        )

        assertThat(fullFrame.left).isWithin(0.0001f).of(0.1f)
        assertThat(fullFrame.top).isWithin(0.0001f).of(0.2f)
        assertThat(fullFrame.width).isWithin(0.0001f).of(0.5f)
        assertThat(fullFrame.height).isWithin(0.0001f).of(0.6f)
    }

    @Test
    fun `toFullFrame returns original box for zero-size input`() {
        val box = BoundingBox(0.1f, 0.2f, 0.5f, 0.6f)

        assertThat(toFullFrame(box, inputWidth = 0, inputHeight = 0)).isSameInstanceAs(box)
    }

    private fun toFullFrame(
        box: BoundingBox,
        inputWidth: Int,
        inputHeight: Int
    ): BoundingBox = with(MediaPipeFaceDetectorAnalyzer) {
        box.toFullFrame(inputWidth = inputWidth, inputHeight = inputHeight)
    }

    private fun yawRotationMatrix(yawRadians: Double): FloatArray {
        val cosine = cos(yawRadians).toFloat()
        val sine = sin(yawRadians).toFloat()
        return floatArrayOf(
            cosine, 0f, -sine, 0f,
            0f, 1f, 0f, 0f,
            sine, 0f, cosine, 0f,
            0f, 0f, 0f, 1f
        )
    }
}
