package com.stripe.android.identity.viewmodel

import android.content.Context
import android.net.Uri
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.identity.utils.ImageChooser
import com.stripe.android.identity.utils.PhotoTaker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class PassportUploadViewModel : ViewModel() {

    /**
     * The passport image is uploaded.
     */
    private val _uploaded = MutableLiveData<Unit>()
    val uploaded: LiveData<Unit> = _uploaded

    private lateinit var photoTaker: PhotoTaker
    private lateinit var imageChooser: ImageChooser

    /**
     * Registers for the [ActivityResultLauncher]s to take photo or pick image, should be called
     * during initialization of an Activity or Fragment.
     */
    internal fun registerActivityResultCaller(activityResultCaller: ActivityResultCaller) {
        photoTaker = PhotoTaker(activityResultCaller)
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

    /**
     * Upload the chosen image, notifies [uploaded] when finished.
     * TODO(ccen): Implement upload functions.
     */
    fun uploadImage(
        uri: Uri
    ) {
        viewModelScope.launch {
            delay(1000)
            _uploaded.postValue(Unit)
        }
    }

    internal class PassportUploadViewModelFactory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PassportUploadViewModel() as T
        }
    }
}
