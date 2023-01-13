package com.stripe.android.identity.utils

import android.content.Context
import android.net.Uri
import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.identity.states.IdentityScanState
import javax.inject.Inject

/**
 * Handler to pick an image or take a photo.
 */
internal class IdentityImageHandler @Inject constructor(
    private val identityIO: IdentityIO
) {
    private lateinit var frontPhotoTaker: PhotoTaker
    private lateinit var backPhotoTaker: PhotoTaker
    private lateinit var frontImageChooser: ImageChooser
    private lateinit var backImageChooser: ImageChooser

    // [ScanType] for front and back, might be updated when current screen changes.
    private var frontScanType: IdentityScanState.ScanType? = null
    private var backScanType: IdentityScanState.ScanType? = null

    fun registerActivityResultCaller(
        activityResultCaller: ActivityResultCaller,
        savedStateHandle: SavedStateHandle,
        onFrontPhotoTaken: (Uri, IdentityScanState.ScanType?) -> Unit,
        onBackPhotoTaken: (Uri, IdentityScanState.ScanType?) -> Unit,
        onFrontImageChosen: (Uri, IdentityScanState.ScanType?) -> Unit,
        onBackImageChosen: (Uri, IdentityScanState.ScanType?) -> Unit
    ) {
        frontScanType = savedStateHandle.get<IdentityScanState.ScanType>(FRONT_SCAN_TYPE)
        backScanType = savedStateHandle.get<IdentityScanState.ScanType>(BACK_SCAN_TYPE)

        frontPhotoTaker = PhotoTaker(
            activityResultCaller,
            identityIO,
            {
                onFrontPhotoTaken(it, frontScanType)
            },
            savedStateHandle,
            FRONT_PHOTO_URI
        )
        backPhotoTaker = PhotoTaker(
            activityResultCaller,
            identityIO,
            {
                onBackPhotoTaken(it, backScanType)
            },
            savedStateHandle,
            BACK_PHOTO_URI
        )
        frontImageChooser = ImageChooser(activityResultCaller) {
            onFrontImageChosen(it, frontScanType)
        }
        backImageChooser = ImageChooser(activityResultCaller) {
            onBackImageChosen(it, backScanType)
        }
    }

    fun updateScanTypes(
        frontScanType: IdentityScanState.ScanType,
        backScanType: IdentityScanState.ScanType?
    ) {
        this.frontScanType = frontScanType
        this.backScanType = backScanType
    }

    /**
     * Takes a photo for front.
     */
    fun takePhotoFront(
        context: Context
    ) {
        frontPhotoTaker.takePhoto(context)
    }

    /**
     * Takes a photo for back.
     */
    fun takePhotoBack(
        context: Context
    ) {
        backPhotoTaker.takePhoto(context)
    }

    /**
     * Choose an image for front.
     */
    fun chooseImageFront() {
        frontImageChooser.chooseImage()
    }

    /**
     * Choose an image for back.
     */
    fun chooseImageBack() {
        backImageChooser.chooseImage()
    }

    companion object {
        const val FRONT_PHOTO_URI = "front_photo_uri"
        const val BACK_PHOTO_URI = "back_photo_uri"
        const val FRONT_SCAN_TYPE = "front_scan_type"
        const val BACK_SCAN_TYPE = "back_scan_type"
    }
}
