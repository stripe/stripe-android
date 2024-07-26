package com.stripe.android.utils

class FakeResultHandler<T> {
    private var callback: ((T) -> Unit)? = null

    fun register(callback: (T) -> Unit) {
        this.callback = callback
    }

    fun onResult(result: T) {
        callback?.invoke(result)
    }
}
