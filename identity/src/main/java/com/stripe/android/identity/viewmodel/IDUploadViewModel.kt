package com.stripe.android.identity.viewmodel

import android.content.Context
import android.net.Uri
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.identity.states.IdentityScanState.ScanType
import com.stripe.android.identity.utils.ImageChooser
import com.stripe.android.identity.utils.PhotoTaker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * View model for IDUploadFragment, responsible for picking images for front of ID and back of ID,
 * either through camera or from local file storage.
 */
internal class IDUploadViewModel : ViewModel() {
    /**
     * The ID front image has been picked, either through camera or local file
     */
    internal val frontPicked = MutableLiveData<Uri>()

    /**
     * The ID back image has been picked, either through camera or local file
     */
    internal val backPicked = MutableLiveData<Uri>()

    /**
     * The ID front image has been uploaded
     */
    internal val frontUploaded = MutableLiveData<Unit>()

    /**
     * The ID back image has been uploaded
     */
    internal val backUploaded = MutableLiveData<Unit>()

    /**
     * Both front and back of ID are uploaded
     */
    internal val uploadFinished = object : MediatorLiveData<Unit>() {
        private var frontUploaded = false
        private var backUploaded = false

        init {
            addSource(this@IDUploadViewModel.frontUploaded) {
                frontUploaded = true
                postValueWhenBothUploaded()
            }
            addSource(this@IDUploadViewModel.backUploaded) {
                backUploaded = true
                postValueWhenBothUploaded()
            }
        }

        private fun postValueWhenBothUploaded() {
            if (frontUploaded && backUploaded) {
                postValue(Unit)
            }
        }
    }

    private lateinit var frontPhotoTaker: PhotoTaker
    private lateinit var backPhotoTaker: PhotoTaker
    private lateinit var frontImageChooser: ImageChooser
    private lateinit var backImageChooser: ImageChooser

    /**
     * Registers for the [ActivityResultLauncher]s to take photo or pick image, should be called
     * during initialization of an Activity or Fragment.
     */
    internal fun registerActivityResultCaller(activityResultCaller: ActivityResultCaller) {
        frontPhotoTaker = PhotoTaker(activityResultCaller)
        backPhotoTaker = PhotoTaker(activityResultCaller)
        frontImageChooser = ImageChooser(activityResultCaller)
        backImageChooser = ImageChooser(activityResultCaller)
    }

    /**
     * Takes a photo for corresponding ScanType, notifies its corresponding live data when finished.
     */
    internal fun takePhoto(
        scanType: ScanType,
        context: Context
    ) {
        if (scanType == ScanType.ID_FRONT) {
            frontPhotoTaker.takePhoto(context, frontPicked::postValue)
        } else if (scanType == ScanType.ID_BACK) {
            backPhotoTaker.takePhoto(context, backPicked::postValue)
        }
    }

    /**
     * Choose an image for corresponding ScanType, notifies its corresponding live data when
     * finished.
     */
    internal fun chooseImage(
        scanType: ScanType
    ) {
        if (scanType == ScanType.ID_FRONT) {
            frontImageChooser.chooseImage(frontPicked::postValue)
        } else if (scanType == ScanType.ID_BACK) {
            backImageChooser.chooseImage(backPicked::postValue)
        }
    }

    /**
     * Upload the chosen image for corresponding ScanType, notifies its corresponding live data when
     * finished.
     * TODO(ccen): Implement upload functions.s
     */
    internal fun uploadImage(
        uri: Uri,
        scanType: ScanType
    ) {
        viewModelScope.launch {
            delay(1000)
            if (scanType == ScanType.ID_FRONT) {
                frontUploaded.postValue(Unit)
            } else if (scanType == ScanType.ID_BACK) {
                backUploaded.postValue(Unit)
            }
        }
    }

    internal class IDUploadViewModelFactory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return IDUploadViewModel() as T
        }
    }
}
