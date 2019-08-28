package com.stripe.example.controller

import android.content.res.Resources
import android.support.annotation.StringRes
import android.support.v4.app.FragmentManager

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
        get() = if (progressFragmentRef != null) progressFragmentRef!!.get() else null

    fun show(@StringRes resId: Int) {
        dismiss()
        val progressDialogFragment = ProgressDialogFragment.newInstance(res.getString(resId))
        progressDialogFragment.show(fragmentManager, "progress")
        progressFragmentRef = WeakReference(progressDialogFragment)
    }

    fun dismiss() {
        val progressDialogFragment = dialogFragment
        if (progressDialogFragment != null) {
            progressDialogFragment.dismiss()

            if (progressFragmentRef != null) {
                progressFragmentRef!!.clear()
                progressFragmentRef = null
            }
        }
    }
}
