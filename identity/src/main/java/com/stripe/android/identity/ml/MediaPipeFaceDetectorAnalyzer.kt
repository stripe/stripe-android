package com.stripe.android.identity.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.Category
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.stripe.android.camera.framework.Analyzer
import com.stripe.android.camera.framework.AnalyzerFactory
import com.stripe.android.camera.framework.image.cropCenter
import com.stripe.android.camera.framework.image.size
import com.stripe.android.camera.framework.util.maxAspectRatioInSize
import com.stripe.android.identity.analytics.ModelPerformanceTracker
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.utils.roundToMaxDecimals
import org.json.JSONArray
import org.json.JSONObject
import java.io.Closeable
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Analyzer for 3D face capture. This path is intentionally MediaPipe-only and does not use the
 * legacy remote face-detector model.
 */
internal class MediaPipeFaceDetectorAnalyzer(
    context: Context,
    private val modelPerformanceTracker: ModelPerformanceTracker
) : Analyzer<AnalyzerInput, IdentityScanState, AnalyzerOutput>, Closeable {

    private val faceLandmarker = createFaceLandmarker(context)

    override suspend fun analyze(
        data: AnalyzerInput,
        state: IdentityScanState
    ): AnalyzerOutput {
        val preprocessStat = modelPerformanceTracker.trackPreprocess()
        val inputBitmap = data.cameraPreviewImage.image.cropCenter(
            maxAspectRatioInSize(
                data.cameraPreviewImage.image.size(),
                1f
            )
        ).asArgb8888()
        val mpImage = BitmapImageBuilder(inputBitmap).build()
        preprocessStat.trackResult()

        val inferenceStat = modelPerformanceTracker.trackInference()
        val result = faceLandmarker.detect(mpImage)
        inferenceStat.trackResult()

        val landmarks = result.faceLandmarks().firstOrNull()
        val output = if (landmarks.isNullOrEmpty()) {
            FaceDetectorOutput(
                boundingBox = EMPTY_BOUNDING_BOX,
                resultScore = 0f
            )
        } else {
            FaceDetectorOutput(
                boundingBox = landmarks.boundingBox(),
                resultScore = MEDIA_PIPE_FACE_SCORE,
                pose = result.facePose(),
                faceLandmarkResult = result.encodedFaceLandmarkResult()
            )
        }

        Log.d(
            TAG,
            "MediaPipeFaceDetectorAnalyzer output " +
                "score=${output.resultScore}, " +
                "bbox=${output.boundingBox}, " +
                "pose=${output.pose}, " +
                "state=${state::class.simpleName}"
        )
        return output
    }

    override fun close() {
        faceLandmarker.close()
    }

    internal class Factory(
        context: Context,
        private val modelPerformanceTracker: ModelPerformanceTracker
    ) : AnalyzerFactory<
        AnalyzerInput,
        IdentityScanState,
        AnalyzerOutput,
        Analyzer<AnalyzerInput, IdentityScanState, AnalyzerOutput>
        > {
        private val applicationContext = context.applicationContext

        override suspend fun newInstance(): Analyzer<AnalyzerInput, IdentityScanState, AnalyzerOutput> {
            return MediaPipeFaceDetectorAnalyzer(
                applicationContext,
                modelPerformanceTracker
            )
        }
    }

    internal companion object {
        const val MODEL_NAME = "media_pipe_face_detector"
        const val MODEL_ASSET_PATH = "face_landmarker.task"
        private const val DEFAULT_FACE_DETECTION_CONFIDENCE = 0.5f
        private const val DEFAULT_FACE_TRACKING_CONFIDENCE = 0.5f
        private const val DEFAULT_FACE_PRESENCE_CONFIDENCE = 0.5f
        private const val DEFAULT_NUM_FACES = 1
        private const val MEDIA_PIPE_FACE_SCORE = 1f
        private const val MAX_LANDMARK_RESULT_DECIMALS = 4
        private val EMPTY_BOUNDING_BOX = BoundingBox(0f, 0f, 0f, 0f)
        val TAG: String = MediaPipeFaceDetectorAnalyzer::class.java.simpleName

        fun assertAvailable(context: Context) {
            createFaceLandmarker(context).close()
        }

        private fun createFaceLandmarker(context: Context): FaceLandmarker {
            return runCatching {
                val baseOptions = BaseOptions.builder()
                    .setModelAssetPath(MODEL_ASSET_PATH)
                    .build()
                val options = FaceLandmarker.FaceLandmarkerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setMinFaceDetectionConfidence(DEFAULT_FACE_DETECTION_CONFIDENCE)
                    .setMinTrackingConfidence(DEFAULT_FACE_TRACKING_CONFIDENCE)
                    .setMinFacePresenceConfidence(DEFAULT_FACE_PRESENCE_CONFIDENCE)
                    .setNumFaces(DEFAULT_NUM_FACES)
                    .setOutputFaceBlendshapes(true)
                    .setOutputFacialTransformationMatrixes(true)
                    .setRunningMode(RunningMode.IMAGE)
                    .build()
                FaceLandmarker.createFromOptions(context, options)
            }.getOrElse { throwable ->
                throw MediaPipeFaceDetectorUnavailableException(throwable)
            }
        }

        private fun Bitmap.asArgb8888(): Bitmap {
            return if (config == Bitmap.Config.ARGB_8888) {
                this
            } else {
                copy(Bitmap.Config.ARGB_8888, false)
            }
        }

        private fun List<NormalizedLandmark>.boundingBox(): BoundingBox {
            var left = Float.MAX_VALUE
            var top = Float.MAX_VALUE
            var right = Float.MIN_VALUE
            var bottom = Float.MIN_VALUE
            forEach { landmark ->
                left = minOf(left, landmark.x())
                top = minOf(top, landmark.y())
                right = maxOf(right, landmark.x())
                bottom = maxOf(bottom, landmark.y())
            }
            return BoundingBox(
                left = left.coerceIn(0f, 1f),
                top = top.coerceIn(0f, 1f),
                width = (right - left).coerceIn(0f, 1f),
                height = (bottom - top).coerceIn(0f, 1f)
            )
        }

        private fun FaceLandmarkerResult.facePose(): FacePose? {
            val matrix = facialTransformationMatrixes()
                .orElse(emptyList())
                .firstOrNull()
                ?: return null
            if (matrix.size < MATRIX_SIZE) {
                return null
            }

            val r00 = matrix[columnMajorIndex(row = 0, column = 0)]
            val r10 = matrix[columnMajorIndex(row = 1, column = 0)]
            val r20 = matrix[columnMajorIndex(row = 2, column = 0)]
            val r21 = matrix[columnMajorIndex(row = 2, column = 1)]
            val r22 = matrix[columnMajorIndex(row = 2, column = 2)]
            val sy = sqrt(r00 * r00 + r10 * r10)

            return FacePose(
                yaw = atan2(-r20, sy).roundToMaxDecimals(MAX_LANDMARK_RESULT_DECIMALS),
                pitch = atan2(r21, r22).roundToMaxDecimals(MAX_LANDMARK_RESULT_DECIMALS),
                roll = atan2(r10, r00).roundToMaxDecimals(MAX_LANDMARK_RESULT_DECIMALS)
            )
        }

        private fun FaceLandmarkerResult.encodedFaceLandmarkResult(): String {
            val payload = JSONObject()
                .put(CATEGORIES, faceBlendshapes().orElse(emptyList()).firstOrNull().categoriesToJsonArray())
                .put(FACE_LANDMARKS, faceLandmarks().firstOrNull().landmarksToJsonArray())
                .put(
                    FACIAL_TRANSFORMATION_MATRIXES,
                    facialTransformationMatrixes().orElse(emptyList()).toMatrixJsonArray()
                )
                .toString()
            return Base64.encodeToString(
                payload.toByteArray(Charsets.UTF_8),
                Base64.NO_WRAP
            )
        }

        private fun List<Category>?.categoriesToJsonArray(): JSONArray {
            val categories = JSONArray()
            this?.forEach { category ->
                categories.put(
                    JSONObject()
                        .put(CATEGORY_NAME, category.categoryName())
                        .put(DISPLAY_NAME, category.displayName())
                        .put(SCORE, category.score())
                        .put(INDEX, category.index())
                )
            }
            return categories
        }

        private fun List<NormalizedLandmark>?.landmarksToJsonArray(): JSONArray {
            val landmarks = JSONArray()
            this?.forEach { landmark ->
                landmarks.put(
                    JSONObject()
                        .put(X, landmark.x().roundToMaxDecimals(MAX_LANDMARK_RESULT_DECIMALS))
                        .put(Y, landmark.y().roundToMaxDecimals(MAX_LANDMARK_RESULT_DECIMALS))
                        .put(Z, landmark.z().roundToMaxDecimals(MAX_LANDMARK_RESULT_DECIMALS))
                )
            }
            return landmarks
        }

        private fun List<FloatArray>.toMatrixJsonArray(): JSONArray {
            val matrices = JSONArray()
            forEach { matrix ->
                val values = JSONArray()
                matrix.forEach { value ->
                    values.put(value.roundToMaxDecimals(MAX_LANDMARK_RESULT_DECIMALS))
                }
                matrices.put(values)
            }
            return matrices
        }

        private fun columnMajorIndex(row: Int, column: Int): Int = column * MATRIX_WIDTH + row

        private const val MATRIX_WIDTH = 4
        private const val MATRIX_SIZE = 16
        private const val CATEGORIES = "categories"
        private const val FACE_LANDMARKS = "face_landmarks"
        private const val FACIAL_TRANSFORMATION_MATRIXES = "facial_transformation_matrixes"
        private const val CATEGORY_NAME = "category_name"
        private const val DISPLAY_NAME = "display_name"
        private const val SCORE = "score"
        private const val INDEX = "index"
        private const val X = "x"
        private const val Y = "y"
        private const val Z = "z"
    }
}
