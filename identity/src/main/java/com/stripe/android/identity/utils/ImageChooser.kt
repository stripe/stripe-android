package com.stripe.android.identity.utils

import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts

/**
 * A class to choose image from local gallery.
 */
internal class ImageChooser(
    activityResultCaller: ActivityResultCaller
) {
    private var onImageChosen: ((Uri) -> Unit)? = null

    private val chooseImageLauncher = activityResultCaller.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        requireNotNull(onImageChosen) {
            "onImageChosen callback is not set."
        }(
            requireNotNull(it?.data?.data) {
                "Intent is null after a image is chosen."
            }
        )
    }

    /**
     * Start an Activity to choose an image from gallery, posts its Uri to [onImageChosen] when finished.
     */
    // TODO(ccen): add error/exception
    internal fun chooseImage(onImageChosen: (Uri) -> Unit) {
        this.onImageChosen = onImageChosen
        chooseImageLauncher.launch(
            Intent(Intent.ACTION_GET_CONTENT).also { it.type = "image/*" }
        )
    }
}
