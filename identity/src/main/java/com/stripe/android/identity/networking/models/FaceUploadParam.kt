package com.stripe.android.identity.networking.models

import android.os.Parcelable
import android.util.Base64
import com.stripe.android.identity.states.FaceDetectorTransitioner
import com.stripe.android.identity.utils.roundToMaxDecimals
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.pow
import kotlin.math.round

@Serializable
@Parcelize
internal data class FaceUploadParam(
    @SerialName("best_high_res_image")
    val bestHighResImage: String,
    @SerialName("best_low_res_image")
    val bestLowResImage: String,
    @SerialName("first_high_res_image")
    val firstHighResImage: String,
    @SerialName("first_low_res_image")
    val firstLowResImage: String,
    @SerialName("last_high_res_image")
    val lastHighResImage: String,
    @SerialName("last_low_res_image")
    val lastLowResImage: String,
    @SerialName("best_face_score")
    val bestFaceScore: Float,
    @SerialName("face_score_variance")
    val faceScoreVariance: Float,
    @SerialName("num_frames")
    val numFrames: Int,
    @SerialName("best_exposure_duration")
    val bestExposureDuration: Int? = null,
    @SerialName("best_brightness_value")
    val bestBrightnessValue: Float? = null,
    @SerialName("best_camera_lens_model")
    val bestCameraLensModel: String? = null,
    @SerialName("best_focal_length")
    val bestFocalLength: Float? = null,
    @SerialName("best_is_virtual_camera")
    val bestIsVirtualCamera: Boolean? = null,
    @SerialName("best_exposure_iso")
    val bestExposureIso: Float? = null,
    @SerialName("training_consent")
    val trainingConsent: Boolean? = null,
    @SerialName("left_high_res_image")
    val leftHighResImage: String? = null,
    @SerialName("right_high_res_image")
    val rightHighResImage: String? = null,
    @SerialName("best_frame_data")
    val bestFrameData: FaceFrameDataParam? = null,
    @SerialName("first_frame_data")
    val firstFrameData: FaceFrameDataParam? = null,
    @SerialName("last_frame_data")
    val lastFrameData: FaceFrameDataParam? = null,
    @SerialName("left_frame_data")
    val leftFrameData: FaceFrameDataParam? = null,
    @SerialName("right_frame_data")
    val rightFrameData: FaceFrameDataParam? = null
) : Parcelable

