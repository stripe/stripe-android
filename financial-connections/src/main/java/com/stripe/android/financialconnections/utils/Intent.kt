package com.stripe.android.financialconnections.utils

import android.content.Intent
import android.os.Parcelable

// The new Intent.getParcelableExtra(String,Class) throws an NPE internally
// see https://issuetracker.google.com/issues/240585930#comment6
internal inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? {
    @Suppress("DEPRECATION")
    return getParcelableExtra(key) as? T
}
