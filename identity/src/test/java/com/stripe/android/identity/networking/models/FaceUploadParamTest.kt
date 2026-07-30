package com.stripe.android.identity.networking.models

import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.ml.BoundingBox
import org.junit.Test

internal class FaceUploadParamTest {
    @Test
    fun `portrait model bounding box is mapped into full frame coordinates`() {
        val boundingBox = FaceFrameDataParam.boundingBoxInFullFrame(
            boundingBox = BoundingBox(
                left = 0.1f,
                top = 0.2f,
                width = 0.5f,
                height = 0.6f
            ),
            inputWidth = 1080,
            inputHeight = 1440
        )

        assertThat(boundingBox).containsExactly(108, 396, 540, 648).inOrder()
    }

    @Test
    fun `landscape model bounding box is mapped into full frame coordinates`() {
        val boundingBox = FaceFrameDataParam.boundingBoxInFullFrame(
            boundingBox = BoundingBox(
                left = 0.1f,
                top = 0.2f,
                width = 0.5f,
                height = 0.6f
            ),
            inputWidth = 1440,
            inputHeight = 1080
        )

        assertThat(boundingBox).containsExactly(288, 216, 540, 648).inOrder()
    }
}
