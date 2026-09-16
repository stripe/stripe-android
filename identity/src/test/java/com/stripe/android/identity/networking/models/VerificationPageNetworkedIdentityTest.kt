package com.stripe.android.identity.networking.models

import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import com.stripe.android.identity.json
import com.stripe.android.identity.networking.VERIFICATION_PAGE_NOT_REQUIRE_LIVE_CAPTURE_JSON_STRING
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertFailsWith

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
    fun `malformed boolean or state is rejected rather than guessing consent`(
        @TestParameter(
            value = [
                """{"state":"invalid"}""",
                """{"state":{"consented":"false","skipped":false}}""",
                """{"state":{"consented":false,"skipped":"false"}}""",
                """{"state":{"consented":0,"skipped":false}}""",
                """{"reuse_available":"true"}""",
                """{"save_available":1}""",
            ],
        ) configuration: String,
    ) {
        assertFailsWith<SerializationException> { pageWithNetworkedIdentity(configuration) }
    }

    @Test
    fun `bootstrap survives requirements updates and redacts contact information and merchant key`() {
        val page = pageWithNetworkedIdentity(
            """{
                "save_available":true,"reuse_available":true,"email":"consumer@example.com",
                "phone_number":"+12025550100","state":{"consented":false,"skipped":false}
            }""",
            extra = """{"merchant_publishable_key":"pk_test_merchant"}""",
        )
        val updated = page.copy(requirements = VerificationPageRequirements(missing = emptyList()))

        assertThat(page.networkedIdentity?.email).isEqualTo("consumer@example.com")
        assertThat(page.networkedIdentity?.phoneNumber).isEqualTo("+12025550100")
        assertThat(page.merchantPublishableKey).isEqualTo("pk_test_merchant")
        assertThat(updated.networkedIdentity).isEqualTo(page.networkedIdentity)
        assertThat(updated.merchantPublishableKey).isEqualTo(page.merchantPublishableKey)
        assertThat(updated.networkedIdentityRoute).isEqualTo(NetworkedIdentityRoute.Reuse)
        assertThat(updated.requirements.missing).isEmpty()
        val diagnostic = "$page ${page.networkedIdentity}"
        assertThat(diagnostic).doesNotContain("consumer@example.com")
        assertThat(diagnostic).doesNotContain("+12025550100")
        assertThat(diagnostic).doesNotContain("pk_test_merchant")
    }

    @Test
    fun `contact details stay absent without falling back to provided details`(
        @TestParameter(value = ["{}", """{"email":null,"phone_number":null}"""]) configuration: String,
    ) {
        val page = pageWithNetworkedIdentity(
            configuration,
            extra = """{"provided_details":{"email":"legacy@example.com"}}""",
        )

        assertThat(page.networkedIdentity?.email).isNull()
        assertThat(page.networkedIdentity?.phoneNumber).isNull()
        assertThat(page.providedDetails?.email).isEqualTo("legacy@example.com")
    }

    @Test
    fun `optional bootstrap fields decode independently of legacy eligibility and merchant key`() {
        val page = pageWithNetworkedIdentity(
            """{"reuse_available":true,"state":{"consented":false,"skipped":false}}"""
        )

        assertThat(page.networkedIdentity?.saveAvailable).isNull()
        assertThat(page.networkedIdentity?.state?.direction).isNull()
        assertThat(page.networkingData).isNull()
        assertThat(page.merchantPublishableKey).isNull()
        assertThat(page.networkedIdentityRoute).isEqualTo(NetworkedIdentityRoute.Reuse)
    }

    private fun pageWithNetworkedIdentity(configuration: String, extra: String = "{}"): VerificationPage {
        val original = json.parseToJsonElement(VERIFICATION_PAGE_NOT_REQUIRE_LIVE_CAPTURE_JSON_STRING).jsonObject
        return json.decodeFromString(
            VerificationPageSerializer,
            JsonObject(
                original + json.parseToJsonElement(extra).jsonObject +
                    ("networked_identity" to json.parseToJsonElement(configuration))
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
        ordinary("MissingState", """{"reuse_available":true}"""),
        ordinary("NullState", """{"reuse_available":true,"state":null}"""),
        ordinary("EmptyState", """{"reuse_available":true,"state":{}}"""),
        ordinary("MissingConsent", """{"reuse_available":true,"state":{"skipped":false}}"""),
        ordinary("MissingSkipped", """{"reuse_available":true,"state":{"consented":false}}"""),
        ordinary("NullConsent", """{"reuse_available":true,"state":{"consented":null,"skipped":false}}"""),
        ordinary("NullSkipped", """{"reuse_available":true,"state":{"consented":false,"skipped":null}}"""),
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
        ordinary(
            "MalformedDirection",
            """{"reuse_available":true,"state":{"consented":true,"skipped":false,"direction":{}}}""",
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
            "reuse_available":$reuse,"save_available":$save,
            "state":{"consented":$consented,"skipped":$skipped,"direction":${direction?.let { "\"$it\"" }}}
        }""",
        expected = expected,
    )
}
