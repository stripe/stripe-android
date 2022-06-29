package com.stripe.android.stripecardscan.framework.util

import androidx.annotation.CheckResult
import com.stripe.android.camera.framework.image.determineViewFinderCrop
import com.stripe.android.camera.framework.image.size
import com.stripe.android.camera.framework.util.move
import com.stripe.android.stripecardscan.cardimageverification.SavedFrame
import com.stripe.android.stripecardscan.framework.api.dto.PayloadInfo
import com.stripe.android.stripecardscan.framework.api.dto.VerificationFrameData
import com.stripe.android.stripecardscan.framework.api.dto.ViewFinderMargins
import com.stripe.android.stripecardscan.framework.image.toImageFormat

@CheckResult
internal suspend fun Collection<SavedFrame>.toVerificationFrameData(
    imageConfigs: AcceptedImageConfigs
): Pair<List<VerificationFrameData>, PayloadInfo> {
    // Attempt to get image data using the configs from the server.
    val format = imageConfigs.preferredFormats?.first() ?: ImageFormat.JPEG
    var imageSettings = imageConfigs.imageSettings(format)
    var imagePayloadSize = 0

    val verificationFramesData = this.map { savedFrame ->
        val image = savedFrame.frame.cameraPreviewImage.image

        val imageDataAndCropRect = image.toImageFormat(format, imageSettings)
        val b64ImageData = b64Encode(imageDataAndCropRect.first)

        imagePayloadSize += b64ImageData.length

        val cropRect = imageDataAndCropRect.second

        val viewFinderRect = determineViewFinderCrop(
            cameraPreviewImageSize = image.size(),
            previewBounds = savedFrame.frame.cameraPreviewImage.viewBounds,
            viewFinder = savedFrame.frame.cardFinder
        )
            .move(-cropRect.left, -cropRect.top)

        VerificationFrameData(
            imageData = b64ImageData,
            viewFinderMargins = ViewFinderMargins.fromRect(viewFinderRect)
        )
    }

    val payloadInfo = PayloadInfo(
        imageCompressionType = format.string,
        imageCompressionQuality = imageSettings.first.toFloat(),
        imagePayloadSizeInBytes = imagePayloadSize
    )

    return Pair(verificationFramesData, payloadInfo)
}
