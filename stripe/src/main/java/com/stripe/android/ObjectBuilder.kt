package com.stripe.android

internal interface ObjectBuilder<ObjectType> {
    fun build(): ObjectType
}
