@file:JvmName("StripeApi")
package com.stripe.android.stripecardscan.framework.api

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import android.util.Size
import androidx.annotation.CheckResult
import com.stripe.android.camera.framework.image.constrainToSize
import com.stripe.android.camera.framework.image.crop
import com.stripe.android.camera.framework.image.determineViewFinderCrop
import com.stripe.android.camera.framework.image.size
import com.stripe.android.camera.framework.image.toJpeg
import com.stripe.android.camera.framework.image.toWebP
import com.stripe.android.camera.framework.util.move
import com.stripe.android.camera.framework.util.scaleAndCenterWithin
import com.stripe.android.stripecardscan.cardimageverification.SavedFrame
import com.stripe.android.stripecardscan.framework.api.dto.AppInfo
import com.stripe.android.stripecardscan.framework.api.dto.CardImageVerificationDetailsAcceptedImageConfigs
import com.stripe.android.stripecardscan.framework.api.dto.CardImageVerificationDetailsFormat
import com.stripe.android.stripecardscan.framework.api.dto.CardImageVerificationDetailsRequest
import com.stripe.android.stripecardscan.framework.api.dto.CardImageVerificationDetailsResult
import com.stripe.android.stripecardscan.framework.api.dto.ClientDevice
import com.stripe.android.stripecardscan.framework.api.dto.ConfigurationStats
import com.stripe.android.stripecardscan.framework.api.dto.ScanStatistics
import com.stripe.android.stripecardscan.framework.api.dto.ScanStatsCIVRequest
import com.stripe.android.stripecardscan.framework.api.dto.ScanStatsOCRRequest
import com.stripe.android.stripecardscan.framework.api.dto.ScanStatsResponse
import com.stripe.android.stripecardscan.framework.api.dto.StatsPayload
import com.stripe.android.stripecardscan.framework.api.dto.StripeServerErrorResponse
import com.stripe.android.stripecardscan.framework.api.dto.VerificationFrameData
import com.stripe.android.stripecardscan.framework.api.dto.VerifyFramesRequest
import com.stripe.android.stripecardscan.framework.api.dto.VerifyFramesResult
import com.stripe.android.stripecardscan.framework.api.dto.ViewFinderMargins
import com.stripe.android.stripecardscan.framework.util.AppDetails
import com.stripe.android.stripecardscan.framework.util.Device
import com.stripe.android.stripecardscan.framework.util.ScanConfig
import com.stripe.android.stripecardscan.framework.util.b64Encode
import com.stripe.android.stripecardscan.framework.util.encodeToJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer

private const val LOG_TAG = "StripeApi"

private const val BASE_URL = "https://api.stripe.com/v1"
internal val CARD_SCAN_RETRY_STATUS_CODES: Iterable<Int> = 500..599

private val network: Network = StripeNetwork(
    baseUrl = BASE_URL,
    retryTotalAttempts = 3,
    retryStatusCodes = CARD_SCAN_RETRY_STATUS_CODES,
)

/**
 * Upload stats data to stripe servers.
 */
internal fun uploadScanStatsCIV(
    stripePublishableKey: String,
    civId: String,
    civSecret: String,
    instanceId: String,
    scanId: String?,
    device: Device,
    appDetails: AppDetails,
    scanStatistics: ScanStatistics,
    scanConfig: ScanConfig,
) = GlobalScope.launch(Dispatchers.IO) {
    val statsPayload = StatsPayload(
        instanceId = instanceId,
        scanId = scanId,
        device = ClientDevice.fromDevice(device),
        app = AppInfo.fromAppDetails(appDetails),
        scanStats = scanStatistics,
        configuration = ConfigurationStats.fromScanConfig(scanConfig)
// TODO: this should probably be reported as part of scanstats, but is not yet supported
//        modelVersions = getLoadedModelVersions().map { ModelVersion.fromModelLoadDetails(it) },
    )

    when (
        val result = network.postForResult(
            stripePublishableKey = stripePublishableKey,
            path = "/card_image_verifications/$civId/scan_stats",
            data = ScanStatsCIVRequest(
                clientSecret = civSecret,
                payload = statsPayload,
            ),
            requestSerializer = ScanStatsCIVRequest.serializer(),
            responseSerializer = ScanStatsResponse.serializer(),
            errorSerializer = StripeServerErrorResponse.serializer(),
        )
    ) {
        is NetworkResult.Success -> Log.v(LOG_TAG, "Scan stats uploaded")
        is NetworkResult.Error ->
            Log.e(
                LOG_TAG,
                "Unable to upload scan stats (${result.responseCode}): ${result.error}",
            )
        is NetworkResult.Exception ->
            Log.e(
                LOG_TAG,
                "Unable to upload scan stats (${result.responseCode})",
                result.exception,
            )
    }
}

