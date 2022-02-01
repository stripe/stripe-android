package com.stripe.android.stripe3ds2.init

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// NOTE: Copied from reference app spec

/**
 * The Warning class shall represent a warning that is produced by the 3DS SDK while performing
 * security checks during initialization.
 */
@Parcelize
data class Warning(

    /**
     * The warning ID.
     */
    val id: String,

    /**
     * The warning message.
     */
    val message: String,

    /**
     * The severity level of the warning.
     */
    val severity: Severity
) : Parcelable {
    enum class Severity {
        LOW,
        MEDIUM,
        HIGH
    }
}
