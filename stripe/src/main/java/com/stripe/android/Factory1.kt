package com.stripe.android

internal fun interface Factory1<ArgType, ReturnType> {
    fun create(arg: ArgType): ReturnType
}
