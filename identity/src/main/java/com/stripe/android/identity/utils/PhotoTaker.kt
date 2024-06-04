package com.stripe.android.identity.utils

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.SavedStateHandle

/**
 * A class to take a photo through camera.
 */
internal class PhotoTaker(
    activityResultCaller: ActivityResultCaller,
    identityIO: IdentityIO,
    onPhotoTaken: (Uri) -> Unit,
    savedStateHandle: SavedStateHandle,
    savedUriId: String
) {
    private val newPhotoTakenUri: Uri =
        savedStateHandle.get<Uri>(savedUriId) ?: run {
            val newUri = identityIO.createInternalFileUri().contentUri
            savedStateHandle.set(savedUriId, newUri)
            newUri
        }

    private val takePhotoLauncher: ActivityResultLauncher<Intent> =
        activityResultCaller.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == RESULT_OK) {
                onPhotoTaken(
                    newPhotoTakenUri
                )
            }
        }

    /**
     * Starts an Activity to take a photo with camera, saves it locally to the app's internal file
     * storage and posts its Uri to [onPhotoTaken] when finished.
     */
    internal fun takePhoto(context: Context) {
        takePhotoLauncher.launch(
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                // Ensure that there's a camera activity to handle the intent
                takePictureIntent.resolveActivity(context.packageManager).also {
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, newPhotoTakenUri)
                }
            }
        )
    }
}
