@file:JvmName("StripeApi")
package com.stripe.android.cardverificationsheet.framework.api

import android.util.Size
import androidx.annotation.CheckResult
import com.stripe.android.cardverificationsheet.cardverifyui.SavedFrame
import com.stripe.android.cardverificationsheet.framework.NetworkConfig
import com.stripe.android.cardverificationsheet.framework.api.dto.AppInfo
import com.stripe.android.cardverificationsheet.framework.api.dto.CardImageVerificationDetailsRequest
import com.stripe.android.cardverificationsheet.framework.api.dto.CardImageVerificationDetailsResult
import com.stripe.android.cardverificationsheet.framework.api.dto.ClientDevice
import com.stripe.android.cardverificationsheet.framework.api.dto.ModelVersion
import com.stripe.android.cardverificationsheet.framework.api.dto.ScanStatistics
import com.stripe.android.cardverificationsheet.framework.api.dto.ScanStatsRequest
import com.stripe.android.cardverificationsheet.framework.api.dto.StatsPayload
import com.stripe.android.cardverificationsheet.framework.api.dto.StripeServerErrorResponse
import com.stripe.android.cardverificationsheet.framework.api.dto.VerificationFrameData
import com.stripe.android.cardverificationsheet.framework.api.dto.VerifyFramesRequest
import com.stripe.android.cardverificationsheet.framework.image.cropCenter
import com.stripe.android.cardverificationsheet.framework.image.scale
import com.stripe.android.cardverificationsheet.framework.image.scaleAndCrop
import com.stripe.android.cardverificationsheet.framework.image.toWebP
import com.stripe.android.cardverificationsheet.framework.ml.getLoadedModelVersions
import com.stripe.android.cardverificationsheet.framework.util.AppDetails
import com.stripe.android.cardverificationsheet.framework.util.Device
import com.stripe.android.cardverificationsheet.framework.util.b64Encode
import com.stripe.android.cardverificationsheet.payment.cropCameraPreviewToViewFinder
import com.stripe.android.cardverificationsheet.payment.ml.SSDOcr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer

/**
 * Upload stats data to stripe servers.
 */
internal fun uploadScanStats(
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
        modelVersions = getLoadedModelVersions().map { ModelVersion.fromModelLoadDetails(it) },
    )

    NetworkConfig.network.postData(
        stripePublishableKey = stripePublishableKey,
        path = "/card_image_verifications/$civId/scan_stats",
        data = ScanStatsRequest(
            clientSecret = civSecret,
            stats = b64Encode(
                NetworkConfig.json.encodeToString(StatsPayload.serializer(), statsPayload)
            ),
        ),
        requestSerializer = ScanStatsRequest.serializer(),
    )
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
    val verificationFramesData = savedFrames.map { savedFrame ->
        val base64FullImageData = b64Encode(
            savedFrame.frame.cameraPreviewImage.image
                .scaleAndCrop(Size(360, 720))
                .toWebP()
        )
        val base64CroppedCenterImageData = b64Encode(
            savedFrame.frame.cameraPreviewImage.image
                .cropCenter(Size(224, 224))
                .toWebP()
        )
        val base64OcrImageData = b64Encode(
            cropCameraPreviewToViewFinder(
                savedFrame.frame.cameraPreviewImage.image,
                savedFrame.frame.cameraPreviewImage.viewBounds,
                savedFrame.frame.cardFinder
            )
                .scale(SSDOcr.Factory.TRAINED_IMAGE_SIZE)
                .toWebP()
        )

        VerificationFrameData(
            fullImageData = base64FullImageData,
            croppedCenterImageData = base64CroppedCenterImageData,
            ocrImageData = base64OcrImageData,
            fullImageOriginalWidth = savedFrame.frame.cameraPreviewImage.image.width,
            fullImageOriginalHeight = savedFrame.frame.cameraPreviewImage.image.height,
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
        responseSerializer = VerifyFramesRequest.serializer(),
        errorSerializer = StripeServerErrorResponse.serializer(),
    )
}
