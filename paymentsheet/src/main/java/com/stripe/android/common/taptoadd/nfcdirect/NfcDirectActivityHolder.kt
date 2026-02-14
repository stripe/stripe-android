package com.stripe.android.common.taptoadd.nfcdirect

import android.app.Activity
import androidx.annotation.RestrictTo
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.lang.ref.WeakReference

/**
 * Holds a reference to the current Activity for NFC Direct operations.
 *
 * NFC Reader Mode requires an Activity reference. This holder allows the
 * NfcDirectConnectionManager to be created via Dagger (which only has Context)
 * and get the Activity reference when needed.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object NfcDirectActivityHolder {
    private var activityRef: WeakReference<Activity>? = null

    /**
     * Set the current activity. Call this from your Activity's onCreate.
     */
    fun set(activity: Activity, lifecycleOwner: LifecycleOwner) {
        activityRef = WeakReference(activity)

        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    if (activityRef?.get() === activity) {
                        activityRef = null
                    }
                }
            }
        )
    }

    /**
     * Get the current activity, or null if not available.
     */
    fun get(): Activity? = activityRef?.get()
}
