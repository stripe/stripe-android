package com.stripe.android.checkout

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.account.DefaultLinkStore
import com.stripe.android.paymentelement.CheckoutSessionPreview
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class CheckoutElementsControllerClearLocalCustomerDataTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val linkStore = DefaultLinkStore(application)

    @After
    fun tearDown() {
        linkStore.clear()
    }

    @Test
    fun `clearLocalCustomerData clears persisted Link state`() {
        linkStore.markLinkAsUsed()
        assertThat(DefaultLinkStore(application).hasUsedLink()).isTrue()

        CheckoutElementsController.clearLocalCustomerData(application)

        assertThat(DefaultLinkStore(application).hasUsedLink()).isFalse()
    }
}
