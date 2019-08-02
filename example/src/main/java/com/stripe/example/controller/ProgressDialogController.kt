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
    private val mFragmentManager: FragmentManager,
    private val mRes: Resources
) {
    private var mProgressFragmentRef: WeakReference<ProgressDialogFragment>? = null

    private val dialogFragment: ProgressDialogFragment?
        get() = if (mProgressFragmentRef != null) mProgressFragmentRef!!.get() else null

    fun show(@StringRes resId: Int) {
        dismiss()
        val progressDialogFragment = ProgressDialogFragment.newInstance(mRes.getString(resId))
        progressDialogFragment.show(mFragmentManager, "progress")
        mProgressFragmentRef = WeakReference(progressDialogFragment)
    }

    fun dismiss() {
        val progressDialogFragment = dialogFragment
        if (progressDialogFragment != null) {
            progressDialogFragment.dismiss()

            if (mProgressFragmentRef != null) {
                mProgressFragmentRef!!.clear()
                mProgressFragmentRef = null
            }
        }
    }
}
