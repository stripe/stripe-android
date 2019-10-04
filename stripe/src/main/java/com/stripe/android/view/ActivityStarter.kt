package com.stripe.android.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.Fragment

abstract class ActivityStarter<TargetActivityType : Activity, ArgsType : ActivityStarter.Args> internal constructor(
    private val activity: Activity,
    private val fragment: Fragment? = null,
    private val targetClass: Class<TargetActivityType>,
    private val defaultArgs: ArgsType,
    private val requestCode: Int
) {
    internal constructor(
        activity: Activity,
        targetClass: Class<TargetActivityType>,
        args: ArgsType,
        requestCode: Int
    ) : this(
        activity = activity,
        targetClass = targetClass,
        defaultArgs = args,
        requestCode = requestCode
    )

    internal constructor(
        fragment: Fragment,
        targetClass: Class<TargetActivityType>,
        args: ArgsType,
        requestCode: Int
    ) : this(
        activity = fragment.requireActivity(),
        fragment = fragment,
        targetClass = targetClass,
        defaultArgs = args,
        requestCode = requestCode
    )

    @JvmOverloads
    fun startForResult(args: ArgsType = defaultArgs) {
        val intent = Intent(activity, targetClass)
            .putExtra(Args.EXTRA, args)

        if (fragment != null) {
            fragment.startActivityForResult(intent, requestCode)
        } else {
            activity.startActivityForResult(intent, requestCode)
        }
    }

    interface Args : Parcelable {
        companion object {
            const val EXTRA: String = "extra_activity_args"
        }
    }

    interface Result : Parcelable {
        fun toBundle(): Bundle

        companion object {
            const val EXTRA: String = "extra_activity_result"
        }
    }
}
