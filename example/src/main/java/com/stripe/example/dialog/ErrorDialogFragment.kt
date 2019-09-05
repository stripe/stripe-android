package com.stripe.example.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import com.stripe.example.R

class ErrorDialogFragment : androidx.fragment.app.DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val title = arguments!!.getString("title")
        val message = arguments!!.getString("message")

        return AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.ok) { dialogInterface, _ -> dialogInterface.dismiss() }
            .create()
    }

    companion object {
        fun newInstance(title: String, message: String): ErrorDialogFragment {
            val fragment = ErrorDialogFragment()

            val args = Bundle()
            args.putString("title", title)
            args.putString("message", message)

            fragment.arguments = args

            return fragment
        }
    }
}
