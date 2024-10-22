package com.stripe.android.view

import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting

/**
 * Superclass for starting Stripe activities.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ActivityStarter private constructor() {
    interface Args : Parcelable {
        companion object {
            @VisibleForTesting
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
