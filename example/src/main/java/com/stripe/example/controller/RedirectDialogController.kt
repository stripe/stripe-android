package com.stripe.example.controller

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AlertDialog

import com.stripe.android.model.Card
import com.stripe.example.R

/**
 * Controller for the redirect dialog used to direct users out of the application.
 */
class RedirectDialogController(private val activity: Activity) {
    private var alertDialog: AlertDialog? = null

    fun showDialog(redirectUrl: String, sourceCardData: Map<String, *>) {
        val brand = sourceCardData["brand"] as String?
        val alertDialog = AlertDialog.Builder(activity, R.style.AlertDialogStyle)
            .setTitle(activity.getString(R.string.authentication_dialog_title))
            .setMessage(activity.getString(R.string.authentication_dialog_message,
                brand, sourceCardData["last4"]))
            .setIcon(Card.getBrandIcon(brand))
            .setPositiveButton(android.R.string.yes) { _, _ ->
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(redirectUrl)))
            }
            .setNegativeButton(android.R.string.no, null)
            .create()
        alertDialog.show()

        this.alertDialog = alertDialog
    }

    fun dismissDialog() {
        alertDialog?.dismiss()
        alertDialog = null
    }
}
