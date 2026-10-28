package com.stripe.android.identity.networking.models

import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import com.stripe.android.identity.json
import com.stripe.android.identity.networked.verificationIfCommitted
import com.stripe.android.identity.networking.VERIFICATION_PAGE_NOT_REQUIRE_LIVE_CAPTURE_JSON_STRING
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
internal class VerificationPageNetworkedIdentityTest {
    @Test
    fun `routes conservatively from explicit v8 consent state`(
        @TestParameter(valuesProvider = NetworkedIdentityRouteCases::class) case: NetworkedIdentityRouteCase,
    ) {
        val page = pageWithNetworkedIdentity(case.configuration)

        assertThat(page.networkedIdentityRoute).isEqualTo(case.expected)
    }

    @Test
    fun `bootstrap fields decode and contact details stay out of diagnostics`() {
        val page = pageWithNetworkedIdentity(
            """{
                "save_enabled":true,"reuse_enabled":true,"reuse_available":true,"lookup_email":"consumer@example.com",
                "phone_number_autofill_hint":"+12025550100","state":{"consented":false,"skipped":false}
            }"""
        )

        assertThat(page.networkedIdentity?.saveEnabled).isTrue()
        assertThat(page.networkedIdentity?.reuseEnabled).isTrue()
        assertThat(page.networkedIdentity?.reuseAvailable).isTrue()
        assertThat(page.networkedIdentity?.lookupEmail).isEqualTo("consumer@example.com")
        assertThat(page.networkedIdentity?.phoneNumberAutofillHint).isEqualTo("+12025550100")
        assertThat(page.toString()).doesNotContain("consumer@example.com")
        assertThat(page.toString()).doesNotContain("+12025550100")
    }

    @Test
    fun `reuse is committed once the shared document satisfies the requirement`() {
        val page = consentedPage("consumer_to_merchant", missing = listOf(Requirement.BIOMETRICCONSENT))

        val committed = page.verificationIfCommitted(NetworkedIdentityRoute.ResumeReuse)

        assertThat(committed?.id).isEqualTo(page.id)
        assertThat(committed?.requirements?.missings).containsExactly(Requirement.BIOMETRICCONSENT)
        assertThat(committed?.requirements?.errors).isEmpty()
    }

    @Test
    fun `reuse is not committed while the document is still missing`() {
        val page = consentedPage("consumer_to_merchant", missing = listOf(Requirement.IDDOCUMENTFRONT))

        assertThat(page.verificationIfCommitted(NetworkedIdentityRoute.ResumeReuse)).isNull()
    }

    @Test
    fun `save is committed once the session records the save consent`() {
        val page = consentedPage("merchant_to_consumer", missing = listOf(Requirement.IDDOCUMENTFRONT))

        assertThat(page.verificationIfCommitted(NetworkedIdentityRoute.ResumeSave)?.id).isEqualTo(page.id)
    }

    @Test
    fun `the other direction is not committed`() {
        val page = consentedPage("merchant_to_consumer", missing = emptyList())

        assertThat(page.verificationIfCommitted(NetworkedIdentityRoute.ResumeReuse)).isNull()
    }

    private fun consentedPage(direction: String, missing: List<Requirement>) = pageWithNetworkedIdentity(
        """{
            "reuse_enabled":true,"save_enabled":true,
            "state":{"consented":true,"skipped":false,"direction":"$direction"}
        }"""
    ).copy(requirements = VerificationPageRequirements(missing = missing))

    private fun pageWithNetworkedIdentity(configuration: String): VerificationPage {
        val original = json.parseToJsonElement(VERIFICATION_PAGE_NOT_REQUIRE_LIVE_CAPTURE_JSON_STRING).jsonObject
        return json.decodeFromString(
            VerificationPage.serializer(),
            JsonObject(
                original + ("networked_identity" to json.parseToJsonElement(configuration))
            ).toString()
        )
    }
}

internal data class NetworkedIdentityRouteCase(
    val name: String,
    val configuration: String,
    val expected: NetworkedIdentityRoute,
) {
    override fun toString(): String = name
}

internal object NetworkedIdentityRouteCases : TestParameterValuesProvider() {
    override fun provideValues(context: Context?): List<NetworkedIdentityRouteCase> = listOf(
        ordinary("Null", "null"),
        // A found document only adds emphasis; reuse has to be enabled.
        ordinary("ReuseAvailableOnly", """{"reuse_available":true,"state":{"consented":false,"skipped":false}}"""),
        ordinary("MissingState", """{"reuse_enabled":true}"""),
        ordinary("NullState", """{"reuse_enabled":true,"state":null}"""),
        ordinary("EmptyState", """{"reuse_enabled":true,"state":{}}"""),
        ordinary("MissingConsent", """{"reuse_enabled":true,"state":{"skipped":false}}"""),
        ordinary("MissingSkipped", """{"reuse_enabled":true,"state":{"consented":false}}"""),
        ordinary("NullConsent", """{"reuse_enabled":true,"state":{"consented":null,"skipped":false}}"""),
        ordinary("NullSkipped", """{"reuse_enabled":true,"state":{"consented":false,"skipped":null}}"""),
        state("ReusePriority", false, false, null, true, true, NetworkedIdentityRoute.Reuse),
        state("ReuseWithoutSave", false, false, null, true, false, NetworkedIdentityRoute.Reuse),
        state("Save", false, false, null, false, true, NetworkedIdentityRoute.Save),
        state("NoAvailability", false, false, null, false, false, NetworkedIdentityRoute.OrdinaryIdentity),
        state("Skipped", false, true, null, true, true, NetworkedIdentityRoute.OrdinaryIdentity),
        state("SkippedReuse", true, true, "consumer_to_merchant", true, true, NetworkedIdentityRoute.OrdinaryIdentity),
        state("SkippedSave", true, true, "merchant_to_consumer", true, true, NetworkedIdentityRoute.OrdinaryIdentity),
        state("ResumeReuse", true, false, "consumer_to_merchant", false, false, NetworkedIdentityRoute.ResumeReuse),
        state("ResumeSave", true, false, "merchant_to_consumer", false, false, NetworkedIdentityRoute.ResumeSave),
        state("MissingDirection", true, false, null, true, true, NetworkedIdentityRoute.OrdinaryIdentity),
        state("UnknownDirection", true, false, "future_direction", true, true, NetworkedIdentityRoute.OrdinaryIdentity),
        state(
            "UnconsentedReuse", false, false, "consumer_to_merchant", true, true,
            NetworkedIdentityRoute.OrdinaryIdentity
        ),
        state(
            "UnconsentedSave", false, false, "merchant_to_consumer", true, true, NetworkedIdentityRoute.OrdinaryIdentity
        ),
    )

    private fun ordinary(name: String, configuration: String) = NetworkedIdentityRouteCase(
        name, configuration, NetworkedIdentityRoute.OrdinaryIdentity
    )

    @Suppress("LongParameterList")
    private fun state(
        name: String,
        consented: Boolean,
        skipped: Boolean,
        direction: String?,
        reuse: Boolean,
        save: Boolean,
        expected: NetworkedIdentityRoute,
    ) = NetworkedIdentityRouteCase(
        name = name,
        configuration = """{
            "reuse_enabled":$reuse,"save_enabled":$save,
            "state":{"consented":$consented,"skipped":$skipped,"direction":${direction?.let { "\"$it\"" }}}
        }""",
        expected = expected,
    )
}
