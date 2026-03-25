package com.stripe.android.identity.analytics

import android.content.Context
import android.util.Log
import com.stripe.android.core.networking.AnalyticsRequestV2
import com.stripe.android.core.networking.AnalyticsRequestV2Factory
import com.stripe.android.identity.IdentityVerificationSheetContract
import com.stripe.android.identity.injection.IdentityCommonModule.Companion.GLOBAL_SCOPE
import com.stripe.android.identity.injection.IdentityVerificationScope
import com.stripe.android.identity.networking.IdentityRepository
import com.stripe.android.identity.networking.models.DocumentUploadParam
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageStaticContentExperiment
import com.stripe.android.identity.states.IdentityScanState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

/**
 * Factory for creating [AnalyticsRequestV2] for Identity.
 */
@IdentityVerificationScope
internal class IdentityAnalyticsRequestFactory @Inject constructor(
    context: Context,
    private val args: IdentityVerificationSheetContract.Args,
    val identityRepository: IdentityRepository,
    @Named(GLOBAL_SCOPE) val scope: CoroutineScope
) {
    enum class CameraSource(val analyticsValue: String) {
        CAMERA_SESSION("camera_session"),
        IMAGE_PICKER("image_picker")
    }

    enum class CameraEventKind(val analyticsValue: String) {
        PERMISSION("permission"),
        RUNTIME_ERROR("runtime_error")
    }

    enum class ScannerName(val analyticsValue: String) {
        DOCUMENT("document"),
        SELFIE("selfie"),
        UNKNOWN("unknown")
    }

    enum class ModelType(val analyticsValue: String) {
        DOCUMENT("document"),
        SELFIE("selfie")
    }
    var verificationPage: VerificationPage? = null
    private val requestFactory = AnalyticsRequestV2Factory(
        context = context,
        clientId = CLIENT_ID,
        origin = ORIGIN
    )
    private fun additionalParamWithEventMetadata(
        vararg pairs: Pair<String, *>
    ): Map<String, Any?> = additionalParamWithEventMetadata(mapOf(*pairs))

    private fun additionalParamWithEventMetadata(
        eventMetadata: Map<String, Any?>
    ): Map<String, Any?> {
        val metadataMap = eventMetadata.toMutableMap()
        verificationPage?.livemode?.let { liveMode ->
            metadataMap[PARAM_LIVE_MODE] = liveMode
        }

        return mapOf(
            PARAM_VERIFICATION_SESSION to args.verificationSessionId,
            PARAM_EVENT_META_DATA to metadataMap
        )
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Suppress("UNCHECKED_CAST")
    private fun maybeLogExperimentAndSendLog(
        eventName: String,
        additionalParams: Map<String, Any?> = mapOf()
    ) {
        runCatching {
            verificationPage?.let { verificationPage ->
                val experiments = verificationPage.experiments
                val userSessionId = verificationPage.userSessionId
                val metaDatas = if (additionalParams.containsKey(PARAM_EVENT_META_DATA)) {
                    additionalParams[PARAM_EVENT_META_DATA] as Map<String, Any?>?
                } else {
                    null
                }

                experiments
                    .filter { it.matches(eventName, metaDatas) }
                    .forEach { exp ->
                        scope.launch {
                            identityRepository.sendAnalyticsRequest(
                                requestFactory.createRequest(
                                    eventName = EVENT_EXPERIMENT_EXPOSURE,
                                    additionalParams = mapOf(
                                        PARAM_ARB_ID to userSessionId,
                                        PARAM_EXPERIMENT_RETRIEVED to exp.experimentName
                                    )
                                )
                            )
                        }
                    }
            }
            val request = requestFactory.createRequest(
                eventName = eventName,
                additionalParams = additionalParams
            )
            scope.launch {
                identityRepository.sendAnalyticsRequest(request)
            }
        }.onFailure {
            Log.e(TAG, "Failed to send analytics event $eventName - $it")
        }
    }

    private fun VerificationPageStaticContentExperiment.matches(
        eventName: String,
        metadata: Map<String, Any?>?
    ): Boolean {
        return if (this.eventMetadata.isEmpty()) {
            this.eventName == eventName && metadata == null
        } else {
            metadata?.let {
                this.eventName == eventName && metadata.entries.containsAll(this.eventMetadata.entries)
            } ?: false
        }
    }

    private fun cameraMetadata(
        screenName: String,
        cameraSource: CameraSource,
        cameraEventKind: CameraEventKind
    ): Map<String, Any?> = mapOf(
        PARAM_SCREEN_NAME to screenName,
        PARAM_CAMERA_SOURCE to cameraSource.analyticsValue,
        PARAM_CAMERA_EVENT_KIND to cameraEventKind.analyticsValue
    )

    private fun cameraAccessState(isGranted: Boolean?): String {
        return when (isGranted) {
            true -> CAMERA_ACCESS_STATE_GRANTED
            false -> CAMERA_ACCESS_STATE_DENIED
            null -> CAMERA_ACCESS_STATE_UNKNOWN
        }
    }

    private fun serializeError(
        throwable: Throwable,
        message: String? = throwable.message
    ): Map<String, Any?> = mapOf(
        PARAM_EXCEPTION to throwable.javaClass.name,
        PARAM_MESSAGE to message,
        PARAM_STACKTRACE to throwable.stackTraceToString()
    )

    fun sheetPresented() = maybeLogExperimentAndSendLog(
        eventName = EVENT_SHEET_PRESENTED,
        additionalParams = mapOf(
            PARAM_VERIFICATION_SESSION to args.verificationSessionId
        )
    )

    fun sheetClosed(
        sessionResult: String,
        lastScreenName: String? = null
    ) = maybeLogExperimentAndSendLog(
        eventName = EVENT_SHEET_CLOSED,
        additionalParams = additionalParamWithEventMetadata(
            PARAM_SESSION_RESULT to sessionResult,
            PARAM_LAST_SCREEN_NAME to lastScreenName
        )
    )

    fun verificationSucceeded(
        isFromFallbackUrl: Boolean,
        scanType: IdentityScanState.ScanType? = null,
        requireSelfie: Boolean? = null,
        docFrontRetryTimes: Int? = null,
        docBackRetryTimes: Int? = null,
        selfieRetryTimes: Int? = null,
        docFrontUploadType: DocumentUploadParam.UploadMethod? = null,
        docBackUploadType: DocumentUploadParam.UploadMethod? = null,
        docFrontModelScore: Float? = null,
        docBackModelScore: Float? = null,
        selfieModelScore: Float? = null,
        docFrontBlurScore: Float? = null,
        docBackBlurScore: Float? = null,
        lastScreenName: String? = null
    ) = maybeLogExperimentAndSendLog(
        eventName = EVENT_VERIFICATION_SUCCEEDED,
        additionalParams = additionalParamWithEventMetadata(
            PARAM_FROM_FALLBACK_URL to isFromFallbackUrl,
            PARAM_SCAN_TYPE to scanType?.toParam(),
            PARAM_REQUIRE_SELFIE to requireSelfie,
            PARAM_DOC_FRONT_RETRY_TIMES to docFrontRetryTimes,
            PARAM_DOC_BACK_RETRY_TIMES to docBackRetryTimes,
            PARAM_SELFIE_RETRY_TIMES to selfieRetryTimes,
            PARAM_DOC_FRONT_UPLOAD_TYPE to docFrontUploadType?.name,
            PARAM_DOC_BACK_UPLOAD_TYPE to docBackUploadType?.name,
            PARAM_DOC_FRONT_MODEL_SCORE to docFrontModelScore,
            PARAM_DOC_BACK_MODEL_SCORE to docBackModelScore,
            PARAM_SELFIE_MODEL_SCORE to selfieModelScore,
            PARAM_DOC_FRONT_BLUR_SCORE to docFrontBlurScore,
            PARAM_DOC_BACK_BLUR_SCORE to docBackBlurScore,
            PARAM_LAST_SCREEN_NAME to lastScreenName
        )
    )

    fun verificationCanceled(
        isFromFallbackUrl: Boolean,
        lastScreenName: String? = null,
        scanType: IdentityScanState.ScanType? = null,
        requireSelfie: Boolean? = null
    ) = maybeLogExperimentAndSendLog(
        eventName = EVENT_VERIFICATION_CANCELED,
        additionalParams = additionalParamWithEventMetadata(
            PARAM_FROM_FALLBACK_URL to isFromFallbackUrl,
            PARAM_SCAN_TYPE to scanType?.toParam(),
            PARAM_REQUIRE_SELFIE to requireSelfie,
            PARAM_LAST_SCREEN_NAME to lastScreenName
        )
    )

    fun verificationFailed(
        isFromFallbackUrl: Boolean,
        scanType: IdentityScanState.ScanType? = null,
        requireSelfie: Boolean? = null,
        docFrontUploadType: DocumentUploadParam.UploadMethod? = null,
        docBackUploadType: DocumentUploadParam.UploadMethod? = null,
        throwable: Throwable,
        lastScreenName: String? = null
    ) = maybeLogExperimentAndSendLog(
        eventName = EVENT_VERIFICATION_FAILED,
        additionalParams = additionalParamWithEventMetadata(
            PARAM_FROM_FALLBACK_URL to isFromFallbackUrl,
            PARAM_SCAN_TYPE to scanType?.toParam(),
            PARAM_REQUIRE_SELFIE to requireSelfie,
            PARAM_DOC_FRONT_UPLOAD_TYPE to docFrontUploadType?.name,
            PARAM_DOC_BACK_UPLOAD_TYPE to docBackUploadType?.name,
            PARAM_LAST_SCREEN_NAME to lastScreenName,
            PARAM_ERROR to serializeError(throwable)
        )
    )

    fun screenPresented(
        scanType: IdentityScanState.ScanType? = null,
        screenName: String
    ) = maybeLogExperimentAndSendLog(
        eventName = EVENT_SCREEN_PRESENTED,
        additionalParams = additionalParamWithEventMetadata(
            PARAM_SCAN_TYPE to scanType?.toParam(),
            PARAM_SCREEN_NAME to screenName
        )
    )

    fun cameraError(
        scanType: IdentityScanState.ScanType,
        throwable: Throwable,
        screenName: String = screenNameForScanType(scanType),
        cameraSource: CameraSource = CameraSource.CAMERA_SESSION
    ) = maybeLogExperimentAndSendLog(
        eventName = EVENT_CAMERA_ERROR,
        additionalParams = additionalParamWithEventMetadata(
            cameraMetadata(
                screenName = screenName,
                cameraSource = cameraSource,
                cameraEventKind = CameraEventKind.RUNTIME_ERROR
            ) + mapOf(
                PARAM_SCAN_TYPE to scanType.toParam(),
                PARAM_ERROR to serializeError(throwable)
            )
        )
    )

    fun cameraPermissionDenied(
        screenName: String = SCREEN_NAME_UNKNOWN,
        cameraSource: CameraSource = CameraSource.CAMERA_SESSION,
        isGranted: Boolean? = false
    ) = maybeLogExperimentAndSendLog(
        eventName = EVENT_CAMERA_PERMISSION_DENIED,
        additionalParams = additionalParamWithEventMetadata(
            cameraMetadata(
                screenName = screenName,
                cameraSource = cameraSource,
                cameraEventKind = CameraEventKind.PERMISSION
            ) + mapOf(
                PARAM_CAMERA_ACCESS_STATE to cameraAccessState(isGranted)
            )
        )
    )
    fun cameraPermissionGranted(
        screenName: String = SCREEN_NAME_UNKNOWN,
        cameraSource: CameraSource = CameraSource.CAMERA_SESSION
    ) = maybeLogExperimentAndSendLog(
        eventName = EVENT_CAMERA_PERMISSION_GRANTED,
        additionalParams = additionalParamWithEventMetadata(
            cameraMetadata(
                screenName = screenName,
                cameraSource = cameraSource,
                cameraEventKind = CameraEventKind.PERMISSION
            ) + mapOf(
                PARAM_CAMERA_ACCESS_STATE to cameraAccessState(true)
            )
        )
    )

    fun cameraPermissionRationaleShown() = maybeLogExperimentAndSendLog(
        eventName = EVENT_CAMERA_PERMISSION_RATIONALE_SHOWN
    )

    fun cameraPermissionAppSettingsClicked() = maybeLogExperimentAndSendLog(
        eventName = EVENT_CAMERA_PERMISSION_APP_SETTINGS_CLICKED
    )

    fun documentTimeout(
        scanType: IdentityScanState.ScanType
    ) = maybeLogExperimentAndSendLog(
        eventName = EVENT_DOCUMENT_TIMEOUT,
        additionalParams = additionalParamWithEventMetadata(
            PARAM_SCAN_TYPE to scanType.toParam(),
            PARAM_SIDE to scanType.toSide()
        )
    )

    fun selfieTimeout() = maybeLogExperimentAndSendLog(
        eventName = EVENT_SELFIE_TIMEOUT,
        additionalParams = additionalParamWithEventMetadata()
    )

    fun averageFps(type: String, value: Int, frames: Int) = maybeLogExperimentAndSendLog(
        eventName = EVENT_AVERAGE_FPS,
        additionalParams = additionalParamWithEventMetadata(
            PARAM_TYPE to type,
            PARAM_VALUE to value,
            PARAM_FRAMES to frames
        )
    )

    fun modelPerformance(mlModel: String, preprocess: Long, inference: Long, frames: Int) =
        maybeLogExperimentAndSendLog(
            eventName = EVENT_MODEL_PERFORMANCE,
            additionalParams = additionalParamWithEventMetadata(
                PARAM_PREPROCESS to preprocess,
                PARAM_INFERENCE to inference,
                PARAM_ML_MODEL to mlModel,
                PARAM_FRAMES to frames
            )
        )

    fun timeToScreen(
        value: Long,
        networkTime: Long? = null,
        fromScreenName: String?,
        toScreenName: String
    ) = maybeLogExperimentAndSendLog(
        eventName = EVENT_TIME_TO_SCREEN,
        additionalParams = additionalParamWithEventMetadata(
            PARAM_VALUE to value,
            PARAM_NETWORK_TIME to networkTime,
            PARAM_FROM_SCREEN_NAME to fromScreenName,
            PARAM_TO_SCREEN_NAME to toScreenName
        )
    )

    fun genericError(
        message: String?,
        stackTrace: String,
        additionalMetadata: Map<String, Any?> = mapOf()
    ) = maybeLogExperimentAndSendLog(
        eventName = EVENT_GENERIC_ERROR,
        additionalParams = additionalParamWithEventMetadata(
            additionalMetadata + mapOf(
                PARAM_MESSAGE to message,
                PARAM_STACKTRACE to stackTrace,
                PARAM_ERROR_DETAILS to mapOf(
                    PARAM_MESSAGE to message,
                    PARAM_STACKTRACE to stackTrace
                )
            )
        )
    )

    fun genericError(
        throwable: Throwable,
        additionalMetadata: Map<String, Any?> = mapOf(),
        overrideMessage: String? = throwable.message
    ) = maybeLogExperimentAndSendLog(
        eventName = EVENT_GENERIC_ERROR,
        additionalParams = additionalParamWithEventMetadata(
            additionalMetadata + mapOf(
                PARAM_MESSAGE to overrideMessage,
                PARAM_STACKTRACE to throwable.stackTraceToString(),
                PARAM_ERROR_DETAILS to serializeError(
                    throwable = throwable,
                    message = overrideMessage
                )
            )
        )
    )

    fun imageUpload(
        value: Long,
        compressionQuality: Float,
        scanType: IdentityScanState.ScanType,
        id: String?,
        fileName: String?,
        fileSize: Long
    ) = maybeLogExperimentAndSendLog(
        eventName = EVENT_IMAGE_UPLOAD,
        additionalParams = additionalParamWithEventMetadata(
            PARAM_VALUE to value,
            PARAM_COMPRESSION_QUALITY to compressionQuality,
            PARAM_SCAN_TYPE to scanType.toParam(),
            PARAM_ID to id,
            PARAM_FILE_NAME to fileName,
            PARAM_FILE_SIZE to fileSize
        )
    )

    fun mbStatus(
        required: Boolean,
        initSuccess: Boolean? = null,
        initFailedReason: String? = null
    ) = maybeLogExperimentAndSendLog(
        eventName = EVENT_MB_STATUS,
        additionalParamWithEventMetadata(
            PARAM_REQUIRED to required,
            PARAM_INIT_SUCCESS to initSuccess,
            PARAM_INIT_FAILED_REASON to initFailedReason
        )
    )

    fun mbError(
        message: String?,
        stackTrace: String?
    ) = maybeLogExperimentAndSendLog(
        eventName = EVENT_MB_ERROR,
        additionalParamWithEventMetadata(
            PARAM_MESSAGE to message,
            PARAM_STACKTRACE to stackTrace
        )
    )

    fun mbCaptureStatus(
        capturedByMb: Boolean
    ) = maybeLogExperimentAndSendLog(
        eventName = EVENT_MB_CAPTURE_STATUS,
        additionalParamWithEventMetadata(
            PARAM_CAPTURED_BY_MB to capturedByMb
        )
    )

    private fun IdentityScanState.ScanType.toParam(): String =
        requireNotNull(analyticsValueForScanType(this)) {
            "Unknown scan type: $this"
        }

    private fun IdentityScanState.ScanType.toSide(): String =
        when (this) {
            IdentityScanState.ScanType.DOC_FRONT -> FRONT
            IdentityScanState.ScanType.DOC_BACK -> BACK
            else -> {
                throw IllegalArgumentException("Unknown type: $this")
            }
        }

    internal companion object {
        const val TAG = "Analytics"
        const val CLIENT_ID = "mobile-identity-sdk"
        const val ORIGIN = "stripe-identity-android"
        const val ID = "id"
        const val DOC_FRONT = "doc_front"
        const val DOC_BACK = "doc_back"
        const val SELFIE = "selfie"
        const val FRONT = "front"
        const val BACK = "back"

        const val EVENT_SHEET_PRESENTED = "sheet_presented"
        const val EVENT_SHEET_CLOSED = "sheet_closed"
        const val EVENT_VERIFICATION_SUCCEEDED = "verification_succeeded"
        const val EVENT_VERIFICATION_FAILED = "verification_failed"
        const val EVENT_VERIFICATION_CANCELED = "verification_canceled"
        const val EVENT_SCREEN_PRESENTED = "screen_presented"
        const val EVENT_CAMERA_ERROR = "camera_error"
        const val EVENT_CAMERA_PERMISSION_DENIED = "camera_permission_denied"
        const val EVENT_CAMERA_PERMISSION_GRANTED = "camera_permission_granted"
        const val EVENT_CAMERA_PERMISSION_RATIONALE_SHOWN = "camera_permission_rationale_shown"
        const val EVENT_CAMERA_PERMISSION_APP_SETTINGS_CLICKED =
            "camera_permission_app_settings_clicked"
        const val EVENT_DOCUMENT_TIMEOUT = "document_timeout"
        const val EVENT_SELFIE_TIMEOUT = "selfie_timeout"
        const val EVENT_AVERAGE_FPS = "average_fps"
        const val EVENT_MODEL_PERFORMANCE = "model_performance"
        const val EVENT_TIME_TO_SCREEN = "time_to_screen"
        const val EVENT_IMAGE_UPLOAD = "image_upload"
        const val EVENT_GENERIC_ERROR = "generic_error"
        const val EVENT_MB_STATUS = "mb_status"
        const val EVENT_MB_ERROR = "mb_error"
        const val EVENT_MB_CAPTURE_STATUS = "mb_capture_status"
        const val EVENT_EXPERIMENT_EXPOSURE = "preloaded_experiment_retrieved"

        const val PARAM_EVENT_META_DATA = "event_metadata"
        const val PARAM_FROM_FALLBACK_URL = "from_fallback_url"
        const val PARAM_VERIFICATION_SESSION = "verification_session"
        const val PARAM_SESSION_RESULT = "session_result"
        const val PARAM_SCAN_TYPE = "scan_type"
        const val PARAM_REQUIRE_SELFIE = "require_selfie"
        const val PARAM_DOC_FRONT_RETRY_TIMES = "doc_front_retry_times"
        const val PARAM_DOC_BACK_RETRY_TIMES = "doc_back_retry_times"
        const val PARAM_SELFIE_RETRY_TIMES = "selfie_retry_times"
        const val PARAM_DOC_FRONT_UPLOAD_TYPE = "doc_front_upload_type"
        const val PARAM_DOC_BACK_UPLOAD_TYPE = "doc_back_upload_type"
        const val PARAM_DOC_FRONT_MODEL_SCORE = "doc_front_model_score"
        const val PARAM_DOC_BACK_MODEL_SCORE = "doc_back_model_score"
        const val PARAM_SELFIE_MODEL_SCORE = "selfie_model_score"
        const val PARAM_DOC_FRONT_BLUR_SCORE = "doc_front_blur_score"
        const val PARAM_DOC_BACK_BLUR_SCORE = "doc_back_blur_score"
        const val PARAM_LAST_SCREEN_NAME = "last_screen_name"
        const val PARAM_ERROR = "error"
        const val PARAM_ERROR_DETAILS = "error_details"
        const val PARAM_EXCEPTION = "exception"
        const val PARAM_STACKTRACE = "stacktrace"
        const val PARAM_SCREEN_NAME = "screen_name"
        const val PARAM_CAMERA_SOURCE = "camera_source"
        const val PARAM_CAMERA_EVENT_KIND = "camera_event_kind"
        const val PARAM_CAMERA_ACCESS_STATE = "camera_access_state"
        const val PARAM_ERROR_CONTEXT = "error_context"
        const val PARAM_SCANNER_NAME = "scanner_name"
        const val PARAM_UPLOAD_METHOD = "upload_method"
        const val PARAM_UPLOAD_STAGE = "upload_stage"
        const val PARAM_IS_HIGH_RES = "is_high_res"
        const val PARAM_SELFIE_VARIANT = "selfie_variant"
        const val PARAM_ML_MODEL_STAGE = "ml_model_stage"
        const val PARAM_ML_MODEL_TYPE = "ml_model_type"
        const val PARAM_SIDE = "side"
        const val PARAM_TYPE = "type"
        const val PARAM_VALUE = "value"
        const val PARAM_PREPROCESS = "preprocess"
        const val PARAM_INFERENCE = "inference"
        const val PARAM_ML_MODEL = "ml_model"
        const val PARAM_FRAMES = "frames"
        const val PARAM_NETWORK_TIME = "network_time"
        const val PARAM_FROM_SCREEN_NAME = "from_screen_name"
        const val PARAM_TO_SCREEN_NAME = "to_screen_name"
        const val PARAM_MESSAGE = "message"
        const val PARAM_COMPRESSION_QUALITY = "compression_quality"
        const val PARAM_ID = "id"
        const val PARAM_FILE_NAME = "file_name"
        const val PARAM_FILE_SIZE = "file_size"
        const val PARAM_LIVE_MODE = "live_mode"
        const val PARAM_REQUIRED = "required"
        const val PARAM_INIT_SUCCESS = "init_success"
        const val PARAM_INIT_FAILED_REASON = "init_failed_reason"
        const val PARAM_EXPERIMENT_RETRIEVED = "experiment_retrieved"
        const val PARAM_ARB_ID = "arb_id"
        const val PARAM_CAPTURED_BY_MB = "captured_by_mb"

        const val SCREEN_NAME_CONSENT = "consent"
        const val SCREEN_NAME_DOC_WARMUP = "document_warmup"
        const val SCREEN_NAME_SELFIE_WARMUP = "selfie_warmup"
        const val SCREEN_NAME_SELFIE = "selfie"
        const val SCREEN_NAME_LIVE_CAPTURE = "live_capture"
        const val SCREEN_NAME_FILE_UPLOAD = "file_upload"
        const val SCREEN_NAME_CONFIRMATION = "confirmation"
        const val SCREEN_NAME_ERROR = "error"
        const val SCREEN_NAME_INDIVIDUAL = "individual"
        const val SCREEN_NAME_INDIVIDUAL_WELCOME = "individual_welcome"
        const val SCREEN_NAME_COUNTRY_NOT_LISTED = "country_not_listed"
        const val SCREEN_NAME_DEBUG = "debug"
        const val SCREEN_NAME_PHONE_OTP = "phone_otp"
        const val SCREEN_NAME_UNKNOWN = "unknown"
        const val TYPE_SELFIE = "selfie"
        const val TYPE_DOCUMENT = "document"
        const val CAMERA_ACCESS_STATE_GRANTED = "granted"
        const val CAMERA_ACCESS_STATE_DENIED = "denied"
        const val CAMERA_ACCESS_STATE_UNKNOWN = "unknown"
        const val ERROR_CONTEXT_ERROR_SCREEN = "error_screen"
        const val ERROR_CONTEXT_IMAGE_SCAN = "image_scan"
        const val ERROR_CONTEXT_IMAGE_UPLOAD = "image_upload"
        const val ERROR_CONTEXT_MODEL_LOADING = "model_loading"
        const val ERROR_CONTEXT_SCANNER_LOAD = "scanner_load"
        const val ERROR_CONTEXT_SCREEN_TRANSITION = "screen_transition"
        const val ERROR_CONTEXT_RESULT_PROCESSING = "result_processing"
        const val MODEL_LOADING_STAGE_DOWNLOAD = "download"
        const val MODEL_LOADING_STAGE_VALIDATE = "validate"
        const val MODEL_LOADING_STAGE_INITIALIZE = "initialize"
        const val UPLOAD_STAGE_PREPARE = "prepare"
        const val UPLOAD_STAGE_REQUEST = "request"

        internal fun analyticsValueForScanType(
            scanType: IdentityScanState.ScanType?
        ): String? = when (scanType) {
            IdentityScanState.ScanType.DOC_FRONT -> DOC_FRONT
            IdentityScanState.ScanType.DOC_BACK -> DOC_BACK
            IdentityScanState.ScanType.SELFIE -> SELFIE
            null -> null
        }

        internal fun screenNameForScanType(
            scanType: IdentityScanState.ScanType?
        ): String = when (scanType) {
            IdentityScanState.ScanType.DOC_FRONT,
            IdentityScanState.ScanType.DOC_BACK -> SCREEN_NAME_LIVE_CAPTURE
            IdentityScanState.ScanType.SELFIE -> SCREEN_NAME_SELFIE
            null -> SCREEN_NAME_UNKNOWN
        }

        internal fun scannerNameForScanType(
            scanType: IdentityScanState.ScanType?
        ): String = when (scanType) {
            IdentityScanState.ScanType.DOC_FRONT,
            IdentityScanState.ScanType.DOC_BACK -> ScannerName.DOCUMENT.analyticsValue
            IdentityScanState.ScanType.SELFIE -> ScannerName.SELFIE.analyticsValue
            null -> ScannerName.UNKNOWN.analyticsValue
        }
    }
}
