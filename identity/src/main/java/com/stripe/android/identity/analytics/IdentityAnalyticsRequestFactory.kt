package com.stripe.android.identity.analytics

import android.content.Context
import com.stripe.android.core.networking.AnalyticsRequestV2
import com.stripe.android.core.networking.AnalyticsRequestV2Factory
import com.stripe.android.identity.IdentityVerificationSheetContract
import com.stripe.android.identity.networking.models.DocumentUploadParam
import com.stripe.android.identity.states.IdentityScanState
import javax.inject.Inject

/**
 * Factory for creating [AnalyticsRequestV2] for Identity.
 */
internal class IdentityAnalyticsRequestFactory @Inject constructor(
    context: Context,
    private val args: IdentityVerificationSheetContract.Args
) {
    private val requestFactory = AnalyticsRequestV2Factory(
        context = context,
        clientId = CLIENT_ID,
        origin = ORIGIN
    )

    fun sheetPresented() = requestFactory.createRequest(
        EVENT_SHEET_PRESENTED,
        additionalParams = mapOf(
            PARAM_VERIFICATION_SESSION to args.verificationSessionId
        )
    )

    fun sheetClosed(sessionResult: String) = requestFactory.createRequest(
        eventName = EVENT_SHEET_CLOSED,
        additionalParams = mapOf(
            PARAM_VERIFICATION_SESSION to args.verificationSessionId,
            PARAM_SESSION_RESULT to sessionResult
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
        selfieModelScore: Float? = null
    ) = requestFactory.createRequest(
        eventName = EVENT_VERIFICATION_SUCCEEDED,
        additionalParams =
        mapOf(
            PARAM_FROM_FALLBACK_URL to isFromFallbackUrl,
            PARAM_VERIFICATION_SESSION to args.verificationSessionId,
            PARAM_SCAN_TYPE to scanType?.toParam(),
            PARAM_REQUIRE_SELFIE to requireSelfie,
            PARAM_DOC_FRONT_RETRY_TIMES to docFrontRetryTimes,
            PARAM_DOC_BACK_RETRY_TIMES to docBackRetryTimes,
            PARAM_SELFIE_RETRY_TIMES to selfieRetryTimes,
            PARAM_DOC_FRONT_UPLOAD_TYPE to docFrontUploadType?.name,
            PARAM_DOC_BACK_UPLOAD_TYPE to docBackUploadType?.name,
            PARAM_DOC_FRONT_MODEL_SCORE to docFrontModelScore,
            PARAM_DOC_BACK_MODEL_SCORE to docBackModelScore,
            PARAM_SELFIE_MODEL_SCORE to selfieModelScore
        )
    )

    fun verificationCanceled(
        isFromFallbackUrl: Boolean,
        lastScreenName: String? = null,
        scanType: IdentityScanState.ScanType? = null,
        requireSelfie: Boolean? = null
    ) = requestFactory.createRequest(
        eventName = EVENT_VERIFICATION_CANCELED,
        additionalParams =
        mapOf(
            PARAM_FROM_FALLBACK_URL to isFromFallbackUrl,
            PARAM_VERIFICATION_SESSION to args.verificationSessionId,
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
        throwable: Throwable
    ) = requestFactory.createRequest(
        eventName = EVENT_VERIFICATION_FAILED,
        additionalParams =
        mapOf(
            PARAM_FROM_FALLBACK_URL to isFromFallbackUrl,
            PARAM_VERIFICATION_SESSION to args.verificationSessionId,
            PARAM_SCAN_TYPE to scanType?.toParam(),
            PARAM_REQUIRE_SELFIE to requireSelfie,
            PARAM_DOC_FRONT_UPLOAD_TYPE to docFrontUploadType?.name,
            PARAM_DOC_BACK_UPLOAD_TYPE to docBackUploadType?.name,
            PARAM_ERROR to mapOf(
                PARAM_EXCEPTION to throwable.javaClass.name,
                PARAM_STACKTRACE to throwable.stackTrace.toString()
            )
        )
    )

    fun screenPresented(
        scanType: IdentityScanState.ScanType? = null,
        screenName: String
    ) = requestFactory.createRequest(
        eventName = EVENT_SCREEN_PRESENTED,
        additionalParams = mapOf(
            PARAM_VERIFICATION_SESSION to args.verificationSessionId,
            PARAM_SCAN_TYPE to scanType?.toParam(),
            PARAM_SCREEN_NAME to screenName
        )
    )

    fun cameraError(
        scanType: IdentityScanState.ScanType,
        throwable: Throwable
    ) = requestFactory.createRequest(
        eventName = EVENT_CAMERA_ERROR,
        additionalParams = mapOf(
            PARAM_VERIFICATION_SESSION to args.verificationSessionId,
            PARAM_SCAN_TYPE to scanType.toParam(),
            PARAM_ERROR to mapOf(
                PARAM_EXCEPTION to throwable.javaClass.name,
                PARAM_STACKTRACE to throwable.stackTrace.toString()
            )
        )
    )

    fun cameraPermissionDenied(
        scanType: IdentityScanState.ScanType
    ) = requestFactory.createRequest(
        eventName = EVENT_CAMERA_PERMISSION_DENIED,
        additionalParams = mapOf(
            PARAM_VERIFICATION_SESSION to args.verificationSessionId,
            PARAM_SCAN_TYPE to scanType.toParam()
        )
    )

    fun cameraPermissionGranted(
        scanType: IdentityScanState.ScanType
    ) = requestFactory.createRequest(
        eventName = EVENT_CAMERA_PERMISSION_GRANTED,
        additionalParams = mapOf(
            PARAM_VERIFICATION_SESSION to args.verificationSessionId,
            PARAM_SCAN_TYPE to scanType.toParam()
        )
    )

    fun documentTimeout(
        scanType: IdentityScanState.ScanType
    ) = requestFactory.createRequest(
        eventName = EVENT_DOCUMENT_TIMEOUT,
        additionalParams = mapOf(
            PARAM_VERIFICATION_SESSION to args.verificationSessionId,
            PARAM_SCAN_TYPE to scanType.toParam(),
            PARAM_SIDE to scanType.toSide()
        )
    )

    fun selfieTimeout() = requestFactory.createRequest(
        eventName = EVENT_SELFIE_TIMEOUT,
        additionalParams = mapOf(
            PARAM_VERIFICATION_SESSION to args.verificationSessionId
        )
    )

    private fun IdentityScanState.ScanType.toParam(): String =
        when (this) {
            IdentityScanState.ScanType.ID_FRONT -> ID
            IdentityScanState.ScanType.ID_BACK -> ID
            IdentityScanState.ScanType.PASSPORT -> PASSPORT
            IdentityScanState.ScanType.DL_FRONT -> DRIVER_LICENSE
            IdentityScanState.ScanType.DL_BACK -> DRIVER_LICENSE
            else -> {
                throw IllegalArgumentException("Unknown type: $this")
            }
        }

    private fun IdentityScanState.ScanType.toSide(): String =
        when (this) {
            IdentityScanState.ScanType.ID_FRONT -> FRONT
            IdentityScanState.ScanType.ID_BACK -> BACK
            IdentityScanState.ScanType.PASSPORT -> FRONT
            IdentityScanState.ScanType.DL_FRONT -> FRONT
            IdentityScanState.ScanType.DL_BACK -> BACK
            else -> {
                throw IllegalArgumentException("Unknown type: $this")
            }
        }

    internal companion object {
        const val CLIENT_ID = "mobile-identity-sdk"
        const val ORIGIN = "stripe-identity-android"
        const val ID = "id"
        const val PASSPORT = "passport"
        const val DRIVER_LICENSE = "driver_license"
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
        const val EVENT_DOCUMENT_TIMEOUT = "document_timeout"
        const val EVENT_SELFIE_TIMEOUT = "selfie_timeout"

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
        const val PARAM_LAST_SCREEN_NAME = "last_screen_name"
        const val PARAM_ERROR = "error"
        const val PARAM_EXCEPTION = "exception"
        const val PARAM_STACKTRACE = "stacktrace"
        const val PARAM_SCREEN_NAME = "screen_name"
        const val PARAM_SIDE = "side"

        const val SCREEN_NAME_CONSENT = "consent"
        const val SCREEN_NAME_DOC_SELECT = "document_select"
        const val SCREEN_NAME_LIVE_CAPTURE = "live_capture"
        const val SCREEN_NAME_FILE_UPLOAD = "file_upload"
        const val SCREEN_NAME_SELFIE = "selfie"
        const val SCREEN_NAME_CONFIRMATION = "confirmation"
        const val SCREEN_NAME_ERROR = "error"
    }
}
