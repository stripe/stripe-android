package com.stripe.android.identity.utils

import android.content.Context
import android.content.DialogInterface.OnClickListener
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog

internal data class AlertButton(
    @StringRes val buttonRes: Int,
    val onClickListener: OnClickListener? = null
)

internal fun showAlertDialog(
    context: Context,
    @StringRes titleRes: Int,
    positiveButton: AlertButton? = null,
    negativeButton: AlertButton? = null
) {
    val builder = AlertDialog.Builder(context)
    builder.setMessage(titleRes)
    positiveButton?.let {
        builder.setPositiveButton(positiveButton.buttonRes, positiveButton.onClickListener)
    }
    negativeButton?.let {
        builder.setNegativeButton(negativeButton.buttonRes, negativeButton.onClickListener)
    }
    builder.show()
}
