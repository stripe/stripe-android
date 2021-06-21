package com.stripe.android.view

internal fun interface AuthActivityStarter<ArgsType> {
    fun start(args: ArgsType)
}
