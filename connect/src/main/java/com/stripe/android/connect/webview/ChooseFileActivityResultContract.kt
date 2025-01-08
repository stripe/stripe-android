package com.stripe.android.connect.webview

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import androidx.activity.result.contract.ActivityResultContract

/**
 * Contract for launching a file chooser activity and handling its result.
 * Used to select files for upload in Connect web flows.
 */
internal class ChooseFileActivityResultContract : ActivityResultContract<Intent, Array<Uri>?>() {
    override fun createIntent(context: Context, input: Intent): Intent {
        return input
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Array<Uri>? {
        return WebChromeClient.FileChooserParams.parseResult(resultCode, intent)
    }
}
