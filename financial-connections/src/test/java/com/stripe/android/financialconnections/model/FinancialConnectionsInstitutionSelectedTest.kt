package com.stripe.android.financialconnections.model

import Alignment
import FinancialConnectionsGenericInfoScreen.Body
import com.google.common.truth.Truth.assertThat
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.test.readResourceAsString
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertIs

internal class FinancialConnectionsInstitutionSelectedTest {

    private val json = Json {
        coerceInputValues = true
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Test
    fun `streamlined consent response decodes`() {
        val response = decode("json/institution_selected_streamlined_consent.json")

        assertThat(response.manifest.nextPane).isEqualTo(Pane.STREAMLINED_CONSENT)
        assertThat(response.text?.idConsentContentPane).isNull()

        val streamlinedConsentPane = requireNotNull(response.text?.streamlinedConsentPane)
        val screen = streamlinedConsentPane.screen
        assertThat(screen.id).isEqualTo("streamlined_consent")
        assertThat(screen.header?.alignment).isEqualTo(Alignment.Center)

        val entries = requireNotNull(screen.body?.entries)
        assertThat(entries).hasSize(1)
        val bullets = assertIs<Body.Entry.Bullets>(entries.single()).bullets
        assertThat(bullets).hasSize(2)
        assertThat(bullets.all { it.icon != null }).isTrue()

        val footer = requireNotNull(screen.footer)
        assertThat(footer.primaryCta?.label).isEqualTo("Agree and continue")
        assertThat(footer.disclaimer).contains("stripe://legal-details-notice")
        assertThat(footer.belowCta).contains("<a href=\"stripe://manual-entry")

        assertThat(streamlinedConsentPane.dataAccessNotice?.title).isEqualTo("Data sharing")
        assertThat(streamlinedConsentPane.legalDetailsNotice.title).isEqualTo("Terms and Privacy Policy")
        assertThat(streamlinedConsentPane.moreInfoNotice?.header?.title).isEqualTo("About Stripe")
        assertThat(streamlinedConsentPane.moreInfoNotice?.footer?.primaryCta?.label).isEqualTo("OK")
    }

    @Test
    fun `ID consent content response still decodes`() {
        val response = decode("json/institution_selected_id_consent_content.json")

        assertThat(response.manifest.nextPane).isEqualTo(Pane.ID_CONSENT_CONTENT)
        assertThat(response.text?.idConsentContentPane).isNotNull()
        assertThat(response.text?.streamlinedConsentPane).isNull()
    }

    private fun decode(resourcePath: String): FinancialConnectionsInstitutionSelected {
        return json.decodeFromString(
            FinancialConnectionsInstitutionSelected.serializer(),
            readResourceAsString(resourcePath),
        )
    }
}
