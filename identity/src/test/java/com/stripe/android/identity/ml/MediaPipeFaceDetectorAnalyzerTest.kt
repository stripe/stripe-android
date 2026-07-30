package com.stripe.android.identity.ml

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

internal class MediaPipeFaceDetectorAnalyzerTest {
    @Test
    fun `rotation matrix pose is returned in degrees`() {
        val yawRadians = Math.toRadians(15.0)
        val cosine = cos(yawRadians).toFloat()
        val sine = sin(yawRadians).toFloat()
        val columnMajorRotationMatrix = floatArrayOf(
            cosine, 0f, -sine, 0f,
            0f, 1f, 0f, 0f,
            sine, 0f, cosine, 0f,
            0f, 0f, 0f, 1f
        )

        val pose = requireNotNull(
            MediaPipeFaceDetectorAnalyzer.rotationMatrixToFacePose(columnMajorRotationMatrix)
        )

        assertThat(pose.yaw).isWithin(0.001f).of(15f)
        assertThat(pose.pitch).isWithin(0.001f).of(0f)
        assertThat(pose.roll).isWithin(0.001f).of(0f)
    }
}
