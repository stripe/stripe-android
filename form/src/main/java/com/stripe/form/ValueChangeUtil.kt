package com.stripe.form

fun <T> Map<Any, ValueChange<*>?>.find(key: Key<T>): ValueChange<T>? {
    return get(key) as? ValueChange<T>?
}