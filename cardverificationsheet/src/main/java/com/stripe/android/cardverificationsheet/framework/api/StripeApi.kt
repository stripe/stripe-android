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
import com.stripe.android.cardverificationsheet.framework.api.dto.VerifyFramesResult
import com.stripe.android.cardverificationsheet.framework.api.dto.ViewFinderMargins
import com.stripe.android.cardverificationsheet.framework.image.crop
import com.stripe.android.cardverificationsheet.framework.image.scale
import com.stripe.android.cardverificationsheet.framework.image.size
import com.stripe.android.cardverificationsheet.framework.image.toWebP
import com.stripe.android.cardverificationsheet.framework.ml.getLoadedModelVersions
import com.stripe.android.cardverificationsheet.framework.util.AppDetails
import com.stripe.android.cardverificationsheet.framework.util.Device
import com.stripe.android.cardverificationsheet.framework.util.b64Encode
import com.stripe.android.cardverificationsheet.framework.util.centerScaled
import com.stripe.android.cardverificationsheet.framework.util.move
import com.stripe.android.cardverificationsheet.framework.util.scaleAndCenterWithin
import com.stripe.android.cardverificationsheet.framework.util.toRect
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
    val requiredImageWidth = 1080
    val requiredImageHeight = 1920

    val verificationFramesData = savedFrames.map { savedFrame ->
        val cropRect = Size(requiredImageWidth, requiredImageHeight)
            .scaleAndCenterWithin(savedFrame.frame.cameraPreviewImage.image.size())

        val base64ImageData = b64Encode(
            savedFrame.frame.cameraPreviewImage.image
                .crop(cropRect)
                .scale(Size(requiredImageWidth, requiredImageHeight))
                .toWebP()
        )

        val viewFinderRect = savedFrame.frame.cameraPreviewImage.image
            .size()
            .toRect()
            .move(-cropRect.left, -cropRect.top)
            .centerScaled(
                scaleX = requiredImageWidth.toFloat() / cropRect.width(),
                scaleY = requiredImageHeight.toFloat() / cropRect.height(),
            )

        VerificationFrameData(
            imageData = base64ImageData,
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
