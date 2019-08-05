package com.stripe.example.controller

import android.support.v7.app.AppCompatActivity

import com.stripe.example.R
import com.stripe.example.dialog.ErrorDialogFragment

import java.lang.ref.WeakReference

/**
 * A convenience class to handle displaying error dialogs.
 */
class ErrorDialogHandler(activity: AppCompatActivity) {

    private val activityRef: WeakReference<AppCompatActivity> = WeakReference(activity)

    fun show(errorMessage: String) {
        val activity = activityRef.get() ?: return

        ErrorDialogFragment.newInstance(activity.getString(R.string.validationErrors), errorMessage)
            .show(activity.supportFragmentManager, "error")
    }
}
