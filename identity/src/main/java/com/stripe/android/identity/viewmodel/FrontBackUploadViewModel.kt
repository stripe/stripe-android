package com.stripe.android.identity.viewmodel

import android.content.Context
import android.net.Uri
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.core.model.InternalStripeFilePurpose
import com.stripe.android.identity.IdentityVerificationSheetContract
import com.stripe.android.identity.networking.IdentityRepository
import com.stripe.android.identity.utils.ImageChooser
import com.stripe.android.identity.utils.PhotoTaker
import com.stripe.android.identity.utils.resizeUriAndCreateFileToUpload
import kotlinx.coroutines.launch

/**
 * ViewModel to upload front and back image of a document either through camera or from local
 * file storage.
 */
internal class FrontBackUploadViewModel(
    private val identityRepository: IdentityRepository,
    private val verificationArgs: IdentityVerificationSheetContract.Args
) : ViewModel() {

    /**
     * The ID front image has been uploaded
     */
    private val _frontUploaded = MutableLiveData<Unit>()
    val frontUploaded: LiveData<Unit> = _frontUploaded

    /**
     * The ID back image has been uploaded
     */
    private val _backUploaded = MutableLiveData<Unit>()
    val backUploaded: LiveData<Unit> = _backUploaded

    /**
     * Both front and back of ID are uploaded
     */
    val uploadFinished = object : MediatorLiveData<Unit>() {
        private var frontUploaded = false
        private var backUploaded = false

        init {
            addSource(this@FrontBackUploadViewModel.frontUploaded) {
                frontUploaded = true
                postValueWhenBothUploaded()
            }
            addSource(this@FrontBackUploadViewModel.backUploaded) {
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

    /**
     * Upload the chosen image for front, notifies its corresponding live data when
     * finished.
     */
    fun uploadImageFront(
        uri: Uri,
        context: Context
    ) {
        viewModelScope.launch {
            identityRepository.uploadImage(
                verificationId = verificationArgs.verificationSessionId,
                ephemeralKey = verificationArgs.ephemeralKeySecret,
                imageFile = resizeUriAndCreateFileToUpload(
                    context,
                    uri,
                    verificationArgs.verificationSessionId,
                    // TODO(ccen) upload both full frame and non full frame
                    true,
                    FRONT
                ),
                // TODO(ccen) pass it over from VerificationPage response
                filePurpose = InternalStripeFilePurpose.IdentityPrivate
            )

            _frontUploaded.postValue(Unit)
        }
    }

    /**
     * Upload the chosen image for back, notifies its corresponding live data when
     * finished.
     */
    fun uploadImageBack(
        uri: Uri,
        context: Context
    ) {
        viewModelScope.launch {
            identityRepository.uploadImage(
                verificationId = verificationArgs.verificationSessionId,
                ephemeralKey = verificationArgs.ephemeralKeySecret,
                imageFile = resizeUriAndCreateFileToUpload(
                    context,
                    uri,
                    verificationArgs.verificationSessionId,
                    // TODO(ccen) upload both full frame and non full frame
                    true,
                    BACK
                ),
                // TODO(ccen) pass it over from VerificationPage response
                filePurpose = InternalStripeFilePurpose.IdentityPrivate
            )

            _backUploaded.postValue(Unit)
        }
    }

    internal class FrontBackUploadViewModelFactory(
        private val identityRepository: IdentityRepository,
        private val verificationArgs: IdentityVerificationSheetContract.Args
    ) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FrontBackUploadViewModel(identityRepository, verificationArgs) as T
        }
    }

    private companion object {
        const val FRONT = "front"
        const val BACK = "back"
    }
}
