package com.stripe.android.identity.states

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import androidx.annotation.IntegerRes
import com.microblink.capture.CaptureSDK
import com.microblink.capture.analysis.FrameAnalysisResult
import com.microblink.capture.analysis.FrameAnalysisStatus
import com.microblink.capture.directapi.AnalysisError
import com.microblink.capture.directapi.AnalyzerRunner
import com.microblink.capture.directapi.FrameAnalysisResultListener
import com.microblink.capture.image.ImageRotation
import com.microblink.capture.image.InputImage
import com.microblink.capture.result.Side
import com.microblink.capture.settings.AnalyzerSettings
import com.microblink.capture.settings.BlurPolicy
import com.microblink.capture.settings.CaptureStrategy
import com.microblink.capture.settings.GlarePolicy
import com.microblink.capture.settings.LightingThresholds
import com.microblink.capture.settings.TiltPolicy
import com.stripe.android.identity.R
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.ml.AnalyzerInput
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import com.stripe.android.identity.networking.models.VerificationPageStaticContentDocumentCaptureMBSettings as MBSettings

internal class MBDetector private constructor(settings: MBSettings) {
    enum class CaptureFeedback(
        @IntegerRes val stringResource: Int?
    ) {
        DOCUMENT_FRAMING_NO_DOCUMENT(null),
        DOCUMENT_FRAMING_CAMERA_TOO_FAR(R.string.stripe_move_closer),
        DOCUMENT_FRAMING_CAMERA_TOO_CLOSE(R.string.stripe_move_farther),
        DOCUMENT_FRAMING_CAMERA_ANGLE_TOO_STEEP(R.string.stripe_align_document),
        DOCUMENT_FRAMING_CAMERA_ORIENTATION_UNSUITABLE(R.string.stripe_rotate_document),
        DOCUMENT_TOO_CLOSE_TO_FRAME_EDGE(R.string.stripe_move_farther),
        LIGHTING_TOO_DARK(R.string.stripe_increase_lighting),
        LIGHTING_TOO_BRIGHT(R.string.stripe_decrease_lighting),
        BLUR_DETECTED(R.string.stripe_reduce_blur),
        GLARE_DETECTED(R.string.stripe_reduce_glare),
        OCCLUDED_BY_HAND(R.string.stripe_keep_fully_visible),
        WRONG_SIDE(R.string.stripe_position_id_back),
        UNKNOWN(null)
    }

    sealed interface DetectorResult {
        data class Captured(
            val original: Bitmap,
            val transformed: Bitmap,
            val isFront: Boolean
        ) : DetectorResult

        data class Capturing(val feedback: CaptureFeedback) : DetectorResult
        data class Error(val reason: Throwable? = null, val message: String? = null) : DetectorResult
    }

    init {
        AnalyzerRunner.reset()
        AnalyzerRunner.settings = AnalyzerSettings(
            captureSingleSide = true,
            returnTransformedDocumentImage = settings.returnTransformedDocumentImage,
            captureStrategy = when (settings.captureStrategy) {
                MBSettings.CaptureStrategy.SINGLE_FRAME -> CaptureStrategy.SingleFrame
                MBSettings.CaptureStrategy.OPTIMIZE_FOR_QUALITY -> CaptureStrategy.OptimizeForQuality
                MBSettings.CaptureStrategy.OPTIMIZE_FOR_SPEED -> CaptureStrategy.OptimizeForSpeed
                MBSettings.CaptureStrategy.DEFAULT -> CaptureStrategy.Default
            },
            minimumDocumentDpi = settings.minimumDocumentDpi,
            adjustMinimumDocumentDpi = settings.adjustMinimumDocumentDpi,
            documentFramingMargin = settings.documentFramingMargin,
            keepMarginOnTransformedDocumentImage = settings.keepMarginOnTransformedDocumentImage,
            lightingThresholds = LightingThresholds(settings.tooDarkThreshold, settings.tooBrightThreshold),
            blurPolicy = when (settings.blurPolicy) {
                MBSettings.BlurPolicy.DISABLED -> BlurPolicy.Disabled
                MBSettings.BlurPolicy.STRICT -> BlurPolicy.Strict
                MBSettings.BlurPolicy.NORMAL -> BlurPolicy.Normal
                MBSettings.BlurPolicy.RELAXED -> BlurPolicy.Relaxed
            },
            glarePolicy = when (settings.glarePolicy) {
                MBSettings.GlarePolicy.DISABLED -> GlarePolicy.Disabled
                MBSettings.GlarePolicy.STRICT -> GlarePolicy.Strict
                MBSettings.GlarePolicy.NORMAL -> GlarePolicy.Normal
                MBSettings.GlarePolicy.RELAXED -> GlarePolicy.Relaxed
            },
            handOcclusionThreshold = settings.handOcclusionThreshold,
            tiltPolicy = when (settings.tiltPolicy) {
                MBSettings.TiltPolicy.DISABLED -> TiltPolicy.Disabled
                MBSettings.TiltPolicy.STRICT -> TiltPolicy.Strict
                MBSettings.TiltPolicy.NORMAL -> TiltPolicy.Normal
                MBSettings.TiltPolicy.RELAXED -> TiltPolicy.Relaxed
            }
        )
    }

    private fun Bitmap.toFullRect() = Rect(0, 0, width, height)

