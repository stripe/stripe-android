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
}
