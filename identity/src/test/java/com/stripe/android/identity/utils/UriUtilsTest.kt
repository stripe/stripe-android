package com.stripe.android.identity.utils

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UriUtilsTest {
    @Test
    fun testUrlWithoutQuery() {
        val uriBefore = Uri.parse("https://path/to/uri.png?query1=123&query2=456")
        assertThat(uriBefore.urlWithoutQuery()).isEqualTo("https://path/to/uri.png")
    }
}
