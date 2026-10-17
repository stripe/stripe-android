package com.stripe.android.paymentsheet.example.playground.checkout

import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutPlaygroundSettings

internal class SettingsImportExport(
    private val activity: AppCompatActivity,
    private val settings: () -> CheckoutPlaygroundSettings,
) {
    private val importLauncher = activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult

        runCatching {
            activity.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("Unable to read the selected file")
        }.fold(
            onSuccess = { json ->
                settings().importJson(json).fold(
                    onSuccess = { showToast("Settings imported") },
                    onFailure = { error -> showToast("Could not import settings: ${error.message}") },
                )
            },
            onFailure = { error -> showToast("Could not read settings: ${error.message}") },
        )
    }

    private val exportLauncher = activity.registerForActivityResult(
        ActivityResultContracts.CreateDocument(JSON_MIME_TYPE)
    ) { uri ->
        uri ?: return@registerForActivityResult

        runCatching {
            activity.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                writer.write(settings().asJsonString())
            } ?: error("Unable to write the selected file")
        }.fold(
            onSuccess = { showToast("Settings exported") },
            onFailure = { error -> showToast("Could not export settings: ${error.message}") },
        )
    }

    fun importSettings() {
        importLauncher.launch(arrayOf(JSON_MIME_TYPE, "text/plain"))
    }

    fun exportSettings() {
        exportLauncher.launch(DEFAULT_SETTINGS_FILE_NAME)
    }

    private fun showToast(message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
    }

    private companion object {
        const val JSON_MIME_TYPE = "application/json"
        const val DEFAULT_SETTINGS_FILE_NAME = "checkout-settings.json"
    }
}
