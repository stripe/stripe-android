package com.stripe.android.link.express

import android.os.Parcelable
import com.stripe.android.link.LinkAccountUpdate
import kotlinx.parcelize.Parcelize

@Parcelize
internal sealed interface LinkExpressResult : Parcelable {
    @Parcelize
    data class Authenticated(
        val linkAccountUpdate: LinkAccountUpdate
    ) : LinkExpressResult

    @Parcelize
    data class Canceled(
        val linkAccountUpdate: LinkAccountUpdate
    ) : LinkExpressResult

    @Parcelize
    data class Failed(
        val error: Throwable,
        val linkAccountUpdate: LinkAccountUpdate
    ) : LinkExpressResult
}