internal fun uploadScanStatsOCR(
    stripePublishableKey: String,
    instanceId: String,
    scanId: String?,
    device: Device,
    appDetails: AppDetails,
    scanStatistics: ScanStatistics,
    scanConfig: ScanConfig,
) = GlobalScope.launch(Dispatchers.IO) {
    val statsPayload = StatsPayload(
        instanceId = instanceId,
        scanId = scanId,
        device = ClientDevice.fromDevice(device),
        app = AppInfo.fromAppDetails(appDetails),
        scanStats = scanStatistics,
        configuration = ConfigurationStats.fromScanConfig(scanConfig),
// TODO: this should probably be reported as part of scanstats, but is not yet supported
//        modelVersions = getLoadedModelVersions().map { ModelVersion.fromModelLoadDetails(it) },
    )

    when (
        val result = network.postForResult(
            stripePublishableKey = stripePublishableKey,
            path = "/card_image_scans/scan_stats",
            data = ScanStatsOCRRequest(
                payload = statsPayload,
            ),
            requestSerializer = ScanStatsOCRRequest.serializer(),
            responseSerializer = ScanStatsResponse.serializer(),
            errorSerializer = StripeServerErrorResponse.serializer(),
        )
    ) {
        is NetworkResult.Success -> Log.v(LOG_TAG, "Scan stats uploaded")
        is NetworkResult.Error ->
            Log.e(
                LOG_TAG,
                "Unable to upload scan stats (${result.responseCode}): ${result.error}",
            )
        is NetworkResult.Exception ->
            Log.e(
                LOG_TAG,
                "Unable to upload scan stats (${result.responseCode})",
                result.exception,
            )
    }
}

@CheckResult
internal suspend fun getCardImageVerificationIntentDetails(
    stripePublishableKey: String,
    civId: String,
    civSecret: String,
) = withContext(Dispatchers.IO) {
    network.postForResult(
        stripePublishableKey = stripePublishableKey,
        path = "/card_image_verifications/$civId/initialize_client",
        data = CardImageVerificationDetailsRequest(civSecret),
        requestSerializer = CardImageVerificationDetailsRequest.serializer(),
        responseSerializer = CardImageVerificationDetailsResult.serializer(),
        errorSerializer = StripeServerErrorResponse.serializer(),
    )
}

@CheckResult
internal suspend fun uploadSavedFrames(
    stripePublishableKey: String,
    civId: String,
    civSecret: String,
    savedFrames: Collection<SavedFrame>,
    imageConfigs: CardImageVerificationDetailsAcceptedImageConfigs?,
) = withContext(Dispatchers.IO) {

    val verificationFramesData = savedFrames.map { savedFrame ->
        val image = savedFrame.frame.cameraPreviewImage.image

        val imageDataAndCropRect = getImageData(image, imageConfigs)
        val b64ImageData = b64Encode(imageDataAndCropRect.first!!)
        val cropRect = imageDataAndCropRect.second

        val viewFinderRect = determineViewFinderCrop(
            cameraPreviewImageSize = image.size(),
            previewBounds = savedFrame.frame.cameraPreviewImage.viewBounds,
            viewFinder = savedFrame.frame.cardFinder,
        )
            .move(-cropRect.left, -cropRect.top)

        VerificationFrameData(
            imageData = b64ImageData,
            viewFinderMargins = ViewFinderMargins.fromRect(viewFinderRect)
        )
    }

    network.postForResult(
        stripePublishableKey = stripePublishableKey,
        path = "card_image_verifications/$civId/verify_frames",
        data = VerifyFramesRequest(
            clientSecret = civSecret,
            verificationFramesData = encodeToJson(
                ListSerializer(VerificationFrameData.serializer()),
                verificationFramesData,
            ),
        ),
        requestSerializer = VerifyFramesRequest.serializer(),
        responseSerializer = VerifyFramesResult.serializer(),
        errorSerializer = StripeServerErrorResponse.serializer(),
    )
}

internal fun isformatSupport(format: CardImageVerificationDetailsFormat) =
    format == CardImageVerificationDetailsFormat.JPEG ||
        format == CardImageVerificationDetailsFormat.WEBP

internal fun imageWithConfig(
    image: Bitmap,
    format: CardImageVerificationDetailsFormat,
    configs: CardImageVerificationDetailsAcceptedImageConfigs
): Pair<ByteArray, Rect>? {

    val imageSettings = configs.imageSettings(format)

    // Size and crop the image per the settings.
    val imageSize = imageSettings.imageSize!!
    val maxImageSize = Size(imageSize.first().toInt(), imageSize.last().toInt())

    val cropRect = maxImageSize
        .scaleAndCenterWithin(image.size())

    val image = image
        .crop(cropRect)
        .constrainToSize(maxImageSize)

    // Now convert formats with the compression ratio from settings.
    val compressionRatio = imageSettings.compressionRatio!!

    // Convert to 0..100
    val convertedRatio = compressionRatio.times(100.0).toInt()

    var result = when (format) {
        CardImageVerificationDetailsFormat.WEBP -> image.toWebP(convertedRatio)
        CardImageVerificationDetailsFormat.HEIC,
        CardImageVerificationDetailsFormat.JPEG -> image.toJpeg(convertedRatio)
    }

    if (result.size == 0) {
        return null
    }

    return Pair(result, cropRect)
}

internal fun getImageData(
    image: Bitmap,
    configs: CardImageVerificationDetailsAcceptedImageConfigs?
): Pair<ByteArray?, Rect> {

    val imageConfigs = configs ?: CardImageVerificationDetailsAcceptedImageConfigs()

    // Attempt to get image data using the configs from the server.
    var result = imageConfigs.preferredFormats?.filter {
        isformatSupport(it)
    }?.firstNotNullOfOrNull {
        imageWithConfig(image, it, imageConfigs)
    }

    if (result == null) {
        // Fallback to JPEG format
        result = imageWithConfig(image, CardImageVerificationDetailsFormat.JPEG, imageConfigs)
    }

    // Fallback to JPEG format
    return result ?: Pair(ByteArray(0), Rect())
}
