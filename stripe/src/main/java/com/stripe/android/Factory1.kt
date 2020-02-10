package com.stripe.android

internal interface Factory1<ArgType, ReturnType> {
    fun create(arg: ArgType): ReturnType
}
