package com.stripe.android.connect.webview

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InlineContractTest {

    private val componentUrl = Uri.parse("$ROOT/v1.0/android_webview.html#component=notification-banner")

    @Test
    fun `accepts the root accessory layer page on the Connect js origin`() {
        assertThat(sheetId("$ROOT/accessory_layer_0a1b2c.html#layer_type=overlay&os_sheet=true&os_sheet_id=a1-b2"))
            .isEqualTo("a1-b2")
    }

    @Test
    fun `rejects another origin`() {
        assertThat(sheetId("https://connect-js.stripe.com.evil.example/accessory_layer_0a1b2c.html$SHEET")).isNull()
    }

    @Test
    fun `rejects a page that isn't an accessory layer`() {
        assertThat(sheetId("$ROOT/v1.0/android_webview.html$SHEET")).isNull()
    }

    @Test
    fun `clamps heights to 0 through 10000`() {
        assertThat(InlineContract.clampHeight(-10.0)).isEqualTo(0f)
        assertThat(InlineContract.clampHeight(25_000.0)).isEqualTo(10_000f)
        assertThat(InlineContract.clampHeight(Double.POSITIVE_INFINITY)).isNull()
    }

    private fun sheetId(url: String): String? {
        return InlineContract.sheetIdFromWindowUrl(Uri.parse(url), componentUrl)
    }

    private companion object {
        const val ROOT = "https://connect-js.stripe.com"
        const val SHEET = "#os_sheet=true&os_sheet_id=a1"
    }
}
