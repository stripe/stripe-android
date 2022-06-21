package com.stripe.android.camera.framework.image

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ConfigurationInfo
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.util.Size
import androidx.annotation.CheckResult
import androidx.annotation.RestrictTo
import com.stripe.android.camera.framework.util.centerOn
import com.stripe.android.camera.framework.util.intersectionWith
import com.stripe.android.camera.framework.util.maxAspectRatioInSize
import com.stripe.android.camera.framework.util.projectRegionOfInterest
import com.stripe.android.camera.framework.util.toRect

/**
 * Get a rect indicating what part of the preview is actually visible on screen. This assumes that the preview
 * is the same size or larger than the screen in both dimensions.
 */
private fun getVisiblePreview(previewBounds: Rect) = Size(
    previewBounds.right + previewBounds.left,
    previewBounds.bottom + previewBounds.top
)

/**
 * Determine how to crop the preview image from the camera based on the view finder's position in
 * the preview bounds.
 *
 * Note: This algorithm makes some assumptions:
 * 1. the [previewBounds] and the [cameraPreviewImageSize] are centered relative to each other.
 * 2. the [previewBounds] circumscribes the [cameraPreviewImageSize]. I.E. they share at least one
 *    field of view, and the [cameraPreviewImageSize] fields of view are smaller than or the same
 *    size as the [previewBounds]
 * 3. the [previewBounds] and the [cameraPreviewImageSize] have the same orientation
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun determineViewFinderCrop(
    cameraPreviewImageSize: Size,
    previewBounds: Rect,
    viewFinder: Rect
): Rect {
    require(
        viewFinder.left >= previewBounds.left &&
            viewFinder.right <= previewBounds.right &&
            viewFinder.top >= previewBounds.top &&
            viewFinder.bottom <= previewBounds.bottom
    ) { "View finder $viewFinder is outside preview image bounds $previewBounds" }

    // Scale the cardFinder to match the full image
    return previewBounds
        .projectRegionOfInterest(
            toSize = cameraPreviewImageSize,
            regionOfInterest = viewFinder
        )
        .intersectionWith(cameraPreviewImageSize.toRect())
}

/**
 * Crop the preview image from the camera based on the view finder's position in the preview bounds.
 *
 * Note: This algorithm makes some assumptions:
 * 1. the [previewBounds] and the [cameraPreviewImage] are centered relative to each other.
 * 2. the [previewBounds] circumscribes the [cameraPreviewImage]. I.E. they share at least one field
 *    of view, and the [cameraPreviewImage] fields of view are smaller than or the same size as the
 *    [previewBounds]
 * 3. the [previewBounds] and the [cameraPreviewImage] have the same orientation
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun cropCameraPreviewToViewFinder(
    cameraPreviewImage: Bitmap,
    previewBounds: Rect,
    viewFinder: Rect
): Bitmap {
    require(
        viewFinder.left >= previewBounds.left &&
            viewFinder.right <= previewBounds.right &&
            viewFinder.top >= previewBounds.top &&
            viewFinder.bottom <= previewBounds.bottom
    ) { "View finder $viewFinder is outside preview image bounds $previewBounds" }

    return cameraPreviewImage.crop(
        determineViewFinderCrop(cameraPreviewImage.size(), previewBounds, viewFinder)
    )
}

/**
 * Crop the preview image from the camera based on a square surrounding the view finder's position in the preview
 * bounds.
 *
 * Note: This algorithm makes some assumptions:
 * 1. the previewBounds and the cameraPreviewImage are centered relative to each other.
 * 2. the previewBounds circumscribes the cameraPreviewImage. I.E. they share at least one field of view, and the
 *    cameraPreviewImage's fields of view are smaller than or the same size as the previewBounds's
 * 3. the previewBounds and the cameraPreviewImage have the same orientation
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun cropCameraPreviewToSquare(
    cameraPreviewImage: Bitmap,
    previewBounds: Rect,
    viewFinder: Rect
): Bitmap {
    require(
        viewFinder.left >= previewBounds.left &&
            viewFinder.right <= previewBounds.right &&
            viewFinder.top >= previewBounds.top &&
            viewFinder.bottom <= previewBounds.bottom
    ) { "Card finder is outside preview image bounds" }

    val visiblePreview = getVisiblePreview(previewBounds)
    val squareViewFinder = maxAspectRatioInSize(visiblePreview, 1F).centerOn(viewFinder)

    // calculate the projected squareViewFinder
    val projectedSquare = previewBounds.projectRegionOfInterest(
        cameraPreviewImage.size(),
        squareViewFinder
    )
        .intersectionWith(cameraPreviewImage.size().toRect())

    return cameraPreviewImage.crop(projectedSquare)
}

/**
 * Determine if the device supports OpenGL version 3.1.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@CheckResult
fun hasOpenGl31(context: Context): Boolean {
    val openGlVersion = 0x00030001
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val configInfo = activityManager.deviceConfigurationInfo

    val isSupported = if (configInfo.reqGlEsVersion != ConfigurationInfo.GL_ES_VERSION_UNDEFINED) {
        configInfo.reqGlEsVersion >= openGlVersion && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    } else {
        false
    }

    Log.d("Image#hasOpenGl31", "OpenGL is supported? $isSupported")
    return isSupported
}
