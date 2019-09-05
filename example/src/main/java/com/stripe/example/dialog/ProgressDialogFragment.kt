package com.stripe.example.dialog

import android.app.Dialog
import android.app.ProgressDialog
import android.os.Bundle

class ProgressDialogFragment : androidx.fragment.app.DialogFragment() {

    private val message: String?
        get() = if (arguments != null) arguments!!.getString("message") else null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = ProgressDialog(activity)
        dialog.setMessage(message)
        return dialog
    }

    companion object {
        fun newInstance(message: String): ProgressDialogFragment {
            val fragment = ProgressDialogFragment()

            val args = Bundle()
            args.putString("message", message)

            fragment.arguments = args

            return fragment
        }
    }
}
