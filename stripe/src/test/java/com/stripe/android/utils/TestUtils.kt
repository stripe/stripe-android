package com.stripe.android.utils

import android.os.Looper
import org.robolectric.Shadows

object TestUtils {
    @JvmStatic
    fun idleLooper() = Shadows.shadowOf(Looper.getMainLooper()).idle()
}
