package com.stripe.android.connections.view

import android.content.Context
import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.browser.BrowserCapabilities
import com.stripe.android.core.browser.BrowserCapabilitiesSupplier
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
internal class BuildAuthFlowIntentTest {

    private val context = mock<Context>()
    private val browserCapabilitiesSupplier = mock<BrowserCapabilitiesSupplier>()
    private val buildAuthFlowIntent = BuildAuthFlowIntent(context, browserCapabilitiesSupplier)

    @Test
    fun `invoke - given chrome available, builds custom tab intent`() {
        // Given
        whenever(browserCapabilitiesSupplier.get()).thenReturn(BrowserCapabilities.CustomTabs)
        val url = "www.authflow.com"

        // When
        val intent = buildAuthFlowIntent(url)

        // Then
        assertThat(intent.data).isEqualTo(url)
        assertThat(intent.getIntExtra(CustomTabsIntent.EXTRA_SHARE_STATE, 0))
            .isEqualTo(CustomTabsIntent.SHARE_STATE_OFF)
    }

    @Test
    fun `invoke - given no chrome available, builds intent chooser intent`() {
        // Given
        whenever(browserCapabilitiesSupplier.get()).thenReturn(BrowserCapabilities.Unknown)
        val url = "www.authflow.com"

        // When
        val intent = buildAuthFlowIntent(url)

        // Then
        assertThat(intent.action).isEqualTo(Intent.ACTION_CHOOSER)
        val chooserIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertThat(chooserIntent.data.toString()).isEqualTo(url)
    }
}
