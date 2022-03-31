package com.stripe.android.link

import android.app.Activity
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class LinkActivityResult(
    val resultCode: Int
) : Parcelable {

    @Parcelize
    object Success : LinkActivityResult(Activity.RESULT_OK)

    @Parcelize
    object Canceled : LinkActivityResult(Activity.RESULT_CANCELED)

    @Parcelize
    data class Failed(
        val error: Throwable
    ) : LinkActivityResult(Activity.RESULT_CANCELED)
}
