package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test

class ConsentUiTest {

    private val format = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun `decodes without error`() {
        assertThat(
            format.decodeFromString<ConsentUi>(ConsentUiFixtures.HAS_CONSENT_PANE)
                .consentPane
        ).isNotNull()

        assertThat(
            format.decodeFromString<ConsentUi>(ConsentUiFixtures.HAS_CONSENT_SECTION)
                .consentSection
        ).isNotNull()
    }

    @Test
    fun `decodes Markdown to HTML`() {
        val consentPane =
            format.decodeFromString<ConsentUi>(ConsentUiFixtures.HAS_CONSENT_PANE)
                .consentPane

        assertThat(consentPane?.scopesSection?.scopes?.firstOrNull()?.description)
            .isEqualTo("Name, email, and <b>profile picture</b>")

        assertThat(consentPane?.disclaimer)
            .isEqualTo(
                "By allowing, you agree to <b>Example</b>'s <a href=\"https://www.stripe.com\">Terms of Service</a>."
            )

        val consentSection =
            format.decodeFromString<ConsentUi>(ConsentUiFixtures.HAS_CONSENT_SECTION)
                .consentSection

        assertThat(consentSection?.disclaimer)
            .isEqualTo(
                "By allowing, you agree to <b>Example</b>'s <a href=\"https://www.stripe.com\">Terms of Service</a>."
            )
    }
}
