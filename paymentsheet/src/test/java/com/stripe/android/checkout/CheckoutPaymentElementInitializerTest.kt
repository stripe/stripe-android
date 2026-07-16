package com.stripe.android.checkout

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.embedded.FakeEmbeddedSheetLauncher
import com.stripe.android.paymentelement.embedded.content.FakeEmbeddedContentHelper
import com.stripe.android.testing.CoroutineTestRule
import org.junit.Rule
import kotlin.test.Test

internal class CheckoutPaymentElementInitializerTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `initialize sets the sheet launcher and clears it when the lifecycle is destroyed`() {
        val contentHelper = FakeEmbeddedContentHelper()
        val lifecycleOwner = TestLifecycleOwner()
        val initializer = CheckoutPaymentElementInitializer(
            sheetLauncher = FakeEmbeddedSheetLauncher(),
            contentHelper = contentHelper,
            lifecycleOwner = lifecycleOwner,
        )

        assertThat(contentHelper.testSheetLauncher).isNull()

        initializer.initialize()
        assertThat(contentHelper.testSheetLauncher).isNotNull()

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        assertThat(contentHelper.testSheetLauncher).isNull()
    }
}
