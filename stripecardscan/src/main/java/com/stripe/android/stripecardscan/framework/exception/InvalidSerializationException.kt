package com.stripe.android.stripecardscan.framework.exception

internal class InvalidSerializationException(type: String) :
    Exception("Serialization result $type is not supported")
