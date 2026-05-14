package com.stripe.android.testing

import android.os.Bundle
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuarantinedTestMatcherTest {

    @Test
    fun `match is true when description matches a quarantined class and method`() {
        val json = """[{"className":"com.example.Foo","testCaseName":"bar"}]"""
        val matcher = matcherForHexPayload(json)

        val description = Description.createTestDescription("com.example.Foo", "bar")
        assertThat(matcher.match(description)).isTrue()
    }

    @Test
    fun `match is false when class does not match`() {
        val json = """[{"className":"com.example.Foo","testCaseName":"bar"}]"""
        val matcher = matcherForHexPayload(json)

        val description = Description.createTestDescription("com.other.Foo", "bar")
        assertThat(matcher.match(description)).isFalse()
    }

    @Test
    fun `match is false when method does not match`() {
        val json = """[{"className":"com.example.Foo","testCaseName":"bar"}]"""
        val matcher = matcherForHexPayload(json)

        val description = Description.createTestDescription("com.example.Foo", "baz")
        assertThat(matcher.match(description)).isFalse()
    }

    @Test
    fun `match is false when bitriseQuarantinedTests is absent`() {
        val matcher = QuarantinedTestMatcher(Bundle())

        val description = Description.createTestDescription("com.example.Foo", "bar")
        assertThat(matcher.match(description)).isFalse()
    }

    @Test
    fun `match is false when bitriseQuarantinedTests is blank after trim`() {
        val bundle = Bundle().apply { putString("bitriseQuarantinedTests", "   \t  ") }
        val matcher = QuarantinedTestMatcher(bundle)

        val description = Description.createTestDescription("com.example.Foo", "bar")
        assertThat(matcher.match(description)).isFalse()
    }

    @Test
    fun `match is false when payload is not valid hex`() {
        val bundle = Bundle().apply {
            putString("bitriseQuarantinedTests", "not-hex-json")
        }
        val matcher = QuarantinedTestMatcher(bundle)

        val description = Description.createTestDescription("com.example.Foo", "bar")
        assertThat(matcher.match(description)).isFalse()
    }

    @Test
    fun `match is false when decoded json is not a valid array`() {
        val matcher = matcherForHexPayload("""{"className":"com.example.Foo","testCaseName":"bar"}""")

        val description = Description.createTestDescription("com.example.Foo", "bar")
        assertThat(matcher.match(description)).isFalse()
    }

    @Test
    fun `match is false when decoded json is invalid`() {
        val matcher = matcherForHexPayload("not json at all")

        val description = Description.createTestDescription("com.example.Foo", "bar")
        assertThat(matcher.match(description)).isFalse()
    }

    @Test
    fun `match respects multiple quarantined entries`() {
        val json =
            """
            [
              {"className":"com.a.First","testCaseName":"m1"},
              {"className":"com.b.Second","testCaseName":"m2"}
            ]
            """.trimIndent()
        val matcher = matcherForHexPayload(json)

        assertThat(matcher.match(Description.createTestDescription("com.a.First", "m1"))).isTrue()
        assertThat(matcher.match(Description.createTestDescription("com.b.Second", "m2"))).isTrue()
        assertThat(matcher.match(Description.createTestDescription("com.a.First", "m2"))).isFalse()
    }

    @Test
    fun `match ignores array entries with missing or empty className or testCaseName`() {
        val json =
            """
            [
              {"className":"","testCaseName":"x"},
              {"className":"com.ok.Cls","testCaseName":""},
              {"className":"com.ok.Cls","testCaseName":"good"}
            ]
            """.trimIndent()
        val matcher = matcherForHexPayload(json)

        assertThat(matcher.match(Description.createTestDescription("com.ok.Cls", "good"))).isTrue()
        assertThat(matcher.match(Description.createTestDescription("com.ok.Cls", "x"))).isFalse()
    }

    private fun matcherForHexPayload(json: String): QuarantinedTestMatcher {
        val hex = json.encodeToByteArray().toHexString()
        val bundle = Bundle().apply { putString("bitriseQuarantinedTests", hex) }
        return QuarantinedTestMatcher(bundle)
    }
}
