package com.stripe.android.link

import android.app.Activity
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Deprecated(
    message = "This isn't meant for public usage and will be removed in a future release.",
)
sealed class LinkActivityResult(
    val resultCode: Int
) : Parcelable {

    /**
     * Indicates that the flow was completed successfully and the Stripe Intent was confirmed.
     */
    @Deprecated(
        message = "This isn't meant for public usage and will be removed in a future release.",
    )
    @Parcelize
    object Completed : LinkActivityResult(Activity.RESULT_OK)

    /**
     * The user cancelled the Link flow without completing it.
     */
    @Deprecated(
        message = "This isn't meant for public usage and will be removed in a future release.",
    )
    @Parcelize
    data class Canceled(
        val reason: Reason
    ) : LinkActivityResult(Activity.RESULT_CANCELED) {
        enum class Reason {
            BackPressed,
            PayAnotherWay,
            LoggedOut
        }
    }

    /**
     * Something went wrong. See [error] for more information.
     */
    @Deprecated(
        message = "This isn't meant for public usage and will be removed in a future release.",
    )
    @Parcelize
    data class Failed(
        val error: Throwable
    ) : LinkActivityResult(Activity.RESULT_CANCELED)
}
