package com.stripe.android.link

import android.app.Activity
import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class LinkActivityResult(
    val resultCode: Int
) : Parcelable {
    /**
     * Indicates that the flow was completed successfully and the Stripe Intent was confirmed.
     */
    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    object Completed : LinkActivityResult(Activity.RESULT_OK)

    /**
     * The user cancelled the Link flow without completing it.
     */
    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Canceled(
        val reason: Reason
    ) : LinkActivityResult(Activity.RESULT_CANCELED) {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        enum class Reason {
            BackPressed,
            LoggedOut
        }
    }

    /**
     * Something went wrong. See [error] for more information.
     */
    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Failed(
        val error: Throwable
    ) : LinkActivityResult(Activity.RESULT_CANCELED)
}
