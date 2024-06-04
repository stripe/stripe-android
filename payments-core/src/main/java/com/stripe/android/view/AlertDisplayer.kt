package com.stripe.android.view

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.stripe.android.R

internal interface AlertDisplayer {
    fun show(message: String)

    class DefaultAlertDisplayer(
        private val activity: Activity
    ) : AlertDisplayer {
        override fun show(message: String) {
            if (!activity.isFinishing) {
                AlertDialog.Builder(activity, R.style.StripeAlertDialogStyle)
                    .setMessage(message)
                    .setCancelable(true)
                    .setPositiveButton(android.R.string.ok) { dialogInterface, _ ->
                        dialogInterface.dismiss()
                    }
                    .create()
                    .show()
            }
        }
    }
}
