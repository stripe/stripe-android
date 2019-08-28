package com.stripe.android

internal interface Factory<ArgType, ReturnType> {
    fun create(arg: ArgType): ReturnType
}
