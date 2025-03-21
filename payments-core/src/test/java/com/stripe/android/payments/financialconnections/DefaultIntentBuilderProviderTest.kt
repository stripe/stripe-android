package com.stripe.android.payments.financialconnections

import android.content.Context
import junit.framework.TestCase.assertTrue
import org.mockito.Mockito.mock
import kotlin.test.Test

class DefaultIntentBuilderProviderTest {

    private val mockContext: Context = mock()

    @Test
    fun `provide returns Full intent builder when module is available`() {
        val provider = DefaultIntentBuilderProvider()
        val intentBuilder = provider.provide(mockContext, isFullSdkAvailable = true)

        assertTrue(intentBuilder is IntentBuilderProvider.IntentBuilder.Full)
    }

    @Test
    fun `provide returns Lite intent builder when module is not available`() {
        val provider = DefaultIntentBuilderProvider()
        val intentBuilder = provider.provide(mockContext, isFullSdkAvailable = false)

        assertTrue(intentBuilder is IntentBuilderProvider.IntentBuilder.Lite)
    }
}
