package com.stripe.android.identity.networking.models

import android.graphics.Bitmap
import android.os.Build
import android.util.Base64
import com.google.common.truth.Truth.assertThat
import com.stripe.android.camera.CameraPreviewImage
import com.stripe.android.identity.TestApplication
import com.stripe.android.identity.ml.AnalyzerInput
import com.stripe.android.identity.ml.BoundingBox
import com.stripe.android.identity.ml.FaceDetectorOutput
import com.stripe.android.identity.ml.FacePose
import com.stripe.android.identity.states.FaceDetectorTransitioner
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [Build.VERSION_CODES.Q])
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

    @Test
    fun `compactedFaceLandmarkResult returns null for null input`() {
        assertThat(FaceFrameDataParam.compactedFaceLandmarkResult(null)).isNull()
    }

    @Test
    fun `compactedFaceLandmarkResult keeps rounded score and category name`() {
        val encoded = encodePayload(
            categoriesPayload(
                JSONObject()
                    .put("category_name", "browDownLeft")
                    .put("display_name", "Brow Down Left")
                    .put("score", 0.123456789)
                    .put("index", 3)
            )
        )

        val compacted = requireNotNull(
            FaceFrameDataParam.compactedFaceLandmarkResult(encoded)
        )

        val categories = JSONObject(decodePayload(compacted)).getJSONArray("categories")
        assertThat(categories.length()).isEqualTo(1)
        val category = categories.getJSONObject(0)
        assertThat(category.getDouble("score")).isWithin(0.00001).of(0.1235)
        assertThat(category.getString("category_name")).isEqualTo("browDownLeft")
        assertThat(category.has("display_name")).isFalse()
        assertThat(category.has("index")).isFalse()
    }

    @Test
    fun `compactedFaceLandmarkResult falls back to display name when category name is missing`() {
        val encoded = encodePayload(
            categoriesPayload(
                JSONObject()
                    .put("category_name", "")
                    .put("display_name", "Brow Down Left")
                    .put("score", 0.5)
            )
        )

        val compacted = requireNotNull(
            FaceFrameDataParam.compactedFaceLandmarkResult(encoded)
        )

        val category = JSONObject(decodePayload(compacted))
            .getJSONArray("categories")
            .getJSONObject(0)
        assertThat(category.getString("category_name")).isEqualTo("Brow Down Left")
    }

    @Test
    fun `compactedFaceLandmarkResult skips categories without a score`() {
        val encoded = encodePayload(
            categoriesPayload(
                JSONObject().put("category_name", "noScore"),
                JSONObject().put("category_name", "hasScore").put("score", 0.5)
            )
        )

        val compacted = requireNotNull(
            FaceFrameDataParam.compactedFaceLandmarkResult(encoded)
        )

        val categories = JSONObject(decodePayload(compacted)).getJSONArray("categories")
        assertThat(categories.length()).isEqualTo(1)
        assertThat(categories.getJSONObject(0).getString("category_name")).isEqualTo("hasScore")
    }

    @Test
    fun `compactedFaceLandmarkResult returns original when payload cannot be decoded`() {
        val undecodable = "not valid base64 payload"

        assertThat(
            FaceFrameDataParam.compactedFaceLandmarkResult(undecodable)
        ).isEqualTo(undecodable)
    }

    @Test
    fun `compactedFaceLandmarkResult returns original when decoded json has no categories`() {
        val encoded = encodePayload(JSONObject().put("face_landmarks", JSONArray()))

        assertThat(
            FaceFrameDataParam.compactedFaceLandmarkResult(encoded)
        ).isEqualTo(encoded)
    }

    @Test
    fun `compactedFaceLandmarkResult returns null when result exceeds max length`() {
        val oversized = "x".repeat(MAX_ENCODED_LENGTH + 1)

        assertThat(
            FaceFrameDataParam.compactedFaceLandmarkResult(oversized)
        ).isNull()
    }

    @Test
    fun `create maps frame metadata with rounding`() {
        val frameData = FaceFrameDataParam.create(
            selfieFrame = selfieFrame(
                output = FaceDetectorOutput(
                    boundingBox = BoundingBox(0.1f, 0.2f, 0.5f, 0.6f),
                    resultScore = 0.87654f,
                    pose = FacePose(yaw = 1.2345f, pitch = -2.5678f, roll = 0.5f)
                )
            ),
            faceScoreVariance = 0.12345f,
            captureOrder = 2,
            cameraLensModel = "wide_angle"
        )

        assertThat(frameData.faceScore).isWithin(0.0001f).of(0.88f)
        assertThat(frameData.faceScoreVariance).isWithin(0.0001f).of(0.12f)
        assertThat(frameData.blurScore).isNull()
        assertThat(frameData.blurScoreVariance).isEqualTo(1f)
        assertThat(frameData.yaw).isWithin(0.0001f).of(1.23f)
        assertThat(frameData.pitch).isWithin(0.0001f).of(-2.57f)
        assertThat(frameData.roll).isWithin(0.0001f).of(0.5f)
        assertThat(frameData.bbox).containsExactly(60, 20, 50, 60).inOrder()
        assertThat(frameData.inputSize).containsExactly(IMAGE_WIDTH, IMAGE_HEIGHT).inOrder()
        assertThat(frameData.capturedAt).isEqualTo(CAPTURED_AT_MS)
        assertThat(frameData.captureOrder).isEqualTo(2)
        assertThat(
            JSONObject(decodePayload(requireNotNull(frameData.cameraInfo))).getString("cameraLabel")
        ).isEqualTo("wide_angle")
    }

    @Test
    fun `create omits pose and camera info when unavailable`() {
        val frameData = FaceFrameDataParam.create(
            selfieFrame = selfieFrame(
                output = FaceDetectorOutput(
                    boundingBox = BoundingBox(0.1f, 0.2f, 0.5f, 0.6f),
                    resultScore = 0.9f
                )
            ),
            faceScoreVariance = 0.1f,
            captureOrder = null,
            cameraLensModel = null
        )

        assertThat(frameData.yaw).isNull()
        assertThat(frameData.pitch).isNull()
        assertThat(frameData.roll).isNull()
        assertThat(frameData.captureOrder).isNull()
        assertThat(frameData.cameraInfo).isNull()
        assertThat(frameData.faceLandmarkResult).isNull()
    }

    private fun selfieFrame(output: FaceDetectorOutput) = FaceDetectorTransitioner.SelfieFrame(
        input = AnalyzerInput(
            CameraPreviewImage(
                image = Bitmap.createBitmap(IMAGE_WIDTH, IMAGE_HEIGHT, Bitmap.Config.ARGB_8888),
                viewBounds = mock()
            ),
            viewFinderBounds = mock()
        ),
        output = output,
        bestFrameScore = output.resultScore,
        capturedAt = CAPTURED_AT_MS
    )

    private fun categoriesPayload(vararg categories: JSONObject): JSONObject {
        return JSONObject().put(
            "categories",
            JSONArray().apply {
                categories.forEach(::put)
            }
        )
    }

    private fun encodePayload(json: JSONObject): String = Base64.encodeToString(
        json.toString().toByteArray(Charsets.UTF_8),
        Base64.NO_WRAP
    )

    private fun decodePayload(encoded: String): String = String(
        Base64.decode(encoded, Base64.DEFAULT),
        Charsets.UTF_8
    )

    private companion object {
        const val IMAGE_WIDTH = 200
        const val IMAGE_HEIGHT = 100
        const val CAPTURED_AT_MS = 123456789L
        const val MAX_ENCODED_LENGTH = 5000
    }
}
