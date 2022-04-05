package com.stripe.android.core.exception

internal class InvalidSerializationException(type: String) :
    Exception("Serialization result $type is not supported")
