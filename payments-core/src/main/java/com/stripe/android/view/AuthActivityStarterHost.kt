package com.stripe.android.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.annotation.RestrictTo
import androidx.fragment.app.Fragment

/**
 * A representation of an Android component (i.e. [ComponentActivity] or [Fragment]) that can start
 * an activity. [ActivityHost] and [FragmentHost] hold references to Android components, so they
 * should only be used in a lifecycle-aware scope.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class AuthActivityStarterHost {
    abstract fun startActivityForResult(
        target: Class<*>,
        extras: Bundle,
        requestCode: Int
    )

    internal abstract val statusBarColor: Int?

    internal class ActivityHost(
        val activity: ComponentActivity,
        override val statusBarColor: Int?
    ) : AuthActivityStarterHost() {

        @Suppress("DEPRECATION")
        override fun startActivityForResult(
            target: Class<*>,
            extras: Bundle,
            requestCode: Int
        ) {
            val intent = Intent(activity, target).putExtras(extras)

            activity.startActivityForResult(intent, requestCode)
        }
    }

    internal class FragmentHost(
        val fragment: Fragment,
        override val statusBarColor: Int?
    ) : AuthActivityStarterHost() {

        @Suppress("DEPRECATION")
        override fun startActivityForResult(
            target: Class<*>,
            extras: Bundle,
            requestCode: Int
        ) {
            val intent = Intent(fragment.activity, target).putExtras(extras)
            if (fragment.isAdded) {
                fragment.startActivityForResult(intent, requestCode)
            }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        @JvmSynthetic
        fun create(
            fragment: Fragment
        ): AuthActivityStarterHost {
            val activity = fragment.requireActivity()
            return FragmentHost(
                fragment = fragment,
                statusBarColor = activity.window?.statusBarColor
            )
        }

        @JvmSynthetic
        fun create(
            activity: ComponentActivity
        ): AuthActivityStarterHost {
            return ActivityHost(
                activity = activity,
                statusBarColor = activity.window?.statusBarColor
            )
        }
    }
}
