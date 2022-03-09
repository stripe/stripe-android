package com.stripe.android.identity.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

/**
 * A class to take a photo through camera.
 */
internal class PhotoTaker(
    activityResultCaller: ActivityResultCaller
) {

    private var newPhotoTakenUri: Uri? = null

    private var onPhotoTaken: ((Uri) -> Unit)? = null

    private val takePhotoLauncher: ActivityResultLauncher<Intent> =
        activityResultCaller.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            requireNotNull(onPhotoTaken) {
                "onPhotoTaken callback is not set."
            }(
                requireNotNull(newPhotoTakenUri) {
                    "newPhotoTakeUri is still null after a photo is taken."
                }
            )
        }

    /**
     * Starts an Activity to take a photo with camera, saves it locally to the app's internal file
     * storage and posts its Uri to [onPhotoTaken] when finished.
     */
    // TODO(ccen): add error/exception
    internal fun takePhoto(context: Context, onPhotoTaken: (Uri) -> Unit) {
        this.onPhotoTaken = onPhotoTaken

        takePhotoLauncher.launch(
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                // Ensure that there's a camera activity to handle the intent
                takePictureIntent.resolveActivity(context.packageManager).also {
                    createInternalFileUri(context).also {
                        newPhotoTakenUri = it.contentUri
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, newPhotoTakenUri)
                    }
                }
            }
        )
    }
}
