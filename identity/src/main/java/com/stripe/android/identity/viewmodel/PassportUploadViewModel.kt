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
 * Fragment to upload passport image.
 */
internal class PassportUploadViewModel(
    private val identityIO: IdentityIO
) : ViewModel() {

    private lateinit var photoTaker: PhotoTaker
    private lateinit var imageChooser: ImageChooser

    /**
     * Registers for the [ActivityResultLauncher]s to take photo or pick image, should be called
     * during initialization of an Activity or Fragment.
     */
    internal fun registerActivityResultCaller(activityResultCaller: ActivityResultCaller) {
        photoTaker = PhotoTaker(activityResultCaller, identityIO)
        imageChooser = ImageChooser(activityResultCaller)
    }

    /**
     * Takes a photo.
     */
    fun takePhoto(
        context: Context,
        onPhotoTaken: (Uri) -> Unit
    ) {
        photoTaker.takePhoto(context, onPhotoTaken)
    }

    /**
     * Choose an image.
     */
    fun chooseImage(
        onImageChosen: (Uri) -> Unit
    ) {
        imageChooser.chooseImage(onImageChosen)
    }

    internal class PassportUploadViewModelFactory(
        private val identityIO: IdentityIO
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PassportUploadViewModel(identityIO) as T
        }
    }
}
