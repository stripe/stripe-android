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

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    interface Args : Parcelable {

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        companion object {
            @VisibleForTesting
            const val EXTRA: String = "extra_activity_args"
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    interface Result : Parcelable {
        fun toBundle(): Bundle

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        companion object {
            const val EXTRA: String = "extra_activity_result"
        }
    }
}
