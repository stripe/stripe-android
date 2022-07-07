package com.stripe.android.identity.utils

import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts

/**
 * A class to choose image from local gallery.
 */
internal class ImageChooser(
    activityResultCaller: ActivityResultCaller,
    onImageChosen: ((Uri) -> Unit)
) {
    private val chooseImageLauncher = activityResultCaller.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        it.data?.data?.let { uri ->
            onImageChosen(uri)
        }
    }

    /**
     * Start an Activity to choose an image from gallery, posts its Uri to [onImageChosen] when finished.
     */
    internal fun chooseImage() {
        chooseImageLauncher.launch(
            Intent(Intent.ACTION_GET_CONTENT).also { it.type = "image/*" }
        )
    }
}
