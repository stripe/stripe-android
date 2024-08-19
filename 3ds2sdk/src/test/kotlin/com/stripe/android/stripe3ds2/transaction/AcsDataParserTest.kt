package com.stripe.android.stripe3ds2.transaction

import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripe3ds2.observability.ErrorReporter
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import java.text.ParseException
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class AcsDataParserTest {
    private val errors = mutableListOf<Throwable>()
    private val errorReporter = ErrorReporter { errors.add(it) }
    private val parser = DefaultAcsDataParser(errorReporter)

    @Test
    fun `parse() with public keys represented as strings should succeed`() {
        val acsData = parser.parse(
            createAcsDataJson(
                acsUrl = ACS_URL,
                acsEphemPubKey = """
                {
                    "kty": "EC",
                    "crv": "P-256",
                    "x": "T38yNZkcevEW54qbml9GhXTzGDYvzXeFFWX0cg9bjjs",
                    "y": "8zAu1kg_-1QGcNa8snUd3yI1TLFSSK5lomfiZyHaAxk"
                }
                """.trimIndent(),
                sdkEphemPubKey = """
                {
                    "kty": "EC",
                    "crv": "P-256",
                    "x": "3EGy3U9YE_hj4AiRlD2JK4kX0X9NiOZYMvZBZQXyuEk",
                    "y": "LUu6NCxGdoO7pJeyhyDX_nuos6UPL5sjZRmzdlm3GgM"
                }
                """.trimIndent()
            )
        )

        assertThat(acsData.acsUrl)
            .isEqualTo(ACS_URL)
        assertThat(acsData.acsEphemPubKey)
            .isNotNull()
        assertThat(acsData.sdkEphemPubKey)
            .isNotNull()
    }

    @Test
    fun `parse() should fail with parse exception if keys are invalid format`() {
        assertFailsWith<ParseException> {
            parser.parse(
                createAcsDataJson(
                    acsUrl = ACS_URL,
                    acsEphemPubKey = "invalid key",
                    sdkEphemPubKey = "invalid key"
                )
            )
        }
    }

    @Suppress("ktlint:standard:max-line-length")
    @Test
    fun `parse() should encode invalid JSON in exception message`() {
        val error = assertFailsWith<ParseException> {
            parser.parse(
                createAcsDataJson(
                    acsUrl = ACS_URL,
                    acsEphemPubKey = JSONArray(),
                    sdkEphemPubKey = 5
                )
            )
        }

        assertIs<IllegalArgumentException>(errors.single()).also {
            assertThat(it.message).isEqualTo(
                "Failed to parse ACS data: {\"acsURL\":\"https://simulator-3ds.selftestplatform.com/v2.1.0/acs/946/\",\"acsEphemPubKey\":[],\"sdkEphemPubKey\":5}"
            )
        }

        assertThat(error.message)
            .isEqualTo("The key type to parse must not be null")
    }

    @Test
    fun `parse() with public keys represented as JSON objects should succeed`() {
        val acsData = parser.parse(AcsDataFixtures.DEFAULT_JSON)

        assertThat(
            acsData.acsUrl
        ).isEqualTo(
            "https://testmode-acs.stripe.com/3d_secure_2_test/acct_123/threeds2_1HPOihJnhvQu/app_challenge/GXnh5fGIdB_oL4EeEc1BBrOYk="
        )
        assertThat(
            acsData.acsEphemPubKey.algorithm
        ).isEqualTo("EC")
        assertThat(
            acsData.sdkEphemPubKey.algorithm
        ).isEqualTo("EC")
    }

    private fun createAcsDataJson(
        acsUrl: Any? = null,
        acsEphemPubKey: Any? = null,
        sdkEphemPubKey: Any? = null
    ) = JSONObject()
        .put(DefaultAcsDataParser.FIELD_ACS_URL, acsUrl)
        .put(DefaultAcsDataParser.FIELD_ACS_EPHEM_PUB_KEY, acsEphemPubKey)
        .put(DefaultAcsDataParser.FIELD_SDK_EPHEM_PUB_KEY, sdkEphemPubKey)

    private companion object {
        private const val ACS_URL = "https://simulator-3ds.selftestplatform.com/v2.1.0/acs/946/"
    }
}
