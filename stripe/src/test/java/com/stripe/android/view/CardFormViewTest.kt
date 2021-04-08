package com.stripe.android.view

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class CardFormViewTest {
    private val cardFormView = CardFormView(ApplicationProvider.getApplicationContext())

    @Test
    fun testToy() {
    }


}