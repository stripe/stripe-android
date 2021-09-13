package com.stripe.android.paymentsheet.elements

open class SingletonHolder<out T, in A>(private val constructor: (A) -> T) {

    @Volatile
    private var instance: T? = null

    fun getInstance(arg: A): T {
        return when {
            instance != null -> instance!!
            else -> synchronized(this) {
                if (instance == null) {
                    instance = constructor(arg)
                }
                instance!!
            }
        }
    }
}
