package com.stripe.android.identity.viewmodel

import android.content.Context
import android.net.Uri
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.stripe.android.identity.utils.IdentityIO
import com.stripe.android.identity.utils.ImageChooser
import com.stripe.android.identity.utils.PhotoTaker

/**
 * ViewModel to upload front and back image of a document either through camera or from local
 * file storage.
 */
internal class IdentityUploadViewModel(
    private val identityIO: IdentityIO,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private lateinit var frontPhotoTaker: PhotoTaker
    private lateinit var backPhotoTaker: PhotoTaker
    private lateinit var frontImageChooser: ImageChooser
    private lateinit var backImageChooser: ImageChooser

    /**
     * Registers for the [ActivityResultLauncher]s to take photo or pick image, should be called
     * during initialization of an Activity or Fragment.
     */
    internal fun registerActivityResultCaller(
        activityResultCaller: ActivityResultCaller,
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

    internal class FrontBackUploadViewModelFactory(
        ownerProvider: () -> SavedStateRegistryOwner,
        private val identityIO: IdentityIO
    ) : AbstractSavedStateViewModelFactory(ownerProvider(), null) {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            key: String,
            modelClass: Class<T>,
            handle: SavedStateHandle
        ): T {
            return IdentityUploadViewModel(identityIO, handle) as T
        }
    }
}
