package com.stripe.android.stripe3ds2.utils

import java.util.Objects

internal object ObjectUtils {
    @JvmStatic
    fun equals(obj1: Any?, obj2: Any?): Boolean {
        return obj1 == obj2
    }

    @JvmStatic
    fun hash(vararg values: Any): Int {
        return Objects.hash(*values)
    }
}
