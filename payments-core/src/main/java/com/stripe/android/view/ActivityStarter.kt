package com.stripe.android.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment

// TODO: debatable for API review: we could make this restricted vis I think
/**
 * Superclass for starting Stripe activities.
 */
@Suppress("UnnecessaryAbstractClass")
abstract class ActivityStarter<TargetActivityType : Activity, ArgsType : ActivityStarter.Args> internal constructor(
    private val activity: Activity,
    private val fragment: Fragment? = null,
    private val targetClass: Class<TargetActivityType>,
    private val requestCode: Int,
    private val intentFlags: Int? = null
) {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
    constructor(
        activity: Activity,
        targetClass: Class<TargetActivityType>,
        requestCode: Int,
        intentFlags: Int? = null
    ) : this(
        activity = activity,
        fragment = null,
        targetClass = targetClass,
        requestCode = requestCode,
        intentFlags = intentFlags
    )

    internal constructor(
        fragment: Fragment,
        targetClass: Class<TargetActivityType>,
        requestCode: Int,
        intentFlags: Int? = null
    ) : this(
        activity = fragment.requireActivity(),
        fragment = fragment,
        targetClass = targetClass,
        requestCode = requestCode,
        intentFlags = intentFlags
    )

    fun startForResult(args: ArgsType) {
        val intent = Intent(activity, targetClass)
            .putExtra(Args.EXTRA, args)
            .also {
                if (intentFlags != null) {
                    it.addFlags(intentFlags)
                }
            }

        if (fragment != null) {
            fragment.startActivityForResult(intent, requestCode)
        } else {
            activity.startActivityForResult(intent, requestCode)
        }
    }

    interface Args : Parcelable {
        companion object {
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            @VisibleForTesting
            const val EXTRA: String = "extra_activity_args"
        }
    }

    interface Result : Parcelable {
        fun toBundle(): Bundle

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
        companion object {
            const val EXTRA: String = "extra_activity_result"
        }
    }
}