    suspend fun analyze(data: AnalyzerInput): DetectorResult = suspendCoroutine { continuation ->
        val mbInputImage = InputImage.createFromBitmap(
            data.cameraPreviewImage.image,
            ImageRotation.ROTATION_0, // bitmap already rotated
            data.cameraPreviewImage.image.toFullRect() // use the full bitmap Rect as cropRect
        )
        AnalyzerRunner.analyzeStreamImage(
            mbInputImage,
            object : FrameAnalysisResultListener {
                override fun onAnalysisDone(result: FrameAnalysisResult) {
                    continuation.resume(
                        value = when (result.captureState) {
                            FrameAnalysisResult.CaptureState.SideCaptured -> {
                                DetectorResult.Error(message = "MBDetector gets unexpected sideCaptured state")
                            }

                            FrameAnalysisResult.CaptureState.DocumentCaptured -> {
                                val analyzeResult = AnalyzerRunner.detachResult()
                                try {
                                    DetectorResult.Captured(
                                        original = requireNotNull(
                                            analyzeResult.firstCapture?.imageResult?.image?.convertToBitmap()
                                        ),
                                        transformed = requireNotNull(
                                            analyzeResult.firstCapture?.transformedImageResult?.image?.convertToBitmap()
                                        ),
                                        isFront = analyzeResult.firstCapture?.side == Side.Front
                                    )
                                } catch (e: Exception) {
                                    DetectorResult.Error(e)
                                }
                            }

                            FrameAnalysisResult.CaptureState.FirstSideCaptureInProgress -> {
                                DetectorResult.Capturing(result.frameAnalysisStatus.toCaptureFeedback())
                            }

                            FrameAnalysisResult.CaptureState.SecondSideCaptureInProgress -> {
                                DetectorResult.Error(message = "MBDetector incorrectly captures second side")
                            }
                        }
                    )
                }

                override fun onError(error: AnalysisError, exception: Exception) {
                    continuation.resume(
                        value = DetectorResult.Error(reason = exception, message = error.name)
                    )
                }
            }
        )
        mbInputImage.dispose()
    }

    companion object {
        suspend fun maybeCreateMBInstance(
            context: Context,
            settings: MBSettings?,
            identityAnalyticsRequestFactory: IdentityAnalyticsRequestFactory
        ): MBDetector? {
            settings?.let {
                try {
                    CaptureSDK.setLicenseKey(
                        settings.licenseKey,
                        context
                    )
                    identityAnalyticsRequestFactory.mbStatus(
                        true,
                        initSuccess = true
                    )
                    return MBDetector(settings)
                } catch (e: Exception) {
                    identityAnalyticsRequestFactory.mbStatus(
                        true,
                        initSuccess = false,
                        initFailedReason = e.message
                    )
                    return null
                }
            } ?: run {
                identityAnalyticsRequestFactory.mbStatus(false)
                return null
            }
        }
    }

    private fun FrameAnalysisStatus.toCaptureFeedback(): CaptureFeedback {
        return when (this.sideAnalysisStatus) {
            FrameAnalysisStatus.DocumentSideAnalysisStatus.SideAlreadyCaptured ->
                CaptureFeedback.WRONG_SIDE

            else -> null
        } ?: when (this.framingStatus) {
            FrameAnalysisStatus.DocumentFramingStatus.NoDocument ->
                CaptureFeedback.DOCUMENT_FRAMING_NO_DOCUMENT

            FrameAnalysisStatus.DocumentFramingStatus.CameraTooFar ->
                CaptureFeedback.DOCUMENT_FRAMING_CAMERA_TOO_FAR

            FrameAnalysisStatus.DocumentFramingStatus.CameraTooClose ->
                CaptureFeedback.DOCUMENT_FRAMING_CAMERA_TOO_CLOSE

            FrameAnalysisStatus.DocumentFramingStatus.CameraOrientationUnsuitable ->
                CaptureFeedback.DOCUMENT_FRAMING_CAMERA_ORIENTATION_UNSUITABLE

            FrameAnalysisStatus.DocumentFramingStatus.CameraAngleTooSteep ->
                CaptureFeedback.DOCUMENT_FRAMING_CAMERA_ANGLE_TOO_STEEP

            FrameAnalysisStatus.DocumentFramingStatus.DocumentTooCloseToFrameEdge ->
                CaptureFeedback.DOCUMENT_TOO_CLOSE_TO_FRAME_EDGE

            else -> null
        } ?: when (this.lightingStatus) {
            FrameAnalysisStatus.DocumentLightingStatus.TooBright -> CaptureFeedback.LIGHTING_TOO_BRIGHT
            FrameAnalysisStatus.DocumentLightingStatus.TooDark -> CaptureFeedback.LIGHTING_TOO_DARK
            else -> null
        } ?: when (this.blurStatus) {
            FrameAnalysisStatus.DocumentBlurStatus.BlurDetected -> CaptureFeedback.BLUR_DETECTED
            else -> null
        } ?: when (this.glareStatus) {
            FrameAnalysisStatus.DocumentGlareStatus.GlareDetected -> CaptureFeedback.GLARE_DETECTED
            else -> null
        } ?: when (this.occlusionStatus) {
            FrameAnalysisStatus.DocumentOcclusionStatus.Occluded -> CaptureFeedback.OCCLUDED_BY_HAND
            else -> CaptureFeedback.UNKNOWN
        }
    }
}