@Serializable
@Parcelize
internal data class FaceFrameDataParam(
    @SerialName("face_score")
    val faceScore: Float? = null,
    @SerialName("face_score_variance")
    val faceScoreVariance: Float? = null,
    @SerialName("blur_score")
    val blurScore: Float? = null,
    @SerialName("blur_score_variance")
    val blurScoreVariance: Float? = null,
    @SerialName("yaw")
    val yaw: Float? = null,
    @SerialName("pitch")
    val pitch: Float? = null,
    @SerialName("roll")
    val roll: Float? = null,
    @SerialName("bbox")
    val bbox: List<Int>? = null,
    @SerialName("input_size")
    val inputSize: List<Int>? = null,
    @SerialName("face_landmark_result")
    val faceLandmarkResult: String? = null,
    @SerialName("captured_at")
    val capturedAt: Long? = null,
    @SerialName("capture_order")
    val captureOrder: Int? = null,
    @SerialName("camera_info")
    val cameraInfo: String? = null
) : Parcelable {
    internal companion object {
        private const val MAX_ENCODED_FACE_LANDMARK_RESULT_LENGTH = 5000
        private const val FACE_LANDMARK_SCORE_PRECISION = 4
        private const val MAX_UPLOAD_FLOAT_DECIMALS = 2
        private const val CAMERA_LABEL = "cameraLabel"
        private const val CATEGORIES = "categories"
        private const val SCORE = "score"
        private const val CATEGORY_NAME = "category_name"
        private const val DISPLAY_NAME = "display_name"

        fun create(
            selfieFrame: FaceDetectorTransitioner.SelfieFrame,
            faceScoreVariance: Float,
            captureOrder: Int?,
            cameraLensModel: String?
        ): FaceFrameDataParam {
            val image = selfieFrame.input.cameraPreviewImage.image
            val boundingBox = selfieFrame.output.boundingBox
            val pose = selfieFrame.output.pose
            return FaceFrameDataParam(
                faceScore = selfieFrame.output.resultScore.roundToMaxDecimals(MAX_UPLOAD_FLOAT_DECIMALS),
                faceScoreVariance = faceScoreVariance.roundToMaxDecimals(MAX_UPLOAD_FLOAT_DECIMALS),
                blurScore = null,
                blurScoreVariance = 1f,
                yaw = pose?.yaw?.roundToMaxDecimals(MAX_UPLOAD_FLOAT_DECIMALS),
                pitch = pose?.pitch?.roundToMaxDecimals(MAX_UPLOAD_FLOAT_DECIMALS),
                roll = pose?.roll?.roundToMaxDecimals(MAX_UPLOAD_FLOAT_DECIMALS),
                bbox = listOf(
                    (boundingBox.left * image.width).toInt(),
                    (boundingBox.top * image.height).toInt(),
                    (boundingBox.width * image.width).toInt(),
                    (boundingBox.height * image.height).toInt()
                ),
                inputSize = listOf(image.width, image.height),
                faceLandmarkResult = compactedFaceLandmarkResult(selfieFrame.output.faceLandmarkResult),
                capturedAt = selfieFrame.capturedAt,
                captureOrder = captureOrder,
                cameraInfo = encodedCameraInfo(cameraLensModel)
            )
        }

        fun compactedFaceLandmarkResult(encodedFaceLandmarkResult: String?): String? {
            encodedFaceLandmarkResult ?: return null
            val compacted = runCatching {
                val decodedJson = String(
                    Base64.decode(encodedFaceLandmarkResult, Base64.DEFAULT),
                    Charsets.UTF_8
                )
                val categories = JSONObject(decodedJson).optJSONArray(CATEGORIES)
                    ?: return encodedFaceLandmarkResult.takeIf {
                        it.length <= MAX_ENCODED_FACE_LANDMARK_RESULT_LENGTH
                    }
                val compactCategories = JSONArray()
                for (index in 0 until categories.length()) {
                    val category = categories.optJSONObject(index) ?: continue
                    if (!category.has(SCORE)) {
                        continue
                    }
                    val compactCategory = JSONObject()
                        .put(SCORE, roundedScore(category.optDouble(SCORE)))
                    val categoryName = category.optString(CATEGORY_NAME).takeIf { it.isNotEmpty() }
                        ?: category.optString(DISPLAY_NAME).takeIf { it.isNotEmpty() }
                    categoryName?.let {
                        compactCategory.put(CATEGORY_NAME, it)
                    }
                    compactCategories.put(compactCategory)
                }
                val compactPayload = JSONObject()
                    .put(CATEGORIES, compactCategories)
                    .toString()
                Base64.encodeToString(
                    compactPayload.toByteArray(Charsets.UTF_8),
                    Base64.NO_WRAP
                )
            }.getOrElse {
                encodedFaceLandmarkResult
            }

            return compacted.takeIf {
                it.length <= MAX_ENCODED_FACE_LANDMARK_RESULT_LENGTH
            }
        }

        private fun roundedScore(score: Double): Double {
            val multiplier = 10.0.pow(FACE_LANDMARK_SCORE_PRECISION)
            return round(score * multiplier) / multiplier
        }

        private fun encodedCameraInfo(cameraLensModel: String?): String? {
            cameraLensModel ?: return null
            val payload = JSONObject()
                .put(CAMERA_LABEL, cameraLensModel)
                .toString()
            return Base64.encodeToString(
                payload.toByteArray(Charsets.UTF_8),
                Base64.NO_WRAP
            )
        }
    }
}
