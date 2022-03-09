@file:JvmName("StripeApi")
package com.stripe.android.stripecardscan.framework.api

import android.util.Log
import android.util.Size
import androidx.annotation.CheckResult
import com.stripe.android.camera.framework.image.constrainToSize
import com.stripe.android.camera.framework.image.crop
import com.stripe.android.camera.framework.image.determineViewFinderCrop
import com.stripe.android.camera.framework.image.size
import com.stripe.android.camera.framework.image.toJpeg
import com.stripe.android.camera.framework.util.move
import com.stripe.android.stripecardscan.cardimageverification.SavedFrame
import com.stripe.android.stripecardscan.framework.Config
import com.stripe.android.stripecardscan.framework.NetworkConfig
import com.stripe.android.stripecardscan.framework.api.dto.AppInfo
import com.stripe.android.stripecardscan.framework.api.dto.CardImageVerificationDetailsRequest
import com.stripe.android.stripecardscan.framework.api.dto.CardImageVerificationDetailsResult
import com.stripe.android.stripecardscan.framework.api.dto.ClientDevice
import com.stripe.android.stripecardscan.framework.api.dto.ScanStatistics
import com.stripe.android.stripecardscan.framework.api.dto.ScanStatsCIVRequest
import com.stripe.android.stripecardscan.framework.api.dto.ScanStatsResponse
import com.stripe.android.stripecardscan.framework.api.dto.StatsPayload
import com.stripe.android.stripecardscan.framework.api.dto.StripeServerErrorResponse
import com.stripe.android.stripecardscan.framework.api.dto.VerificationFrameData
import com.stripe.android.stripecardscan.framework.api.dto.VerifyFramesRequest
import com.stripe.android.stripecardscan.framework.api.dto.VerifyFramesResult
import com.stripe.android.stripecardscan.framework.api.dto.ViewFinderMargins
import com.stripe.android.stripecardscan.framework.util.AppDetails
import com.stripe.android.stripecardscan.framework.util.Device
import com.stripe.android.stripecardscan.framework.util.b64Encode
import com.stripe.android.camera.framework.util.scaleAndCenterWithin
import com.stripe.android.stripecardscan.framework.api.dto.ScanStatsOCRRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer

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
) = GlobalScope.launch(Dispatchers.IO) {
    val statsPayload = StatsPayload(
        instanceId = instanceId,
        scanId = scanId,
        device = ClientDevice.fromDevice(device),
        app = AppInfo.fromAppDetails(appDetails),
        scanStats = scanStatistics,
// TODO: this should probably be reported as part of scanstats, but is not yet supported
//        modelVersions = getLoadedModelVersions().map { ModelVersion.fromModelLoadDetails(it) },
    )

    when (
        val result = NetworkConfig.network.postForResult(
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
        is NetworkResult.Success -> Log.v(Config.logTag, "Scan stats uploaded")
        is NetworkResult.Error ->
            Log.e(
                Config.logTag,
                "Unable to upload scan stats (${result.responseCode}): ${result.error}",
            )
        is NetworkResult.Exception ->
            Log.e(
                Config.logTag,
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
) = GlobalScope.launch(Dispatchers.IO) {
    val statsPayload = StatsPayload(
        instanceId = instanceId,
        scanId = scanId,
        device = ClientDevice.fromDevice(device),
        app = AppInfo.fromAppDetails(appDetails),
        scanStats = scanStatistics,
// TODO: this should probably be reported as part of scanstats, but is not yet supported
//        modelVersions = getLoadedModelVersions().map { ModelVersion.fromModelLoadDetails(it) },
    )

    when (
        val result = NetworkConfig.network.postForResult(
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
        is NetworkResult.Success -> Log.v(Config.logTag, "Scan stats uploaded")
        is NetworkResult.Error ->
            Log.e(
                Config.logTag,
                "Unable to upload scan stats (${result.responseCode}): ${result.error}",
            )
        is NetworkResult.Exception ->
            Log.e(
                Config.logTag,
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
    NetworkConfig.network.postForResult(
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
) = withContext(Dispatchers.IO) {
    val maxImageWidth = 1080
    val maxImageHeight = 1920

    val verificationFramesData = savedFrames.map { savedFrame ->
        val cropRect = Size(maxImageWidth, maxImageHeight)
            .scaleAndCenterWithin(savedFrame.frame.cameraPreviewImage.image.size())

        val b64ImageData = b64Encode(
            savedFrame.frame.cameraPreviewImage.image
                .crop(cropRect)
                .constrainToSize(Size(maxImageWidth, maxImageHeight))
                .toJpeg() // ideally, this would be WebP, but python can't decode android WebPs
        )

        val viewFinderRect = determineViewFinderCrop(
            cameraPreviewImageSize = savedFrame.frame.cameraPreviewImage.image.size(),
            previewBounds = savedFrame.frame.cameraPreviewImage.viewBounds,
            viewFinder = savedFrame.frame.cardFinder,
        )
            .move(-cropRect.left, -cropRect.top)

        VerificationFrameData(
            imageData = b64ImageData,
            viewFinderMargins = ViewFinderMargins.fromRect(viewFinderRect)
        )
    }

    NetworkConfig.network.postForResult(
        stripePublishableKey = stripePublishableKey,
        path = "card_image_verifications/$civId/verify_frames",
        data = VerifyFramesRequest(
            clientSecret = civSecret,
            verificationFramesData = b64Encode(
                NetworkConfig.json.encodeToString(
                    ListSerializer(VerificationFrameData.serializer()),
                    verificationFramesData,
                ),
            ),
        ),
        requestSerializer = VerifyFramesRequest.serializer(),
        responseSerializer = VerifyFramesResult.serializer(),
        errorSerializer = StripeServerErrorResponse.serializer(),
    )
}
