package com.stripe.android.link.express

import android.os.Parcelable
import com.stripe.android.link.model.LinkAccount
import kotlinx.parcelize.Parcelize

@Parcelize
internal sealed interface LinkExpressResult: Parcelable {
    @Parcelize
    data class Authenticated(val linkAccount: LinkAccount): LinkExpressResult

    @Parcelize
    data object Canceled : LinkExpressResult

    @Parcelize
    data class Failed(val error: Throwable) : LinkExpressResult
}
