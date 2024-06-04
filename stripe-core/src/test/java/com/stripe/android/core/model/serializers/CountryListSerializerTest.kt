package com.stripe.android.core.model.serializers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.model.Country
import com.stripe.android.core.model.CountryCode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Test

class CountryListSerializerTest {

    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Test
    fun testDeserialize() {
        assertThat(
            json.decodeFromString(
                CountryListSerializer,
                TEST_COUNTRIES_STRING
            )
        ).containsExactly(US, SG, BR)
    }

    @Test
    fun testSerialize() {
        assertThat(
            json.encodeToJsonElement(CountryListSerializer, listOf(US, SG, BR)).toString()
        ).isEqualTo(
            TEST_COUNTRIES_STRING
        )
    }

    private companion object {
        private val US = Country(CountryCode("US"), "United States")
        private val SG = Country(CountryCode("SG"), "Singapore")
        private val BR = Country(CountryCode("BR"), "Brazil")

        private val TEST_COUNTRIES_STRING = """
            {"US":"United States","SG":"Singapore","BR":"Brazil"}
        """.trimIndent()
    }
}

@Serializable
data class CountryListTest(
    @Serializable(with = CountryListSerializer::class)
    val countries: List<Country>
)
