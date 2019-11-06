package com.stripe.example.controller

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentManager
import com.stripe.example.dialog.ProgressDialogFragment
import java.lang.ref.WeakReference

/**
 * Class used to show and hide the progress spinner.
 */
class ProgressDialogController(
    private val fragmentManager: FragmentManager,
    private val res: Resources
) {
    private var progressFragmentRef: WeakReference<ProgressDialogFragment>? = null

    private val dialogFragment: ProgressDialogFragment?
        get() = progressFragmentRef?.get()

    fun show(@StringRes resId: Int) {
        dismiss()
        val progressDialogFragment =
            ProgressDialogFragment.newInstance(res.getString(resId))
        progressDialogFragment.show(fragmentManager, "progress")
        progressFragmentRef = WeakReference(progressDialogFragment)
    }

    fun dismiss() {
        dialogFragment?.let {
            it.dismiss()
            progressFragmentRef?.clear()
            progressFragmentRef = null
        }
    }
}
