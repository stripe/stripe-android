package com.stripe.android.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import java.lang.ref.WeakReference

internal interface AuthActivityStarter<ArgsType> {
    fun start(args: ArgsType)

    /**
     * A representation of an object (i.e. Activity or Fragment) that can start an activity.
     */
    class Host internal constructor(
        activity: Activity,
        fragment: Fragment?,
        val statusBarColor: Int?
    ) {
        private val activityRef: WeakReference<Activity> = WeakReference(activity)
        private val fragmentRef: WeakReference<Fragment>? = fragment?.let { WeakReference(it) }

        internal val activity: Activity?
            get() = activityRef.get()

        internal val fragment: Fragment?
            get() = fragmentRef?.get()

        internal fun startActivityForResult(
            target: Class<*>,
            extras: Bundle,
            requestCode: Int
        ) {
            val activity = activityRef.get() ?: return

            val intent = Intent(activity, target).putExtras(extras)

            if (fragmentRef != null) {
                val fragment = fragmentRef.get()
                if (fragment?.isAdded == true) {
                    fragment.startActivityForResult(intent, requestCode)
                }
            } else {
                activity.startActivityForResult(intent, requestCode)
            }
        }

        internal companion object {
            @JvmSynthetic
            internal fun create(
                fragment: Fragment
            ): Host {
                val activity = fragment.requireActivity()
                return Host(
                    activity = activity,
                    fragment = fragment,
                    statusBarColor = activity.window?.statusBarColor
                )
            }

            @JvmSynthetic
            internal fun create(
                activity: Activity
            ): Host {
                return Host(
                    activity = activity,
                    fragment = null,
                    statusBarColor = activity.window?.statusBarColor
                )
            }
        }
    }
}
