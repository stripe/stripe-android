package com.stripe.android.connect

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NotificationBannerTest {

    @Test
    fun `the notification banner APIs require the restricted private preview opt-in`() {
        assertThat(classFileText("PrivatePreviewConnectSDK")).contains("Landroidx/annotation/RestrictTo;")
        assertThat(classFileText("EmbeddedComponentManager")).contains(PREVIEW_ANNOTATION)
        assertThat(classFileText("NotificationBannerProps")).contains(PREVIEW_ANNOTATION)
        assertThat(classFileText("NotificationBannerListener")).contains(PREVIEW_ANNOTATION)
    }

    // The annotation has binary retention, so it's only visible in the class files.
    private fun classFileText(className: String): String {
        val path = "/com/stripe/android/connect/$className.class"
        val bytes = checkNotNull(javaClass.getResourceAsStream(path)).use { it.readBytes() }
        return String(bytes, Charsets.ISO_8859_1)
    }

    private companion object {
        const val PREVIEW_ANNOTATION = "Lcom/stripe/android/connect/PrivatePreviewConnectSDK;"
    }
}
