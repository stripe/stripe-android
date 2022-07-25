package com.stripe.android.link

import android.app.Activity
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class LinkActivityResult(
    val resultCode: Int
) : Parcelable {
    /**
     * Indicates that the flow was completed successfully and the Stripe Intent was confirmed.
     */
    @Parcelize
    object Completed : LinkActivityResult(Activity.RESULT_OK)

    /**
     * The user cancelled the Link flow without completing it.
     */
    @Parcelize
    object Canceled : LinkActivityResult(Activity.RESULT_CANCELED)

    /**
     * Something went wrong. See [error] for more information.
     */
    @Parcelize
    data class Failed(
        val error: Throwable
    ) : LinkActivityResult(Activity.RESULT_CANCELED)
}
