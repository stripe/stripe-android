package com.stripe.android

import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class FileFactoryTest {
    @Test
    fun testCreate() {
        val file = FileFactory(ApplicationProvider.getApplicationContext()).create()
        assertTrue(file.extension == "png")
    }
}
