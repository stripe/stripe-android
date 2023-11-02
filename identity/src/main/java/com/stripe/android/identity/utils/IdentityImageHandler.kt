package com.stripe.android.identity.utils

import android.content.Context
import android.net.Uri
import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.SavedStateHandle
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

    fun registerActivityResultCaller(
        activityResultCaller: ActivityResultCaller,
        savedStateHandle: SavedStateHandle,
        onFrontPhotoTaken: (Uri) -> Unit,
        onBackPhotoTaken: (Uri) -> Unit,
        onFrontImageChosen: (Uri) -> Unit,
        onBackImageChosen: (Uri) -> Unit
    ) {
        frontPhotoTaker = PhotoTaker(
            activityResultCaller,
            identityIO,
            onFrontPhotoTaken,
            savedStateHandle,
            FRONT_PHOTO_URI
        )
        backPhotoTaker = PhotoTaker(
            activityResultCaller,
            identityIO,
            onBackPhotoTaken,
            savedStateHandle,
            BACK_PHOTO_URI
        )
        frontImageChooser = ImageChooser(activityResultCaller, onFrontImageChosen)
        backImageChooser = ImageChooser(activityResultCaller, onBackImageChosen)
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
    }
}
