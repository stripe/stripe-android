package com.stripe.android.identity.viewmodel

import android.content.Context
import android.net.Uri
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.identity.utils.IdentityIO
import com.stripe.android.identity.utils.ImageChooser
import com.stripe.android.identity.utils.PhotoTaker

/**
 * ViewModel to upload front and back image of a document either through camera or from local
 * file storage.
 */
internal class IdentityUploadViewModel(
    private val identityIO: IdentityIO
) : ViewModel() {

    private lateinit var frontPhotoTaker: PhotoTaker
    private lateinit var backPhotoTaker: PhotoTaker
    private lateinit var frontImageChooser: ImageChooser
    private lateinit var backImageChooser: ImageChooser

    /**
     * Registers for the [ActivityResultLauncher]s to take photo or pick image, should be called
     * during initialization of an Activity or Fragment.
     */
    internal fun registerActivityResultCaller(activityResultCaller: ActivityResultCaller) {
        frontPhotoTaker = PhotoTaker(activityResultCaller, identityIO)
        backPhotoTaker = PhotoTaker(activityResultCaller, identityIO)
        frontImageChooser = ImageChooser(activityResultCaller)
        backImageChooser = ImageChooser(activityResultCaller)
    }

    /**
     * Takes a photo for front.
     */
    fun takePhotoFront(
        context: Context,
        onPhotoTaken: (Uri) -> Unit
    ) {
        frontPhotoTaker.takePhoto(context, onPhotoTaken)
    }

    /**
     * Takes a photo for back.
     */
    fun takePhotoBack(
        context: Context,
        onPhotoTaken: (Uri) -> Unit
    ) {
        backPhotoTaker.takePhoto(context, onPhotoTaken)
    }

    /**
     * Choose an image for front.
     */
    fun chooseImageFront(
        onImageChosen: (Uri) -> Unit
    ) {
        frontImageChooser.chooseImage(onImageChosen)
    }

    /**
     * Choose an image for back.
     */
    fun chooseImageBack(
        onImageChosen: (Uri) -> Unit
    ) {
        backImageChooser.chooseImage(onImageChosen)
    }

    internal class FrontBackUploadViewModelFactory(
        private val identityIO: IdentityIO
    ) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return IdentityUploadViewModel(identityIO) as T
        }
    }
}
