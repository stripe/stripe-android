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
class RedirectDialogController(private val mActivity: Activity) {
    private var mAlertDialog: AlertDialog? = null

    fun showDialog(url: String, sourceCardData: Map<String, *>) {
        val brand = sourceCardData["brand"] as String?
        mAlertDialog = AlertDialog.Builder(mActivity)
            .setTitle(mActivity.getString(R.string.authentication_dialog_title))
            .setMessage(mActivity.getString(R.string.authentication_dialog_message,
                brand, sourceCardData["last4"]))
            .setIcon(Card.getBrandIcon(brand))
            .setPositiveButton(android.R.string.yes) { _, _ ->
                mActivity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
            .setNegativeButton(android.R.string.no, null)
            .create()
        mAlertDialog!!.show()
    }

    fun dismissDialog() {
        if (mAlertDialog != null) {
            mAlertDialog!!.dismiss()
            mAlertDialog = null
        }
    }
}
