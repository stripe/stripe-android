package com.stripe.android.stripe3ds2.views

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class ChallengeZoneWebViewTest {
    private val webView = ChallengeZoneWebView(ApplicationProvider.getApplicationContext())

    @Test
    fun transformHtml_formMethodAndActionUrlUpdated() {
        assertThat(webView.transformHtml(HTML_INVALID))
            .isEqualTo(HTML_VALID)
    }

    companion object {
        private const val HTML_INVALID =
            """
                <html>
                    <head></head>
                    <body>
                        <form action="http://tds2.0" method="POST"></form>
                        <a href="http://tds2.0/?RESEND=Y">Resend SMS</a>
                    </body>
                </html>
            """

        private const val HTML_VALID =
            """
                <html>
                    <head></head>
                    <body>
                        <form action="https://emv3ds/challenge" method="get"></form>
                        <a href="https://emv3ds/challenge/?RESEND=Y">Resend SMS</a>
                    </body>
                </html>
            """
    }
}
