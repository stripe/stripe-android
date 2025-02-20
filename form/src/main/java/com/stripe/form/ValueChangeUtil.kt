package com.stripe.form

fun <T> Map<Key<*>, ValueChange<*>?>.find(key: Key<T>): ValueChange<T>? {
    return get(key) as? ValueChange<T>?
}