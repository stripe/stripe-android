package com.stripe.android.model

import androidx.annotation.StringDef
import kotlinx.android.parcel.Parcelize

/**
 * Model for a [redirect](https://stripe.com/docs/api/sources/object#source_object-redirect) object
 * in the Sources API.
 */
@Parcelize
data class SourceRedirect internal constructor(
    val returnUrl: String?,
    @param:Status @field:Status @get:Status val status: String?,
    val url: String?
) : StripeModel {
    @Retention(AnnotationRetention.SOURCE)
    @StringDef(Status.PENDING, Status.SUCCEEDED, Status.FAILED, Status.NOT_REQUIRED)
    internal annotation class Status {
        companion object {
            const val PENDING = "pending"
            const val SUCCEEDED = "succeeded"
            const val FAILED = "failed"
            const val NOT_REQUIRED = "not_required"
        }
    }
}
