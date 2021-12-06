package com.stripe.android.stripecardscan.framework.exception

class InvalidSerializationException(type: String) :
    Exception("Serialization result $type is not supported")