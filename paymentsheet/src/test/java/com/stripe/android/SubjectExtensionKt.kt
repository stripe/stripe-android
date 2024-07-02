package com.stripe.android

import com.google.common.truth.Subject

inline fun <reified T> Subject?.isInstanceOf() {
    this?.isNotNull()
    this!!.isInstanceOf(T::class.java)
}
