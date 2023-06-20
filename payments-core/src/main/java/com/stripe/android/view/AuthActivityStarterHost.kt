package com.stripe.android.view

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.annotation.RestrictTo
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.view.AuthActivityStarterHost.ActivityHost
import com.stripe.android.view.AuthActivityStarterHost.FragmentHost

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

    abstract val statusBarColor: Int?
    abstract val lifecycleOwner: LifecycleOwner
    abstract val application: Application

    internal class ActivityHost(
        val activity: ComponentActivity,
        override val statusBarColor: Int?
    ) : AuthActivityStarterHost() {

        override val application: Application
            get() = activity.application

        override val lifecycleOwner: LifecycleOwner = activity

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

        override val application: Application
            get() = fragment.requireActivity().application

        override val lifecycleOwner: LifecycleOwner = fragment

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

    internal companion object {
        @JvmSynthetic
        internal fun create(
            fragment: Fragment
        ): AuthActivityStarterHost {
            val activity = fragment.requireActivity()
            return FragmentHost(
                fragment = fragment,
                statusBarColor = activity.window?.statusBarColor
            )
        }

        @JvmSynthetic
        internal fun create(
            activity: ComponentActivity,
            statusBarColor: Int? = activity.window?.statusBarColor,
        ): AuthActivityStarterHost {
            return ActivityHost(
                activity = activity,
                statusBarColor = statusBarColor,
            )
        }
    }
}
